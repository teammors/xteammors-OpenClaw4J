import smtplib
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
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from email.utils import formataddr

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

def send_email(host, port, username, password, to_addrs, subject, body):
    """发送邮件给多个收件人
    
    Args:
        to_addrs: 邮箱地址列表
    
    Returns:
        tuple: (success_count, total_count, failed_emails)
    """
    if not to_addrs or len(to_addrs) == 0:
        print("Error: No recipients specified", file=sys.stderr)
        return 0, 0, []
    
    success_count = 0
    failed_emails = []
    
    # 逐个发送给每个收件人
    for to_addr in to_addrs:
        try:
            msg = MIMEMultipart()
            msg['From'] = formataddr((username.split('@')[0], username))
            msg['To'] = to_addr
            msg['Subject'] = subject

            msg.attach(MIMEText(body, 'plain'))

            # Try connecting
            print(f"Connecting to {host}:{port}...", file=sys.stderr)

            # Determine connection type based on port
            if port == 465:
                server = smtplib.SMTP_SSL(host, port, timeout=30)
            else:
                server = smtplib.SMTP(host, port, timeout=30)
                if port == 587:
                    server.starttls()

            print("Logging in...", file=sys.stderr)
            try:
                server.login(username, password)
            except smtplib.SMTPAuthenticationError as e:
                if "ERR.LOGIN.REQCODE" in str(e) or "535" in str(e):
                    print(f"Authentication failed: The server requires an Authorization Code (App Password), not the login password. Error: {e}", file=sys.stderr)
                else:
                    print(f"Authentication failed: {e}", file=sys.stderr)
                failed_emails.append(to_addr)
                continue

            print(f"Sending mail to {to_addr}...", file=sys.stderr)
            server.sendmail(username, [to_addr], msg.as_string())

            server.quit()
            print(f"Email sent successfully to {to_addr}!", file=sys.stderr)
            success_count += 1
            
        except Exception as e:
            print(f"Failed to send email to {to_addr}: {e}", file=sys.stderr)
            failed_emails.append(to_addr)
    
    return success_count, len(to_addrs), failed_emails

def parse_email_params(input_text):
    """从用户输入中解析邮件参数"""
    # 检查是否是 AI 返回的格式
    if "收件人:" in input_text and "主题:" in input_text and "内容:" in input_text:
        # 解析 AI 返回的格式
        to_addrs_str = extract_ai_field(input_text, "收件人:")
        subject = extract_ai_field(input_text, "主题:")
        content = extract_ai_field(input_text, "内容:")
        
        # 解析多个邮箱地址 (支持逗号、分号、空格分隔)
        email_pattern = r'[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+'
        to_addrs = re.findall(email_pattern, to_addrs_str)
        
        # 如果没有找到邮箱，使用默认值
        if not to_addrs:
            to_addrs = ["test@example.com"]
        
        # 构建正式的邮件正文
        body = f"您好!\n\n"
                
        # 处理多行内容：确保保留原始格式
        # 使用引号块或分隔线来突出显示系统生成的内容
        if '\n' in content or len(content) > 100:
            # 对于长内容或多行内容，添加额外的格式
            body += "以下是您请求的信息:\n"
            body += "━━━━━━━━━━━━━━━━━━━━━━━━\n"
            body += f"{content}\n"
            body += "━━━━━━━━━━━━━━━━━━━━━━━━\n"
        else:
            # 短内容直接显示
            body += f"{content}\n"
                
        body += "\n如有任何问题，请随时联系我们。\n\n"
        body += "此致，\n"
        body += "系统自动发送"
        
        return to_addrs, subject, body
    else:
        # 传统解析方式
        # 提取所有收件人邮箱
        email_pattern = r'[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+'
        to_addrs = re.findall(email_pattern, input_text)
        
        # 如果没有找到邮箱，使用默认值
        if not to_addrs:
            to_addrs = ["test@example.com"]
        
        # 提取主题和正文
        # 智能处理：组织更正式的邮件内容
        subject = "邮件通知"
        
        # 尝试从输入中提取更具体的主题
        if "主题" in input_text or "标题" in input_text:
            subject_match = re.search(r'(主题 | 标题)[：:]([^\n]+)', input_text)
            if subject_match:
                subject = subject_match.group(2).strip()
        else:
            # 从输入中提取主题关键词
            if "通知" in input_text:
                subject = "重要通知"
            elif "提醒" in input_text:
                subject = "提醒事项"
            elif "测试" in input_text:
                subject = "测试内容通知"
            elif "优化" in input_text:
                subject = "优化需求"
            elif "问题" in input_text:
                subject = "问题反馈"
        
        # 智能组织邮件正文
        # 移除收件人邮箱信息，使正文更干净
        clean_text = re.sub(email_pattern, '', input_text)
        
        # 移除指令性话语
        directive_phrases = [
            "帮我发一份邮件给",
            "给我发邮件",
            "发邮件给",
            "把以下内容通过邮件发送给",
            "以下是邮件的内容",
            "-----------------以下是邮件内容----------------",
            "通知他",
            "，通知他"
        ]
        
        for phrase in directive_phrases:
            clean_text = clean_text.replace(phrase, "")
        
        # 清理空白和换行
        clean_text = clean_text.strip()
        
        # 如果清理后内容为空，使用默认内容
        if not clean_text:
            clean_text = "请查看附件或联系发件人获取详细信息"
        
        # 构建正式的邮件正文
        body = f"您好！\n\n"
        body += f"{clean_text}\n\n"
        body += "如有任何问题，请随时联系我们。\n\n"
        body += "此致，\n"
        body += "系统自动发送"
        
        return to_addrs, subject, body

def extract_ai_field(text, field_name):
    """从 AI 返回的文本中提取指定字段"""
    import re
    # 使用正则表达式匹配字段名后的所有内容 (直到行尾)
    # 对于单行字段 (如收件人、主题),只取当前行
    # 对于多行字段 (如内容),需要特殊处理
    
    lines = text.split('\n')
    
    for i, line in enumerate(lines):
        if line.strip().startswith(field_name):
            # 找到字段行
            content_start = line.find(field_name) + len(field_name)
            current_value = line[content_start:].strip()
            
            # 如果是"内容"字段，需要收集所有后续行直到文件结束
            # 因为内容可能包含任何字符，包括看起来像字段的行
            if field_name == "内容:":
                # 收集当前行剩余内容
                collected_lines = [current_value] if current_value else []
                # 收集所有后续行
                for j in range(i + 1, len(lines)):
                    collected_lines.append(lines[j])
                return '\n'.join(collected_lines).strip()
            else:
                # 对于其他字段 (收件人、主题),只返回当前行的值
                return current_value
    
    return ""

if __name__ == "__main__":
    # 从 SKILL.md 加载配置
    config = load_config()
    if not config:
        print("Error: Failed to load configuration", file=sys.stderr)
        sys.exit(1)
    
    # 从配置中获取参数
    host = config.get('host')
    port = config.get('port')
    username = config.get('username')
    password = config.get('password')
    
    # 从命令行参数中获取用户输入
    input_text = " ".join(sys.argv[1:]) if len(sys.argv) > 1 else ""
    
    # 解析邮件参数
    to_addrs, subject, body = parse_email_params(input_text)
    
    # 检查必要参数
    if not all([host, port, username, password]):
        print("Error: Missing required configuration parameters", file=sys.stderr)
        sys.exit(1)
    
    # 执行邮件发送 (支持多个收件人)
    success_count, total_count, failed_emails = send_email(host, port, username, password, to_addrs, subject, body)
    
    # 输出结果
    if success_count > 0:
        # 成功发送邮件
        if success_count == total_count:
            # 全部发送成功
            if total_count == 1:
                print(f"Email sent successfully to {to_addrs[0]}")
            else:
                print(f"Successfully sent emails to {total_count} recipients: {', '.join(to_addrs)}")
        else:
            # 部分发送成功
            print(f"Partially sent: {success_count}/{total_count} emails sent successfully")
            if failed_emails:
                print(f"Failed to send to: {', '.join(failed_emails)}", file=sys.stderr)
    else:
        # 全部发送失败
        print("Error: Failed to send all emails", file=sys.stderr)
        sys.exit(1)