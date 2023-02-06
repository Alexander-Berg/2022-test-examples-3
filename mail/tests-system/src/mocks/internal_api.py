import threading
import msgpack
from socket import AF_INET6
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse


class InternalApiMock(HTTPServer):
    address_family = AF_INET6

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self._process_query()

        def _process_query(self):
            self.params = self._extract_params()

            if self.path.startswith("/folders"):
                self._send_folders()

            elif self.path.startswith("/labels"):
                self._send_labels()

            elif self.path.startswith("/next_message_chunk"):
                mid = self.params["mid"]
                sent = self._send_messages_from_mid(mid)
                if sent == 0 and self.server.empty_chunk_cb:
                    self.server.empty_chunk_cb()

            elif self.path.startswith("/pop3_folders"):
                mid = self.params["mid"]
                self._send_pop3_folders(mid)

            elif self.path.startswith("/pop3_next_message_chunk"):
                self._send_pop3_messages_from_mid()

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

        def _send_folders(self):
            folders_info = tuple(tuple(x) for x in self.server.folders_info)
            self._send_response_msgpack(200, folders_info)

        def _send_pop3_folders(self):
            folders_info = tuple(tuple(x) for x in self.server.pop3_folders_info)
            self._send_response_msgpack(200, folders_info)

        def _send_labels(self):
            labels = tuple(tuple(x) for x in self.server.labels)
            self._send_response_msgpack(200, labels)

        def _send_messages_from_mid(self, mid):
            messages = self._messages_from_mid(self.server.messages, mid)
            self._send_response_msgpack(200, messages)
            return len(messages)

        def _send_pop3_messages_from_mid(self, mid):
            messages = self._messages_from_mid(self.server.pop3_messages, mid)
            self._send_response_msgpack(200, messages)

        def _messages_from_mid(self, messages, mid):
            tmp_arr = []
            mid_not_faced = True
            if int(mid) == 0:
                mid_not_faced = False

            mid_index = 0
            labels_index = 4

            for message in messages:
                if mid_not_faced:
                    if int(mid) == message[mid_index]:
                        mid_not_faced = False
                    else:
                        continue

                message[labels_index] = tuple(tuple(x) for x in message[labels_index])
                tmp_arr.append(message)

            messages = tuple(tuple(x) for x in tmp_arr)
            return messages

        def _send_response_msgpack(self, code, body):
            pack = msgpack.packb(body, use_bin_type=False)
            self._send_response_bytes(code, pack)

        def _send_response_text(self, code, body):
            body = body.encode("utf-8")
            self._send_response_bytes(code, body)

        def _send_response_bytes(self, code, body):
            self.send_response(code)
            self.send_header("Content-Length", len(body))
            self.send_header("Content-type", "text/html")
            self.end_headers()
            self.wfile.write(body)

    def __init__(self, port, folders_info, pop3_folders_info, labels, messages, pop3_messages):
        HTTPServer.__init__(self, ("", port), InternalApiMock.Handler)

        self.folders_info = folders_info
        self.pop3_folders = pop3_folders_info
        self.labels = labels
        self.messages = messages
        self.pop3_messages = pop3_messages
        self.params = dict()

        self.empty_chunk_cb = None

    def set_empty_chunk_cb(self, handler):
        self.empty_chunk_cb = handler

    def start(self):
        threading.Thread(target=self.serve_forever).start()
