import imaplib
import email
from email.header import decode_header
import sys
import argparse
import json

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

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Fetch unread emails via IMAP')
    parser.add_argument('--host', required=True, help='IMAP server host')
    parser.add_argument('--port', type=int, required=True, help='IMAP server port')
    parser.add_argument('--username', required=True, help='IMAP username')
    parser.add_argument('--password', required=True, help='IMAP password')
    parser.add_argument('--limit', type=int, default=5, help='Limit number of emails to fetch')

    args = parser.parse_args()

    emails = get_unread_emails(args.host, args.port, args.username, args.password, args.limit)
    
    if emails is not None:
        print(json.dumps(emails, ensure_ascii=False, indent=2))
    else:
        sys.exit(1)
