#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Chat Query Skill - Query chat messages from database
This script only queries the database and returns raw data.
AI processing is done by Java side using ChatClient.
"""

import sys
import os
import re
import subprocess
import json

# Try to import required packages, install if missing
try:
    import pymysql
except ImportError:
    try:
        subprocess.check_call(
            [sys.executable, "-m", "pip", "install", "pymysql", "--break-system-packages"],
            stdout=sys.stderr,
            stderr=sys.stderr
        )
        import pymysql
    except Exception as e:
        print(f"Error: Failed to install pymysql: {str(e)}", file=sys.stderr)
        sys.exit(1)

try:
    import yaml
except ImportError:
    try:
        subprocess.check_call(
            [sys.executable, "-m", "pip", "install", "pyyaml", "--break-system-packages"],
            stdout=sys.stderr,
            stderr=sys.stderr
        )
        import yaml
    except Exception as e:
        print(f"Error: Failed to install pyyaml: {str(e)}", file=sys.stderr)
        sys.exit(1)


def load_config():
    """Load configuration from SKILL.md"""
    script_dir = os.path.dirname(os.path.abspath(__file__))
    skill_dir = os.path.dirname(script_dir)
    skill_md_path = os.path.join(skill_dir, "SKILL.md")
    
    if not os.path.exists(skill_md_path):
        print(f"Error: SKILL.md not found at {skill_md_path}", file=sys.stderr)
        return None
    
    try:
        with open(skill_md_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Extract YAML block
        yaml_match = re.search(r'```yaml\s*(.*?)\s*```', content, re.DOTALL)
        if not yaml_match:
            print("Error: YAML configuration not found in SKILL.md", file=sys.stderr)
            return None
        
        yaml_content = yaml_match.group(1)
        config = yaml.safe_load(yaml_content)
        return config
    except Exception as e:
        print(f"Error loading config: {e}", file=sys.stderr)
        return None


def query_database(db_config, sql):
    """Execute SQL query and return results"""
    try:
        connection = pymysql.connect(
            host=db_config['host'],
            port=db_config.get('port', 3306),
            user=db_config['username'],
            password=db_config['password'],
            database=db_config['database'],
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
        
        with connection.cursor() as cursor:
            cursor.execute(sql)
            results = cursor.fetchall()
        
        connection.close()
        return results
    except Exception as e:
        print(f"Error querying database: {e}", file=sys.stderr)
        return None


def main():
    """Main function"""
    # Get SQL from command line arguments
    if len(sys.argv) < 2:
        print("Error: Missing SQL query", file=sys.stderr)
        sys.exit(1)
    
    sql = sys.argv[1]
    
    print(f"Executing SQL: {sql}", file=sys.stderr)
    
    # Load configuration
    config = load_config()
    if not config or 'database' not in config:
        print("Error: Database configuration not found", file=sys.stderr)
        sys.exit(1)
    
    db_config = config['database']
    
    # Query database
    messages = query_database(db_config, sql)
    if messages is None:
        print("Error: Failed to query database", file=sys.stderr)
        sys.exit(1)
    
    print(f"Retrieved {len(messages)} messages", file=sys.stderr)
    
    # Output results as JSON
    print(json.dumps(messages, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
