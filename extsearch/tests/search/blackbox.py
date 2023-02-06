# -*- coding: utf-8 -*-

import copy
import logging
import requests

from extsearch.geo.meta.tests.requester import make_url, set_up_requests_session
from extsearch.geo.meta.tests.response.pb_response_wrapper import PbSearchResult

from .reqans import parse_reqans


class GeometasearchBlackbox(object):
    search_path = None

    class Options(object):
        def __init__(self):
            self.auto_add_origin = True
            self.increase_timeouts = True
            self.headers = {}

    def __init__(self, metasearch):
        '''
        metasearch: a string in a form of
                    'host:port',
                    'http://host:port',
                    'http://host:port/',
                    'http://host:port/yandsearch',
                    'http://host:port/yandsearch?foo=bar'
        '''
        self._logger = logging.getLogger('blackbox')
        self._metasearch = metasearch
        self._base_params = {}
        self.options = GeometasearchBlackbox.Options()

    def set_query(self, **kwargs):
        self._base_params = kwargs

    def get_raw(self, *args, **kwargs):
        '''
        returns requests.Response
        '''
        params = self._prepare_params(kwargs)
        if args:
            if len(args) != 1:
                raise ValueError('Single path argument expected')
            path = args[0]
        else:
            path = self.search_path

        url = make_url(self._metasearch, path=path, params=params)
        self._logger.info('Request: %s', url)

        session = set_up_requests_session()
        return session.get(url, headers=self.options.headers)

    def get_pb(self, **kwargs):
        '''
        returns PbSearchResponse
        '''
        kwargs.setdefault('ms', 'pb')
        return self._parse_proto(self.get_raw(**kwargs))

    def reopen_logs(self):
        url = make_url(self._metasearch, path='/admin', params={'action': 'reopenlog'})
        r = requests.get(url)
        r.raise_for_status()

    def _prepare_params(self, cur_params):
        '''
        Applies parameters previously set by `set_query()` and returns a dict.
        '''
        params = copy.deepcopy(self._base_params)

        if self.options.auto_add_origin:
            params.setdefault('origin', 'test')
        if self.options.increase_timeouts:
            self._increase_timeouts(params)

        params.update(cur_params)
        return params

    def _parse_proto(self, response):
        response.raise_for_status()

        try:
            return PbSearchResult(response.content)
        except Exception as e:
            self._logger.debug('Bad response content (%s): %r', e, response.content)
            raise

    def _increase_timeouts(self, params):
        raise NotImplementedError()


class UpperGeometasearchBlackbox(GeometasearchBlackbox):
    search_path = '/yandsearch'

    def __init__(self, metasearch, reqans_log_path=None):
        super().__init__(metasearch)
        self._reqans_log_path = reqans_log_path

    def list_reqans_records(self):
        assert self._reqans_log_path is not None
        for _ in range(3):
            self.reopen_logs()
        with open(self._reqans_log_path) as fd:
            return list(parse_reqans(fd))

    def _increase_timeouts(self, params):
        # TODO(sobols): find out what each parameter exactly does
        params.setdefault('waitall', 'da')
        params.setdefault('timeout', '1000000000')
        params.setdefault('middle_timeout', '1000000000')


class GeometasearchV2Blackbox(GeometasearchBlackbox):
    search_path = '/search'

    def _increase_timeouts(self, params):
        pass
