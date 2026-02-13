---
name: "crypto-price"
description: "Retrieves current cryptocurrency prices (BTC, ETH, etc.). Invoke when user asks for crypto prices, market rates, or coin values."
---

# Crypto Price Skill

This skill retrieves real-time cryptocurrency prices using the CCXT library (connecting to Binance public API).

## Capabilities
- Get current price for specific coins (BTC, ETH, SOL, etc.)
- Returns Price, 24h Change (if available), and other ticker data.

## Usage
When the user asks "What is the price of BTC?", "Check ETH rate", or "How is the crypto market?", this skill should be invoked.

## Dependencies
This skill requires `ccxt` python library.
The script handles dependency installation automatically.
**Note**: The first run might be slow due to dependency installation (1-2 minutes). Subsequent runs will be faster.

## Configuration
No specific configuration required. Uses public APIs.
