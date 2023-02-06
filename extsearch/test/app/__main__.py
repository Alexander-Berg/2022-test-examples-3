import argparse
import socket
import sys
import time
from http.server import HTTPServer, BaseHTTPRequestHandler


class RequestHandler(BaseHTTPRequestHandler):
    enable_shutdown_handler = True

    def _make_response(self, content):
        self.send_response(200)
        self.send_header('Content-type', 'text/plain')
        self.end_headers()
        self.wfile.write(content.encode('utf-8'))
        self.wfile.flush()

    def do_GET(self):
        if self.path == '/ping':
            self._make_response('pong')
            return

        if self.path == '/yandsearch':
            self._make_response('nothing found:(')
            return

        if self.enable_shutdown_handler and self.path == '/admin?action=shutdown':
            self._make_response('OK')
            print('Bye!', file=sys.stderr)
            sys.exit(0)

        self.send_response(404)
        self.end_headers()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='', help='host name')
    parser.add_argument('--port', type=int, default=8000, help='port number')
    parser.add_argument('--delay', type=float, default=0.0, help='startup time (in seconds)')
    parser.add_argument('--crash', action='store_true', help='crash on startup')
    parser.add_argument('--ipv6', action='store_true', help='use AF_INET6 socket')
    parser.add_argument('--ignore-shutdown', action='store_true', help='do not terminate on action=shutdown command')
    args = parser.parse_args()

    if args.delay:
        print('Loading data...', file=sys.stderr)
        time.sleep(args.delay)

    if args.crash:
        print('Service crashed!', file=sys.stderr)
        sys.exit(42)

    if args.ignore_shutdown:
        RequestHandler.enable_shutdown_handler = False

    if args.ipv6:
        HTTPServer.address_family = socket.AF_INET6
    server_address = (args.host, args.port)
    httpd = HTTPServer(server_address, RequestHandler)

    host, port = httpd.socket.getsockname()[:2]
    if ':' in host:
        host = '[{}]'.format(host)
    print('Starting development server at http://{}:{}/'.format(host, port), file=sys.stderr)

    httpd.serve_forever()


if __name__ == '__main__':
    main()
