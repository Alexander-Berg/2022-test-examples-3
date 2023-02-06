#!/usr/bin/env python3

from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from threading import Thread
from urllib.parse import parse_qsl
import argparse
import json
import requests
import re
import time
import random


class Handler(SimpleHTTPRequestHandler):
    def send_response_text(self, code, body):
        self.send_response(code)
        self.send_header("Content-Length", len(body))
        self.send_header("Content-type", "text/html")
        self.end_headers()
        self.wfile.write(body)

    def send_telegram_response(self, result, ok=True):
        body = json.dumps({"ok": ok, "result": result}).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Length", len(body))
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(body)

    def parse_form_urlencoded(self):
        body = self.rfile.read(int(self.headers["content-length"]))
        return dict(parse_qsl(body.decode("utf-8")))

    def do_GET(self):
        if self.path == "/ping":
            self.send_response_text(200, b"pong")
        else:
            self.send_response_text(500, b"unimplemented")

    def do_POST(self):
        send_message_re = r"^/bot([a-zA-Z0-9_]+)/sendmessage$"

        if re.search(send_message_re, self.path.lower()):
            api_token = re.search(send_message_re, self.path.lower()).group(1)
            params = self.parse_form_urlencoded()
            self.server.send_message_requests.append({"api_token": api_token, **params})
            self.send_telegram_response(params["text"])
        else:
            self.send_response_text(500, b"unimplemented")


class Webhook:
    def __init__(self, bind_path):
        self.bind_path = bind_path

    def send_update(self, data):
        headers = {"Content-Type": "application/json", "Cache-Control": "no-cache"}
        return requests.post(self.bind_path, headers=headers, data=data.encode("utf-8"))

    def send_update_text(self, chat_id, text, update_id=None):

        data = {
            "update_id": update_id if update_id is not None else random.randint(10000, 100000),
            "message": {
                "date": 1441645532,
                "chat": {
                    "last_name": "Test Lastname",
                    "id": chat_id,
                    "type": "private",
                    "first_name": "Test Firstname",
                    "username": "Testusername",
                },
                "message_id": 1365,
                "from": {
                    "last_name": "Test Lastname",
                    "id": chat_id,
                    "first_name": "Test Firstname",
                    "username": "Testusername",
                },
                "text": text,
            },
        }
        return self.send_update(json.dumps(data))


class FakeTelegram(ThreadingHTTPServer, Webhook):
    def __init__(self, host, port, bind_path):
        self.send_message_requests = []
        ThreadingHTTPServer.__init__(self, (host, port), Handler)
        Webhook.__init__(self, bind_path)

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
    argparser.add_argument("bind_path", help="bind path", type=str)
    args = argparser.parse_args()

    fake = FakeTelegram(args.host, args.port, args.bind_path)
    fake.start()

    while True:
        time.sleep(1)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        pass
