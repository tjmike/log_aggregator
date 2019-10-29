# Log Forwarding Agent  - Data Pump

Ths package is responsible for detecting serialized log file chunks and forwarding them 
to the web server. By maintaining this as a separate package the data pump can be started,
stopped, updated without taking down the log tailing application. The only constraint
being the size of the cache filesystem and the ability for the system to catch up.


