import sys
import json
import time
import subprocess

# Try to import playwright, install if missing
try:
    from playwright.sync_api import sync_playwright
except ImportError:
    try:
        # Install playwright and its browsers automatically
        subprocess.check_call([sys.executable, "-m", "pip", "install", "playwright", "--break-system-packages"])
        subprocess.check_call([sys.executable, "-m", "playwright", "install", "chromium"])
        from playwright.sync_api import sync_playwright
    except Exception as e:
        print(json.dumps({"error": f"Failed to install playwright: {str(e)}"}, indent=2))
        sys.exit(1)

def get_keno_results():
    results = []
    
    with sync_playwright() as p:
        # Launch browser (headless)
        browser = p.chromium.launch(headless=True)
        
        # Create a new context with a realistic user agent
        context = browser.new_context(
            user_agent='Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            viewport={'width': 1280, 'height': 800}
        )
        
        page = context.new_page()
        
        # Variable to store the API data if we catch it
        api_data = []
        
        # 1. Intercept network responses to find the hidden API
        def handle_response(response):
            try:
                # Look for JSON responses that might contain keno data
                if "keno" in response.url.lower() and "json" in response.headers.get("content-type", "").lower():
                    # print(f"DEBUG: Intercepted potential API: {response.url}", file=sys.stderr)
                    try:
                        data = response.json()
                        # Check if it looks like a list of draws
                        if isinstance(data, list) and len(data) > 0 and "drawNbr" in data[0]:
                             api_data.extend(data)
                        elif isinstance(data, dict) and "draws" in data:
                             api_data.extend(data["draws"])
                    except:
                        pass
            except:
                pass

        page.on("response", handle_response)
        
        # 2. Navigate to the page
        url = "https://www.playnow.com/keno/winning-numbers/"
        # print(f"DEBUG: Navigating to {url}", file=sys.stderr)
        
        try:
            page.goto(url, timeout=60000, wait_until="networkidle")
        except Exception as e:
             # print(f"DEBUG: Navigation timeout or error: {e}", file=sys.stderr)
             pass
        
        # Wait a bit more for any lazy loading
        time.sleep(5)
        
        # 3. If we intercepted the API, return that data
        if api_data:
            # print("DEBUG: Successfully intercepted API data", file=sys.stderr)
            browser.close()
            return api_data[:10] # Return top 10
            
        # 4. If API interception failed, try DOM scraping
        # print("DEBUG: API interception failed, trying DOM scraping", file=sys.stderr)
        
        # Selector guesses based on typical class names
        # Usually tables have rows <tr>
        rows = page.query_selector_all("table tbody tr")
        
        scraped_data = []
        for row in rows:
            if len(scraped_data) >= 10: break
            
            # Extract text from cells
            cells = row.query_selector_all("td")
            if len(cells) < 3: continue
            
            # This is a guess at the column structure: Date, Draw #, Numbers
            # We need to be flexible
            try:
                date_text = cells[0].inner_text().strip()
                draw_num = cells[1].inner_text().strip()
                
                # Numbers might be in a container
                numbers_container = cells[2]
                numbers = [n.inner_text().strip() for n in numbers_container.query_selector_all(".ball, .number, span")]
                # Filter out empty strings
                numbers = [n for n in numbers if n.isdigit()]
                
                if not numbers:
                    # Maybe raw text?
                    raw = numbers_container.inner_text().strip()
                    numbers = [n.strip() for n in raw.split() if n.isdigit()]
                
                if date_text and draw_num:
                    scraped_data.append({
                        "drawDate": date_text,
                        "drawNbr": draw_num,
                        "winningNumbers": numbers
                    })
            except:
                continue
                
        browser.close()
        
        if not scraped_data:
            # Last resort: Try to find *any* text that looks like keno results
            # print("DEBUG: DOM scraping failed to find standard table", file=sys.stderr)
            return {"error": "Could not retrieve data via API interception or DOM scraping. The site structure might have changed."}
            
        return scraped_data

    
def format_output(data):
    """Format the results into the required text format"""
    if isinstance(data, dict) and "error" in data:
        return f"Failed to retrieve Keno numbers: {data['error']}"
    
    if not data:
        return "No Keno draws found."
    
    sb = []
    sb.append("🎱 **Latest 10 Keno Draws**")
    sb.append("")
    
    for draw in data:
        # Updated keys based on user-provided JSON sample
        date = draw.get("drawDate")
        time = draw.get("drawTime")
        number = draw.get("drawNbr")
        winning = draw.get("drawNbrs")
        bonus = draw.get("drawBonus")
        
        # Fallbacks
        if date is None:
            date = draw.get("date")
        if number is None:
            number = draw.get("drawNumber")
        if winning is None:
            winning = draw.get("numbers")
        if winning is None:
            winning = draw.get("results")
        if winning is None:
            winning = draw.get("winningNumbers")
        
        date_time = f"{date} {time}" if time else date
        
        sb.append(f"**Draw #{number if number else 'N/A'}** ({date_time if date_time else 'N/A'})")
        if winning:
            sb.append(f"Numbers: {winning}")
        if bonus is not None:
            sb.append(f"Bonus: {bonus}")
        elif "bonusNumber" in draw:
            sb.append(f"Bonus: {draw['bonusNumber']}")
        sb.append("")
    
    return "\n".join(sb)

if __name__ == "__main__":
    try:
        data = get_keno_results()
        formatted_output = format_output(data)
        print(formatted_output)
    except Exception as e:
        print(f"Error: Script execution failed: {str(e)}")
        sys.exit(1)
