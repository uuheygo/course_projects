#ifndef DLSSERVER_HPP_
#define DLSSERVER_HPP_

#include <iostream>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <netdb.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <arpa/inet.h>
#include <vector>
#include <math.h>
#include <signal.h>
#include <sys/wait.h>
#include <sstream>

using namespace std;

// constant declaration
#define BACKLOG 10 // how many pending connections queue will hold
#define MAXDATASIZE 1025


// function prototype
void sigchld_handler(int s);
void *get_in_addr(struct sockaddr *sa);
int hostname_to_ip(char * hostname , char* ip);
double distance(double x1, double y1, double x2, double y2);
string stringify(double x);


void sigchld_handler(int s){
	while(waitpid(-1, NULL, WNOHANG) > 0);
}

// get sockaddr, IPv4 or IPv6:
void *get_in_addr(struct sockaddr *sa) {
	if (sa->sa_family == AF_INET) {
		return &(((struct sockaddr_in*)sa)->sin_addr);
	}
	return &(((struct sockaddr_in6*)sa)->sin6_addr);
}

// get ip from hostname
int hostname_to_ip(char * hostname , char* ip) {
    struct hostent *he;
    struct in_addr **addr_list;
    int i;

    if ( (he = gethostbyname( hostname ) ) == NULL) {
        // get the host info
        perror("gethostbyname");
        return 1;
    }

    addr_list = (struct in_addr **) he->h_addr_list;

    for(i = 0; addr_list[i] != NULL; i++) {
        //Return the first one;
        strcpy(ip , inet_ntoa(*addr_list[i]) );
        return 0;
    }

    return 1;
}


// find the distance between two locations
double distance(double x1, double y1, double x2, double y2) { // consider earth as a sphere, 180 is same as -180
	double xDis = min(fabs(x1 - x2), 360 - fabs(x1 - x2));
	double yDis = min(fabs(y1 - y2), 360 - fabs(y1 - y2));
	return sqrt(pow(xDis, 2) + pow(yDis, 2));
}

// convert double to string
string stringify(double x) {
   ostringstream o;
   o << x;
   return o.str();
}

#endif /* DLSSERVER_HPP_ */
