import threading
from socket import AF_INET6
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse
import cgi
import json


class NwMock(HTTPServer):
    address_family = AF_INET6

    class Handler(BaseHTTPRequestHandler):
        mid = 100

        def do_POST(self):
            self._process_query()

        def _process_query(self):
            self.params = self._extract_params()

            if self.path.startswith("/mail/store"):
                self._store_fid()
                self._store_message()
                self._send_store_response()
            else:
                self._send_response_text(500, "not supported")

        def _extract_params(self):
            req = urlparse(self.path)
            params = (
                dict(param.split("=") for param in req.query.split("&"))
                if len(req.query) > 0
                else {}
            )
            return params

        def _store_fid(self):
            self.server.fids.append(self.params["fid"])

        def _store_message(self):
            fields = self._extract_request_fields()
            labels = self._extract_labels(fields)
            message = self._extract_message_body(fields)
            self._store_labels(labels)
            self._store_message_body(message)
            self.mid = self._next_mid()

        def _extract_request_fields(self):
            _, pdict = cgi.parse_header(self.headers["content-type"])
            pdict["boundary"] = bytes(pdict["boundary"], "utf-8")
            fields = cgi.parse_multipart(self.rfile, pdict)
            return fields

        def _extract_labels(self, fields):
            labels = fields[None][0]
            labels = json.loads(labels)["mail_info"]["labels"]
            return labels

        def _extract_message_body(self, fields):
            message_field = fields[None][-1]
            message_body = message_field.split("@imap.yandex.ru")[-1].strip()
            return message_body

        def _store_labels(self, labels):
            self.server.labels.append(labels)

        def _store_message_body(self, message):
            self.server.messages.append(message)

        def _next_mid(self):
            return self.mid + 1

        def _send_store_response(self):
            message = '{"mid" : ' + str(self.mid) + "}"
            self._send_response_text(200, message)

        def _send_response_text(self, code, text):
            body = text.encode("utf-8")
            self.send_response(code)
            self.send_header("Content-Length", len(body))
            self.send_header("Content-type", "text/html")
            self.end_headers()
            self.wfile.write(body)

    def __init__(self, port):
        HTTPServer.__init__(self, ("localhost", port), NwMock.Handler)

        self.params = dict()

        self.fids = []
        self.labels = []
        self.messages = []

    def start(self):
        threading.Thread(target=self.serve_forever).start()
