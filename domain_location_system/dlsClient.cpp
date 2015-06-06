#include "dlsClient.hpp"


int main(int argc, char *argv[])
{
	int sockfd, numbytes;
	char buf[MAXDATASIZE];
	struct addrinfo hints, *servinfo, *p;
	int rv;
	char s[INET6_ADDRSTRLEN];
	string str;

	// show input
	printf("You entered:\n");
	for(int i = 3; i < argc; i++)
		printf("%s  ", argv[i]);
	printf("\n");

	// check number of cmd parameters
	if (argc < 4 || argc > 6) {
		fprintf(stderr,"Invalid input\n");
		exit(1);
	}

	// load address information
	memset(&hints, 0, sizeof hints);
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;
	if ((rv = getaddrinfo(argv[1], argv[2], &hints, &servinfo)) != 0) {
		fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
		return 1;
	}

	// loop through all the results and connect to the first we can
	for(p = servinfo; p != NULL; p = p->ai_next) {
		if ((sockfd = socket(p->ai_family, p->ai_socktype,
			p->ai_protocol)) == -1) {
			perror("client: socket");
			continue;
		}
		if (connect(sockfd, p->ai_addr, p->ai_addrlen) == -1) {
			close(sockfd);
			perror("client: connect");
			continue;
		}
		break;
	}
	if (p == NULL) {
		fprintf(stderr, "client: failed to connect\n");
		return 2;
	}

	inet_ntop(p->ai_family, get_in_addr((struct sockaddr *)p->ai_addr),
	s, sizeof s);
	printf("client: connecting to %s\n", s);
	freeaddrinfo(servinfo); // all done with this structure

	char dummy[MAXDATASIZE]; // receive notification from server

	// register address and location
	if(argc == 6) {
		// send number of messages to server
		str = "3";
		char *msg = new char[str.size()+1]; // store input
		msg[str.size()]=0;
		memcpy(msg,str.c_str(),str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) perror("send");
		recv(sockfd, dummy, MAXDATASIZE - 1, 0); // receive confirmation from server

		// send latitude
		str = string(argv[3]);
		msg = new char[str.size()+1]; // store input
		msg[str.size()]=0;
		memcpy(msg,str.c_str(),str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) perror("send");
		recv(sockfd, dummy, MAXDATASIZE- 1, 0); // receive confirmation from server

		// send longtitude
		str = string(argv[4]);
		msg = new char[str.size()+1]; // store input
		msg[str.size()]=0;
		memcpy(msg,str.c_str(),str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) perror("send");
		recv(sockfd, dummy, MAXDATASIZE - 1, 0); // receive confirmation from server

		// send address
		str = string(argv[5]);
		msg = new char[str.size()+1]; // store input
		msg[str.size()]=0;
		memcpy(msg,str.c_str(),str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) perror("send");
	}

	// get closest address from server
	else if(argc == 5) {
		// send number of messages to server
		str = "2";
		char *msg = new char[str.size()+1]; // store input
		msg[str.size()]=0;
		memcpy(msg,str.c_str(),str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) perror("send");
		if(recv(sockfd, dummy, MAXDATASIZE - 1, 0) > 6) {
			printf("Client received: %s\n", dummy); // error msg from server: no registered addresses
			exit(1);
		}

		// send latitude
		str = string(argv[3]);
		msg = new char[str.size()+1]; // store input
		msg[str.size()]=0;
		memcpy(msg,str.c_str(),str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) perror("send");
		recv(sockfd, dummy, MAXDATASIZE- 1, 0); // receive confirmation from server

		// send longtitude
		str = string(argv[4]);
		msg = new char[str.size()+1]; // store input
		msg[str.size()]=0;
		memcpy(msg,str.c_str(),str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) perror("send");
	}

	// get location for an address
	else if(argc == 4) {
		// send number of messages to server
		str = "1";
		char *msg = new char[str.size()+1]; // store input
		msg[str.size()]=0;
		memcpy(msg,str.c_str(),str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) perror("send");
		recv(sockfd, dummy, MAXDATASIZE - 1, 0);

		// send address
		str = string(argv[3]);
		msg = new char[str.size()+1]; // store input
		msg[str.size()]=0;
		memcpy(msg,str.c_str(),str.size());
		if (send(sockfd, msg, strlen(msg), 0) == -1) perror("send");
	}

	// receive msg from server
	if ((numbytes = recv(sockfd, buf, MAXDATASIZE-1, 0)) == -1) {
		perror("recv");
		exit(1);
	}
	buf[numbytes] = '\0';
	printf("client received: %s\n",buf);
	close(sockfd);
	return 0;
}
