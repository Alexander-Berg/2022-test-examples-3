# -*- coding: utf-8 -*-

import logging
import re
import os
import pytest
from urlparse import urlparse

from report.functional.web.base import BaseFuncTest
from report.const import *

UA_PAD = 'Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36'  # noqa
UA_MOBILE = 'Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Mobile Safari/537.36'  # noqa


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestSearchapp(BaseFuncTest):

    @pytest.mark.skip(reason="use integration test")
    @pytest.mark.parametrize('path', (
        '/searchapp/meta',
    ))
    @pytest.mark.ticket('SERP-71275')    # Check ll param
    def test_geopoint_ll_swap_searchappmeta(self, query, path):
        query.set_query_type(SEARCH_APP)
        query.set_url(path)
        query.replace_params({'ll': '55.13,37.14'})
        resp = self.request(query)
        assert '&ll=37.14%2C55.13' in resp.content

        resp = self.json_request(query)
        assert resp.data['cgidata']['args']['ll'][0] == '37.14,55.13'

    # swap coordinates in ll param (searchapp uses "lat,lon")
    @pytest.mark.skip(reason="use integration test")
    @pytest.mark.ticket('SERP-67548')
    @pytest.mark.parametrize('path', (
        '/searchapp/meta',
    ))
    def test_searchapp_meta_ll(self, query, path):
        query.set_query_type(SEARCH_APP)
        query.set_url(path)
        query.replace_params({
            'll': '55.13,37.14'
        })
        resp = self.request(query, host='hamster.yandex.ru', port=80)
        assert '&ll=37.14%2C55.13' in resp.content
        query.replace_params({'export': 'json'})
        resp = self.json_request(query)
        assert '37.14,55.13' == resp.data['cgidata']['args']['ll'][0]

    @pytest.mark.ticket('WEBREPORT-86')
    @pytest.mark.parametrize("path", [
        '/searchapp/sdch/some/test.dict',
        '/searchapp/sdch/yandex.ru/http/hC6cDRqW.dict'
    ])
    def test_searchapp_java_sdch(self, query, path):
        query.set_query_type(SEARCH_APP)
        query.set_url(path)
        query.set_params({})
        self.request(query, require_status=404)

    def run_jsonproxy_response_test(self, query, text):

        query.set_params({'text': text})

        resp = self.request(query)

        doc = resp.data["docs"][0]
        if doc["type"] != "sites":
            logging.error('`%s` wrong type', text)
            return False

        if len(doc["list"]) < 10:
            print text + "less than 10 docs"
            return False

        wizard = doc["list"][0]

        if (
            wizard["type"] != "wizards" or
            wizard["subtype"] != "banner"
        ):
            logging.error('no banner: `%s`', text)
            return False

        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        assert yabs_setup[0]['metahost'] == ['YABS:yabs.yandex.ru:80/code/3916?']

        return True

    @pytest.mark.skip(reason="RUNTIMETESTS-114")
    @pytest.mark.ticket('SERP-72300')
    def test_jsonproxy_response(self, query):
        query.set_query_type(JSON_PROXY)
        query.set_params({
            'ver': 1,
            'type': 'sites',
            'line': 'db58818d513bdb5d7eece939246fd017',
            'query_source': 'suggest',
            'clid': '212068',
            'uuid': '2db9a3192b76f70204f7464669ef38d6',
            'app_id': 'ru.yandex.searchplugin',
            'app_platform': 'android',
            'app_version': '315',
            'lang': 'en-US',
            'country_init': 'ru',
            'll': '55.734214782714844,37.58828353881836',
            'manufacturer': 'HUAWEI',
            'model': 'HUAWEI_CUN-U29',
            'os_version': '5.1',
            'exp': '0',
            'tl_lat': '55.739214782714846',
            'tl_lon': '37.58328353881836',
            'br_lat': '55.72921478271484',
            'br_lon': '37.59328353881836'
        })

        passed = 0
        for text in [
            u"пицца",
            u"купить смартфон",
            u"переезд"
        ]:
            if self.run_jsonproxy_response_test(query, text):
                passed += 1

        assert passed > 0

    def run_searchapp_response_test(self, query, text):
        query.set_params({'text': text})
        resp = self.json_dump_request(query, 'banner')
        if 'direct_premium' in resp['data']:
            return True
        else:
            return False

    @pytest.mark.ticket('SERP-82464')
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_long_text_redirect(self, query):
        query.set_query_type(SEARCH_APP)
        text = "0123456789" * 40 + "012345"  # long text
        query.set_params({'lr': 213, 'serp_only': 1, 'text': text})
        query.add_flags({'redirect_searchapp_on_long_queries': 1})

        resp = self.request(query, require_status=302)

        assert 'Location' in resp.headers
        location = resp.headers["Location"]
        assert location[0].startswith('/searchapp')

        parsed = urlparse.urlparse(location[0])
        params = urlparse.parse_qs(parsed.query)
        assert 'text' in params
        assert len(params['text'][0]) == 401
