Basic idea:
The communication between server and client is realized using send() and recv() alternatedly.Only one message(address, latitude, or longitude) is transported using send() at a time. Every time when the server or client calls send(), it waits for feedback using recv() before sends another message. 

dlsServer implementation:
1. Follow the instructions in the book to load the address, create a socket, bind the socket to a port and start listen(). During this process, in getaddrinfo(), service is set to "0" which means a unused port will be assigned when calling bind().
2. To get the hostname, IP and port number for client to use, gethostname() is called to first get hostname. Then, gethostbyname() is used to find the IP of host. Port number is got using getsocketname().
3. The server handles requests continueously in a while loop. It uses accept() to get socket descriptor and address information of client.
4. After creating a connection with the client, server first receives the number of messages that will be sent by the client in order to determine what to do with the data. Every time when the server gets a message, it will send back a dummy message to notify the client to continue. Or it will send an error message if the request is invalid. Or it will send back the requested data.
5. The server handles the requestion using if-else based on the number of messages. When the number is 3, a new address/location pair will be registered in a vecter of struct. If the same address is already registered or the location is invalid (>180 or <-180), an error message is sent back. Otherwise, the server will notify the client with success.
6. If the number is 2, the first closest address to a location will be found and sent back to client. The distance is calculated considering that earth is a sphere (180 is equal to -180) using a function distance(). Alse the validity of the location is checked.
7. If the number is 1, the location is requested for an address. If the address is registered, the location is converted to E/W/S/N expressions and sent back. If not, an error message is sent back.

dlsClient implementation:
1. The client will first check the number of parameters passed in command line and then load address information, create socket connect to the server.
2. After get connected, client handles user input using if-else blocks. Every time it sents a message to the server, it waits for response before sending another. If the total number of parameters is 6, then it will send address/location pairs to server for registration. If the total number is 5, it will request the closest address from the server. If the number is 4, it will request location for an address from the server.
3. On windows machine, the process is basically the same except that different libraries and function names are used and winsock need to be initialized.