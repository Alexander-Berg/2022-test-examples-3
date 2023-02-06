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


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestSearchapp(BaseFuncTest):

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-60083')    # Don't swap coordinates in ll param for search/touch
    @pytest.mark.ticket('SERP-71275')    # Check ll param
    def test_search_touch_ll(self, query):
        query.set_query_type(TOUCH)
        query.replace_params({'ll': '55.13,37.14'})
        geopoint = self.json_dump_request(query, 'rdat.geopoint')
        assert geopoint == '55.13/37.14/250/ll'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize('path', (
        '/searchapp',
        '/searchapp/searchapp',
        '/jsonproxy'
    ))
    @pytest.mark.ticket('SERP-71275')    # Check ll param
    def test_geopoint_ll_swap(self, query, path):
        query.set_query_type(SEARCH_APP)
        query.set_url(path)
        query.replace_params({'ll': '55.13,37.14'})
        geopoint = self.json_dump_request(query, 'rdat.geopoint')
        assert geopoint == '37.14/55.13/250/ll'

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

    # Don't insert ui=webmobileapp.yandex into /search/touch requests
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-54274')
    # change order of coordinates in ll param
    @pytest.mark.ticket('SERP-60083')
    def test_search_touch_no_rewrite(self, query):
        query.set_query_type(TOUCH)
        query.replace_params({'ll': '55.13,37.14'})
        url = self.json_dump_request(query, 'rdat.cgi')
        assert re.search(r'yandex.ru/search/touch/\?', url)
        assert not re.search(r'ui=webmobileapp\.yandex', url)
        # SERP-60083
        pos = re.search('[?&]ll=([^&]+)', url)
        assert pos is not None
        assert pos.group(1) == '55.13%2C37.14'

    # swap coordinates in ll param (searchapp uses "lat,lon")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-60083')
    @pytest.mark.ticket('SERP-67548')
    @pytest.mark.ticket('WEBREPORT-348')
    @pytest.mark.parametrize('path', (
        '/searchapp', '/searchapp/',
        '/searchapp/searchapp', '/searchapp/searchapp/',
        '/jsonproxy', '/searchapi', '/brosearch',
        '/search/searchapi'
    ))
    def test_searchapp_ll(self, query, path):
        query.set_query_type(SEARCH_APP)
        query.set_url(path)
        query.replace_params({'ll': '55.13,37.14'})
        ll = self.json_dump_request(query, 'rdat.cgi.args.ll')
        assert ll[0] == '37.14,55.13'  # swap original order (lat,lon) => (lon,lat)

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
        resp = self.request(query)
        assert '&ll=37.14%2C55.13' in resp.content
        query.replace_params({'export': 'json'})
        resp = self.json_request(query)
        assert '37.14,55.13' == resp.data['cgidata']['args']['ll'][0]

    # change /searchapp to /search/touch/?ui=webmobileapp.yandex
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-54274')
    # change order of coordinates in ll param
    @pytest.mark.ticket('SERP-60083')
    @pytest.mark.parametrize('path', (
        '/searchapp', '/searchapp/',
        '/searchapp/searchapp', '/searchapp/searchapp/'
    ))
    def test_searchapp_rewrite(self, query, path):
        query.set_query_type(SEARCH_APP)
        query.set_url(path + '?text=sport&lr=213&ll=55.13,37.14')
        url = self.json_dump_request(query, 'rdat.cgi')
        assert re.search('yandex.ru/search/touch/\?', url)
        assert re.search('ui=webmobileapp\.yandex', url)
        # SERP-63007
        assert re.search('service=www.yandex', url)
        # SERP-60083
        pos = re.search('[?&]ll=([^&]+)', url)
        assert pos is not None
        assert pos.group(1) == '37.14%2C55.13'  # swap original order (lat,lon) => (lon,lat)

    # set flag 'multipart' for /searchapp requests
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-55194')
    @pytest.mark.parametrize('path', (
        '/searchapp', '/searchapp/searchapp'
    ))
    def test_searchapp_multipart_flag(self, query, path):
        query.set_query_type(SEARCH_APP)
        query.set_url(path + '?text=sport&lr=213')
        resp = self.json_dump_request(query, 'rdat.flags.all')
        assert 'multipart' in resp
        assert resp['multipart'] == 1

    # no 'multipart' flag with either serp_only or dbg_serp_only
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-57335')
    @pytest.mark.parametrize('path', (
        '/searchapp', '/searchapp/searchapp'
    ))
    @pytest.mark.parametrize('param', (
        'serp_only', 'dbg_serp_only'
    ))
    def test_searchapp_no_multipart_flag(self, query, path, param):
        query.set_query_type(SEARCH_APP)
        query.set_url(path + '?text=sport&lr=213&' + param + '=1')
        resp = self.json_dump_request(query, 'rdat.flags.all')
        assert 'multipart' not in resp

    # parse lang into l10n/tld pair, remove lang afterwards
    # allowed lang/tld combinations are in I18N/Language.pm
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-54417')
    @pytest.mark.parametrize('lang, l10n, tld', [
        ('ru-RU', 'ru', 'ru'),
        ('en-RU', 'ru', 'ru'),
        ('uk-RU', 'uk', 'ru'),
        ('ru-BY', 'ru', 'by'),
        ('be-BY', 'be', 'by'),
        ('ru-UA', 'ru', 'ua'),
        ('uk-UA', 'uk', 'ua'),
        ('tr-TR', 'tr', 'com.tr'),
        ('en-US', 'en', 'com'),  # SERP-60848
        ('en-CN', 'en', 'com')   # SERP-60848
    ])
    @pytest.mark.parametrize('query_type', (
        SEARCH_APP, JSON_PROXY
    ))
    def test_searchapp_lang(self, query, lang, tld, l10n, query_type):
        query.set_query_type(query_type)
        query.replace_params({'lang': lang})
        rdat = self.json_dump_request(query, 'rdat')
        assert rdat['tld'] == tld
        assert rdat['language'] == l10n

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-54576')
    @pytest.mark.parametrize("tld, platform, bpage, bshost_tld, app_version", [
        (RU, 'android', 203, None, 8000500),
        (UA, 'android', 204, None, 8000500),
        (KZ, 'android', 205, None, 8000500),
        (BY, 'android', 206, None, 8000500),
        (COMTR, 'android', 207, RU, 8000500),
        (RU, 'android', 383474, None, 8000400),
        (UA, 'android', 383475, None, 8000400),
        (KZ, 'android', 383476, None, 8000400),
        (BY, 'android', 383477, None, 8000400),
        (COMTR, 'android', 383478, RU, 8000400),
        (COM, 'android', 171, RU, 8000500),  # default page for com
        (RU, 'apad', 188, None, 8000500),
        (UA, 'apad', 189, None, 8000500),
        (KZ, 'apad', 190, None, 8000500),
        (BY, 'apad', 191, None, 8000500),
        (COMTR, 'apad', 192, RU, 8000500),
        (COM, 'apad', 188, RU, 8000500),
        (RU, '', 171, None, 8000500)    # unknown platform => default bpage
    ])
    def test_searchapp_bpage(self, query, tld, platform, bpage, bshost_tld, app_version):
        query.set_query_type(SEARCH_APP)
        query.set_host(tld)
        query.replace_params({'app_platform': platform})
        if app_version != None:
            query.replace_params({'app_version': app_version})

        for yabs_setup in self.json_dump_context(query, ['yabs_setup']):
            if bshost_tld is not None:
                tld = bshost_tld
            assert yabs_setup['metahost'] == ['YABS:yabs.yandex.' + tld + ':80/code/' + str(bpage) + '?']

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-54525')
    def test_searchapp_params(self, query):
        """
        Проверяем, что параметры не отфильтровываются
        """
        params = [
            "app_id",
            "model",
            "manufacturer",
            "mobile-connection-type",
            "os_version",
            "app_platform",
            "app_version",
        ]

        cgi = {
            x: 1 for x in params
        }

        query.set_query_type(SEARCH_APP)
        query.replace_params(cgi)

        resp = self.json_dump_request(query, 'rdat.cgi.args')
        for param in params:
            assert param in resp

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

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('WEBREPORT-382')
    def test_searchapp_response(self, query):
        query.set_query_type(SEARCH_APP)
        query.set_params({'lr': 213, 'serp_only': 1})

        passed = 0
        for text in [
            u"пластиковые окна",
            u"пицца",
            u"купить смартфон",
            u"переезд"
        ]:
            if self.run_searchapp_response_test(query, text):
                passed += 1
            else:
                print "Warning: no banner for " + text.encode('UTF-8')

        assert passed > 0

    @pytest.mark.ticket('SERP-82464')
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_long_text_redirect(self, query):
        query.set_query_type(SEARCH_APP)
        text = "0123456789" * 40 + "012345"  # long text
        query.set_internal()
        query.replace_params({'lr': 213, 'serp_only': 1, 'text': text})
        query.replace_params({'redirect_searchapp_on_long_queries': 1})

        resp = self.request(query, require_status=302)

        assert 'Location' in resp.headers
        location = resp.headers["Location"]
        assert location[0].startswith('/search/touch/') or location[0].startswith('/searchapp')

        parsed = urlparse.urlparse(location[0])
        params = urlparse.parse_qs(parsed.query)
        assert 'text' in params
        assert len(params['text'][0]) == 401
