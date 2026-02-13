import smtplib
import sys
import argparse
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from email.utils import formataddr

def send_email(host, port, username, password, to_addr, subject, body):
    try:
        msg = MIMEMultipart()
        msg['From'] = formataddr((username.split('@')[0], username))
        msg['To'] = to_addr
        msg['Subject'] = subject

        msg.attach(MIMEText(body, 'plain'))

        # Try connecting
        print(f"Connecting to {host}:{port}...")

        # Determine connection type based on port
        if port == 465:
            server = smtplib.SMTP_SSL(host, port, timeout=30)
        else:
            server = smtplib.SMTP(host, port, timeout=30)
            if port == 587:
                server.starttls()

        print("Logging in...")
        try:
            server.login(username, password)
        except smtplib.SMTPAuthenticationError as e:
            if "ERR.LOGIN.REQCODE" in str(e) or "535" in str(e):
                print(f"Authentication failed: The server requires an Authorization Code (App Password), not the login password. Error: {e}")
            else:
                print(f"Authentication failed: {e}")
            return False

        print("Sending mail...")
        server.sendmail(username, [to_addr], msg.as_string())

        server.quit()
        print("Email sent successfully!")
        return True
    except Exception as e:
        print(f"Failed to send email: {e}")
        return False

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Send an email via SMTP')
    parser.add_argument('--host', required=True, help='SMTP server host')
    parser.add_argument('--port', type=int, required=True, help='SMTP server port')
    parser.add_argument('--username', required=True, help='SMTP username')
    parser.add_argument('--password', required=True, help='SMTP password')
    parser.add_argument('--to', required=True, help='Recipient email address')
    parser.add_argument('--subject', required=True, help='Email subject')
    parser.add_argument('--body', required=True, help='Email body')

    args = parser.parse_args()

    success = send_email(args.host, args.port, args.username, args.password, args.to, args.subject, args.body)
    if not success:
        sys.exit(1)