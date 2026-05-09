# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x     | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability, please **DO NOT** create a public issue.

Instead, send an email to the project maintainers or use GitHub's "Report a vulnerability" feature.

We will respond within 48 hours and work on a fix.

## Security Measures

- Passwords are hashed with BCrypt (cost=10)
- All APIs use JWT + Redis blacklist for authentication
- SQL injection prevention via MyBatis parameterized queries
- XSS prevention via OWASP HTML Sanitizer
- CSRF protection via same-origin policy + token validation
- File upload whitelist + file header validation
- Rate limiting via Redis + Lua scripts
- Full audit logging for admin operations
- Multi-tenant data isolation with dual safeguards
