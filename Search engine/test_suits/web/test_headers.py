# -*- coding: utf-8 -*-

from util.tsoy import TSoY
import pytest
import re
from util.const import USER_AGENT, HNDL, TLD, GEO, PLATFORM, IP
from urllib.parse import quote
import time


class TestHeaders():
    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-48084')
    @pytest.mark.parametrize(("url", "is_exists", "origin"), [
        (HNDL.SEARCH_SUGGESTHISTORY, False, "yandex.ru"),
        (HNDL.SEARCH_SUGGESTHISTORY, True, "https://yandex.ru"),
        (HNDL.SEARCH_SUGGESTHISTORY, True, "http://yandex.ru"),
        (HNDL.SEARCH_SUGGESTHISTORY, False, "https://byandex.ru"),
        (HNDL.SEARCH, False, None),
    ])
    @TSoY.yield_test
    def test_cors(self, query, url, is_exists, origin):
        if origin:
            query.SetHeaders({"Origin": origin})
        query.SetDomain(TLD.RU)
        query.SetPath(url)
        query.SetRequireStatus(200)

        resp = yield query

        if is_exists:
            assert resp.headers.get('Access-Control-Allow-Origin') == origin
            assert resp.headers.get('Access-Control-Allow-Credentials') == 'true'
        else:
            assert not resp.headers.get('Access-Control-Allow-Origin')
            assert not resp.headers.get('Access-Control-Allow-Credentials')

    @pytest.mark.parametrize(("lite"), [
        (0),
        (1),
        (None),
    ])
    @pytest.mark.parametrize(("granoff"), [
        (1),
        (None),
    ])
    @TSoY.yield_test
    def test_granoff(self, query, lite, granoff):
        query.SetDomain(TLD.RU)
        query.SetPath(HNDL.SEARCH_TOUCH)
        query.SetUserAgent(USER_AGENT.TOUCH)
        query.SetYandexUid()

        start_time = str(int(time.time()))
        query.SetHeaders({'X-Start-Time': '{}000000'.format(start_time)})
        query.SetParams({'lite': lite})
        if not(granoff is None):
            query.SetCookies({'ys': "granoff." + start_time})
        query.SetRequireStatus(200)

        resp = yield query

        set_cookie = resp.headers.get('Set-Cookie', '')
        re_result = re.match(r'^.*?ys=granoff\.\d+;', set_cookie)
        if lite == 0 and not granoff:
            assert re_result is not None
        else:
            assert re_result is None

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.ticket("SERP-61529", "LAAS-1799")
    @pytest.mark.parametrize(("referer", "expect"), [
        ('https://yandex.ru/something', None),
        ('https://ya.ru/', None),
        ('http://notyandex.ru/', 'DENY'),
        ('http://not-yandex.ru/', 'DENY'),
        ('http://some.not-yandex.ru/', 'DENY'),
        ('https://hamster.yandex.ru/', None),
        ('https://yandex.ua/', 'DENY'),
        (None, 'DENY')
    ])
    @TSoY.yield_test
    def test_frames_for_referer(self, query, referer, expect):
        query.SetDomain(TLD.RU)
        query.SetExternal()
        query.SetHeaders({'Referer': referer})
        query.SetRequireStatus(200)

        resp = yield query

        if expect is None:
            assert resp.headers.get('X-Frame-Options') is None
        else:
            assert resp.headers.get('X-Frame-Options') == expect

    @pytest.mark.ticket('SERP-67947')
    # content type header set from report renderer overrides the one set by report
    @TSoY.yield_test
    def test_content_type_from_rr(self, query):
        query.SetQueryType(PLATFORM.SEARCHAPP_META)
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.headers.get('Content-Type') == 'multipart/related;boundary=Asrf456BGe4h;charset=UTF-8'

    @pytest.mark.ticket('SERP-56212')
    @TSoY.yield_test
    def test_searchapp_serp_only_content_type(self, query):
        query.SetQueryType(PLATFORM.SEARCHAPP)
        query.SetDomain(TLD.RU)
        query.SetParams({'serp_only': "1"})
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.headers.get('Content-Type') == 'text/html; charset=utf-8'

    @pytest.mark.ticket('SERP-55292')
    @TSoY.yield_test
    def test_searchapp_content_type(self, query):
        query.SetQueryType(PLATFORM.SEARCHAPP)
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(200)

        resp = yield query

        assert re.match('multipart/related; ?boundary=Asrf456BGe4h; ?charset=utf-8', resp.headers.get('Content-Type'))

    @pytest.mark.ticket('SERP-44826')
    @pytest.mark.parametrize(("flag_value"), [
        ('100'),
    ])
    @pytest.mark.parametrize(("tld", "geo"), [
        (TLD.RU, GEO.RU_MOSCOW),
        (TLD.COM, GEO.W_OSLO),
        (TLD.COMTR, GEO.COMTR_ISTANBUL),
        pytest.param(TLD.UA, GEO.UA_KIEV, marks=pytest.mark.xfail(reason="SEARCH-11856")),
        (TLD.KZ, GEO.KZ_ASTANA),
        (TLD.BY, GEO.BY_MINSK),
        (TLD.UZ, GEO.UZ_TASHKENT)
    ])
    @pytest.mark.ticket('WEBREPORT-855')
    @TSoY.yield_test
    def test_hsts_flag_ok(self, query, flag_value, tld, geo):
        query.SetDomain(tld)
        query.SetRwrHeaders({
            'X-Forwarded-For': IP[geo]
        })
        query.SetRequireStatus(200)

        resp = yield query

        #                                                        hamster                                                           production
        assert resp.headers.get('Strict-Transport-Security') == 'max-age=600' or resp.headers.get('Strict-Transport-Security') == 'max-age=31536000; includeSubDomains'

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.ticket("SERP-48817", "LAAS-1799")
    @pytest.mark.parametrize(("ua", "is_external", "expect"), [
        (USER_AGENT.TOUCH, True, 'DENY'),
        (USER_AGENT.TOUCH, False, None),
        (USER_AGENT.TIZEN_SEARCHAPP, True, None),
        (USER_AGENT.TIZEN_SEARCHAPP, False, None)
    ])
    @TSoY.yield_test
    def test_frame_options(self, query, ua, is_external, expect):
        query.SetQueryType(PLATFORM.TOUCH)
        query.SetHeaders({
            'User-Agent': ua
        })
        if is_external:
            query.SetExternal()
        query.SetRequireStatus(200)

        resp = yield query

        if expect is None:
            assert 'X-Frame-Options' not in resp.headers
        else:
            assert resp.headers.get('X-Frame-Options') == expect

    @pytest.mark.ticket('SERP-56957')
    @pytest.mark.parametrize(("url", "user_agent", "project", "has_csp"), [
        (HNDL.SEARCH,            USER_AGENT.DESKTOP,     'web4:desktop', True),
        (HNDL.SEARCH,            USER_AGENT.GRANNY,      'granny:desktop', False),
        (HNDL.SEARCH_TOUCH,      USER_AGENT.TOUCH,       'web4:phone', True),
        (HNDL.SEARCH_PAD,        USER_AGENT.PAD,         'web4:desktop', True),
    ])
    @TSoY.yield_test
    def test_csp_headers_from(self, query, url, user_agent, project, has_csp):
        query.SetPath(url)
        query.SetUserAgent(user_agent)
        query.SetYandexUid()
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.headers.get('content-security-policy-report-only') is None
        if has_csp:
            assert resp.headers.get('content-security-policy') is not None
            re_str = r'^.*?;report-uri https://csp\.yandex\.net/csp\?from={}.*?&reqid='.format(quote(project))
            assert re.match(re_str, resp.headers.get('content-security-policy'))
        else:
            assert resp.headers.get('content-security-policy') is None

    @pytest.mark.parametrize(("tld", "geo"), [
        (TLD.RU, GEO.RU_MOSCOW),
        (TLD.COM, GEO.W_OSLO),
        (TLD.COMTR, GEO.COMTR_ISTANBUL),
        pytest.param(TLD.UA, GEO.UA_KIEV, marks=pytest.mark.xfail(reason="SEARCH-11856")),
        (TLD.KZ, GEO.KZ_ASTANA),
        (TLD.BY, GEO.BY_MINSK),
        (TLD.UZ, GEO.UZ_TASHKENT)
    ])
    @pytest.mark.ticket('RUNTIMETESTS-21')
    @TSoY.yield_test
    def test_double_headers(self, query, tld, geo):
        query.SetDomain(tld)
        query.SetRwrHeaders({
            'X-Forwarded-For': IP[geo]
        })
        query.SetRequireStatus(200)

        resp = yield query

        ignore_list = ['Set-Cookie', 'Cache-Control', 'Expires']
        name_counter = {}
        for hName in resp.headers.keys():
            if hName not in ignore_list:
                name_counter.update({hName: 1})
                val_counter = {}
                for val in resp.headers.get(hName).split(','):
                    val = val.strip()
                    if val in val_counter:
                        name_counter[hName] = name_counter[hName] + 1
                        break
                    else:
                        val_counter.update({val: 1})
        name_dups = []
        for hName in name_counter.keys():
            if name_counter[hName] > 1:
                name_dups.append(hName)
        if len(name_dups) > 0:
            raise AssertionError('Duplicate headers: {}. CURL: {}'.format(name_dups, query.Dump()))
