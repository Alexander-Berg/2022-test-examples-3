from StringIO import StringIO

import pycurl
import time
import re


class request_http:
    def __init__(self, url):
        self._url = url

    def __enter__(self):
        self._downloader = HttpRequest(self._url)
        return self._downloader

    def __exit__(self, type, value, traceback):
        pass


class RequestTime:
    def __init__(self, start, end=None):
        self.start = start
        self.end = end if end else time.time()

    @property
    def duration(self):
        return self.end - self.start

    @property
    def start_str(self):
        return self._format_time(self.start)

    @property
    def end_str(self):
        return self._format_time(self.end)

    @staticmethod
    def _format_time(t):
        return time.strftime("%H:%M:%S", time.localtime(t))


class HttpRequest:
    def __init__(self, url):
        self._url = url
        self._headers = StringIO()
        self._body = StringIO()
        self.p = pycurl.Curl()
        self.p.setopt(pycurl.HEADERFUNCTION, self._headers.write)
        self.p.setopt(pycurl.WRITEFUNCTION, self._body.write)
        self.p.setopt(pycurl.URL, url)
        self.time = None

    def perform(self):
        start = time.time()
        try:
            self.p.perform()
        finally:
            self.time = RequestTime(start)

    @property
    def filename(self):
        return re.search('^[^?]+/([^/?]+)(?=\?|$)', self._url).group(1)

    @property
    def status(self):
        return self.p.getinfo(pycurl.RESPONSE_CODE)

    @property
    def body(self):
        return self._body.getvalue()

    def header(self, name):
        m = re.search('%s:\s*(.+?)\s*?\r\n' % name, self._headers.getvalue())
        return m.group(1) if m else ''

    def close(self):
        self._headers.close()
        self._body.close()
        self.p.close()
