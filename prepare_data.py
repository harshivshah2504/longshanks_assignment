# 1_prepare_data.py

import yfinance as yf
import pandas as pd
import os
import requests 

def get_sp500_tickers():
    """Fetches S&P 500 tickers from Wikipedia."""
    print("Fetching S&P 500 tickers...")
    try:
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36'
        }
        url = 'https://en.wikipedia.org/wiki/List_of_S%26P_500_companies'
        response = requests.get(url, headers=headers)
        response.raise_for_status() 
        tables = pd.read_html(response.text)
        sp500_table = tables[0]
        tickers = sp500_table['Symbol'].tolist()
        tickers = [t.replace('.', '-') for t in tickers]
        print(f"Successfully fetched {len(tickers)} tickers.")
        return tickers[:100] # Use N=100 stocks
    except Exception as e:
        print(f"Error fetching tickers: {e}. Using a small fallback list.")
        return ['AAPL', 'MSFT', 'GOOGL', 'AMZN', 'JPM', 'NVDA', 'V']

def download_data(tickers, start_date, end_date):
    """Downloads OHLCV data for all tickers."""
    print(f"Downloading daily OHLCV data for {len(tickers)} stocks...")
    data = yf.download(tickers, start=start_date, end=end_date, auto_adjust=True)
    if data.empty:
        raise ValueError("No data downloaded. Check tickers and date range.")
    
    data_long = data.stack(future_stack=True)
    data_long.index.names = ['Date', 'Ticker']
    return data_long

def prepare_data_for_java(prices):
    """
    Calculates only the Next_Day_Return, which Java will use for backtesting.
    """
    print("Calculating next day returns...")
    
    # We will build a list of DataFrames, one for each ticker,
    # and then concat them all at the end.
    output_dfs = []
    
    for ticker, group_df in prices.groupby(level='Ticker'):
        group = group_df.copy()
        
        # Calculate NEXT DAY's return. This is what we will trade.
        group['Next_Day_Return'] = group['Close'].pct_change().shift(-1)
        
        output_dfs.append(group)

    final_df = pd.concat(output_dfs)
    
    # Drop NaNs created by the shift
    return final_df.dropna()

def main():
    N_stocks = 100 
    tickers = get_sp500_tickers()[:N_stocks]
    start_date = '2015-01-01'
    end_date = '2024-12-31'
    
    output_filename = 'data_for_java.csv'

    prices_long = download_data(tickers, start_date, end_date)
    
    final_data = prepare_data_for_java(prices_long)
    
    # Reset index to get Date and Ticker as columns
    final_df_csv = final_data.reset_index()
    
    # Select only the columns Java needs
    columns_to_keep = ['Date', 'Ticker', 'Open', 'High', 'Low', 'Close', 'Volume', 'Next_Day_Return']
    final_df_csv = final_df_csv[columns_to_keep]
    
    # Convert Date to simple string format
    final_df_csv['Date'] = final_df_csv['Date'].dt.strftime('%Y-%m-%d')
    
    # Save to CSV for Java to read
    final_df_csv.to_csv(output_filename, index=False)
    print(f"\nSuccessfully created {output_filename} with {len(final_df_csv)} rows.")
    print("Columns:", final_df_csv.columns.tolist())

if __name__ == "__main__":
    main()