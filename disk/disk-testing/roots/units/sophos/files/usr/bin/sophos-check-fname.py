#!/usr/bin/python

# Scans a file via SAVID/SSSP 
# Uses QUERY to decided which SCAN request to use

# Copyright (c) 2015 Sophos Limited, www.sophos.com.


import os
import re
import socket
import sys

from sophos.sssp_utils import *


# Define exit code for uploader, default is 'unknown'

rcode = 2

# Define the server

server = 'localhost'
port = 4010

# and connect to it

try:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
    s.connect ((server, port))
except:
    print "Can't connect"
    sys.exit(rcode)


try:
    # Do the initial exchange

    if not exchangeGreetings(s):
        print "Greetings rejected!!"
        sys.exit(rcode)


    # Query the server to see what we can do

    s.send ('QUERY SERVER\n')
    if not accepted(s):
        print "QUERY Rejected!!"
        sys.exit(rcode)

    resp = receivemsg(s)

    scanfile = 0
    scandata = 0
    scandir = 0
    scandirr = 0
    
    for l in resp:
        parts = optionsyntax.findall(l)
        if (parts[0][0] == 'method' and parts[0][1] == 'SCANDATA'):
            #print "SCANDATA is allowed"
            scandata = 1
        elif(parts[0][0] == 'method' and parts[0][1] == 'SCANFILE'):
            #print "SCANFILE is allowed"
            scanfile = 1
        elif(parts[0][0] == 'method' and parts[0][1] == 'SCANDIR'):
            #print "SCANDIR is allowed"
            scandir = 1
        elif(parts[0][0] == 'method' and parts[0][1] == 'SCANDIRR'):
            #print "SCANDIRR is allowed"
            scandirr = 1

    if not scandata and not scanfile and not scandir:
        print "Nothing is allowed!"
        sys.exit(rcode)


    # For each file on the command line use the most permissive
    # request that we can. though using SCANDIRR isn't always the
    # best idea. 

    for filename in sys.argv[1:]:

        #print filename

        #Send the scan request according to what we are allowed

        if scandirr:
            s.send ('SCANDIRR ' + filename + '\n')
            if not accepted(s):
                print "SCANDIRR Rejected!!"
                sys.exit(rcode)
        elif scandir:
            s.send ('SCANDIR ' + filename + '\n')
            if not accepted(s):
                print "SCANDIR Rejected!!"
                sys.exit(rcode)
        elif scanfile:
            s.send ('SCANFILE ' + filename + '\n')
            if not accepted(s):
                print "SCANFILE Rejected!!"
                sys.exit(rcode)
        elif scandata:
            if not exists (filename) or not isfile(filename):
                print "No such file as " + filename
                sys.exit(rcode)
            else:
                filesize = os.stat (filename)[6]
                s.send ('SCANDATA ' + str(filesize) + '\n')
                if not accepted(s):
                    print "SCANDATA Rejected!!"
                    sys.exit(rcode)

                thefile = open (filename)
                while 1:
                    b = thefile.read(4096)
                    if len(b) == 0:
                        break;
                    s.send (b)
                thefile.close()


        #Analyse the response to the scan request
        # looking for VIRUS and DONE statements.

        resp = receivemsg(s)
#	print "%s\n\n" % resp
        for l in resp:
            if virussyntax.match(l):
                parts = virussyntax.findall(l)
                print "Virus: " + parts[0][0] + " in " + parts[0][1] 
		rcode = 1
            elif donesyntax.match(l):
                parts = donesyntax.findall(l)
                print parts[0][0] + " (" + parts[0][1] + ") " + parts[0][2]
		if parts[0][0] == "OK" and rcode != 1:
                    rcode = 0
                  
    
    # And bid a fond farewell

    sayGoodbye(s)

finally:
    s.close();

sys.exit(rcode)

