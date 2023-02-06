#!/usr/bin/env python3

from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from json import dumps
from threading import Thread
import argparse
import time


class Handler(SimpleHTTPRequestHandler):
    def send_response_text(self, code, text):
        body = text.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Length", len(body))
        self.send_header("Content-type", "text/html")
        self.end_headers()
        self.wfile.write(body)

    def send_response_json(self, code, json):
        body = dumps(json).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Length", len(body))
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/ping":
            self.send_response_text(200, "pong")
        elif self.path.startswith("/blackbox"):
            self.send_response_json(self.server.response_code, self.server.response_json)
        else:
            self.send_response_text(500, "unimplemented")


class FakeBlackbox(ThreadingHTTPServer):
    def __init__(self, host, port):
        self.response_code = 200
        self.response_json = {}
        ThreadingHTTPServer.__init__(self, (host, port), Handler)

    def set_response_error(self, err):
        self.response_code = 200
        self.response_json = {"error": err}

    def set_response(self, *uids):
        self.response_code = 200
        self.response_json = {"users": []}
        for uid in uids:
            self.response_json["users"].append({"uid": {"value": uid}})

    def start(self):
        self.thread = Thread(target=self.serve_forever)
        self.thread.daemon = True
        self.thread.start()

    def fini(self):
        self.shutdown()
        self.server_close()
        self.thread.join()


def main():
    argparser = argparse.ArgumentParser()
    argparser.add_argument("host", help="host", type=str)
    argparser.add_argument("port", help="port", type=int)
    args = argparser.parse_args()

    fake = FakeBlackbox(args.host, args.port)
    fake.start()

    while True:
        time.sleep(1)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        pass
