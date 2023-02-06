# -*- coding: utf-8 -*-

import pytest
from base64 import b64decode
from copy import deepcopy
from lxml import etree

import yt.yson
from util.tsoy import TSoY
from util.const import HNDL, CTXS, TLD, TEXT, USER_AGENT
from util.helpers import Auth, JsonSchemaValidator, EventLogFrameParser


def get_data_from_yson(item, new_data):
    assert (new_data is not None and isinstance(new_data, list)), 'only list support'

    # иногда приходят не utf8 данные, апхост в этом случае выдает base64_yson
    if ('__content_type' in item and item['__content_type'] == 'base64_yson'):
        new_data.append(yt.yson.loads(b64decode(item['binary'])))


class TestInternal(JsonSchemaValidator, EventLogFrameParser, Auth):
    @pytest.mark.ticket("RUNTIMETESTS-112")
    @pytest.mark.xfail(reason='COOKIERELEVANCE-41', strict=True)
    def test_set_cookie_relevance(self, blackbox, query):
        bb = deepcopy(blackbox)
        tld = TLD.AUTH_TLD[0]
        cookies = []
        resp = Auth.GetCookieRelevanceByTld(bb, tld)

        for r in resp.history:
            for domain in r.cookies.list_domains():
                if not domain.endswith('.' + tld):
                    continue
                for path in r.cookies.list_paths():
                    yp = r.cookies.get('yp', domain=domain, path=path)
                    if yp:
                        parsed_yp = query.ReqYpCookie.parse(yp)
                        if 'sdrlv' in parsed_yp:
                            cookies.append({'domain': domain, 'path': path, 'value': yp})

        assert len(cookies) == 1, 'Problem in https://cookie.serp.yandex.ru/superman_cookie. Got cookies: {}'.format(cookies)
        assert cookies[0]['path'] == '/', 'Problem in https://cookie.serp.yandex.ru/superman_cookie. Got cookie: {}'.format(cookies[0])

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-37047', 'SERP-36068')
    @pytest.mark.parametrize(('path'), [
        HNDL.SEARCH_V,
        HNDL.V
    ])
    @TSoY.yield_test
    def test_version_json(self, query, path):
        query.SetInternal()
        query.SetPath(path)
        query.SetParams({'json': 1})
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        self.validate_schema(js, 'version.json')
        assert 'metainfo' in js

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("external", (False, True))
    @TSoY.yield_test
    def test_eventlog_dump_app_host(self, query, external):
        query.SetDumpFilter(req=[CTXS.INIT])
        query.SetMode(external=external)
        query.SetParams({
            'dump': 'eventlog',
            'nocache': 'da'
        })
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        app_host_params = ctxs['app_host_params'][-1]

        if external:
            # assert 'dump' not in app_host_params
            assert 'dump' in app_host_params  # Запросы для http_adapter всегда внутренние
        else:
            assert app_host_params['dump'] == 'eventlog'

    @pytest.mark.parametrize(('params', 'exists'), [
        ({}, False),
        ({'dump': 'eventlog'}, True)
    ])
    @TSoY.yield_test
    def test_eventlog_report(self, query, params, exists):
        query.SetInternal()
        query.SetParams(params)
        query.SetRequireStatus(200)

        resp = yield query

        if exists:
            assert '//DEBUGINFO[' in resp.text
        else:
            assert '//DEBUGINFO[' not in resp.text


class TestKukaInSources(Auth):
    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(('enable_kuka', 'is_external', 'require_status'), [
        (False, True, 403),
        (False, False, 403),
        (True, True, 403),
        (True, False, 200),
    ])
    @TSoY.yield_test
    def test_kuka_inforequest_access(self, query, kuka, enable_kuka, is_external, require_status):
        query.SetPath(HNDL.SEARCH_INFOREQUEST)
        if is_external:
            query.SetExternal()
        else:
            query.SetInternal()
        if enable_kuka:
            self.EnableKuka(query, kuka)
        query.SetRequireStatus(require_status)

        yield query

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @TSoY.yield_test
    def test_kuka_ajax_wrong_2(self, query, kuka):
        """
        Без правильных параметров ручка выдает ошибку
        """
        self.EnableKuka(query, kuka)
        query.SetPath(HNDL.SEARCH_INFOREQUEST)
        query.SetRequireStatus(200)

        resp = yield query
        assert resp.text.startswith('error:')

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @TSoY.yield_test
    def test_kuka_inforequest(self, query, kuka):
        """
        С правильными параметрами ручка не должна выдавать ошибку
        В URL указываем хост, который на любой запрос средиректит.
        Нам достаточно задетектить, что редирект произошел
        """
        self.EnableKuka(query, kuka)
        query.SetMethod('POST')
        query.SetPath(HNDL.SEARCH_INFOREQUEST)
        query.SetParams({
            'request_type': 'meta-response',
            'doc_url': 'https://www.1tv.ru/live',
            'user_request': '1::221'
        })
        query.SetData({
            'url': 'http://hamster.yandex.ru/yandsearch?search_info=da&reqinfo=RUNTIMETESTS-integration-tests',
            'content_type': 'text/plain'
        })
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.text.startswith('error when sending HTTP request: "302"')

    @TSoY.yield_test
    def test_sbh(self, query):
        query.SetParams({
            'sbh': 1,
            'text': 'путин'
        })
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][-1]

        assert 'dump' not in noapache_setup['global_ctx']
        assert noapache_setup['client_ctx']['WEB']['fsgta'] == ['_SearcherHostname']
        assert 'search_info' not in noapache_setup['client_ctx']['WEB']

    @TSoY.yield_test
    def test_kuka_relevance1(self, query, kuka):
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        self.EnableKuka(query, kuka)
        query.SetRequireStatus(200)

        resp = yield query
        noapache_setup = resp.GetCtxs()['noapache_setup'][-1]

        new_data = []
        # иногда приходят не utf8 данные, апхост в этом случае выдает base64_yson
        get_data_from_yson(noapache_setup, new_data)
        if len(new_data):
            noapache_setup = new_data[0]

        assert 'dump' not in noapache_setup['global_ctx']
        assert noapache_setup['client_ctx']['WEB']['fsgta'] == ['_SearcherHostname']
        assert noapache_setup['client_ctx']['WEB']['search_info'] == ['da']
        assert 'scheme_RuleParamsForKuka={Enabled:1}' in noapache_setup['global_ctx']['rearr'][0]


class TestKukaInTemplates(Auth):
    @pytest.mark.parametrize(('dbgwzr', 'expect'), [
        (-1, 0),
        (0, 0),
        (1, 1),
        (2, 2),
        (3, 2),
    ])
    @TSoY.yield_test
    def test_dbgwzr_tmpl(self, query, dbgwzr, expect):
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetParams({
            'dbgwzr': dbgwzr,
            'text': 'hello'
        })
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        sp = js['reqdata']['special_prefs']
        assert sp['debug_wizard'] == expect
        assert not sp['view_search_hosts']
        assert not sp['show_stuff']
        assert not sp['view_relevance']

    @TSoY.yield_test
    def test_sbh(self, query):
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetParams({
            'sbh': 1,
            'text': 'путин'
        })
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        sp = js['reqdata']['special_prefs']
        assert sp['view_search_hosts']
        assert not sp['show_stuff']
        assert not sp['debug_wizard']
        assert not sp['view_relevance']

        assert 'eventlog' not in js or js['eventlog'] == ''
        # assert js['url_event_name']

        rw = js['search']['request_wizards']
        # https://a.yandex-team.ru/arc/trunk/arcadia/web/report/lib/YxWeb/Util/Template/JS.pm?rev=2474873#L282
        assert not rw or len(rw) > 2

        props = js['search_props']['UPPER'][0]['properties']
        scheme = props.get('scheme.json.nodump')
        assert not scheme or not scheme.startswith('REPORT:')

    @TSoY.yield_test
    def test_json_dump_sp(self, query):
        query.SetInternal()
        query.SetParams({
            'json_dump': 'search_props',
            'text': 'путин'
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert len(js['search_props']) > 5

    @TSoY.yield_test
    def test_show_stuff(self, query):
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetParams({
            'show_stuff': 1
        })
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        sp = js['reqdata']['special_prefs']
        assert sp['show_stuff']
        assert not sp['view_search_hosts']
        assert not sp['debug_wizard']
        assert not sp['view_relevance']

    @TSoY.yield_test
    def test_kuka_relevance2(self, query, kuka):
        self.EnableKuka(query, kuka)
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetRequireStatus(200)

        resp = yield query
        template_data = resp.GetCtxs()['template_data'][0]

        # иногда приходят не utf8 данные, апхост в этом случае выдает base64_yson
        new_data = []
        get_data_from_yson(template_data, new_data)
        if len(new_data):
            template_data = new_data[0]

        js = template_data['data']

        sp = js['reqdata']['special_prefs']
        assert sp['view_relevance']
        assert not sp['view_search_hosts']
        assert not sp['show_stuff']
        assert not sp['debug_wizard']

        rw = js['search']['request_wizards']
        # https://a.yandex-team.ru/arc/trunk/arcadia/web/report/lib/YxWeb/Util/Template/JS.pm?rev=2474873#L282
        assert not rw or len(rw) > 2


class TestInternal_2(Auth):
    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize(("path"), [
        (HNDL.SEARCH_INFOREQUEST),
        (HNDL.SEARCH_CHECKCONFIG),
        (HNDL.SEARCH_VIEWCONFIG),
    ])
    @pytest.mark.parametrize(("tld"), [
        (TLD.RU),
        pytest.param(TLD.UA, marks=pytest.mark.xfail(reason="RUNTIMETESTS-143")),
    ])
    @TSoY.yield_test
    def test_nomda(self, query, kuka, path, tld):
        self.EnableKuka(query, kuka)
        query.SetPath(path)
        query.SetDomain(tld)
        query.SetRequireStatus(200)

        yield query

    @pytest.mark.ticket('SERP-43610')
    @pytest.mark.ticket('RUNTIMETESTS-144')
    @pytest.mark.parametrize(('flag'), ('qqq', 'www'))
    @TSoY.yield_test
    def test_flag_via_cgi(self, query, flag):
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetFlags({
            'force_https': flag
        })
        query.SetRequireStatus(200)

        resp = yield query

        flags = resp.GetCtxs()['flags']
        assert len(flags) != 0

        assert flags[0]['all']['force_https'] == flag

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.ticket("LAAS-1799")
    @pytest.mark.parametrize(('is_external, content_type'), [
        (True, 'text/html'),
        (False, 'application/json'),
    ])
    @pytest.mark.parametrize(('params'), (
        {},
        {'test-mode': 0},
        {'test-mode': 1},
    ))
    @TSoY.yield_test
    def test_json_dump(self, query, params, is_external, content_type):
        if is_external:
            query.SetExternal()
        else:
            query.SetInternal()
        query.SetParams({
            'json_dump': 'reqdata.ua'
        })
        query.SetParams(params)
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.headers.get('Content-Type').startswith(content_type)
        if is_external:
            assert not resp.text.startswith('{')
        else:
            assert resp.json().get('reqdata.ua')

    @pytest.mark.ticket('SERP-44238')
    @TSoY.yield_test
    def test_rawdump(self, query):
        query.SetInternal()
        query.SetParams({
            'raw_dump': 'reqparam.user_request.0'
        })
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.text == TEXT + "\n"

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize(("path", 'params'), [
        (HNDL.SEARCH_CHECK_CONDITION, {'condition': 'touch || smart'}),
    ])
    @TSoY.yield_test
    def test_search_check_condition(self, query, path, params):
        query.SetInternal()
        query.SetPath(path)
        query.SetParams(params)
        query.SetRequireStatus(200)

        resp = yield query

        js = resp.json()
        assert 0 == js['error_code']
        assert 'OK' == js['status']

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("path", [
        HNDL.SEARCH_VERSIONS,
        HNDL.SEARCH_V,
        HNDL.SEARCH_VL
    ])
    @TSoY.yield_test
    def test_search_versions(self, query, path):
        query.SetInternal()
        query.SetPath(path)
        query.SetRequireStatus(200)

        resp = yield query

        assert 'report' in resp.text
        assert 'apache' in resp.text

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @TSoY.yield_test
    def test_search_all_supported_params(self, query):
        query.SetInternal()
        query.SetPath(HNDL.SEARCH_ALLSUPPORTEDPARAMS)
        query.SetRequireStatus(200)

        resp = yield query

        assert 'Параметр' in resp.text
        assert 'json_dump' in resp.text

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @TSoY.yield_test
    def test_search_all_supported_flags(self, query):
        query.SetInternal()
        query.SetPath(HNDL.SEARCH_ALLSUPPORTEDFLAGS)
        query.SetRequireStatus(200)

        resp = yield query

        assert 'Описание' in resp.text
        assert 'disable_https' in resp.text

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @TSoY.yield_test
    def test_search_tail_log(self, query):
        query.SetInternal()
        query.SetPath(HNDL.SEARCH_TAILLOG)
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.text

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @TSoY.yield_test
    def test_search_viewconfig(self, query):
        query.SetInternal()
        query.SetPath(HNDL.SEARCH_VIEWCONFIG)
        query.SetRequireStatus(200)

        resp = yield query

        assert '<SearchSource>' in resp.text

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-41451')
    @pytest.mark.parametrize("flags", [
        'checkconfig:read-only:1;deep:gg',
        'wrong_param',
        'checkconfig:read-only:1;deep:-1',
    ])
    @TSoY.yield_test
    def test_info_checkconfig_error(self, query, flags):
        query.SetInternal()  # TODO external
        query.SetParams({'info': flags})
        query.SetPath(HNDL.SEARCH_CHECKCONFIG)
        query.SetRequireStatus(200)

        resp = yield query

        parser = etree.XMLParser()
        root = etree.fromstring(resp.content, parser)

        assert root.xpath('//check-config/error')

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-41451')
    @pytest.mark.parametrize("flags", [
        'checkconfig:read-only:1;deep:1',
        'checkconfig:read-only:1;deep:2'
    ])
    @TSoY.yield_test
    def test_info_checkconfig_source_exists(self, query, flags):
        query.SetInternal()  # TODO external
        query.SetParams({'info': flags})
        query.SetPath(HNDL.SEARCH_CHECKCONFIG)
        query.SetRequireStatus(200)

        resp = yield query

        parser = etree.XMLParser()
        root = etree.fromstring(resp.content, parser)

        assert root.xpath('//source')

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-41451')
    @pytest.mark.parametrize("flags", [
        'checkconfig:read-only:1;deep:0'
    ])
    @TSoY.yield_test
    def test_info_checkconfig_deep0(self, query, flags):
        query.SetInternal()  # TODO external
        query.SetParams({'info': flags})
        query.SetPath(HNDL.SEARCH_CHECKCONFIG)
        query.SetRequireStatus(200)

        resp = yield query

        parser = etree.XMLParser()
        root = etree.fromstring(resp.content, parser)

        assert root.xpath('//source') == []

        dict = {}

        for child in root:
            if child.__len__() > 1:
                dict[child[0].text] = child[1].text
            else:
                dict[child[0].text] = ''

        assert 'revision' in dict
        assert dict['host']

    # Internal params work for these user agents
    @pytest.mark.ticket('SERP-62360')
    @pytest.mark.parametrize("ua", [
        'Mozilla/5.0 (Linux; Android 6.0.1; SM-G925F Build/MMB29K; wv) AppleWebKit/537.36 (KHTML, like Gecko) '
        'Version/4.0 Chrome/64.0.3282.137 Mobile Safari/537.36 YandexSearch/7.30 YandexSearchWebView/7.30',
        USER_AGENT.SEARCHAPP_ANDROID,
        USER_AGENT.JSON_PROXY_ANDROID,
        'some other user agent',
        'YandexSearchBrowser',
        'YandexSearch-something'
    ])
    @TSoY.yield_test
    def test_user_agent_internal(self, query, ua):
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetInternal()
        query.SetPath(HNDL.SEARCHAPP)
        query.SetUserAgent(ua)
        query.SetRequireStatus(200)
        my_flag = 'super_puper'
        query.SetParams({'flags': my_flag + '=1'})

        resp = yield query

        flags = resp.GetCtxs()['flags'][0]['all']
        assert my_flag in flags

    # Internal params do not work for these user agents
    @pytest.mark.ticket('SERP-62360')
    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("ua", [
        'Some other Yandex user agent',
        'Some user agent for Yandex',
        'YandexSearchRobot',
        'YandexRobot'
    ])
    @TSoY.yield_test
    def test_robot_user_agent(self, query, ua):
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetInternal()
        query.SetUserAgent(ua)
        query.SetRequireStatus(200)
        my_flag = 'super_puper'
        query.SetParams({'flags': my_flag + '=1'})

        resp = yield query

        flags = resp.GetCtxs()['flags'][0]['all']
        assert my_flag not in flags

    @pytest.mark.ticket('SERP-60902')
    @TSoY.yield_test
    def test_app_host_timeout(self, query):
        query.SetDumpFilter(req=[CTXS.INIT])
        query.SetParams({'timeout': '130100'})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        app_host_params = ctxs['app_host_params'][-1]

        assert app_host_params["timeout"] == '130100'

    @pytest.mark.ticket('WEBREPORT-72')
    @TSoY.yield_test
    def test_graphrwr_flag(self, query):
        query.SetDumpFilter(req=[CTXS.INIT])
        value = 'test1:test2:80'
        query.SetFlags({'graphrwr': value})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        app_host_params = ctxs['app_host_params'][-1]

        assert value in app_host_params["graphrwr"]

    @TSoY.yield_test
    def test_adjust_serp_size(self, query):
        """
        проверяем количество документов на выдаче. флаг adjust_serp_size
        """
        query.SetInternal()
        query.SetRequireStatus(200)
        query.SetParams({'text': 'котики'})
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])

        resp = yield query
        template_data = resp.GetCtxs()['template_data'][0]
        docs = list(filter(lambda x: x['server_descr'] != 'FAKE', template_data['data']['searchdata']['docs']))
        assert len(docs) == 10
