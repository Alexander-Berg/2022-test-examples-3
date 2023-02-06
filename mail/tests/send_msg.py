#!/usr/bin/python
import sys
sys.path.append("include/ymod_messenger")
import optparse
import socket
import struct
import header_pb2
import random
import string
from google.protobuf import text_format

def parseArgs():
    parser = optparse.OptionParser()
    parser.add_option('-H', '--host', action="store", dest="host", help="remote host", default="localhost")
    parser.add_option('-p', '--port', action="store", dest="port", help="remote port", default="1083")
    parser.add_option('-s', '--size', action="store", dest="size", help="data size in bytes", default="2048")
    options, args = parser.parse_args()
    return (options, args, parser)

options, args, parser = parseArgs()

host = options.host
port = int(options.port)
size = int(options.size)

print "host: " + host
print "port: " + str(port)

header = header_pb2.header()
header.length = size
header_str = header.SerializeToString()
print len(header_str)

open("file.txt", "w").write(header_str)

s = socket.socket()
s.connect((host, port))

s.send(struct.pack('!I', len(header_str)))
s.send(header_str)
char_set = string.ascii_uppercase + string.digits
msg = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(size))
print "msg: ", msg
s.send(msg)


s.close
