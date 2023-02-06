# -*- coding: utf-8 -*-
import os
import pytest
import six
import tempfile

from six.moves import BaseHTTPServer
from six.moves import socketserver as SocketServer
from six.moves.BaseHTTPServer import BaseHTTPRequestHandler
from threading import Thread

from market.idx.pylibrary.curllib.curl import (
    Curl,
    SafeCurl,
)

from yatest.common.network import PortManager


class ThreadingSimpleServer(
        SocketServer.ThreadingMixIn,
        BaseHTTPServer.HTTPServer
):
    pass


class SimpleHTTPHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        # simple echo on post
        if hasattr(self.headers, 'getheader'):
            content_length = self.headers.getheader('content-length')
        else:
            content_length = self.headers.get('content-length')
        content_length = int(content_length)
        post_body = six.ensure_binary(self.rfile.read(content_length))
        self.send_response(200)
        self.end_headers()
        self.wfile.write(post_body)

    def do_GET(self):
        # hello world on get
        data = six.ensure_binary('Hello, World!')
        self.send_response(200)
        self.send_header('content-type', 'html/text')
        self.send_header('content-length', len(data))
        self.end_headers()
        self.wfile.write(data)


class SimpleHTTPServer(object):
    def __init__(self, host=None, port=None):
        self._host = host or 'localhost'
        self._port = port

    @property
    def host(self):
        return self._host

    @property
    def port(self):
        return self._port

    def start(self):
        server = ThreadingSimpleServer((self.host, self.port), SimpleHTTPHandler)
        mock_server_thread = Thread(target=server.serve_forever)
        mock_server_thread.setDaemon(True)
        mock_server_thread.start()


@pytest.yield_fixture(scope='module')
def simple_server():
    with PortManager() as pm:
        server = SimpleHTTPServer(port=pm.get_port())
        server.start()
        yield server


@pytest.fixture(params=[Curl, SafeCurl], ids=['curl', 'safecurl'])
def curl_cls(request):
    return request.param


def test_download(simple_server, curl_cls):
    url = '{}:{}/download'.format(simple_server.host, simple_server.port)
    with tempfile.NamedTemporaryFile() as tmp:
        headers = curl_cls().download(url, tmp.name)
        assert headers['content-type'] == 'html/text'
        assert headers['content-length'] == str(os.path.getsize(tmp.name))


def test_upload(simple_server, curl_cls):
    url = '{}:{}/upload'.format(simple_server.host, simple_server.port)
    with tempfile.NamedTemporaryFile() as tmp:
        tmp.write(six.ensure_binary('Some data'))
        tmp.flush()

        curl_cls().upload(url, tmp.name)
