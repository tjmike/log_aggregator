# Data Decoder

The data decoder is responsible for:

- Monitor an on disc cache
- Order the log chunk files
- Decode the protocol buffer log chunks
- Append the decoded chunks to the reconstituted log file

## Discussion

The decoder assumes that all log chunks will be delivered. It tracks the next
chunk that needs to be appended to the reconstituted log file. If it never shows
the log file will not be appended and the chunk cache would continue too grow. 

The decoder also rebuilds the log file with the agent sessionID. We can be confident that a given 
agent session on the capture side will produce contiguous log chunks. Once the agent is restarted
we have no way of knowing how much data will be missed. Associating the sessionID with the log file name
provides a means too differentiate these sessions. The spec requires that the sessionIDs increase for each
capture session. This enables multiple log files to be ordered.
  

