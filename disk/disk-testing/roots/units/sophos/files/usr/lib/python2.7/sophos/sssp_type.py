#!/usr/bin/python

# Demonstrates using savid/SSSP to retrieve the classification
# of a file

# Copyright (c) 2015 Sophos Limited, www.sophos.com.


import os
from os.path import *
import re
import socket
import sys
from sssp_utils import *


try:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
    s.connect (('localhost', 4010))

except:
    print "Can't connect"
    sys.exit(1)

try:
    # Read the welcome message

    if not exchangeGreetings(s):
        print "Greetings rejected!!"
        sys.exit(1)


    # QUERY to discover the maxclassificationsize

    s.send ('QUERY SERVER\n')

    if not accepted(s):
        print "QUERY rejected!!"
        sys.exit(1)

    options = readoptions(s)
    if options.has_key('maxclassificationsize'):
        maxclassification = int (options['maxclassificationsize'][0])
    else:
        maxclassification = 4096



    # Set the options for classification

    s.send ('OPTIONS\nreport:classification\nsavists: storagedetonly 1\nsavigrp: GrpArchiveUnpack 1\n\n');

    if not accepted(s):
        print "Options rejected!!"
        sys.exit(1)

    resp = receivemsg(s)
    for l in resp:
        if donesyntax.match(l):
            parts = donesyntax.findall(l)
            if parts[0][0] != 'OK':
                print "OPTIONS failed"
                sys.exit(1)
            break


    # TRy to classify each file in the command line

    for filename in sys.argv[1:]:

        print filename

        if not exists (filename) or not isfile (filename):
            print "No such file as " + filename
        else:
            filesize = os.stat (filename)[6]
            thefile = open (filename)

            b = thefile.read(maxclassification)
            thefile.close()

            if len(b) == 0:
                print "The file is empty or it is not readable"
                sys.exit(1)

            # Send the SCAN request

            s.send ('SCANDATA ' + str(len(b)) + '\n')
            if not accepted(s):
                print "Rejected!!"
                sys.exit(1)

            s.send (b)

            # and read the result
            events = receivemsg(s)

            for t in events:
                if typesyntax.match(t):
                    parts = typesyntax.findall(t)
                    print "Type: " + parts[0]
    
    sayGoodbye(s)

finally:
    s.close()

