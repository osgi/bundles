# Transaction Servlet Filter
A Servlet Filter to set the transaction. This filter will begin and commit/rollback 
a transaction when the web request has been done.

## Configuration
You can specify the following properties:

* pattern — A regular expression that must match the url. This is identical to the [Apache Felix Whiteboard][1] pattern handler

[1]: http://felix.apache.org/site/white-board-pattern-handler.html