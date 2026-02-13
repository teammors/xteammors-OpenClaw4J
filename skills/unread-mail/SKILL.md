---
name: "unread-mail"
description: "Retrieves unread emails from the mailbox. Invoke this skill when the user asks to check for unread emails or what's new in their inbox."
---

# Unread Mail Skill

This skill enables the agent to fetch unread emails from the configured mailbox.
It connects via IMAP and retrieves the subject and sender of unread messages.

## Capabilities
- List unread emails
- Show sender and subject
- Limit the number of emails retrieved

## Usage
When the user says "Check my unread emails" or "Do I have any new mail?", this skill should be invoked.

## Configuration
```yaml
mail:
  imap_host: imap server address
  imap_port: 993
  username: your mail address
  password: your mail client password (It's not a login password, but a client password)
```
