# Log file server

The log file server is responsible for:
- Accept log chunks and store to file system
- Accept a throttle message
- Forward current throttle message to clients
- Supports multiple http clients
    