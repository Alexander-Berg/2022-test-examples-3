# -*- coding: utf-8 -*-
import datetime
from urllib2 import HTTPError

import requests
from mock import patch, MagicMock

from helpers.stubs.services import EmptyPreparedRequest
from mpfs.common.errors import MPFSError
from mpfs.core.db_rps_limiter import InDBRPSLimiter
from mpfs.platform.rate_limiters import PerSomethingUniqueLimiter
from test.base import DiskTestCase, time_machine


class TestInDBRPSLimiter(DiskTestCase):
    def mock_settings(cls, burst=1, rph=3600):
        return patch('mpfs.config.settings.db_rate_limiter', {
            'test': {
                'burst': burst,
                'rph': rph,
            },
        })

    def _resource_update_wrapper(self, method):
        def wrapper(self, *args, **kwargs):
            mock(*args, **kwargs)
            if mock.call_count == 1:
                return {'updatedExisting': False}
            return method(self, *args, **kwargs)

        mock = MagicMock()
        wrapper.mock = mock
        return wrapper

    def test_limit_exceeds(self):
        burst = 2
        with self.mock_settings(burst=burst):
            ratelimiter = InDBRPSLimiter('test')

        start = datetime.datetime.now()
        with time_machine(start):
            assert ratelimiter.check_limit_and_increment_counter(self.uid)
            assert ratelimiter.check_limit_and_increment_counter(self.uid)
            assert not ratelimiter.check_limit_and_increment_counter(self.uid)

    def test_limit_restores(self):
        with self.mock_settings():
            ratelimiter = InDBRPSLimiter('test')

        start = datetime.datetime.now()
        with time_machine(start):
            assert ratelimiter.check_limit_and_increment_counter(self.uid)
            assert not ratelimiter.check_limit_and_increment_counter(self.uid)
        start += datetime.timedelta(seconds=1)
        with time_machine(start):
            assert ratelimiter.check_limit_and_increment_counter(self.uid)

    def test_changed_version(self):
        with self.mock_settings(burst=3):
            ratelimiter = InDBRPSLimiter('test')
        wrapper = self._resource_update_wrapper(InDBRPSLimiter._update_counter)
        with patch.object(InDBRPSLimiter, '_update_counter', wrapper):
            ratelimiter.check_limit_and_increment_counter(self.uid)
        assert wrapper.mock.call_count == 2


class TestRateLimiterService(DiskTestCase):
    def setup_method(self, method):
        super(TestRateLimiterService, self).setup_method(method)
        self.rate_limiter = PerSomethingUniqueLimiter('not_relevant')

        self.good_response = requests.Response()
        self.good_response.request = EmptyPreparedRequest()
        self.good_response.status_code = 200

    def test_ratelimiter_get(self):
        with patch.object(self.rate_limiter.service, 'request', return_value=self.good_response):
            assert not self.rate_limiter.service.is_limit_exceeded('a', 'b')

    def test_ratelimiter_exceeded(self):
        with patch.object(self.rate_limiter.service, 'request', side_effect=HTTPError('url', 429, None, None, None)):
            assert self.rate_limiter.service.is_limit_exceeded('a', 'b')
