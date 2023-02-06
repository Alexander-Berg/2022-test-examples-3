# coding=utf-8

import six
from pytest_localserver.http import WSGIServer


class ItsAppMock:
    def __init__(self, get_responses):
        self.input_get = []
        self.input_post = {}
        self.get_responses = get_responses
        self.server = None

    def get_handler(self, env, start_response):
        uri = six.ensure_str(env['RAW_URI'])
        if uri in self.get_responses:
            self.input_get.append(uri)
            status = '200 OK'
            response = [six.ensure_binary(self.get_responses[uri])]
        else:
            status = '404 Not Found'
            response = [six.ensure_bionary('Error')]
        response_headers = [('Content-type', 'application/json; charset=utf-8')]
        start_response(status, response_headers)
        return response

    def post_handler(self, env, start_response):
        status = '200 OK'
        response_headers = [('Content-type', 'text/html')]
        start_response(status, response_headers)
        uri = six.ensure_str(env['RAW_URI'])
        content_length = int(env['CONTENT_LENGTH'])
        self.input_post[uri] = six.ensure_str(env['wsgi.input'].read(content_length))
        return [six.ensure_binary('OK')]

    def app(self, env, start_response):
        method = env['REQUEST_METHOD']
        if method == 'GET':
            return self.get_handler(env, start_response)
        elif method == 'POST':
            return self.post_handler(env, start_response)
        start_response('501 Not Implemented', [('Content-type', 'text/html')])
        return []

    def reset_history(self):
        self.input_get = []
        self.input_post = {}

    def __enter__(self):
        self.server = WSGIServer(application=self.app)
        self.server.start()
        return self

    def __exit__(self, *args, **kwargs):
        self.server.stop()
