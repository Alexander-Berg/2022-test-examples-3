# encoding: utf-8
import copy
import logging

import library.python.retry as retry

from extsearch.geo.meta.tests.requester import make_url, set_up_requests_session
from extsearch.geo.meta.tests.requester.request import Request

from extsearch.geo.meta.tests.response.base_response_wrapper import ServerError
from extsearch.geo.meta.tests.response.pb_response_wrapper import PbSearchResult
from extsearch.geo.meta.tests.response.proto_response_wrapper import ProtoSearchResult
from extsearch.geo.meta.tests.response.menu_discovery_pb_response_wrapper import MenuPbResult


@retry.retry(conf=retry.RetryConf().on(ServerError).waiting(delay=0.3, backoff=2.0).upto_retries(3))
def _search(endpoint, cgi_params, result_cls):
    # response-specific
    response_specific_params = result_cls.get_specific_params()
    cgi_params = copy.deepcopy(cgi_params)
    cgi_params['ms'] = response_specific_params.ms
    cgi_params.setdefault('gta', []).extend(response_specific_params.gta)

    # common
    cgi_params.update(
        {
            'origin': '1',
            'waitall': 'da',
            'strict_unanswer': 1,
            'timestamp': '2020-02-27T16:00:00+03',
            'geocoder_sco': 'latlong',
        }
    )
    cgi_params.setdefault('rearr', []).append('scheme_Local/Geo/UseFixedSnippets=1')

    full_url = make_url(endpoint, cgi_params)
    logging.info('Request [%s]: %s&hr=yes', response_specific_params.ms, full_url)

    session = set_up_requests_session()
    r = session.get(full_url)
    if not r.ok:
        logging.error('Request: %s Code: %d Error: %s', full_url, r.status_code, r.text)
        r.raise_for_status()
    return result_cls(r.content)


class Context(object):
    def __init__(self, endpoint):
        self.endpoint = endpoint
        self.request = Request()

    def search(self, cgi_params):
        return {
            'pb': _search(self.endpoint, cgi_params, PbSearchResult),
            'proto': _search(self.endpoint, cgi_params, ProtoSearchResult),
        }

    def get_menu(self, cgi_params):
        return _search(self.endpoint, cgi_params, MenuPbResult)


class Searcher(object):
    result_cls = None

    def __init__(self, endpoint):
        self._endpoint = endpoint

    def execute(self, request):
        return _search(self._endpoint, request.params, self.result_cls)


class PbSearcher(Searcher):
    result_cls = PbSearchResult


class ProtoSearcher(Searcher):
    result_cls = ProtoSearchResult
