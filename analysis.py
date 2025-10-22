# 3_analyze_results.py

import pandas as pd
import numpy as np
import statsmodels.api as sm
import pandas_datareader.data as web
import matplotlib.pyplot as plt
import warnings

warnings.filterwarnings('ignore')

def run_factor_regression(strategy_returns):
    start_date = strategy_returns.index.min()
    end_date = strategy_returns.index.max()
    
    try:
        ff_data = web.DataReader('F-F_Research_Data_Factors_daily', 'famafrench', start=start_date, end=end_date)[0]
        ff_data = ff_data / 100
        ff_data.rename(columns={'Mkt-RF': 'Mkt_RF'}, inplace=True)
        merged_data = pd.DataFrame(strategy_returns).rename(columns={'Portfolio_Return': 'Strategy_Ret'})
        merged_data = merged_data.join(ff_data, how='inner')
        merged_data['Strategy_Excess_Ret'] = merged_data['Strategy_Ret'] - merged_data['RF']
        
        Y = merged_data['Strategy_Excess_Ret']
        X = merged_data[['Mkt_RF', 'SMB', 'HML']]
        X = sm.add_constant(X) 
        
        model = sm.OLS(Y, X).fit()
        print(model.summary())
        
        alpha_annualized = model.params['const'] * 252
        print(f"\nAnnualized Alpha: {alpha_annualized:.2%} (p-value: {model.pvalues['const']:.4f})")
        
    except Exception as e:
        print(f"Could not run factor regression: {e}")

def plot_results(strategy_returns):
    print("\nPlotting results...")
    
    cumulative_returns = (1 + strategy_returns).cumprod()
    
    plt.figure(figsize=(12, 6))
    cumulative_returns.plot(label='Java Strategy', color='blue')
    plt.title('Strategy Cumulative Returns (Java Logic)')
    plt.xlabel('Date')
    plt.ylabel('Cumulative Returns')
    plt.legend()
    plt.grid(True)
    plt.show()

if __name__ == "__main__":
    
    results_file = 'portfolio_returns.csv'
    try:
        returns_df = pd.read_csv(results_file, parse_dates=['Date'], index_col='Date')
    except FileNotFoundError:
        print(f"Error: {results_file} not found.")
        print("Please run `1_prepare_data.py` and then the Java `StrategyProcessor` first.")
        exit()
        
    strategy_returns = returns_df['Portfolio_Return'].squeeze()
    
    if strategy_returns.empty:
        print("No returns found in file. Exiting.")
        exit()
        
    run_factor_regression(strategy_returns)
    plot_results(strategy_returns)