import threading
from socket import AF_INET6
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse


class WebServer(HTTPServer):
    address_family = AF_INET6

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.process_query()

        def do_POST(self):
            self.process_query()

        def process_query(self):
            req = urlparse(self.path)
            params = (
                dict(param.split("=") for param in req.query.split("&"))
                if len(req.query) > 0
                else {}
            )
            self.server.queries.append(
                {"path": req.path, "params": params, "headers": self.headers}
            )
            self.send_response(204)

    def __init__(self, port):
        HTTPServer.__init__(self, ("", port), WebServer.Handler)
        self.queries = []

    def start(self):
        threading.Thread(target=self.serve_forever).start()
