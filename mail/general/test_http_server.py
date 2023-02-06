#!/usr/bin/env python3
# encoding: utf-8
# kate: space-indent on; indent-width 4; replace-tabs on;
#
import sys, argparse
from http.server import HTTPServer, BaseHTTPRequestHandler
from io import BytesIO


class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'Hello, world!')

    def do_POST(self):
        content_length = int(self.headers['Content-Length']) if 'Content-Length' in self.headers else 0
        body = self.rfile.read(content_length)
        self.send_response(200)
        self.end_headers()
        response = BytesIO()
        response.write(body)
        self.wfile.write(response.getvalue())


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', type = str, help = "Host")
    parser.add_argument('--port', type = int, help = "Port")
    args = parser.parse_known_args()[0]
    host = args.host if args.host else 'localhost'
    port = args.port if args.port else 8000

    httpd = HTTPServer((host, port), SimpleHTTPRequestHandler)
    httpd.serve_forever()
