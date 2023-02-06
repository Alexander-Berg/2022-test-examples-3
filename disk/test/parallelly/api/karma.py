# -*- coding: utf-8 -*-
import httplib
import json
from copy import deepcopy

import mock

from nose_parameterized import parameterized

from mpfs.core.services.passport_service import blackbox
from test.parallelly.api.disk.base import DiskApiTestCase
from test.fixtures.users import default_user
from test.fixtures.passport import oauth

from mpfs.common.static import tags
from mpfs.config import settings

PLATFORM_KARMA_BAD_KARMA = settings.platform['karma']['bad_karma']
PLATFORM_KARMA_ENABLE_CHECK = settings.platform['karma']['enable_check']


class BadKarmaTestCase(DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    uid = default_user.uid

    def __init__(self, *args, **kwargs):
        super(BadKarmaTestCase, self).__init__(*args, **kwargs)
        self.oauth_normal_resp = oauth.NORMAL_JSON
        bad_karma_resp = deepcopy(oauth.NORMAL_JSON)
        bad_karma_resp['karma']['value'] = PLATFORM_KARMA_BAD_KARMA
        self.oauth_bad_karma_resp = bad_karma_resp

    _TEST_OAUTH_REQUESTS = [
        ('GET', '/v1/disk', None, 200),
        ('GET', '/v1/disk/resources', {'path': 'disk:/'}, 200),
        ('GET', '/v1/disk/resources/upload', {'path': 'disk:/test.txt'}, 200),
        ('GET', '/v1/disk/trash/resources', {'path': 'trash:/'}, 200),
        ('PUT', '/v1/disk/resources', {'path': 'disk:/test'}, 201),
    ]
    _test_oauth_func_name = lambda testcase_func, param_num, param: (
        "%s_%s" %(testcase_func.__name__, '_'.join(param.args[:2]))
    ).replace('/', '_')

    @parameterized.expand(_TEST_OAUTH_REQUESTS, _test_oauth_func_name)
    def test_oauth_bad_karma(self, method, uri, query, normal_status_code):
        args = (method, uri, query)
        kwargs = {'headers': {'Authorization': 'OAuth MyCoOlTeStToKeN'}}

        response = lambda: None
        response.content = json.dumps(self.oauth_bad_karma_resp)
        with mock.patch('mpfs.platform.handlers.PLATFORM_KARMA_ENABLE_CHECK', True):
            with mock.patch.object(blackbox, 'request', return_value=response):
                resp = self.client.request(*args, **kwargs)
                assert resp.status_code == httplib.FORBIDDEN
                assert json.loads(resp.content)['error'] == 'BadKarmaError'

    @parameterized.expand(_TEST_OAUTH_REQUESTS, _test_oauth_func_name)
    def test_oauth_good_karma(self, method, uri, query, normal_status_code):
        args = (method, uri, query)
        kwargs = {'headers': {'Authorization': 'OAuth MyCoOlTeStToKeN'}}

        response = lambda: None
        response.content = json.dumps(self.oauth_normal_resp)
        with mock.patch('mpfs.platform.handlers.PLATFORM_KARMA_ENABLE_CHECK', True):
            with mock.patch.object(blackbox, 'request', return_value=response):
                resp = self.client.request(*args, **kwargs)
                assert resp.status_code == normal_status_code
