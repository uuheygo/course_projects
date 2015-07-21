#include "dlsServer.hpp"

typedef struct Place {
    char *address;
    double latitude;
	double longitude;
} Place;

int main(void)
{
	struct sockaddr_storage their_addr;
	socklen_t sin_size;
	struct sigaction sa;
	struct addrinfo hints, *servinfo, *p;
	int sockfd, new_fd, numbytes, status;
	int yes=1;
	char s[INET6_ADDRSTRLEN];
	char receiveBuff[MAXDATASIZE], sendBuff[MAXDATASIZE];
	vector<Place> v; // store address/location pairs

	// first, load up address structs with getaddrinfo():
	memset(&hints, 0, sizeof hints);
	hints.ai_family = AF_UNSPEC; // use IPv4 or IPv6, whichever
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_flags = AI_PASSIVE; // fill in my IP for me
 	if((status = getaddrinfo(NULL, "0", &hints, &servinfo)) != 0) {
 		fprintf(stderr, "getaddrinfo error: %s\n", gai_strerror(status));
		exit(1);
 	}

 	// loop through all the results and bind to the first we can
	for(p = servinfo; p != NULL; p = p->ai_next) {
		if ((sockfd = socket(p->ai_family, p->ai_socktype, p->ai_protocol)) == -1) {
			perror("server: socket");
			continue;
		}
		if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int)) == -1) {
			perror("setsockopt");
			exit(1);
		}
		if (bind(sockfd, p->ai_addr, p->ai_addrlen) == -1) {
			close(sockfd);
			perror("server: bind");
			continue;
		}
		break;
	}
	if (p == NULL) {
		fprintf(stderr, "server: failed to bind\n");
		return 2;
	}

	freeaddrinfo(servinfo); // all done with this structure

	// listen for incoming connections
	if (listen(sockfd, BACKLOG) == -1) {
		perror("listen");
		exit(1);
	}

	// print out host ip and port number
	char hostname[1024];
	char ip[100];
	hostname[1023] = '\0';
	gethostname(hostname, 1023);
	hostname_to_ip(hostname, ip);
	printf("Hostname: %s\nIP: %s\n", hostname, ip);

	struct sockaddr_in sin;
	socklen_t len = sizeof(sin);
	if (getsockname(sockfd, (struct sockaddr *)&sin, &len) == -1)
	    perror("getsockname");
	else
	    printf("port: %d\n", ntohs(sin.sin_port));

	// reap all dead processes
	sa.sa_handler = sigchld_handler;
	sigemptyset(&sa.sa_mask);
	sa.sa_flags = SA_RESTART;
	if (sigaction(SIGCHLD, &sa, NULL) == -1) {
		perror("sigaction");
		exit(1);
	}

	printf("server: waiting for connections...\n");

 	// loop to accept and handle requests
 	while(1) {
 		sin_size = sizeof their_addr;
		new_fd = accept(sockfd, (struct sockaddr *)&their_addr, &sin_size);
		if (new_fd == -1) {
			perror("accept");
			continue;
		}

		inet_ntop(their_addr.ss_family,
				get_in_addr((struct sockaddr *)&their_addr), s, sizeof s);
		printf("server: got connection from %s\n", s);

		if ((numbytes = recv(new_fd, receiveBuff, MAXDATASIZE-1, 0)) == -1) {
			perror("recv");
			continue;
		}
		receiveBuff[numbytes] = '\0';

		char *dummy = new char[10]; // dummy used to send notification
		string temp = "dummy";
		strcpy(dummy, temp.c_str());

		// register address and location
		if(receiveBuff[0] == '3') {
			send(new_fd, dummy, strlen(dummy), 0);// notify the client
			// receive latitude
			numbytes = recv(new_fd, receiveBuff, MAXDATASIZE-1, 0);
			receiveBuff[numbytes] = '\0';
			send(new_fd, dummy, strlen(dummy), 0);// notify the client
			double lat = atof(receiveBuff);
			// receive longitude
			numbytes = recv(new_fd, receiveBuff, MAXDATASIZE-1, 0);
			send(new_fd, dummy, strlen(dummy), 0);// notify the client
			receiveBuff[numbytes] = '\0';
			double lon = atof(receiveBuff);
			// receive address
			numbytes = recv(new_fd, receiveBuff, MAXDATASIZE-1, 0);
			receiveBuff[numbytes] = '\0';
			char *addr = new char[strlen(receiveBuff)+1];
			strcpy(addr, receiveBuff);

			// reject address that is already registered
			int isRegistered = 0;
			for(std::vector<Place>::iterator it = v.begin(); it != v.end(); it++) {
				if(strcmp(addr, it->address) == 0) {
					// found duplicate address
					isRegistered = 1;
					string s = "Address already Registered";
					char *msg = new char[s.size()+1];
					msg[s.size()]=0;
					memcpy(msg,s.c_str(),s.size());
					send(new_fd, msg, strlen(msg), 0);
					break;
				}
			}
			if(isRegistered) continue;

			// reject invalid location
			if(lat < -180 || lat >180 || lon < -180 || lon > 180) {
				string s = "Invalid location";
				char *msg = new char[s.size()+1];
				msg[s.size()]=0;
				memcpy(msg,s.c_str(),s.size());
				send(new_fd, msg, strlen(msg), 0);
				continue;
			}

			// create an entry and add new address to vector
			Place newPlace = {addr, lat, lon};
			v.push_back(newPlace);
			printf("New address registered: %s (%f, %f)\n", newPlace.address, newPlace.latitude, newPlace.longitude);

			// send confirm msg back to client
			string s = "New Address Registered";
			char *msg = new char[s.size()+1];
			msg[s.size()]=0;
			memcpy(msg,s.c_str(),s.size());
			send(new_fd, msg, strlen(msg), 0);
		}

		//find closest address
		else if(receiveBuff[0] == '2') {
			// No registered addresses yet
			if(v.size() == 0) {
				string s = "No registered addresses";
				char *msg = new char[s.size()+1];
				msg[s.size()]=0;
				memcpy(msg,s.c_str(),s.size());
				if (send(new_fd, msg, strlen(msg), 0) == -1) perror("send");
				continue;
			}

			send(new_fd, dummy, strlen(dummy), 0);// notify the client
			// receive latitude
			numbytes = recv(new_fd, receiveBuff, MAXDATASIZE-1, 0);
			send(new_fd, dummy, strlen(dummy), 0);// notify the client
			receiveBuff[numbytes] = '\0';
			double lat = atof(receiveBuff);
			// receive longitude
			numbytes = recv(new_fd, receiveBuff, MAXDATASIZE-1, 0);
			receiveBuff[numbytes] = '\0';
			double lon = atof(receiveBuff);

			// reject invalid location
			if(lat < -180 || lat >180 || lon < -180 || lon > 180) {
				string s = "Invalid location";
				char *msg = new char[s.size()+1];
				msg[s.size()]=0;
				memcpy(msg,s.c_str(),s.size());
				send(new_fd, msg, strlen(msg), 0);
				continue;
			}

			// find the closet place and send its address to client
			char *closest = v[0].address;
			double min = distance(v[0].latitude, v[0].longitude, lat, lon);
			for(std::vector<Place>::size_type i = 1; i != v.size(); i++) {
				double cur = distance(v[i].latitude, v[i].longitude, lat, lon);
				if(cur < min) {
					min = cur;
					closest = v[i].address;
				}
			}

			string temp = "Closest address is: " + string(closest);
			char *t = new char[temp.size() + 1];
			t[temp.size()]=0;
			memcpy(t,temp.c_str(),temp.size());
			printf("Closest address to (%f, %f) is: %s\n", lat, lon, closest);
			if(send(new_fd, t, strlen(t), 0) == -1) perror("send");
		}

		// get location of an address
		else if(receiveBuff[0] == '1') {
			send(new_fd, dummy, strlen(dummy), 0);// notify the client
			numbytes = recv(new_fd, receiveBuff, MAXDATASIZE-1, 0);
			receiveBuff[numbytes] = '\0';

			int mark = 0; // mark whether the address is registered
			for(std::vector<Place>::iterator it = v.begin(); it != v.end(); it++) {
				if(strcmp(receiveBuff, it->address) == 0) { // address found
					mark = 1;
					char *lat = new char[100];
					char *lon = new char[100];

					if(it->latitude >= 0) {
						string str = "N" + stringify(it->latitude);
						strcpy(lat, str.c_str());
					}
					else {
						string str = "S" + stringify(-(it->latitude));
						strcpy(lat, str.c_str());
					}
					if(it->longitude >= 0) {
						string str = "E" + stringify(it->longitude);
						strcpy(lon, str.c_str());
					}
					else {
						string str = "W" + stringify(-(it->longitude));
						strcpy(lon, str.c_str());;
					}

					string s = "location is " + string(lat) + ", " + string(lon);
					char *location = new char[s.size()+1];
					location[s.size()]=0;
					memcpy(location,s.c_str(),s.size());
					printf("Location of %s is (%s, %s)\n", receiveBuff, lat, lon);
					send(new_fd, location, strlen(location), 0);
					break;
				}
			}

			if(mark == 0) { // address not found
				string s = "Address not registered";
				char *msg = new char[s.size()+1];
				msg[s.size()]=0;
				memcpy(msg,s.c_str(),s.size());
				send(new_fd, msg, strlen(msg), 0);
			}
		}

		close(new_fd);
 	}

 	return 0;
}
