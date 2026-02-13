import sys
import json
import subprocess
import argparse

# Try to import ccxt, install if missing
try:
    import ccxt
except ImportError:
    try:
        # Install ccxt automatically
        subprocess.check_call([sys.executable, "-m", "pip", "install", "ccxt", "--break-system-packages"])
        import ccxt
    except Exception as e:
        print(json.dumps({"error": f"Failed to install ccxt: {str(e)}"}, indent=2))
        sys.exit(1)

def get_prices(symbols):
    try:
        exchange = ccxt.binance({'enableRateLimit': True})
        # If exchange is not accessible (e.g. geo-blocked), try others like coingecko or kraken
        # But let's start with binance or maybe kraken which is widely available
        
        # Mapping common names to symbols if needed, but usually users say BTC, ETH
        # Ensure symbols are in standard format like BTC/USDT
        
        results = []
        
        for sym in symbols:
            try:
                # Normalize symbol
                sym = sym.upper()
                if "/" not in sym:
                    pair = f"{sym}/USDT"
                else:
                    pair = sym
                
                ticker = exchange.fetch_ticker(pair)
                
                results.append({
                    "symbol": pair,
                    "price": ticker['last'],
                    "change_24h_percent": ticker['percentage'],
                    "high_24h": ticker['high'],
                    "low_24h": ticker['low'],
                    "volume": ticker['baseVolume']
                })
            except Exception as e:
                # Try finding valid symbol if direct match failed?
                # For now just report error for this symbol
                results.append({
                    "symbol": sym,
                    "error": str(e)
                })
                
        return results
        
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Get crypto prices')
    parser.add_argument('--symbols', default="BTC,ETH", help='Comma separated list of symbols (e.g. BTC,ETH)')

    args = parser.parse_args()
    
    symbol_list = [s.strip() for s in args.symbols.split(',') if s.strip()]
    if not symbol_list:
        symbol_list = ["BTC", "ETH"]

    try:
        data = get_prices(symbol_list)
        print(json.dumps(data, indent=2))
    except Exception as e:
        print(json.dumps({"error": f"An unexpected error occurred: {str(e)}"}, indent=2))
        sys.exit(1)
