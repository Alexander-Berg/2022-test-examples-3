# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import L10N, TLD, REGION_BY_TLD, COOKIEMY_LANG, PLATFORM, USER_AGENT, USERAGENT_BY_TYPE, TEXT, HNDL, CTXS, TEMPLATE


class TestTemplates():
    @TSoY.yield_test
    def test_broken_yp(self, query):
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        SIZE_sz = '%231502456097%2Est_browser_s%2E20'
        query.ReqYpCookie.set_sz(SIZE_sz)
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert js['reqdata']['ycookie']['yp']['sz'] == SIZE_sz

    @TSoY.yield_test
    def test_cookies_empty(self, query):
        """
        Проверяем репорт по json-схеме с пустыми куками
        """
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert js['reqdata']['cookie'] == {}

    @pytest.mark.parametrize(('l10n'), (
        L10N.RU, L10N.UK, L10N.KK, L10N.TT
    ))
    @TSoY.yield_test
    def test_lang_switcher(self, query, l10n):
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetDomain(TLD.KZ)
        query.SetRegion(REGION_BY_TLD[TLD.RU])
        query.SetCookies({'my': COOKIEMY_LANG[l10n]})
        query.SetParams({
            'lr': REGION_BY_TLD[TLD.KZ]
        })
        query.SetRwrHeaders({'X-LaaS-Answered': 1})
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert js['langSwitcher']
        assert js['langSwitcher']['current']['code'] == l10n

    @TSoY.yield_test
    def test_lang_switcher_empty(self, query):
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert not js['langSwitcher']

    @TSoY.yield_test
    def test_rdat_app_version(self, query):
        """
        SERP-36011 - Не могу найти cgidata в выдаче с флагом export=json
        Смотрим, что параметр присутствует
        SERP-35756 - Прокидывать до верстки в rdat параметр и значение app_version
        Проверяем, что параметр должен приходит в cgidata
        """
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetParams({
            'app_version': '317'
        })
        query.SetQueryType(PLATFORM.PAD)
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert js['cgidata']['args']['app_version'][0] == '317'

    @pytest.mark.ticket('SERP-37057')
    @pytest.mark.ticket('RUNTIMETESTS-144')
    @TSoY.yield_test
    def test_header_tablet(self, query):
        """
        SERP-37057 - Добавить новый флаг appsearch_header_tablet=1 для планшетных приложений я.поиск
        В случае наличия хедера X-Yandex-Flags: appsearch-header-tablet=1 пробрасываем на выдачу appsearch-header-tablet=1
        """
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetHeaders({'X-Yandex-Flags': 'appsearch-header-tablet=1'})
        query.SetRequireStatus(200)

        resp = yield query

        flags = resp.GetCtxs()['flags']
        assert len(flags) != 0

        assert flags[0]['all']['appsearch_header_tablet'] == 1

    @pytest.mark.ticket('SERP-37191')
    @TSoY.yield_test
    def test_header_tablet_for_apad(self, query):
        """
        SERP-37191 - Для UA планшетного приложения ЯндексПоиск включать флаг в rdat appsearch_header_tablet=1
        для юзер-агентов apad не редиректить на тачи и выставлять appsearch_header_tablet=1
        """
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetQueryType(PLATFORM.PAD)
        query.SetUserAgent(USER_AGENT.PAD_YANDEX)
        query.SetRequireStatus(200)

        resp = yield query

        # check not granny
        assert resp.GetCtxs()['device_config'][0]['template_name'] == TEMPLATE.WEB4_PHONE

        flags = resp.GetCtxs()['flags']
        assert len(flags) != 0

        assert flags[0]['all']['appsearch_header_tablet'] == 1

    @pytest.mark.ticket('SERP-37191')
    @TSoY.yield_test
    def test_header_tablet_for_apad_no(self, query):
        """
        SERP-37191 - Для UA планшетного приложения ЯндексПоиск включать флаг в rdat appsearch_header_tablet=1
        для юзер-агентов apad не редиректить на тачи и выставлять appsearch_header_tablet=1
        """
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetQueryType(PLATFORM.PAD)
        query.SetUserAgent(USER_AGENT.PAD_ANDROID_4_3)
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert 'appsearch_header_tablet' not in js['reqdata']['flags']

    @TSoY.yield_test
    def test_allowed_params(self, query):
        """
        Тестируем что репорт фильтрует параметры по белому списку
        """
        ALLOWED_PARAMS = ['brorich', 'broload']
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetParams(dict([(x, x) for x in ALLOWED_PARAMS + ['__garbage_param__']]))
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        args = js['cgidata']['args']
        assert '__garbage_param__' not in args

        for p in ALLOWED_PARAMS:
            assert p in args
            assert len(args[p]) == 1
            assert args[p][0] == p

    @pytest.mark.ticket('SERP-46712')
    @pytest.mark.ticket('RUNTIMETESTS-144')
    @TSoY.yield_test
    def test_internal_flags(self, query):
        """
        SERP-46712 - Заголовок для добавления внутренних параметров в запросы
        """
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetFlags({"tmplrwr": {"qqq": 112}})
        query.SetRequireStatus(200)

        resp = yield query

        flags = resp.GetCtxs()['flags']
        assert len(flags) != 0

        assert flags[0]['all']['tmplrwr'] == {'qqq': 112}, "flag set from X-Yandex-Internal-Flags header"

    @pytest.mark.ticket('SERP-40963')
    @TSoY.yield_test
    def test_touch_ucbrowser_ru(self, query):
        """
        SERP-40963 - перенос touchsearch на перловый репорт
        """
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetQueryType(PLATFORM.TOUCH)
        query.SetUserAgent(USER_AGENT.UCBROWSER)
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert js['reqdata']['flags']['direct_page'] == 3895, \
            "direct_page flag is not set to 3895 for UCBrowser/RU"

    @pytest.mark.ticket('SERP-40963')
    @TSoY.yield_test
    def test_touch_ucbrowser_comtr(self, query):
        """
        SERP-40963 - перенос touchsearch на перловый репорт
        """
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetQueryType(PLATFORM.TOUCH)
        query.SetUserAgent(USER_AGENT.UCBROWSER)
        query.SetDomain(TLD.COMTR)
        query.SetParams({'lr': 11508})
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert 'direct_page' not in js['reqdata']['flags'], \
            "direct_page flag is set for UCBrowser/COMTR"

    @TSoY.yield_test
    def test_touch_no_geo_head(self, query):
        """
        SERP-42396 Отключить геошапку на тачах ("Показаны результаты для Москвы")
        """
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetQueryType(PLATFORM.TOUCH)
        query.SetParams({'lr': 21})
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        assert js['localization']['top'] == 0

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("can_app_host, query_type, url_path, prms, template", [
        (1, PLATFORM.DESKTOP, HNDL.SEARCH,                   {"template": "granny:phone"}, 'granny:phone'),
        (1, PLATFORM.PAD,     HNDL.SEARCH_PAD,               {"template": "web4:desktop"}, 'web4:desktop'),
        (1, PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH,             {"template": "web4:desktop"}, 'web4:desktop'),

        (1, PLATFORM.DESKTOP, HNDL.SEARCH,                   {}, 'web4:desktop'),
        (1, PLATFORM.PAD,     HNDL.SEARCH_PAD,               {}, 'web4:desktop'),
        (1, PLATFORM.PADAPP,  HNDL.SEARCHAPP,                {}, 'web4:phone'),
        (1, PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH,             {}, 'web4:phone'),
        (1, PLATFORM.GRANNY,  HNDL.SEARCH,                   {}, 'granny:desktop'),
        pytest.param(1, PLATFORM.DESKTOP, HNDL.SEARCH, {"template": "granny:phone", "tmplrwr": "granny:suggest"}, 'suggest:phone', marks=pytest.mark.xfail(reason="APPHOSTSUPPORT-814")),
        (1, PLATFORM.PAD,     HNDL.SEARCH_PAD,               {"template": "web4:desktop", "tmplrwr": "web4:web_exp"}, 'web_exp:desktop'),
        (1, PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH,             {"template": "web4:desktop", "tmplrwr": "web4:granny"}, 'granny:desktop'),
        pytest.param(1, PLATFORM.DESKTOP, HNDL.SEARCH, {"template": "granny:phone", "tmplrwr": "granny:phone:suggest"}, 'suggest:phone', marks=pytest.mark.xfail(reason="APPHOSTSUPPORT-814")),
        (1, PLATFORM.PAD,     HNDL.SEARCH_PAD,               {"template": "template_id:device_id", "tmplrwr": "template_id:device_id:web_exp:desktop"}, 'web_exp:desktop'),

        (1, PLATFORM.DESKTOP, HNDL.SEARCH,                   {"template": "web4:desktop", "tmplrwr": "web4::web_exp:desktop"}, 'web_exp:desktop'),
        (1, PLATFORM.PAD,     HNDL.SEARCH_PAD,               {"template": "web4:pad",     "tmplrwr": "web4::web_exp:desktop"}, 'web_exp:desktop'),
        (1, PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH,             {"template": "web4:phone",   "tmplrwr": "web4::web_exp:desktop"}, 'web_exp:desktop'),

        (1, PLATFORM.DESKTOP, HNDL.SEARCH,                   {"template": "web4:desktop", "tmplrwr": "web4::web_exp:"}, 'web_exp:desktop'),
        (1, PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH,             {"template": "web4:phone",   "tmplrwr": "web4::web_exp:"}, 'web_exp:phone'),

        (1, PLATFORM.DESKTOP, HNDL.SEARCH,                   {"template": "web4:desktop", "tmplrwr": "web4:::"}, 'web4:desktop'),
        pytest.param(1, PLATFORM.PAD,     HNDL.SEARCH_PAD,               {"template": "web4:pad",     "tmplrwr": "web4:::"}, 'web4:pad', marks=pytest.mark.xfail(reason="APPHOSTSUPPORT-814")),
        (1, PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH,             {"template": "web4:phone",   "tmplrwr": "web4:::"}, 'web4:phone'),

        (1, PLATFORM.DESKTOP, HNDL.SEARCH,                   {"template": "web4:desktop", "tmplrwr": ":pad:web_exp:desktop"}, 'web4:desktop'),
        (1, PLATFORM.PAD,     HNDL.SEARCH_PAD,               {"template": "web3:pad",     "tmplrwr": ":pad:web_exp:desktop"}, 'web_exp:desktop'),
        (1, PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH,             {"template": "granny:phone", "tmplrwr": ":pad:web_exp:desktop"}, 'granny:phone'),

        (1, PLATFORM.DESKTOP, HNDL.SEARCH,                   {"template": "web4:desktop", "tmplrwr": ":pad:web_exp:"}, 'web4:desktop'),
        (1, PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH,             {"template": "granny:phone", "tmplrwr": ":pad:web_exp:"}, 'granny:phone'),

        (1, PLATFORM.DESKTOP, HNDL.SEARCH,                   {"template": "web4:desktop", "tmplrwr": ":pad::"}, 'web4:desktop'),
        pytest.param(1, PLATFORM.PAD,     HNDL.SEARCH_PAD,               {"template": "web4:pad",     "tmplrwr": ":pad::"}, 'web4:pad', marks=pytest.mark.xfail(reason="APPHOSTSUPPORT-814")),
        (1, PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH,             {"template": "web4:phone",   "tmplrwr": ":pad::"}, 'web4:phone'),

        pytest.param(1, PLATFORM.DESKTOP, HNDL.SEARCH,                   {"tmplrwr": "web4:suggest"}, 'suggest:desktop', marks=pytest.mark.xfail(reason="APPHOSTSUPPORT-814")),
        (1, PLATFORM.PAD,     HNDL.SEARCH_PAD,               {"tmplrwr": "web4:web_exp"}, 'web_exp:desktop'),
        (1, PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH,             {"tmplrwr": "web4:granny"},  'granny:phone'),
        (1, PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH,             {"tmplrwr": "web4:phone:granny_exp:phone"},  'granny_exp:phone'),
    ])
    @TSoY.yield_test
    def test_template_name(self, query, can_app_host, query_type, url_path, prms, template):
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetPath(url_path)
        query.SetUserAgent(USERAGENT_BY_TYPE[query_type])

        internal_flags = {
            'json_template_external': 'by_name',
            'json_template': 1,
            'serp3_granny_https': 1
        }

        if 'web_exp' in template:
            internal_flags['srcrwr'] = {
                'LATEST_TEMPLATES_VERSION': 'WEB__RENDERER_WEBEXP'
            }

        query.SetFlags(internal_flags)
        query.SetParams({
            'text': TEXT,
            'searchid': '2053249',
            'web': '0'
        })
        query.SetInternal()  # XXX hack to make internal cgi param 'template' work
        query.SetParams(prms)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['device_config'][-1]['template_name'] == template

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("query_type, url_path, template", [
        (PLATFORM.DESKTOP, HNDL.SEARCH_YANDCACHE, 'cacher_frame:desktop'),
        (PLATFORM.PAD,     HNDL.SEARCH_YANDCACHE, 'cacher_frame:desktop'),
        (PLATFORM.TOUCH,   HNDL.SEARCH_YANDCACHE, 'cacher_frame:desktop'),
        (PLATFORM.GRANNY,  HNDL.SEARCH_YANDCACHE, 'cacher_frame:desktop'),

        (PLATFORM.DESKTOP, HNDL.SEARCH_PADCACHE, 'cacher_frame:pad'),
        (PLATFORM.PAD,     HNDL.SEARCH_PADCACHE, 'cacher_frame:pad'),
        (PLATFORM.TOUCH,   HNDL.SEARCH_PADCACHE, 'cacher_frame:pad'),
        (PLATFORM.GRANNY,  HNDL.SEARCH_PADCACHE, 'cacher_frame:pad'),

        (PLATFORM.DESKTOP, HNDL.SEARCH_TOUCHCACHE, 'cacher_frame:phone'),
        (PLATFORM.PAD,     HNDL.SEARCH_TOUCHCACHE, 'cacher_frame:phone'),
        (PLATFORM.TOUCH,   HNDL.SEARCH_TOUCHCACHE, 'cacher_frame:phone'),
        (PLATFORM.GRANNY,  HNDL.SEARCH_TOUCHCACHE, 'cacher_frame:phone'),

        (PLATFORM.DESKTOP, HNDL.SEARCH_PREFETCH, 'cacher_frame:desktop'),
        (PLATFORM.PAD,     HNDL.SEARCH_PREFETCH, 'cacher_frame:desktop'),
        (PLATFORM.TOUCH,   HNDL.SEARCH_PREFETCH, 'cacher_frame:phone'),
        (PLATFORM.GRANNY,  HNDL.SEARCH_PREFETCH, 'cacher_frame:desktop'),
    ])
    @TSoY.yield_test
    def test_cache_template_name(self, query, query_type, url_path, template):
        query.SetDumpFilter(resp=[CTXS.RENDERER])
        query.SetPath(url_path)
        query.SetUserAgent(USERAGENT_BY_TYPE[query_type])
        query.SetInternal()
        query.SetRequireStatus(200)

        resp = yield query
        template_params = resp.GetCtxs()['template_params'][0]

        assert template_params["template"] == template

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("query_type, url_path, prms, template", [
        (PLATFORM.DESKTOP, HNDL.SEARCH_CATALOGSEARCH, {}, 'sitesearch_search:desktop'),
        (PLATFORM.PAD,     HNDL.SEARCH_CATALOGSEARCH, {}, 'sitesearch_search:desktop'),
        (PLATFORM.TOUCH,   HNDL.SEARCH_CATALOGSEARCH, {}, 'sitesearch_search:phone'),
        (PLATFORM.GRANNY,  HNDL.SEARCH_CATALOGSEARCH, {}, 'sitesearch_search:desktop'),
    ])
    @TSoY.yield_test
    def test_template_name_catalogsearch(self, query, query_type, url_path, prms, template):
        query.SetDumpFilter(req=[CTXS.RENDERER])
        query.SetPath(url_path)
        query.SetUserAgent(USERAGENT_BY_TYPE[query_type])
        query.SetFlags({
            'json_template_external': 'by_name',
            'json_template': 1,
            'serp3_granny_https': 1
        })
        query.SetParams({
            'text': TEXT,
            'searchid': '2053249',
            'web': '0'})
        query.SetInternal()
        query.SetParams(prms)
        query.SetRequireStatus(200)

        resp = yield query
        template_data = resp.GetCtxs()['template_data'][0]

        assert template_data["name"] == template

    @pytest.mark.ticket('SERP-42564')
    @TSoY.yield_test
    def test_familysearch_for_china(self, query):
        query.SetDumpFilter(resp=[CTXS.BLENDER])
        query.SetFlags({'json_template_external': 1})
        query.SetParams({'lr': 134})
        query.SetRequireStatus(200)

        resp = yield query
        template_data = resp.GetCtxs()['template_data'][0]

        assert template_data['data']['reqdata']['prefs']['afamily'] == 2
        assert template_data['data']['reqdata']['prefs']['family'] == 2

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize(('url', 'user_agent'), (
        (HNDL.SEARCH_PRE, USER_AGENT.DESKTOP),
        (HNDL.SEARCH_PAD_PRE, USER_AGENT.PAD),
        (HNDL.SEARCH_TOUCH_PRE, USER_AGENT.TOUCH),
    ))
    @TSoY.yield_test
    def test_renderer_pre(self, query, url, user_agent):
        query.SetPath(url)
        query.SetDumpFilter(resp=[CTXS.HANDLER_OUTPUT, CTXS.INIT_HTTP_RESPONSE])
        query.SetUserAgent(user_agent)
        query.SetParams({
            # удалить после переключения search/pre SEARCH-11563
            "http_proxy": "HTTP_ADAPTER"
        })
        query.SetRequireStatus(200)

        resp = yield query
        resp.CheckHttpResponseStatus(200)

        template_data = resp.GetCtxs()["template_data"]
        assert len(template_data) != 0 and "data" in template_data[0]
        assert len(template_data[0]["data"])

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize('url', (
        HNDL.SEARCH_PREFETCH,
        HNDL.SEARCH_YANDCACHE,
        HNDL.SEARCH_PADCACHE,
        HNDL.SEARCH_TOUCHCACHE,
        HNDL.PREFETCH,
        HNDL.YANDCACHE,
        HNDL.PADCACHE,
        HNDL.TOUCHCACHE
    ))
    @pytest.mark.parametrize('query_type', (
        PLATFORM.DESKTOP,
        PLATFORM.PAD,
        PLATFORM.TOUCH,
        PLATFORM.SMART
    ))
    @TSoY.yield_test
    def test_prefetch(self, query, query_type, url):
        query.SetQueryType(query_type)
        query.SetPath(url)
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.text

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize(('url', 'user_agent', 'forever_on_top', 'vertex'), (
        (HNDL.SEARCH, USER_AGENT.DESKTOP, True, CTXS.BLENDER_TEMPLATE_DATA),
        (HNDL.SEARCH_PAD, USER_AGENT.PAD, True, CTXS.BLENDER_TEMPLATE_DATA),
        (HNDL.SEARCH_TOUCH, USER_AGENT.TOUCH, True, CTXS.BLENDER_TEMPLATE_DATA),
        (HNDL.SEARCH_RESULT, USER_AGENT.DESKTOP, False, CTXS.HANDLER_OUTPUT),
    ))
    @TSoY.yield_test
    def test_foreverdata(self, query, url, user_agent, forever_on_top, vertex):
        query.SetDumpFilter(resp=[vertex])
        query.SetPath(url)
        query.SetUserAgent(user_agent)
        query.SetInternal()
        query.SetParams({
            'promo': 'nomooa',
            'foreverdata': '244708688'
        })
        query.SetFlags({
            'foreverdata_in_templates': 1
        })
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        forever = None
        if forever_on_top:
            assert 'forever' in js
            forever = js['forever']
        else:
            assert 'foreverdata' in js['app_host']
            forever = js['app_host']['foreverdata']

        assert forever
        assert 'data' in forever

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-53448')
    @TSoY.yield_test
    def test_app_host_handler_x_frame_options_null(self, query):
        query.SetInternal()
        query.SetPath(HNDL.SEARCH_TURBO)
        query.SetParams({
            'format': 'json'
        })
        query.SetRequireStatus(200)

        resp = yield query

        assert not resp.headers.get('X-Frame-Options')

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize(('url', 'user_agent', 'forever_on_top', 'vertex'), (
        (HNDL.SEARCH, USER_AGENT.DESKTOP, True, CTXS.BLENDER_TEMPLATE_DATA),
        (HNDL.SEARCH_PAD, USER_AGENT.PAD, True, CTXS.BLENDER_TEMPLATE_DATA),
        (HNDL.SEARCH_TOUCH, USER_AGENT.TOUCH, True, CTXS.BLENDER_TEMPLATE_DATA),
    ))
    @TSoY.yield_test
    def test_foreverdata_in_blender(self, query, url, user_agent, forever_on_top, vertex):
        query.SetDumpFilter(resp=[vertex])
        query.SetPath(url)
        query.SetUserAgent(user_agent)
        query.SetInternal()
        query.SetParams({
            'promo': 'nomooa',
            'foreverdata': '244708688',
            'text': ''
        })
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        forever = None
        if forever_on_top:
            assert 'forever' in js
            forever = js['forever']
        else:
            assert 'foreverdata' in js['app_host']
            forever = js['app_host']['foreverdata']

        assert forever is None
        docs = js['searchdata']['docs']
        assert len(docs) != 0
