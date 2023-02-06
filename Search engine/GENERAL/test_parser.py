from base_parsers import SerpParser


class TestParser(SerpParser):

    URL_TEMPLATE = 'https://{host}/search/'

    def _prepare_cgi(self, basket_query, host, additional_parameters, user_cgi_parameters):
        return [('text', basket_query.get('text'))]

    def _prepare_headers_multimap(self, basket_query, host, additional_parameters={}):
        return {'X-MyHeader': ['v']}

    def _prepare_cookies_multimap(self, basket_query, host, additional_parameters={}):
        return {'MyCookie': ['v']}
