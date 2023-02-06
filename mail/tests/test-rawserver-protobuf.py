#!/usr/bin/python

import sys
sys.path.append("include/xiva_proto")
import socket
import event_pb2
import request_pb2
import time
from google.protobuf import text_format

s = socket.socket()
host = socket.gethostname() 
port = 18087  

s.connect((host, port))

req = request_pb2.request()
req.uid="108691181"
req.action = request_pb2.request.SUBSCRIBE

str = req.SerializeToString()
s.send(str)

while 1:
    data = s.recv(2048)
    if data:
        event = event_pb2.event()
        event.ParseFromString(data)
        sys.stdout.write('event received: ' + text_format.MessageToString(event))
        #print event.text

s.close
