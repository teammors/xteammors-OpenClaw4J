import imaplib
import email
from email.header import decode_header
import sys

def test_imap(host, port, username, password):
    try:
        print(f"Connecting to {host}:{port}...")
        mail = imaplib.IMAP4_SSL(host, port)
        
        print("Logging in...")
        mail.login(username, password)
        print("Login successful!")
        
        print("\nListing Mailboxes:")
        status, mailboxes = mail.list()
        if status == "OK":
            for mb in mailboxes:
                print(mb.decode())
        
        print("\nSelecting INBOX...")
        status, data = mail.select("INBOX")
        print(f"Status: {status}")
        print(f"Total messages in INBOX: {data[0].decode()}")
        
        print("\nSearching for UNSEEN (Unread) messages...")
        status, messages = mail.search(None, "UNSEEN")
        unread_ids = messages[0].split()
        print(f"Found {len(unread_ids)} unread messages.")
        
        if len(unread_ids) > 0:
            print("Fetching latest unread message header...")
            latest_id = unread_ids[-1]
            status, msg_data = mail.fetch(latest_id, "(RFC822.HEADER)")
            for response_part in msg_data:
                if isinstance(response_part, tuple):
                    msg = email.message_from_bytes(response_part[1])
                    subject, encoding = decode_header(msg["Subject"])[0]
                    if isinstance(subject, bytes):
                        subject = subject.decode(encoding if encoding else "utf-8")
                    print(f"Subject: {subject}")
        else:
            print("No unread messages. Fetching latest READ message to verify fetch works...")
            # Fetch total count again to get latest ID
            status, data = mail.select("INBOX")
            total_messages = int(data[0].decode())
            if total_messages > 0:
                latest_id = str(total_messages)
                status, msg_data = mail.fetch(latest_id, "(RFC822.HEADER)")
                for response_part in msg_data:
                    if isinstance(response_part, tuple):
                        msg = email.message_from_bytes(response_part[1])
                        subject_header = msg["Subject"]
                        if subject_header:
                            subject, encoding = decode_header(subject_header)[0]
                            if isinstance(subject, bytes):
                                subject = subject.decode(encoding if encoding else "utf-8")
                            print(f"Latest Email Subject: {subject}")
                        else:
                            print("Latest email has no subject.")
            else:
                print("Inbox is empty.")

        mail.logout()
        
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    # Hardcoded for testing based on known config
    host = "imap.qiye.163.com"
    port = 993
    username = "lkx@xmsy666.com"
    password = "9wfgDcSNQ%1C$cU$"
    
    test_imap(host, port, username, password)
