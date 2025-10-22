Backtest Report: Mean Reversion Strategy

Date: October 22, 2025
Strategy Name: RSI + Bollinger Bands Mean Reversion
Universe: Subset of S&P 500 Components (N=100)
Timeframe: 2015-01-01 to 2024-12-31 (Daily Data)

1. Strategy Description

This report details the backtest of a mean-reversion trading strategy applied to a portfolio of US equities. The strategy aims to identify potential short-term price reversals using a combination of technical indicators:

Relative Strength Index (RSI): Measures the magnitude of recent price changes to evaluate overbought or oversold conditions.

Bollinger Bands (BBands): Consist of a middle band (Simple Moving Average) and upper/lower bands based on standard deviations, indicating volatility and potential price extremes.

Volume Weighted Moving Average (VWMA): A moving average that gives more weight to bars with higher volume.

Entry Logic:
The strategy generates long and short signals based on the following conditions:

Long Entry (Strict): RSI < 25 AND Close < Lower Bollinger Band AND Close > VWMA(50)

Long Entry (Aggressive): RSI < 30 AND Close < Midpoint between BB-Mid and BB-Lower AND Close > VWMA(50)

Short Entry (Strict): RSI > 75 AND Close > Upper Bollinger Band AND Close < VWMA(50)

Short Entry (Aggressive): RSI > 70 AND Close > Midpoint between BB-Mid and BB-Upper AND Close < VWMA(50)

Exit Logic:
Positions are held until the opposite signal is generated (implicitly handled by the portfolio rebalancing).

Parameters Used:

RSI Period: 5 days

Bollinger Bands Period: 10 days

Bollinger Bands Standard Deviation: 2.5

VWMA Period: 50 days

2. Backtesting Process

A hybrid Python-Java-Python workflow was used:

Data Preparation (prepare_data.py - Python):

Fetched the list of S&P 500 components (limited to N=100).

Downloaded daily Open, High, Low, Close, Volume (OHLCV) data from Yahoo Finance (yfinance) for the specified timeframe.

Calculated the next day's return for each stock (required for scoring trades in the backtest).

Saved the raw data and next-day returns to data_for_java.csv.

Indicator Calculation & Backtesting (StrategyProcessor.java - Java):

Read data_for_java.csv.

Grouped data by stock ticker.

Used the ta4j library to calculate RSI, Bollinger Bands, and VWMA for each stock. Indicator values were cached for efficiency.

Grouped data by date.

Iterated through each day:

Applied the strategy's getSignal logic to each stock using the cached indicator values.

Calculated the equal-weighted portfolio return for the day: (Sum of Long Returns - Sum of Short Returns) / Total Number of Positions. Stocks with no signal were excluded.

Saved the daily portfolio returns to portfolio_returns.csv.

Calculated and printed summary performance metrics (Annualized Return, Volatility, Sharpe, Max Drawdown).

Analysis & Visualization (analysis.py - Python):

Read portfolio_returns.csv.

Performed a Fama-French 3-Factor regression using statsmodels and factor data from the Kenneth French Data Library (pandas_datareader).

Generated a plot of the strategy's cumulative returns using matplotlib.

Assumptions:

No trading costs (commissions, slippage, spreads) were considered.

The portfolio was equally weighted across all signaled positions each day.

Trades occur at the closing price of the signal day, and returns are realized on the next day's close.

3. Performance Metrics

The following metrics were calculated based on the daily portfolio returns from 2015 to 2024:

Annualized Return: 0.59%

Annualized Volatility: 18.35%

Sharpe Ratio: 0.03

Maximum Drawdown: -48.80%

Interpretation:

The strategy generated a slightly positive return over the backtest period, but very close to zero.

The volatility was significant (18.35%), comparable to broad market volatility.

The resulting Sharpe Ratio is extremely low (0.03), indicating very poor risk-adjusted returns. A Sharpe ratio below 0.5 is generally considered poor, and below 0.1 suggests the strategy does not outperform a risk-free asset on a risk-adjusted basis.

The Maximum Drawdown of nearly -49% is substantial, indicating significant potential downside risk during unfavorable periods.

4. Factor Regression Analysis (Fama-French 3-Factor)

The strategy's excess returns (portfolio return - risk-free rate) were regressed against the three Fama-French factors: Market (Mkt-RF), Size (SMB), and Value (HML).

Regression Results:

                            OLS Regression Results                            
==============================================================================
Dep. Variable:     Strategy_Excess_Ret   R-squared:                       0.023
Model:                             OLS   Adj. R-squared:                  0.022
Method:                  Least Squares   F-statistic:                     19.77
Date:                 Wed, 22 Oct 2025   Prob (F-statistic):           1.15e-12
Time:                         20:40:26   Log-Likelihood:                 7674.4
No. Observations:                 2514   AIC:                        -1.534e+04
Df Residuals:                     2510   BIC:                        -1.532e+04
Df Model:                            3                                         
Covariance Type:             nonrobust                                         
==============================================================================
                 coef    std err          t      P>|t|      [0.025      0.975]
------------------------------------------------------------------------------
const       2.023e-05      0.000      0.089      0.929      -0.000       0.000
Mkt_RF        -0.1373      0.020     -6.751      0.000      -0.177      -0.097
SMB            0.1119      0.036      3.144      0.002       0.042       0.182
HML           -0.0940      0.025     -3.712      0.000      -0.144      -0.044
==============================================================================
Omnibus:                     1147.935   Durbin-Watson:                   2.216
Prob(Omnibus):                  0.000   Jarque-Bera (JB):           172041.039
Skew:                          -1.094   Prob(JB):                         0.00
Kurtosis:                      43.467   Cond. No.                         158.
==============================================================================



Annualized Alpha: 0.51% (p-value: 0.9294)

Market Beta (Mkt-RF): -0.1373 (p-value: 0.000)

Size Beta (SMB): 0.1119 (p-value: 0.002)

Value Beta (HML): -0.0940 (p-value: 0.000)

R-squared: 0.023

Interpretation:

Alpha: The annualized alpha is statistically insignificant (p-value = 0.93). This means the strategy did not generate returns beyond what can be explained by its exposure to the Fama-French risk factors. The near-zero alpha aligns with the low overall returns.

Market Beta: The strategy has a significant negative beta to the market (-0.14). This suggests the strategy tends to perform slightly better when the overall market goes down and slightly worse when it goes up. This is unusual for a long/short equity strategy and might indicate the short signals are more dominant or effective than the long signals, or that the signals trigger counter-trend moves.

Size Beta: The strategy has a significant positive loading on the SMB (Small Minus Big) factor (0.11). This indicates a tilt towards smaller-cap stocks within the chosen universe.

Value Beta: The strategy has a significant negative loading on the HML (High Minus Low) factor (-0.09). This indicates a tilt towards growth stocks over value stocks.

R-squared: The very low R-squared (0.023 or 2.3%) means that the Fama-French factors explain only a tiny fraction of the strategy's return variance. Most of the strategy's day-to-day performance is driven by factors other than market, size, or value exposure (often referred to as idiosyncratic risk or strategy-specific behavior).

5. Performance Visualization

(The plot generated by analysis.py shows the cumulative return of the strategy over the backtest period.)

Interpretation:
The cumulative return plot visually confirms the low overall performance. While there might be periods of gains, the lack of a clear upward trend and the significant drawdowns (implied by the large difference between peaks and subsequent troughs) highlight the strategy's inconsistency and risk. A comparison against a benchmark like the S&P 500 would be beneficial to assess relative performance.

6. Conclusion and Next Steps

This backtest demonstrates the implementation of a mean-reversion strategy using RSI, Bollinger Bands, and VWMA across a portfolio of 100 US stocks. The results indicate that, with the chosen parameters and equal weighting, the strategy does not generate significant risk-adjusted returns.

Performance: Near-zero Sharpe ratio and substantial drawdown suggest the strategy is currently ineffective.

Risk Profile: The negative market beta and low R-squared indicate its returns are largely disconnected from broad market movements, but not in a consistently profitable way. The tilts towards small-cap and growth stocks are notable.

Recommendations for Improvement (as per original assignment):

Parameter Tuning: Experiment systematically with different periods for RSI (e.g., 14), Bollinger Bands (e.g., 20), and VWMA (e.g., 20, 100). Optimize for Sharpe Ratio, not just total return.

Indicator Variants:

Consider using EMA instead of SMA for the Bollinger Band middle line.

Explore alternative indicators mentioned in the assignment (e.g., moving average crossovers, fundamental ratios like P/E or P/B if data is available).

Signal Logic: Refine the entry/exit thresholds (e.g., RSI 30/70 instead of 25/75). Add conditions based on the trend (e.g., only take long mean-reversion signals if the long-term trend, like EMA200, is up).

Portfolio Construction:

Weighting: Instead of equal weighting, consider volatility-based weighting (inversely proportional to recent volatility) or conviction-based weighting (based on signal strength).

Filtering: Implement filters to exclude illiquid stocks (e.g., based on average daily volume or dollar volume).

Percentile Cutoffs: Instead of taking all signals, select only the top/bottom X% of stocks based on indicator readings (e.g., lowest RSI for longs).

Risk Management: While this backtest didn't include trade-level stops, portfolio-level risk management could be added (e.g., reducing overall exposure during high market volatility).

Out-of-Sample Testing: Perform the analysis on separate in-sample (for parameter finding) and out-of-sample (for validation) periods to ensure robustness and avoid overfitting.

This initial backtest serves as a baseline. Significant refinement is needed to develop a potentially viable strategy. The focus should be on improving the risk-adjusted return profile (Sharpe Ratio) and understanding the drivers of performance (or lack thereof).