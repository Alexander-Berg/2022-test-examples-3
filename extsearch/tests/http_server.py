import os
import threading
import wsgiref.simple_server


class HttpServer:

    def __init__(self, port, dir):
        self.httpd = wsgiref.simple_server.make_server('localhost', port, self._handle)
        self.thread = threading.Thread(target=self.__serve)
        self.dir = dir

    def __serve(self):
        self.httpd.serve_forever(0.1)

    def start(self):
        self.thread.start()

    def stop(self):
        self.httpd.shutdown()
        self.thread.join()
        self.httpd.server_close()

    def _handle(self, environ, start_response):
        assert environ['REQUEST_METHOD'] == 'GET'

        path = environ['PATH_INFO']
        file = os.path.join(self.dir, path[1:])
        if not os.path.isfile(file):
            start_response('404 NOT FOUND', [])
            return ''

        offset = None
        to = None
        range = environ.get('HTTP_RANGE', None)
        if range:
            offset, to = range.split('=')[1].split('-')
            offset = int(offset)
            if len(to) > 0:
                to = int(to)
            else:
                to = None

        with open(file, 'rb') as f:
            if offset:
                f.seek(offset)

            data = f.read(to - offset) if to else f.read()

            headers = []
            if offset is not None and to is not None:
                full_size = os.path.getsize(file)
                headers = [('Content-Range', 'bytes %d-%d/%d' % (offset, to, full_size))]

            start_response('200 OK', headers)
            return data
