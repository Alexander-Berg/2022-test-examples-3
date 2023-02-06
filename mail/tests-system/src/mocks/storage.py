import threading
from socket import AF_INET6
from http.server import BaseHTTPRequestHandler, HTTPServer


class StorageMock(HTTPServer):
    address_family = AF_INET6

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self._process_query()

        def _process_query(self):
            if self.path.startswith("/gate/get/"):
                stid = self._get_stid(self.path)
                self._send_message_with_stid(stid)
            else:
                self._send_response_text(500, "not supported")

        def _get_stid(self, request):
            return request.split("/")[-1]

        def _send_message_with_stid(self, stid):
            message = self._find_message_by_stid(stid)
            if len(message) > 0:
                self._send_response_text(200, message)
            else:
                self._send_response_text(500, "not existing message with stid=" + stid)

        def _find_message_by_stid(self, stid):
            for i in range(len(self.server.messages)):
                if stid in self.server.messages_info[i]:
                    return self.server.messages[i]
            return ""

        def _send_response_text(self, code, text):
            body = text.encode("utf-8")
            self.send_response(code)
            self.send_header("Content-Length", len(body))
            self.send_header("Content-type", "text/html")
            self.end_headers()
            self.wfile.write(body)

    def __init__(self, port, messages_info, messages):
        HTTPServer.__init__(self, ("localhost", port), StorageMock.Handler)

        assert len(messages_info) == len(messages)
        self.messages_info = messages_info
        self.messages = messages

    def start(self):
        threading.Thread(target=self.serve_forever).start()
