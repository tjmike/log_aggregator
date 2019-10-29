# Log Forwarding Agent

The logging agent will tail a log file and deliver its contents to a server in a timely matter.

## Log File Constraints
- The log file will only be appended, never truncated
- Log files are on a local file system


## Logging Environment Assumption
The logging host supports the ability to uniquely identify a file via Java's BasicFileAttributeView.fileKey. 


As an example: 
Given a log file server.log.
- If we have an input stream open to the file and the file is moved to server.log.001 we can continue to read from 
that file (now named server.log.001).

- The file system supports the concept of a unique identifier (device/inode for example) and that identifier can be
utilized from Java

- If we get the attributes of our existing file (server.log.001) and a newly created (server.log) they will be
reported by Java as different.

## Basic Operation
At startup the logging agent will begin tailing the log file. It will only capture new data from the time it is started.
The agent will forward capture log data to a server. If the server is not available or notifies the agent to back off
the agent will store the data until the server is ready. 


## Some Other Options To Consider
- exec tail -F to stdin and read from that stream

- use Java's WatchService to monitor files and grab data when changes are detected

- Create a reader and attempt to continually read the log file while handing events like log rotations.
  This is the current approach with limited (eg untested) support foor log rotation.


