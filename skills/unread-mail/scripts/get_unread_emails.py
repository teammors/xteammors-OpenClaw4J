import imaplib
import email
from email.header import decode_header
import sys
import os
import re
import subprocess

# Try to import yaml, install if missing
try:
    import yaml
except ImportError:
    try:
        # Install pyyaml automatically
        subprocess.check_call(
            [sys.executable, "-m", "pip", "install", "pyyaml", "--break-system-packages"],
            stdout=sys.stderr,  # 重定向输出到标准错误
            stderr=sys.stderr
        )
        import yaml
    except Exception as e:
        print(f"Error: Failed to install pyyaml: {str(e)}", file=sys.stderr)
        sys.exit(1)

def load_config():
    """从 SKILL.md 文件中加载配置"""
    # 获取脚本所在目录的父目录，即技能目录
    script_dir = os.path.dirname(os.path.abspath(__file__))
    skill_dir = os.path.dirname(script_dir)
    skill_md_path = os.path.join(skill_dir, "SKILL.md")
    
    if not os.path.exists(skill_md_path):
        print(f"Error: SKILL.md not found at {skill_md_path}", file=sys.stderr)
        return None
    
    try:
        with open(skill_md_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 提取 YAML 块
        yaml_match = re.search(r'```yaml\s*(.*?)\s*```', content, re.DOTALL)
        if not yaml_match:
            print("Error: YAML configuration not found in SKILL.md", file=sys.stderr)
            return None
        
        yaml_content = yaml_match.group(1)
        config = yaml.safe_load(yaml_content)
        
        if 'mail' not in config:
            print("Error: 'mail' section not found in configuration", file=sys.stderr)
            return None
        
        return config['mail']
    except Exception as e:
        print(f"Error loading config: {e}", file=sys.stderr)
        return None

def get_unread_emails(host, port, username, password, limit=5):
    try:
        # Connect to the server
        print(f"Connecting to {host}:{port}...", file=sys.stderr)
        mail = imaplib.IMAP4_SSL(host, port)
        
        # Login
        print("Logging in...", file=sys.stderr)
        mail.login(username, password)
        
        # Select the mailbox (Inbox)
        mail.select("inbox")
        
        # Search for unread emails
        status, messages = mail.search(None, "UNSEEN")
        if status != "OK":
            print("No messages found or error searching.", file=sys.stderr)
            return []
            
        mail_ids = messages[0].split()
        
        # Get the latest emails first
        mail_ids = mail_ids[::-1]
        
        results = []
        count = 0
        
        for i in mail_ids:
            if count >= limit:
                break
                
            status, msg_data = mail.fetch(i, "(RFC822)")
            for response_part in msg_data:
                if isinstance(response_part, tuple):
                    msg = email.message_from_bytes(response_part[1])
                    
                    # Decode Subject
                    subject, encoding = decode_header(msg["Subject"])[0]
                    if isinstance(subject, bytes):
                        subject = subject.decode(encoding if encoding else "utf-8")
                        
                    # Decode From
                    from_, encoding = decode_header(msg.get("From"))[0]
                    if isinstance(from_, bytes):
                        from_ = from_.decode(encoding if encoding else "utf-8")
                    
                    results.append({
                        "id": i.decode(),
                        "subject": subject,
                        "from": from_
                    })
                    count += 1
        
        mail.close()
        mail.logout()
        
        return results
        
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        return None

def format_output(emails):
    """Format the results into the required text format"""
    if emails is None:
        return "Error: Failed to retrieve emails"
    
    if not emails:
        return "Here are your unread emails:\nUnRead Number: 0\nMail List: (Empty)"
    
    sb = []
    sb.append("Here are your unread emails:")
    sb.append(f"UnRead Number: {len(emails)}")
    sb.append("Mail List:")
    
    count = len(emails)
    for i, email in enumerate(emails):
        count -= 1
        if count == 0:
            sb.append(f"{i + 1}、From：{email['from']}  Subject：{email['subject']}")
        else:
            sb.append(f"{i + 1}、From：{email['from']}  Subject：{email['subject']}")
    
    return "\n".join(sb)

if __name__ == "__main__":
    # 从 SKILL.md 加载配置
    config = load_config()
    if not config:
        print("Error: Failed to load configuration", file=sys.stderr)
        sys.exit(1)
    
    # 从配置中获取参数
    host = config.get('imap_host')
    port = config.get('imap_port')
    username = config.get('username')
    password = config.get('password')
    limit = 5  # 默认限制为 5 封邮件
    
    # 检查必要参数
    if not all([host, port, username, password]):
        print("Error: Missing required configuration parameters", file=sys.stderr)
        sys.exit(1)
    
    # 执行邮件获取
    emails = get_unread_emails(host, port, username, password, limit)
    
    if emails is not None:
        formatted_output = format_output(emails)
        # 只输出业务数据到标准输出
        print(formatted_output)
    else:
        print("Error: Failed to retrieve emails", file=sys.stderr)
        sys.exit(1)
