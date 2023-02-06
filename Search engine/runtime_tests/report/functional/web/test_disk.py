# -*- coding: utf-8 -*-

import time
import json

import os
import pytest

from report.const import *
from report.functional.web.base import BaseFuncTest
from report.functional.conftest import create_runtime_test_data, remove_runtime_test_data, create_permanent_test_data, remove_permanent_test_data


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
@pytest.mark.skipif(pytest.config.option.enable_datatests, reason="Disable data tests")
class TestDataRuntime(BaseFuncTest):
    SHOW_DATA = {"voffset":"","retina":"","hoffset":"","content":"big","url":"//avatars.mds.yandex.net/get-serp/15285/dea33859-17da-4b73-a128-ac1fb5f871bf/orig","counter":"promo","title":"hi"}

    @classmethod
    @pytest.fixture(scope='class', autouse=True)
    def runtime_test_data(cls, request, sandbox, report_ctx):
        create_runtime_test_data(sandbox, report_ctx)

        def fin():
            #можно выставить в False, для ускорения выполнения тестов во время отладки, но тогда есть шанс,
            # что репорт будет работать на невалидных данных
            remove_runtime_test_data(sandbox, report_ctx, restart=True)
        request.addfinalizer(fin)

    @classmethod
    @pytest.fixture(scope='class', autouse=True)
    def permanent_test_data(cls, request, sandbox, report_ctx, runtime_test_data):
        create_permanent_test_data(sandbox, report_ctx)

        def fin():
            remove_permanent_test_data(sandbox, report_ctx)
        request.addfinalizer(fin)

    @pytest.mark.unstable
    def test_data_l_cookie_keys(self, query, runtime_summary_json, write_data):
        """
        SERP-40431 - авторизация
        Нужно два кейса - когда data['blackbox'] == {} и data['blackbox'] != {}
        """
        l_cookie_key = '12301;YQxzWARRAFdRfXNhZAdLXVZFBmZEXnlbJVEFFTcDaUMMF0NBQAM;1458680402\n'

        t_now = int(time.time())

        runtime_summary = runtime_summary_json

        time.sleep(1.01)
        runtime_summary['util/l_cookie_keys']["mtime"] = t_now
        runtime_summary['util/l_cookie_keys']['files'][1] = t_now
        runtime_summary['util/l_cookie_keys']['files'][0] = t_now
        write_data('data.runtime', 'util/l_cookie_keys/L-cookie-keys.txt', l_cookie_key)

        query.set_auth()

        resp = self.json_test(query)

        #TODO
        #Почему-то всегда приходит ошибка 'error': [{u'content': u'key requested that has id with invalid format'}]
        #даже в случае, если взять значение куки L из L-cookie-keys.txt и подложить в запрос
        assert resp.data['blackbox'] == {}


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestITS(BaseFuncTest):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_its_no_flags(self, query, write_its_flags):
        resp = self.json_request(query)
        assert 'its_location' not in resp.data['reqdata']['flags']

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_its_flags(self, query, write_its_flags):
        write_its_flags({'its_location': 'some'})
        resp = self.json_request(query)
        assert resp.data['reqdata']['flags']['its_location'] == 'some'

    def test_no_its_flags_with_no_flags(self, query, write_its_flags):
        write_its_flags({'its_location': 'some'})
        query.add_params({ 'no-flags': 1 })
        resp = self.json_request(query)
        assert 'its_location' not in resp.data['reqdata']['flags']
