#!/usr/bin/env python

from pycommon import *
import socket
import wsgiref
import datetime
import sys
import msgpack
import threading
import functools
import time

class Subscriber:
    def __init__(self, port):
        self.impl = fake_server(host="::", port=port, start=False)
        self.url = 'http://' + socket.gethostname() + ':' + str(port) + '/'
        self.messages = []
        self.requests = []

    def set_response(self, code=200, response=None, raw_response=None):
        self.impl.response.code = code
        self.impl.set_response(response, raw_response)

    def start(self):
        self.impl.set_request_hook(self.handle_request)
        self.impl.start();

    def stop(self):
        self.impl.fini();

    def handle_request(self, req):
        self.messages.append(msgpack.unpackb(req.body))
        self.requests.append(req)


class FakeXivaMobile:
    def __init__(self, port):
        self.impl = fake_server(host="::", port=port, start=False)
        self.url = 'http://' + socket.gethostname() + ':' + str(port) + '/'
        self.requests = []
        self.bodies = []

    def start(self):
        self.impl.set_request_hook(self.handle_request)
        self.impl.async_serve_forever();

    def stop(self):
        self.impl.fini();

    def handle_request(self, req):
        self.requests.append(req)
        self.bodies.append(req.body)

if __name__ == "__main__":
    d = ()
    try:
        port = sys.argv[1]
        port = int(port)
        subscriber = Subscriber(port=port)
        subscriber.set_response(raw_response="OK")
        subscriber.start()
        while True: time.sleep(100)
    except KeyboardInterrupt:
        pass

    subscriber.stop()
