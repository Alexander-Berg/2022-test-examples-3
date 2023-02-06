#!/usr/bin/python

# Scans a file given on the command line
# It determines what methods are allowed it using QUERY
# and uses the best of them to do the SCAN
# It uses the XML form of output

# Copyright (c) 2015 Sophos Limited, www.sophos.com.


import os
from os.path import *
import re
import socket
import sys
from xml.dom import minidom

from sssp_utils import *


# Define the server

server = 'localhost'
port = 4010


# Function to receive a some XML
# Lines are read until a blank line is received
# whereupon it does an XML parse.

def receivexml(s):

    # Not entirely necessary, but for niceness
    response = '<?xml version="1.0" encoding="UTF-8"?>'

    incomplete = 1

    while incomplete:
        msg = receiveline(s)
        incomplete = (len(msg) != 0)
        if incomplete:
            response = response + msg
    dom = minidom.parseString(response)

    return dom

        
# Connect to the server

try:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
    s.connect ((server, port))
except:
    print "Can't connect to "+server+" on "+port
    sys.exit(1)


# Do the initial exchange

try:
    if not exchangeGreetings(s):
        print "Greetings rejected!!"
        sys.exit(1)


    # Set the options we want:
    # output: xml to get results in XML form
    # savigrp: GrpArchiveUnpack is useful and as demonstration of
    # the client setting SAVI options.
 
    s.send ('OPTIONS\noutput: xml\nsavigrp: GrpArchiveUnpack 1\n\n')
    if not accepted(s):
        print "OPTIONS Rejected!!"
        sys.exit(1)

    resp = receivexml(s)

    # and assume it went OK...

    resp.unlink()


    # Use QUERY to find out what requests I'm allowed to make

    s.send ('QUERY\n')
    if not accepted(s):
        print "QUERY Rejected!!"
        sys.exit(1)

    resp = receivexml(s)

    methods = resp.getElementsByTagName("method")

    scandata = 0
    scanfile = 0
    scandir = 0

    # NB could also check for SCANDIRR but I'm not interested
    # in that option here.

    for m in methods:
        mn = m.childNodes[0].data
        if (mn == 'SCANDATA'):
            scandata = 1
            print "SCANDATA is allowed"
        elif(mn == 'SCANFILE'):
            scanfile = 1
            print "SCANFILE is allowed"
        elif(mn == 'SCANDIR'):
            scandir = 1
            print "SCANDIR is allowed"

    resp.unlink()

    if not scandata and not scanfile and not scandir:
        print "Nothing is allowed"
        sys.exit(1)

    # For each filename in the command line request a scan
    # using the most favourable allowed request.
    # NB SCANDIR allows files to be scanned as well as whole directories

    for filename in sys.argv[1:]:
        print filename
        #Send the scan request according to what we are allowed

        if scandir:
            s.send ('SCANDIR ' + filename + '\n')
            if not accepted(s):
                print "SCANDIR Rejected!!"
                sys.exit(1)
        elif scanfile:
            s.send ('SCANFILE ' + filename + '\n')
            if not accepted(s):
                print "SCANFILE Rejected!!"
                sys.exit(1)
        elif scandata:
            if not exists (filename) or not isfile(filename):
                print "No such file as " + sys.argv[1]
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

        resp = receivexml(s)
        #print (resp.toprettyxml())

        # Go through the sweepresults elements and print
        # the virus name etc.
    
        viruses = resp.getElementsByTagName("sweepresult")
        for v in viruses:
            #print (v.toprettyxml())
            name = v.getElementsByTagName("name")[0].childNodes[0].data
            loc = v.getElementsByTagName("location")[0].childNodes[0].data
            disinfectable = v.getElementsByTagName("disinfectable")[0].childNodes[0].data

            # For reasons less than clear to me, the encodes are needed
            # when dealing with non-ASCI text. burble, burble...

            print "Virus: " + name.encode("utf-8") + " in " + loc.encode("utf-8") + " Disinfectable? " + disinfectable.encode("utf-8")


        # There should really be only the one 'done' element
    
        done = resp.getElementsByTagName("done")
        for d in done:
            result = d.attributes["result"]
            text = d.getElementsByTagName("text")[0].childNodes[0].data
            print "Done: " + result.value + " " + text
            
        resp.unlink()

finally:
    # Say goodbye and close the connection

    sayGoodbye(s)
    s.close()

sys.exit(0)

