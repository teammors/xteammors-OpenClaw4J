---
name: "browser-automation"
description: "Crawls and collects data for user-specified keywords using browser automation, returning structured results."
---

# Browser Automation Skill

This skill enables the agent to search for specific keywords using a search engine and collect relevant data.
It uses Playwright (via Python) to automate the browser interaction, ensuring reliable data extraction even from dynamic websites.

## Capabilities
- Search Google/Bing for keywords
- Extract search result titles, snippets, and links
- Return data in a structured format (JSON/List)

## Usage
When the user asks "Find latest news about AI", "Search for 'Java tutorial' and give me links", or "Crawl data about SpaceX", this skill should be invoked.

## Dependencies
This skill requires `playwright` python library.
The script handles dependency installation automatically.

## Configuration
No specific configuration required for this skill.
