#!/usr/bin/env python
import sys, os, uuid
import os.path
from time import gmtime, strftime

if __name__ == '__main__':
    username = sys.argv[1]
    ymd = strftime('%Y-%m-%d', gmtime())
    fname = str(uuid.uuid4())

    if not os.path.isdir(username):
        try:
            os.mkdir(username)
        except OSError:
            pass

    if not os.path.isdir(os.path.join(username, ymd)):
        try:
            os.mkdir(os.path.join(username, ymd))
        except OSError:
            pass

    f = open(os.path.join(username, ymd, fname), 'w')
    for line in sys.stdin:
        f.write(line)
    f.flush()
    f.close()
