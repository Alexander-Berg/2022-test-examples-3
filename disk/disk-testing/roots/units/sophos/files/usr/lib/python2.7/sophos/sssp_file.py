#!/usr/bin/python

# Scans a file via SAVID/SSSP 
# Always sends the file using SCANDATA

# Copyright (c) 2015 Sophos Limited, www.sophos.com.


import os
from os.path import *
import re
import socket
import sys

from sssp_utils import *


# Define the server

server = 'localhost'
port = 4010

# and connect to it

try:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
    s.connect ((server, port))
except:
    print "Can't connect"
    sys.exit(1)


try:
    # Do the initial exchange

    if not exchangeGreetings(s):
        print "Greetings rejected!!"
        sys.exit(1)


    # Query the server to see what we can do

    s.send ('QUERY SERVER\n')
    if not accepted(s):
        print "QUERY Rejected!!"
        sys.exit(1)

    resp = receivemsg(s)

    scandata = 0
    
    for l in resp:
        parts = optionsyntax.findall(l)
        if (parts[0][0] == 'method' and parts[0][1] == 'SCANDATA'):
            print "SCANDATA is allowed"
            scandata = 1

    if not scandata:
        print "SCANDATA is not allowed!"
        sys.exit(1)


    # For each file on the command line use the most permissive
    # request that we can. though using SCANDIRR isn't always the
    # best idea. 

    for filename in sys.argv[1:]:

        print filename

        #Send the scan request according to what we are allowed

        if not exists (filename) or not isfile(filename):
            print "No such file as " + filename
            sys.exit(1)
        else:
            filesize = os.stat (filename)[6]
            s.send ('SCANDATA ' + str(filesize) + '\n')
            if not accepted(s):
                print "SCANDATA Rejected!!"
                sys.exit(1)

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
        for l in resp:
            if virussyntax.match(l):
                parts = virussyntax.findall(l)
                print "Virus: " + parts[0][0] + " in " + parts[0][1] 
            elif donesyntax.match(l):
                parts = donesyntax.findall(l)
                print parts[0][0] + " (" + parts[0][1] + ") " + parts[0][2]
    
    # And bid a fond farewell

    sayGoodbye(s)

finally:
    s.close();

sys.exit(0)

