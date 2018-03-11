/* A simple server in the internet domain using TCP
   The port number is passed as an argument */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include "dji_status.hpp"
#include <dji_vehicle.hpp>
#include <dji_linux_helpers.hpp>
#include <string>
#include <sstream>

using namespace DJI::OSDK;
using namespace DJI::OSDK::Telemetry;
using namespace std;


void error(const char *msg)
{
    perror(msg);
    exit(1);
}

int main(int argc, char **argv)
{

    // Initialize variables
    //int functionTimeout = 1;
       
    LinuxSetup linuxEnvironment(argc, argv);
    Vehicle*   vehicle = linuxEnvironment.getVehicle();
    // Setup OSDK.
    if (vehicle==NULL){
        error("Cannot find the drone\n");
        return -1;
    }
    // Obtain Control Authority
    //vehicle->obtainCtrlAuthority(functionTimeout);
    MobileCommunication comm(vehicle);
    const int TIMEOUT = 20;
    ACK::ErrorCode ack = vehicle->broadcast->setBroadcastFreqDefaults(TIMEOUT);
    Telemetry::GlobalPosition globalPosition;
    int sockfd, newsockfd, portno;
    socklen_t clilen;
    char buffer[2048];
    struct sockaddr_in serv_addr, cli_addr;
    int n;
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0)
        error("ERROR opening socket");
    bzero((char *) &serv_addr, sizeof(serv_addr));
    portno = 5099;
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(portno);
    if (bind(sockfd, (struct sockaddr *) &serv_addr,
            sizeof(serv_addr)) < 0)
            error("ERROR on binding");
    listen(sockfd,5);
    clilen = sizeof(cli_addr);
    while(1){
        newsockfd = accept(sockfd,
                 (struct sockaddr *) &cli_addr,
                 &clilen);
        globalPosition = vehicle->broadcast->getGlobalPosition();
        double lat = globalPosition.latitude;
        double lng = globalPosition.longitude;
        if (newsockfd < 0)
            error("ERROR on accept");
        bzero(buffer,256);
        n = read(newsockfd,buffer,2048);
        if (n < 0) error("ERROR reading from socket");
        ostringstream oss;
        oss << buffer <<lat<<"\n"<<lng<<"\n";
        string tmp = oss.str();
        strncpy(buffer,tmp.c_str(),2048);
        printf("%s\n",buffer);
        comm.sendDataToMSDK((uint8_t*) buffer,99);
        close(newsockfd);
     }
     close(sockfd);
     return 0;
}
