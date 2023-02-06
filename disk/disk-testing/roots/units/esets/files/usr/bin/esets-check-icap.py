#!/usr/bin/env python
import socket
import sys
import re

HOST = '127.0.0.1'
SERVICE = 'icap://127.0.0.1:1334/esets_icap'
PORT = 1344

def result(status, info):
    print "PASSIVE-CHECK:esets;"+str(status)+";"+info
    sys.exit(status)

try:
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
except socket.error, msg:
    result(2, "Error: "+msg[1])

try:
    sock.connect((HOST, PORT))
except socket.error, msg:
    result(2, "Error: "+msg[1])

sock.send("OPTIONS %s ICAP/1.0\r\n" % (SERVICE))
sock.send("\r\n<NULL>")

data = sock.recv(1024)
string = ""

while len(data):
    string = string + data
    if '\r\n\r\n' in string:
        break
    data = sock.recv(1024)

sock.close()

if re.search("200 OK", string):
    result(0, "OK")
else:
    result(2, "something is wrong with esets")
