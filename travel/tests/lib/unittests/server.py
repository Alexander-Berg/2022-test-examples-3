# -*- coding: utf-8 -*-

import errno
import socket
import select
import threading
from contextlib import contextmanager
from BaseHTTPServer import BaseHTTPRequestHandler
from urlparse import urljoin

from django.core.servers.basehttp import WSGIServer
from django.http import HttpResponse


class BaseHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        return

    def do_GET(self):
        self.send_response(200)

        self.wfile.write(str(HttpResponse()))


class LiveServerThread(threading.Thread):
    """
    From django.test.testcases
    """

    def __init__(self, host, possible_ports, HandlerClass):
        self.host = host
        self.port = None
        self.possible_ports = possible_ports
        self.is_ready = threading.Event()
        self.error = None
        self.HandlerClass = HandlerClass

        super(LiveServerThread, self).__init__()

    def run(self):
        try:
            for index, port in enumerate(self.possible_ports):
                try:
                    self.httpd = StoppableWSGIServer((self.host, port), self.HandlerClass)
                except Exception as e:
                    error_code = e.args[0].errno

                    if (
                        index + 1 < len(self.possible_ports)
                        and error_code == errno.EADDRINUSE
                    ):
                        continue
                    else:
                        raise
                else:
                    self.port = port
                    break

            self.is_ready.set()
            self.httpd.serve_forever()
        except Exception as e:
            self.error = e
            self.is_ready.set()

    def join(self, timeout=None):
        if hasattr(self, 'httpd'):
            self.httpd.shutdown()
            self.httpd.server_close()
        super(LiveServerThread, self).join(timeout)

    def get_base_url(self):
        return 'http://%s:%s' % (self.host, self.port)


class _ImprovedEvent(threading._Event):
    """
    Does the same as `threading.Event` except it overrides the wait() method
    with some code borrowed from Python 2.7 to return the set state of the
    event (see: http://hg.python.org/cpython/rev/b5aa8aa78c0f/). This allows
    to know whether the wait() method exited normally or because of the
    timeout. This class can be removed when Django supports only Python >= 2.7.
    """

    def wait(self, timeout=None):
        self._Event__cond.acquire()
        try:
            if not self._Event__flag:
                self._Event__cond.wait(timeout)
            return self._Event__flag
        finally:
            self._Event__cond.release()


class StoppableWSGIServer(WSGIServer):
    """
    The code in this class is borrowed from the `SocketServer.BaseServer` class
    in Python 2.6. The important functionality here is that the server is non-
    blocking and that it can be shut down at any moment. This is made possible
    by the server regularly polling the socket and checking if it has been
    asked to stop.
    Note for the future: Once Django stops supporting Python 2.6, this class
    can be removed as `WSGIServer` will have this ability to shutdown on
    demand and will not require the use of the _ImprovedEvent class whose code
    is borrowed from Python 2.7.
    """

    def __init__(self, *args, **kwargs):
        super(StoppableWSGIServer, self).__init__(*args, **kwargs)
        self.__is_shut_down = _ImprovedEvent()
        self.__serving = False

    def serve_forever(self, poll_interval=0.5):
        """
        Handle one request at a time until shutdown.

        Polls for shutdown every poll_interval seconds.
        """
        self.__serving = True
        self.__is_shut_down.clear()
        while self.__serving:
            r, w, e = select.select([self], [], [], poll_interval)
            if r:
                self._handle_request_noblock()
        self.__is_shut_down.set()

    def shutdown(self):
        """
        Stops the serve_forever loop.

        Blocks until the loop has finished. This must be called while
        serve_forever() is running in another thread, or it will
        deadlock.
        """
        self.__serving = False
        if not self.__is_shut_down.wait(2):
            raise RuntimeError(
                "Failed to shutdown the live test server in 2 seconds. The "
                "server might be stuck or generating a slow response.")

    def handle_request(self):
        """Handle one request, possibly blocking.
        """
        fd_sets = select.select([self], [], [], None)
        if not fd_sets[0]:
            return
        self._handle_request_noblock()

    def _handle_request_noblock(self):
        """
        Handle one request, without blocking.

        I assume that select.select has returned that the socket is
        readable before this function was called, so there should be
        no risk of blocking in get_request().
        """
        try:
            request, client_address = self.get_request()
        except socket.error:
            return
        if self.verify_request(request, client_address):
            try:
                self.process_request(request, client_address)
            except Exception:
                self.handle_error(request, client_address)
                self.close_request(request)


@contextmanager
def serve_file(filepath, content_type="application/octet-stream"):
    class Handler(BaseHandler):
        def do_GET(self):
            self.send_response(200)

            with open(filepath) as f:
                response = HttpResponse(f.read(), content_type=content_type)

            self.wfile.write(str(response))

    possible_ports = range(8000, 9000)
    host = 'localhost'

    server_thread = LiveServerThread(host, possible_ports, Handler)
    server_thread.daemon = True
    server_thread.start()

    server_thread.is_ready.wait()

    if server_thread.error:
        raise server_thread.error

    url = urljoin(server_thread.get_base_url(), '/file.xml')

    try:
        yield url
    finally:
        server_thread.join()
