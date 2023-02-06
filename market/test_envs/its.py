# coding: utf-8

import functools
import json
import logging
import SocketServer
import SimpleHTTPServer
import threading
import time


logger = logging.getLogger('its')

SHUTDOWN_SERVER_TIMEOUT = 0.5


class RequesFailed(Exception):
    def __init__(self, code, message):
        self.code = code
        self.message = message

    def __str__(self):
        return '{}: {}'.format(self.code, self.message)


class IgnoreRequest(Exception):
    pass


class ETag(object):
    def __init__(self):
        self.__etag = 0

    def __str__(self):
        return '"{}"'.format(self.__etag)

    def __iadd__(self, other):
        self.__etag += other
        return self


class ItsConfig(object):
    def __init__(self, oauth=None, handle_name=None, report_flags=None, script=None):
        self.oauth = oauth
        self.handle_name = handle_name
        self.report_flags = report_flags
        self.script = script
        self.request_count = 0
        self.etag = ETag()


class ItsHandler(SimpleHTTPServer.BaseHTTPServer.BaseHTTPRequestHandler):
    def __init__(self, request, client_address, server, config=None):
        self.__config = config
        SimpleHTTPServer.BaseHTTPServer.BaseHTTPRequestHandler.__init__(self, request, client_address, server)

    def do_GET(self):
        self.__process_request(self.__on_get_handle_value)

    def do_POST(self):
        self.__process_request(self.__on_post_handle_value)

    def log_message(self, format, *args):
        logger.info(format, *args)

    def __process_request(self, handler):
        try:
            logger.debug('Current request count: %s', self.__config.request_count)
            self.__run_script()
            self.__check_request()
            handler()
        except IgnoreRequest:
            logger.info('Ignore request by script command')
        except RequesFailed as error:
            logger.warn('Request failed: %s', error)
            self.__send_response(error.code, error.message)
        except Exception as error:
            logger.exception(error)
            self.__send_response(500, str(error))
        finally:
            self.__config.request_count += 1

    def __run_script(self):
        if self.__config.script is None:
            return
        for cmd, params in self.__config.script.get(self.command, {}).iteritems():
            if cmd == 'replace_etag':
                self.headers['If-Match'] = params.get('etag', '')
            elif cmd == 'replace_oauth':
                self.headers['Authorization'] = params.get('oauth', '')
            elif cmd == 'sleep':
                time.sleep(params['time'])
            elif cmd == 'ignore_request':
                raise IgnoreRequest
            elif cmd == 'error_on_request':
                if self.__config.request_count in params['request_counts']:
                    raise RequesFailed(params['code'], '{}: {}'.format('failed by script', params['message']))

    def __check_request(self):
        parts = self.path.split('/')
        if not self.path.endswith('/') or len(parts) < 8:
            raise RequesFailed(400, 'Wrong request path: {}'.format(self.path))
        handle_name = parts[-2]
        if handle_name != self.__config.handle_name:
            raise RequesFailed(404, 'Wrong handle name: {}'.format(handle_name))
        if self.command not in {'GET', 'POST'}:
            raise RequesFailed(404, 'Wrong request command: {}'.format(self.command))
        expected_headers = {
            'Authorization': 'OAuth {}'.format(self.__config.oauth),
            'Content-Type': 'application/json; charset=utf-8',
            'Accept': 'application/json',
            'Accept-Encoding': 'gzip',
        }
        for name, value in expected_headers.iteritems():
            if self.headers.get(name) is None:
                raise RequesFailed(400, 'Header absent {}'.format(name))
            if self.headers[name] != value:
                raise RequesFailed(400, 'Wrong header {}: {}'.format(name, self.headers[name]))
        if self.command == 'POST':
            if not self.headers.get('If-Match'):
                raise RequesFailed(400, 'Header If-Match not found')
            if not self.headers.get('Content-Length'):
                raise RequesFailed(400, 'Header Content-Length not found')

    def __on_get_handle_value(self):
        flags = json.dumps(self.__config.report_flags)
        data = json.dumps({'user_value': flags})
        logger.debug('[<<] %s', flags)
        self.__send_response(200, data=data)

    def __on_post_handle_value(self):
        etag = self.headers['If-Match']
        if etag != str(self.__config.etag):
            raise RequesFailed(412, 'ETag mismatch, etag: {}'.format(etag))
        request_data_len = int(self.headers.get('Content-Length', 0))
        data = json.loads(self.rfile.read(request_data_len))
        self.__config.report_flags = json.loads(data['value'])
        logger.debug('[>>] %s', json.dumps(self.__config.report_flags))
        self.__config.etag += 1
        self.__send_response(200)

    def __send_response(self, code, message=None, data=None, **kwargs):
        self.send_response(code, message)
        self.send_header("ETag", str(self.__config.etag))
        self.send_header("Content-Type", "application/json")
        if data is not None:
            self.send_header("Content-Length", len(data))
            self.end_headers()
            self.wfile.write(data)
        else:
            self.end_headers()


class ThreadingSimpleServer(SocketServer.ThreadingMixIn, SimpleHTTPServer.BaseHTTPServer.HTTPServer):
    pass


class Its(object):
    def __init__(self, port, **kwargs):
        self.__port = port
        self.__config = ItsConfig(**kwargs)
        self.__server = ThreadingSimpleServer(
            ('localhost', self.__port),
            functools.partial(ItsHandler, config=self.__config))
        self.__server_thread = threading.Thread(target=self.__server.serve_forever)

    def __enter__(self):
        self.__server_thread.start()
        return self

    def __exit__(self, *args):
        self.__server.shutdown()
        self.__server_thread.join(SHUTDOWN_SERVER_TIMEOUT)

    @property
    def port(self):
        return self.__port

    @property
    def config(self):
        return self.__config
