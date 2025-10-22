package com.example.strategy;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.ta4j.core.*;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator; // ADDED
import org.ta4j.core.indicators.helpers.VolumeIndicator; // ADDED
// THIS IS THE CORRECT IMPORT PATH
// import org.ta4j.core.indicators.averages.VWMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num; // ADDED

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class StrategyProcessor {
    private static final int BB_PERIOD = 10;
    private static final double BB_STD_DEV = 2.5;
    private static final int RSI_PERIOD = 5;
    private static final int VWMA_PERIOD = 50;
    private static final int TRADING_DAYS_PER_YEAR = 252;


    static class StockData {
        String date;
        String ticker;
        double open, high, low, close, volume, nextDayReturn;

        public StockData(CSVRecord record) {
            this.date = record.get("Date");
            this.ticker = record.get("Ticker");
            this.open = Double.parseDouble(record.get("Open"));
            this.high = Double.parseDouble(record.get("High"));
            this.low = Double.parseDouble(record.get("Low"));
            this.close = Double.parseDouble(record.get("Close"));
            this.volume = Double.parseDouble(record.get("Volume"));
            this.nextDayReturn = Double.parseDouble(record.get("Next_Day_Return"));
        }
    }

    static class IndicatorValues {
        double rsi, bbUpper, bbLower, bbMid;

        public IndicatorValues(double rsi, double bbUpper, double bbLower, double bbMid) {
            this.rsi = rsi;
            this.bbUpper = bbUpper;
            this.bbLower = bbLower;
            this.bbMid = bbMid;
        }
    }

    public static void main(String[] args) {
        String inputFile = "data_for_java.csv";
        String outputFile = "portfolio_returns.csv";

        try {
            List<StockData> allData = readCsvData(inputFile);
            Map<String, List<StockData>> dataByTicker = groupDataByTicker(allData);
            System.out.println("Calculating indicators with ta4j...");
            Map<String, Map<String, IndicatorValues>> indicatorCache = calculateAllIndicators(dataByTicker);
            Map<String, List<StockData>> dataByDate = groupDataByDate(allData);
            System.out.println("Running portfolio backtest...");
            List<Double> portfolioDailyReturns = runBacktest(dataByDate, indicatorCache, outputFile);
            System.out.println("\n--- Strategy Performance (Calculated in Java) ---");
            calculateAndPrintPerformance(portfolioDailyReturns);

            System.out.println("\nJava strategy processing complete. Output: " + outputFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<StockData> readCsvData(String inputFile) throws IOException {
        List<StockData> allData = new ArrayList<>();
        try (Reader in = new FileReader(inputFile);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(in)) {
            for (CSVRecord record : parser) {
                try {
                    allData.add(new StockData(record));
                } catch (NumberFormatException e) {
                    System.err.println("Skipping bad row: " + record);
                }
            }
        }
        return allData;
    }

    private static Map<String, List<StockData>> groupDataByTicker(List<StockData> allData) {
        Map<String, List<StockData>> dataByTicker = new HashMap<>();
        for (StockData data : allData) {
            dataByTicker.computeIfAbsent(data.ticker, k -> new ArrayList<>()).add(data);
        }
        for (List<StockData> list : dataByTicker.values()) {
            list.sort(Comparator.comparing(data -> data.date));
        }
        return dataByTicker;
    }

    private static Map<String, List<StockData>> groupDataByDate(List<StockData> allData) {
        Map<String, List<StockData>> dataByDate = new TreeMap<>();
        for (StockData data : allData) {
            dataByDate.computeIfAbsent(data.date, k -> new ArrayList<>()).add(data);
        }
        return dataByDate;
    }
    private static Map<String, Map<String, IndicatorValues>> calculateAllIndicators(Map<String, List<StockData>> dataByTicker) {
        Map<String, Map<String, IndicatorValues>> indicatorCache = new HashMap<>();

        for (Map.Entry<String, List<StockData>> entry : dataByTicker.entrySet()) {
            String ticker = entry.getKey();
            List<StockData> stockDataList = entry.getValue();
            
            if (stockDataList.isEmpty()) continue;
            BarSeries series = new BaseBarSeries(ticker);
            for (StockData data : stockDataList) {
                ZonedDateTime zdt = LocalDate.parse(data.date).atStartOfDay(ZoneId.systemDefault());
                series.addBar(zdt, data.open, data.high, data.low, data.close, data.volume);
            }
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            RSIIndicator rsi = new RSIIndicator(closePrice, RSI_PERIOD);
            SMAIndicator bbSma = new SMAIndicator(closePrice, BB_PERIOD);
            BollingerBandsMiddleIndicator bbMid = new BollingerBandsMiddleIndicator(bbSma); 
            
            StandardDeviationIndicator bbStd = new StandardDeviationIndicator(closePrice, BB_PERIOD);
            Num k = series.function().apply(BB_STD_DEV);
            BollingerBandsUpperIndicator bbUpper = new BollingerBandsUpperIndicator(bbMid, bbStd, k);
            BollingerBandsLowerIndicator bbLower = new BollingerBandsLowerIndicator(bbMid, bbStd, k);
            Map<String, IndicatorValues> dailyValues = new HashMap<>();
            for (int i = series.getBeginIndex(); i <= series.getEndIndex(); i++) {
                String date = series.getBar(i).getEndTime().toLocalDate().toString();
                
                IndicatorValues values = new IndicatorValues(
                        rsi.getValue(i).doubleValue(),
                        bbUpper.getValue(i).doubleValue(),
                        bbLower.getValue(i).doubleValue(),
                        bbMid.getValue(i).doubleValue()
                );
                dailyValues.put(date, values);
            }
            indicatorCache.put(ticker, dailyValues);
        }
        return indicatorCache;
    }

    private static List<Double> runBacktest(Map<String, List<StockData>> dataByDate,
                                            Map<String, Map<String, IndicatorValues>> indicatorCache,
                                            String outputFile) throws IOException {
        
        List<Double> portfolioDailyReturns = new ArrayList<>();
        try (FileWriter out = new FileWriter(outputFile)) {
            out.write("Date,Portfolio_Return\n"); // Write header

            for (Map.Entry<String, List<StockData>> entry : dataByDate.entrySet()) {
                String date = entry.getKey();
                List<StockData> stocksForDay = entry.getValue();
                
                double totalReturn = 0;
                int positionsTaken = 0;

                for (StockData stock : stocksForDay) {
                    IndicatorValues indicators = indicatorCache.getOrDefault(stock.ticker, Collections.emptyMap()).get(date);
                    if (indicators == null) continue;

                    int signal = getSignal(stock.close, indicators);

                    if (signal != 0) {
                        totalReturn += (signal * stock.nextDayReturn);
                        positionsTaken++;
                    }
                }

                double dailyReturn = (positionsTaken == 0) ? 0.0 : (totalReturn / positionsTaken);
                portfolioDailyReturns.add(dailyReturn);
                out.write(date + "," + dailyReturn + "\n");
            }
        }
        return portfolioDailyReturns;
    }


    private static int getSignal(double close, IndicatorValues indicators) {
        double rsi = indicators.rsi;
        double bbUpper = indicators.bbUpper;
        double bbLower = indicators.bbLower;
        double bbMid = indicators.bbMid;
        if (rsi < 25 && close < bbLower) return 1;
        if (rsi < 30 && close < (bbMid + bbLower) / 2) return 1;
        if (rsi > 75 && close > bbUpper) return -1;
        if (rsi > 70 && close > (bbMid + bbUpper) / 2) return -1;

        return 0;
    }

    private static void calculateAndPrintPerformance(List<Double> returns) {
        if (returns.isEmpty()) {
            System.out.println("No returns to analyze.");
            return;
        }

        double sum = 0;
        for (double r : returns) sum += r;
        double mean = sum / returns.size();
        
        double sumSqDev = 0;
        for (double r : returns) sumSqDev += Math.pow(r - mean, 2);
        double stdDev = Math.sqrt(sumSqDev / returns.size());
        double annReturn = mean * TRADING_DAYS_PER_YEAR;
        double annVolatility = stdDev * Math.sqrt(TRADING_DAYS_PER_YEAR);
        double sharpe = (annVolatility == 0) ? 0 : (annReturn / annVolatility);
        double maxDrawdown = 0;
        double cumulativeReturn = 1.0;
        double peakReturn = 1.0;

        for (double r : returns) {
            cumulativeReturn *= (1 + r);
            if (cumulativeReturn > peakReturn) {
                peakReturn = cumulativeReturn;
            }
            double drawdown = (cumulativeReturn - peakReturn) / peakReturn;
            if (drawdown < maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }
        
        System.out.printf("Annualized Return:    %.2f%%\n", annReturn * 100);
        System.out.printf("Annualized Volatility: %.2f%%\n", annVolatility * 100);
        System.out.printf("Sharpe Ratio:           %.2f\n", sharpe);
        System.out.printf("Maximum Drawdown:     %.2f%%\n", maxDrawdown * 100);
    }
}