---
name: "send-email"
description: "Sends an email to a specified recipient. Invoke this skill when the user explicitly asks to send an email."
---

# Send Email Skill

This skill enables the agent to send emails.
It is capable of parsing the recipient, subject, and body from the user's natural language request.

## Capabilities
- Send text emails
- Automatic extraction of email details

## Usage
When the user says "Send an email to bob@example.com saying hello", this skill should be invoked.

## Configuration
```yaml
mail:
  host: smtp server address
  port: 465
  username: your mail address
  password: your mail client password (It's not a login password, but a client password)
  properties:
    mail:
      smtp:
        auth: true
        starttls:
          enable: true
```

