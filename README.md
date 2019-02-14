
# Byte Buffet for faster byte-buffers and Date parsing  

This package was created to make byte-buffers even faster by removing common String bound operations. 
Exellent for read time-series data and to interact with big-data scans in HBase or tsv-files via Spark. 
The Date-parsing code allows for concurrent date-parsing without GC overhead. 

This was a solution as SimpleDateFormat is not concurrent.
Consider using java.time.DateTimeFormatter instead with a small overhead. 

/Jonas
