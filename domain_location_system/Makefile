all:	dlsServer.cpp dlsClient.cpp dlsServer.hpp dlsClient.hpp
	g++ -o dlsServer dlsServer.cpp
	g++ -o dlsClient dlsClient.cpp
debug:	dlsServer.cpp dlsClient.cpp dlsServer.hpp dlsClient.hpp
	g++ -g -o dlsServer dlsServer.cpp
	g++ -g -o dlsClient dlsClient.cpp
clean:
	rm -f *.o *~ dlsServer core
	rm -f *.o *~ dlsClient core
