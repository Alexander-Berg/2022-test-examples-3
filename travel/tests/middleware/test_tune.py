# -*- coding: utf-8 -*-

import urlparse

from django.test.client import RequestFactory

from travel.avia.library.python.common.geotargeting.middleware import Tune as TuneMiddleware

from travel.avia.library.python.tester.testcase import TestCase


class TestTuneMiddleware(TestCase):

    def setUp(self):
        self.factory = RequestFactory()

    def test_build_correct_url(self):
        middleware = TuneMiddleware()
        request = self.factory.get('/some/', {'some_param': 'some_value'})
        request.tune_host = 'some_tune_domain.ru'

        middleware.process_request(request)

        assert request.tune_url

        tune_url = urlparse.urlparse(request.tune_url)
        assert tune_url.hostname == 'some_tune_domain.ru'
        assert tune_url.path == '/region/'

        tune_url_params = urlparse.parse_qs(tune_url.query)
        assert len(tune_url_params) == 1
        assert 'retpath' in tune_url_params

        retpath_url = urlparse.urlparse(tune_url_params['retpath'][0])
        assert retpath_url.hostname == 'testserver'
        assert retpath_url.path == '/some/'

        retpath_url_params = urlparse.parse_qs(retpath_url.query)
        assert len(retpath_url_params) == 2
        assert 'domredir' in retpath_url_params and retpath_url_params['domredir'][0] == '1'
        assert 'some_param' in retpath_url_params and retpath_url_params['some_param'][0] == 'some_value'
