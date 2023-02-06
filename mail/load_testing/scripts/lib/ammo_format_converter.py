from collections import namedtuple
from urllib.parse import urlencode, urlparse

HTTPRequestArguments = namedtuple('HTTPRequestArguments', ['body', 'params'])


class AmmoFormatConverter(object):
    def __init__(self,
                 case_tag: str,
                 url: str,
                 method: str = 'POST',
                 keep_alive: bool = True):
        self.method = method
        self.url = url
        self.case_tag = case_tag
        self.keep_alive = keep_alive

    def _get_headers(self):
        keep_alive = 'Keep-Alive' if self.keep_alive else 'Close'
        headers = f'Host: {urlparse(self.url).netloc}\r\n' \
                  f'User-Agent: tank\r\n'                  \
                  f'Connection: {keep_alive}'
        return headers

    def _build_url_path_and_parameters(self, params: dict):
        query_string = '?' + urlencode(params) if params else ''
        return urlparse(self.url).path + query_string

    def _build_request_with_body(self, body: str, params: dict = None):
        url = self._build_url_path_and_parameters(params)
        return f'{self.method} {url} HTTP/1.1\r\n' \
               f'{self._get_headers()}\r\n'        \
               f'Content-Length: {len(body)}\r\n'  \
               f'\r\n'                             \
               f'{body}\r\n'

    def _build_bodiless_request(self, params: dict):
        url = self._build_url_path_and_parameters(params)
        return f"{self.method} {url} HTTP/1.1\r\n" \
               f"{self._get_headers()}\r\n"        \
               f"\r\n"

    def convert(self, request_arguments: HTTPRequestArguments):
        body, params = request_arguments.body, request_arguments.params
        if body:
            http_request = self._build_request_with_body(body, params)
        else:
            http_request = self._build_bodiless_request(params)

        return f'{len(http_request)} {self.case_tag}\n' \
               f'{http_request}'
