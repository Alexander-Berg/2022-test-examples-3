class Request2Curl(object):
    def __init__(self, request, **request_kwargs):
        """
        :param request: instance of requests.models.PreparedRequest.
        :param request_kwargs: additional request kwargs as they are passed to hooks by requests library.
        """
        self.request = request
        self.timeout = request_kwargs.get('timeout')
        self.verify_ssl = request_kwargs.get('verify')
        self.proxies = request_kwargs.get('proxies', {})

    def get_method_and_data(self):
        method, data = self.request.method, self.request.body

        if data:
            data_str = "-d '{}'".format(data)
            if method == 'POST':
                return data_str
            else:
                return '-X {} '.format(method) + data_str
        elif method == 'GET':
            return ''
        else:
            return '-X {}'.format(method)

    def get_multi_param(self, param_str, params_dict):
        if not params_dict:
            return ""

        params = " {} ".format(param_str).join(
            '"{}: {}"'.format(header, value)
            for header, value in params_dict.items())
        return param_str + " " + params

    def get_curl(self, add_parts=None):
        add_parts = add_parts if add_parts else []
        headers = self.get_multi_param('-H', self.request.headers)
        proxies = self.get_multi_param('--proxy', self.proxies)

        parts = add_parts + [
            headers,
            proxies,
            self.get_method_and_data(),
            '"{}"'.format(self.request.url),
        ]

        return 'curl ' + ' '.join(part for part in parts if part)


class CurlHook(object):
    """
    Usage:

    class MyCurlHook(CurlHook):
        def process_curl(self, curl_str):
            do_stuff_with_curl(curl_str)

    requests.get('http://example.com', timeout=3, hooks={'response': MyCurlHook()})
    """

    def __init__(self, only_first_request=False, allow_redirects=True):
        """
        :param only_first_request: in case of redirects there will be several requests.
            Set this parameter to True if you need only the first request to be processed.
        :param allow_redirects: currently there is no way to get actual value of allow_redirects
            parameter from request or response objects, so pass it manually.
        """
        self.only_first_request = only_first_request
        self.calls_num = 0
        self.allow_redirects = allow_redirects

    def __call__(self, response, **request_kwargs):
        if not self.only_first_request or self.calls_num == 0:
            curler = Request2Curl(response.request, **request_kwargs)
            add_parts = ["-L" if self.allow_redirects else ""]
            self.process_curl(curler.get_curl(add_parts), response, **request_kwargs)
            self.calls_num += 1

    def process_curl(self, curl_str, response, **request_kwargs):
        raise NotImplementedError


class PrintCurlHook(CurlHook):
    """ This hook prints curl commands of requests. """

    def process_curl(self, curl_str, response, **request_kwargs):
        print(curl_str)
