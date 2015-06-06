// dlsClient_win.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include <winsock2.h>
#include <windows.h>
#include <ws2tcpip.h>
#include <stdlib.h>
#include <stdio.h>

#include <iostream>
#include <sys/types.h>
#include <string.h>
#include <errno.h>
#include <vector>
#include <math.h>


// Need to link with Ws2_32.lib, Mswsock.lib, and Advapi32.lib
#pragma comment (lib, "Ws2_32.lib")
#pragma comment (lib, "Mswsock.lib")
#pragma comment (lib, "AdvApi32.lib")

using namespace std;

#define MAXDATASIZE 100

// get sockaddr, IPv4 or IPv6:
void *get_in_addr(struct sockaddr *sa) {
	if (sa->sa_family == AF_INET) {
		return &(((struct sockaddr_in*)sa)->sin_addr);
	}
	return &(((struct sockaddr_in6*)sa)->sin6_addr);
}

int main(int argc, char **argv)
{
	WSADATA wsaData;
	int sockfd, numbytes;
	char buf[MAXDATASIZE];
	struct addrinfo hints, *servinfo, *p;
	int rv;
	char s[INET6_ADDRSTRLEN];
	string str;

	// show input
	printf("You entered:\n");
	for (int i = 3; i < argc; i++)
		printf("%s  ", argv[i]);
	printf("\n");

	// check number of cmd parameters
	if (argc < 4 || argc > 6) {
		fprintf(stderr, "Invalid input\n");
		exit(1);
	}

	// Initialize Winsock
	rv = WSAStartup(MAKEWORD(2, 2), &wsaData);
	if (rv != 0) {
		printf("WSAStartup failed with error: %d\n", rv);
		return 1;
	}

	// load address information
	ZeroMemory(&hints, sizeof(hints));
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_protocol = IPPROTO_TCP;
	if ((rv = getaddrinfo(argv[1], argv[2], &hints, &servinfo)) != 0) {
		//fprintf(stderr, "getaddrinfo failed with error: %s\n", gai_strerror(rv));
		fprintf(stderr, "getaddrinfo failed with error: %d\n", WSAGetLastError());
		WSACleanup();
		return 1;
	}

	// loop through all the results and connect to the first we can
	for (p = servinfo; p != NULL; p = p->ai_next) {
		if ((sockfd = socket(p->ai_family, p->ai_socktype,
			p->ai_protocol)) == -1) {
			fprintf(stderr, "%s: %d\n",
				"client: socket", WSAGetLastError());
			continue;
		}
		if ((rv = connect(sockfd, p->ai_addr, p->ai_addrlen)) == -1) {
			closesocket(sockfd);
			fprintf(stderr, "%s: %d\n", "client: connect", WSAGetLastError());
			continue;
		}
		break;
	}
	if (p == NULL) {
		fprintf(stderr, "client: failed to connect\n");
		WSACleanup();
		return 2;
	}

	inet_ntop(p->ai_family, get_in_addr((struct sockaddr *)p->ai_addr),
		s, sizeof s);
	printf("client: connecting to %s\n", s);
	freeaddrinfo(servinfo); // all done with this structure

	char dummy[MAXDATASIZE];// receive notification from server
	// send request to server
	if (argc == 6) { // register address and location
		// send number of messages to server
		str = "3";
		char *msg = new char[str.size() + 1]; // store input
		msg[str.size()] = 0;
		memcpy(msg, str.c_str(), str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) 
			fprintf(stderr, "%s: %d\n", "send", WSAGetLastError());
		recv(sockfd, dummy, MAXDATASIZE - 1, 0); // receive confirmation from server

		// send latitude
		str = string(argv[3]);
		msg = new char[str.size() + 1]; // store input
		msg[str.size()] = 0;
		memcpy(msg, str.c_str(), str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) 
			fprintf(stderr, "%s: %d\n", "send", WSAGetLastError());
		recv(sockfd, dummy, MAXDATASIZE - 1, 0); // receive confirmation from server

		// send longtitude
		str = string(argv[4]);
		msg = new char[str.size() + 1]; // store input
		msg[str.size()] = 0;
		memcpy(msg, str.c_str(), str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) 
			fprintf(stderr, "%s: %d\n", "send", WSAGetLastError());
		recv(sockfd, dummy, MAXDATASIZE - 1, 0); // receive confirmation from server

		// send address
		str = string(argv[5]);
		msg = new char[str.size() + 1]; // store input
		msg[str.size()] = 0;
		memcpy(msg, str.c_str(), str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) 
			fprintf(stderr, "%s: %d\n", "send", WSAGetLastError());
	}
	else if (argc == 5) { // get closest address from server
		// send number of messages to server
		str = "2";
		char *msg = new char[str.size() + 1]; // store input
		msg[str.size()] = 0;
		memcpy(msg, str.c_str(), str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) 
			fprintf(stderr, "%s: %d\n", "send", WSAGetLastError());
		if (recv(sockfd, dummy, MAXDATASIZE - 1, 0) > 6) {
			printf("%s", dummy); // error msg from server: no registered addresses
			exit(1);
		}

		// send latitude
		str = string(argv[3]);
		msg = new char[str.size() + 1]; // store input
		msg[str.size()] = 0;
		memcpy(msg, str.c_str(), str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) 
			fprintf(stderr, "%s: %d\n", "send", WSAGetLastError());
		recv(sockfd, dummy, MAXDATASIZE - 1, 0); // receive confirmation from server

		// send longtitude
		str = string(argv[4]);
		msg = new char[str.size() + 1]; // store input
		msg[str.size()] = 0;
		memcpy(msg, str.c_str(), str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) 
			fprintf(stderr, "%s: %d\n", "send", WSAGetLastError());
	}
	else if (argc == 4) { // get location for an address
		// send number of messages to server
		str = "1";
		char *msg = new char[str.size() + 1]; // store input
		msg[str.size()] = 0;
		memcpy(msg, str.c_str(), str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) 
			fprintf(stderr, "%s: %d\n", "send", WSAGetLastError());
		recv(sockfd, dummy, MAXDATASIZE - 1, 0);

		// send address
		str = string(argv[3]);
		msg = new char[str.size() + 1]; // store input
		msg[str.size()] = 0;
		memcpy(msg, str.c_str(), str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) 
			fprintf(stderr, "%s: %d\n", "send", WSAGetLastError());
	}

	// receive msg from server
	if ((numbytes = recv(sockfd, buf, MAXDATASIZE - 1, 0)) == -1) {
		fprintf(stderr, "%s: %d\n", "recv", WSAGetLastError());
		exit(1);
	}

	buf[numbytes] = '\0';
	printf("client received: '%s'\n", buf);
	closesocket(sockfd);
	WSACleanup();

	return 0;
}

