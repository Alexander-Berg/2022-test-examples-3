# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import HNDL, LANG, L10N, TLD, DEVICE, TEMPLATE, USER_AGENT, SEARCH_APP, TOUCH, JSON_PROXY, CTXS


class TestSearchApp():
    @pytest.mark.ticket('SERP-60083')    # Don't swap coordinates in ll param for search/touch
    @pytest.mark.ticket('SERP-71275')    # Check ll param
    @TSoY.yield_test
    def test_search_touch_ll(self, query):
        query.SetQueryType(TOUCH)
        query.SetParams({'ll': '55.13,37.14'})
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['region'][0]['geopoint']['location'] == ["55.130000", "37.140000"]

    @pytest.mark.ticket('SERP-71275')    # Check ll param
    @pytest.mark.parametrize(('path'), [
        (HNDL.SEARCHAPP),
        (HNDL.SEARCHAPP_SEARCHAPP),
        (HNDL.JSONPROXY)
    ])
    @TSoY.yield_test
    def test_geopoint_ll_swap(self, query, path):
        query.SetQueryType(SEARCH_APP)
        query.SetPath(path)
        query.SetParams({'ll': '55.13,37.14'})
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['region'][0]['geopoint']['location'] == ["37.140000", "55.130000"]

    @pytest.mark.ticket('SERP-71275')    # Check ll param
    @pytest.mark.parametrize(('path'), [
        (HNDL.SEARCHAPP_META)
    ])
    @TSoY.yield_test
    def test_geopoint_ll_swap_searchappmeta(self, query, path):
        query.SetQueryType(SEARCH_APP)
        query.SetPath(path)
        query.SetParams({'ll': '55.13,37.14'})
        query.SetRequireStatus(200)

        resp = yield query

        assert '&ll=37.14%2C55.13' in resp.text

    @pytest.mark.ticket('SERP-71275')    # Check ll param
    @pytest.mark.parametrize(('path'), [
        (HNDL.SEARCHAPP_META)
    ])
    @TSoY.yield_test
    def test_geopoint_ll_swap_searchappmeta_json(self, query, path):
        query.SetDumpFilter(resp=[CTXS.HANDLER_OUTPUT])
        query.SetQueryType(SEARCH_APP)
        query.SetPath(path)
        query.SetParams({
            'll': '55.13,37.14'
        })
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert js['cgidata']['args']['ll'][0] == '37.14,55.13'

    # Don't insert ui=webmobileapp.yandex into /search/touch requests
    # change order of coordinates in ll param
    @pytest.mark.ticket('SERP-60083')
    @pytest.mark.ticket('SERP-54274')
    @TSoY.yield_test
    def test_search_touch_no_rewrite(self, query):
        query.SetQueryType(TOUCH)
        query.SetParams({'ll': '55.13,37.14'})
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['request'][0]['path'] == HNDL.SEARCH_TOUCH[1:]
        assert 'ui' not in ctxs['request'][0]['params'] or ctxs['request'][0]['params']['ui'] != 'webmobileapp.yandex'
        assert ctxs['request'][0]['params']['ll'] == ['55.13,37.14']

    # swap coordinates in ll param (searchapp uses "lat,lon")
    @pytest.mark.ticket('SERP-60083')
    @pytest.mark.ticket('SERP-67548')
    @pytest.mark.ticket('WEBREPORT-348')
    @pytest.mark.parametrize(('path'), [
        (HNDL.SEARCHAPP),
        (HNDL.SEARCHAPP_FLD),
        (HNDL.SEARCHAPP_SEARCHAPP),
        (HNDL.SEARCHAPP_SEARCHAPP_FLD),
        (HNDL.JSONPROXY),
        (HNDL.SEARCHAPI),
        (HNDL.BROSEARCH),
        (HNDL.SEARCH_SEARCHAPI)
    ])
    @TSoY.yield_test
    def test_searchapp_ll(self, query, path):
        query.SetQueryType(SEARCH_APP)
        query.SetPath(path)
        query.SetParams({'ll': '55.13,37.14'})
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['request'][0]['params']['ll'] == ['37.14,55.13']

    # swap coordinates in ll param (searchapp uses "lat,lon")
    @pytest.mark.ticket('SERP-67548')
    @pytest.mark.parametrize(('path'), [
        (HNDL.SEARCHAPP_META)
    ])
    @TSoY.yield_test
    def test_searchapp_meta_ll(self, query, path):
        query.SetQueryType(SEARCH_APP)
        query.SetPath(path)
        query.SetParams({'ll': '55.13,37.14'})
        query.SetRequireStatus(200)

        resp = yield query

        assert '&ll=37.14%2C55.13' in resp.text

    # swap coordinates in ll param (searchapp uses "lat,lon")
    @pytest.mark.ticket('SERP-67548')
    @pytest.mark.parametrize(('path'), [
        (HNDL.SEARCHAPP_META)
    ])
    @TSoY.yield_test
    def test_searchapp_meta_ll_json(self, query, path):
        query.SetDumpFilter(resp=[CTXS.HANDLER_OUTPUT])
        query.SetQueryType(SEARCH_APP)
        query.SetPath(path)
        query.SetParams({
            'll': '55.13,37.14'
        })
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert '37.14,55.13' == js['cgidata']['args']['ll'][0]

    # change /searchapp to /search/touch/?ui=webmobileapp.yandex
    @pytest.mark.ticket('SERP-54274')
    # change order of coordinates in ll param
    @pytest.mark.ticket('SERP-60083')
    @pytest.mark.parametrize(('path'), [
        (HNDL.SEARCHAPP),
        (HNDL.SEARCHAPP_FLD),
        (HNDL.SEARCHAPP_SEARCHAPP),
        (HNDL.SEARCHAPP_SEARCHAPP_FLD),
    ])
    @TSoY.yield_test
    def test_searchapp_rewrite(self, query, path):
        query.SetQueryType(SEARCH_APP)
        query.SetParams({
            'text': 'sport',
            'lr': 213,
            'll': '55.13,37.14'
        })
        query.SetPath(path)
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        # SERP-60083
        assert ctxs['request'][0]['params']['ll'] == ['37.14,55.13']  # swap original order (lat,lon) => (lon,lat)
        assert ctxs['request'][0]['path'] == HNDL.SEARCH_TOUCH[1:]
        assert ctxs['request'][0]['params']['ui'] == ['webmobileapp.yandex']
        # SERP-63007
        assert ctxs['request'][0]['params']['service'] == ['www.yandex']

    # set flag 'multipart' for /searchapp requests
    @pytest.mark.ticket('SERP-55194')
    @pytest.mark.parametrize(('path'), [
        (HNDL.SEARCHAPP),
        (HNDL.SEARCHAPP_SEARCHAPP),
    ])
    @TSoY.yield_test
    def test_searchapp_multipart_flag(self, query, path):
        query.SetQueryType(SEARCH_APP)
        query.SetPath(path)
        query.SetParams({
            'text': 'sport',
            'lr': 213
        })
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert 'multipart' in ctxs['flags'][0]['all']
        assert ctxs['flags'][0]['all']['multipart'] == 1

    # no 'multipart' flag with either serp_only or dbg_serp_only
    @pytest.mark.ticket('SERP-57335')
    @pytest.mark.parametrize(('path'), [
        (HNDL.SEARCHAPP),
        (HNDL.SEARCHAPP_SEARCHAPP),
    ])
    @pytest.mark.parametrize(('param'), [
        ('serp_only'),
        ('dbg_serp_only')
    ])
    @TSoY.yield_test
    def test_searchapp_no_multipart_flag(self, query, path, param):
        query.SetQueryType(SEARCH_APP)
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetPath(path)
        query.SetParams({
            'text': 'sport',
            'lr': 213,
            param: 1
        })
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert 'multipart' not in ctxs['flags'][0]['all']

    # parse lang into l10n/tld pair, remove lang afterwards
    # allowed lang/tld combinations are in I18N/Language.pm
    @pytest.mark.ticket('SERP-54417')
    @pytest.mark.parametrize('lang, l10n, tld', [
        (LANG.RU_RU, L10N.RU, TLD.RU),
        (LANG.EN_RU, L10N.RU, TLD.RU),
        (LANG.UK_RU, L10N.UK, TLD.RU),
        (LANG.RU_BY, L10N.RU, TLD.BY),
        (LANG.BE_BY, L10N.BE, TLD.BY),
        (LANG.RU_UA, L10N.RU, TLD.UA),
        (LANG.UK_UA, L10N.UK, TLD.UA),
        (LANG.TR_TR, L10N.TR, TLD.COMTR),
        (LANG.EN_US, L10N.EN, TLD.COM),
        (LANG.EN_CN, L10N.EN, TLD.COM)
    ])
    @pytest.mark.parametrize('query_type', (
        SEARCH_APP, JSON_PROXY
    ))
    @TSoY.yield_test
    def test_searchapp_lang(self, query, lang, tld, l10n, query_type):
        query.SetQueryType(query_type)
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetParams({'lang': lang})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['report'][0]['tld'] == tld
        assert ctxs['report'][0]['language'] == l10n

    @pytest.mark.ticket('SERP-54576')
    @pytest.mark.parametrize("tld, device, bpage, bshost_tld, app_version", [
        (TLD.RU, DEVICE.ANDROID, 203, None, 8000500),
        pytest.param(TLD.UA, DEVICE.ANDROID, 204, None, 8000500, marks=pytest.mark.xfail(reason="RUNTIMETESTS-143")),
        (TLD.KZ, DEVICE.ANDROID, 205, None, 8000500),
        (TLD.BY, DEVICE.ANDROID, 206, None, 8000500),
        (TLD.COMTR, DEVICE.ANDROID, 207, TLD.RU, 8000500),
        (TLD.RU, DEVICE.ANDROID, 383474, None, 8000400),
        pytest.param(TLD.UA, DEVICE.ANDROID, 383475, None, 8000400, marks=pytest.mark.xfail(reason="RUNTIMETESTS-143")),
        (TLD.KZ, DEVICE.ANDROID, 383476, None, 8000400),
        (TLD.BY, DEVICE.ANDROID, 383477, None, 8000400),
        (TLD.COMTR, DEVICE.ANDROID, 383478, TLD.RU, 8000400),
        (TLD.COM, DEVICE.ANDROID, 171, TLD.RU, 8000500),  # default page for com
        (TLD.RU, DEVICE.APAD, 188, None, 8000500),
        pytest.param(TLD.UA, DEVICE.APAD, 189, None, 8000500, marks=pytest.mark.xfail(reason="RUNTIMETESTS-143")),
        (TLD.KZ, DEVICE.APAD, 190, None, 8000500),
        (TLD.BY, DEVICE.APAD, 191, None, 8000500),
        (TLD.COMTR, DEVICE.APAD, 192, TLD.RU, 8000500),
        (TLD.COM, DEVICE.APAD, 188, TLD.RU, 8000500),
        (TLD.RU, '', 171, None, 8000500)    # unknown platform => default bpage
    ])
    @TSoY.yield_test
    def test_searchapp_bpage(self, query, tld, device, bpage, bshost_tld, app_version):
        query.SetQueryType(SEARCH_APP)
        query.SetDomain(tld)
        query.SetParams({'app_platform': device})
        if app_version is not None:
            query.SetParams({'app_version': app_version})
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        if bshost_tld is not None:
            tld = bshost_tld
        assert ctxs['yabs_setup'][0]['metahost'] == ['YABS:yabs.yandex.' + tld + ':80/code/' + str(bpage) + '?']

    @pytest.mark.ticket('SERP-54525')
    @TSoY.yield_test
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
        query.SetQueryType(SEARCH_APP)
        query.SetParams(cgi)
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        for param in params:
            assert param in ctxs['request'][0]['params']

    @pytest.mark.ticket('SERP-72536')
    @pytest.mark.parametrize("path, ua, template", [
        (HNDL.SEARCHAPP, USER_AGENT.PAD, TEMPLATE.PHONE),
        (HNDL.SEARCHAPP, USER_AGENT.PADAPP, TEMPLATE.PHONE),
        (HNDL.SEARCHAPP, USER_AGENT.MOBILE, TEMPLATE.PHONE),
        (HNDL.SEARCH_TOUCH, USER_AGENT.MOBILE, TEMPLATE.PHONE)
    ])
    @pytest.mark.parametrize("lite, prefix", [
        (0, 'web4'),
        (1, 'granny_exp')
    ])
    @TSoY.yield_test
    def test_searchapp_template(self, query, path, ua, template, lite, prefix):
        query.SetPath(path)
        query.SetUserAgent(ua)
        if lite:
            query.SetParams({'lite': lite})
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['report'][0]['template_path'] == "v8:{}:{}".format(prefix, template)

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('WEBREPORT-86')
    @pytest.mark.parametrize("path", [
        '/searchapp/sdch/some/test.dict',
        '/searchapp/sdch/yandex.ru/http/hC6cDRqW.dict'
    ])
    @TSoY.yield_test
    def test_searchapp_java_sdch(self, query, path):
        query.SetQueryType(SEARCH_APP)
        query.SetPath(path)
        query.ResetParams()
        query.SetRequireStatus(require_status=[404])

        yield query

    @pytest.mark.ticket('SERP-72300')
    @pytest.mark.parametrize('text', [
        (u"пицца"),
        (u"купить смартфон"),
        (u"переезд")
    ])
    @TSoY.yield_test
    def test_jsonproxy_response(self, query, text):
        query.SetQueryType(JSON_PROXY)
        query.SetParams({
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
            'br_lon': '37.59328353881836',
            'text': text
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        doc = js["docs"][0]
        assert doc["type"] == "sites"
        assert len(doc["list"]) >= 10
        wizard = doc["list"][0]
        assert wizard["type"] == "wizards"
        assert wizard["subtype"] in ['banner', 'market']

    @pytest.mark.ticket('SERP-72300')
    @pytest.mark.parametrize('text', [
        (u"пицца"),
        (u"купить смартфон"),
        (u"переезд")
    ])
    @TSoY.yield_test
    def test_jsonproxy_response_check_metahost(self, query, text):
        query.SetQueryType(JSON_PROXY)
        query.SetParams({
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
            'br_lon': '37.59328353881836',
            'text': text
        })
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['yabs_setup'][0]['metahost'] == ['YABS:yabs.yandex.ru:80/code/3916?']

    @pytest.mark.ticket('WEBREPORT-382')
    @pytest.mark.parametrize('text', [
        (u"пластиковые окна"),
        (u"пицца"),
        (u"купить смартфон"),
        (u"переезд")
    ])
    @TSoY.yield_test
    def test_searchapp_response(self, query, text):
        query.SetQueryType(SEARCH_APP)
        query.SetParams({
            'lr': 213,
            'serp_only': 1,
            'text': text
        })
        query.SetDumpFilter(resp=[CTXS.WEB_SEARCH])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert 'direct_premium' in ctxs['template_data'][0]['data']['banner']['data']

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-82464')
    @TSoY.yield_test
    def test_long_text_redirect(self, query):
        query.SetQueryType(SEARCH_APP)
        text = "0123456789" * 40 + "012345"  # long text
        query.SetInternal()
        query.SetParams({
            'lr': 213,
            'serp_only': 1,
            'redirect_searchapp_on_long_queries': 1,
            'text': text
        })
        query.SetRequireStatus(302)

        resp = yield query

        assert resp.GetLocation().path == HNDL.SEARCH_TOUCH or resp.GetLocation().path == HNDL.SEARCHAPP
        assert len(resp.GetLocationParams()['text'][0]) == 401
