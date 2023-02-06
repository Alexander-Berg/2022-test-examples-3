#
# sssp_utils.py Some utility functions used by the SSSP python sampl apps.
#
# Copyright (c) 2015 Sophos Limited, www.sophos.com.
#


import re


# Regular Expressions defining some messages from the server.

acceptsyntax = re.compile ("^ACC\s+(.*?)\s*$")
optionsyntax = re.compile ("^(\w+):\s*(.*?)\s*$")
virussyntax = re.compile ("^VIRUS\s+(\S+)\s+(.*)")
typesyntax = re.compile ("^TYPE\s+(\w+)")
donesyntax = re.compile ("^DONE\s+(\w+)\s+(\w+)\s+(.*?)\s*$")
eventsyntax = re.compile ("^([A-Z]+)\s+(\w+)")


# Receives a line of text from the socket
# \r chars are discarded
# The line is terminated by a \n
# NUL chars indicate a broken socket

def receiveline(s):
    line = ''
    done  = 0
    while (not done):
        c = s.recv(1)
        if (c == ''):
            return ''
        done = (c == '\n')
        if (not done and c != '\r'):
            line = line + c

    return line


# Receives a whole message. Messages are terminated by 
# a blank line

def receivemsg(s):

    response = []
    finished = 0

    while not finished:
        msg = receiveline(s)
        finished = (len(msg) == 0)
        if not finished:
            response.append (msg)

    return response


# Receives the ACC message which is a single line
# conforming to the acceptsyntax RE.

def accepted(s):
    acc = receiveline(s)
    return acceptsyntax.match(acc)


# Reads a message which should be a list of options
# and transforms them into a dictionary

def readoptions(s):
    resp = receivemsg(s)
    opts ={}

    for l in resp:
        parts = optionsyntax.findall(l)
        for p in parts:
            if not opts.has_key(p[0]):
                opts[p[0]] = []

            opts[p[0]].append(p[1])

    return opts


# Performs the initial exchange of messages.

def exchangeGreetings(s):

    line = receiveline (s)

    if (not line.startswith ('OK SSSP/1.0')):
        return 0

    s.send ('SSSP/1.0\n')

    if not accepted(s):
        print "Greeting Rejected!!"
        return 0

    return 1


# performs the final exchange of messages

def sayGoodbye (s):
    s.send('BYE\n')
    receiveline(s)

