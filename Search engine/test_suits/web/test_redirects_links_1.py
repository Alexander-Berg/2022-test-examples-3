# -*- coding: utf-8 -*-

from util.tsoy import TSoY
import pytest
from util.helpers import ParseDomain
# from base64 import b64encode
from util.const import HNDL, PLATFORM, L10N, REGION_BY_TLD, COOKIEMY_PREFER_SERP, TLD, USER_AGENT, CTXS
# from urllib.parse import quote
# import time


class TestRedirectsLinks_1():
    """
    Проверяем все редиректы в репорте.
    Только статус ответа и простые проверки валидности ответа.

    По мотивам тикета SERP-28157 и Аква-пака SERP Redirects
    """

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @TSoY.yield_test
    def test_search_wrong_url(self, query):
        query.SetPath(HNDL.SEARCH_WRONG)
        query.SetRequireStatus(302)

        resp = yield query

        assert resp.GetLocation().path == HNDL.SEARCH

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("path", [
        HNDL.CGIBIN_YANDSEARCH,
        HNDL.YANDPAGE,
        HNDL.LARGESEARCH,
    ])
    @TSoY.yield_test
    def test_search_obsolete(self, query, path):
        query.SetPath(path)
        query.SetParams({
            'text': None
        })
        query.SetRequireStatus(302)

        resp = yield query

        assert resp.GetLocation().path == HNDL.SEARCH

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("url", [
        HNDL.SEARCH_SMART,
        HNDL.SEARCH_FAMILY_FLD,
    ])
    @TSoY.yield_test
    def test_search_default_redirect(self, query, url):
        query.SetPath(url)
        query.SetRequireStatus(302)

        resp = yield query

        assert resp.GetLocation().path == HNDL.SEARCH

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("url", [
        HNDL.FAMILYSEARCH,
        HNDL.SCHOOLSEARCH,
        HNDL.SEARCH_SCHOOL,
    ])
    @TSoY.yield_test
    def test_search_school(self, query, url):
        query.SetPath(url)
        query.SetParams({
            'text': None
        })
        query.SetRequireStatus(302)

        resp = yield query

        assert resp.GetLocation().path == HNDL.SEARCH_FAMILY

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize(("url", "ua", "params", "require_status"), [
        (HNDL.SEARCH, USER_AGENT.DESKTOP, {}, 302),
        (HNDL.SEARCH_PAD, USER_AGENT.PAD, {}, 200),
        (HNDL.SEARCH_SMART, USER_AGENT.SMART, {}, 302),
        (HNDL.SEARCH_SITE, USER_AGENT.DESKTOP, {'searchid': 2244093}, 200),
    ])
    @TSoY.yield_test
    def test_search_no_slash(self, query, ua, url, params, require_status):
        query.SetPath(url[:-1])  # Откусываем последний символ, равный '/'
        query.SetUserAgent(ua)
        query.SetParams(params)
        query.SetRequireStatus(require_status)

        resp = yield query

        if require_status in [302]:
            assert resp.GetLocation().path == url

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @TSoY.yield_test
    def test_advanced(self, query):
        """
        WEBREPORT-714 - закрываем /search/advanced
        раньше редиректили /advanced.html на /search/advanced
        """
        query.SetPath(HNDL.ADVANCEDHTML)
        query.SetRequireStatus(404)

        yield query

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-46893')
    @pytest.mark.parametrize(("path, ua, disable_redirects, location, template"), [
        # desktop to mobile
        (HNDL.SEARCH, USER_AGENT.TOUCH, False, HNDL.SEARCH_TOUCH, None),
        (HNDL.SEARCH, USER_AGENT.SMART, False, HNDL.SEARCH_SMART, None),
        (HNDL.SEARCH, USER_AGENT.TOUCH, True, HNDL.SEARCH_TOUCH, "web4:desktop"),
        (HNDL.SEARCH, USER_AGENT.SMART, True, HNDL.SEARCH_SMART, "web4:desktop"),
        # mobile to desktop
        (HNDL.SEARCH_TOUCH, USER_AGENT.DESKTOP, False, HNDL.SEARCH, None),
        (HNDL.SEARCH_SMART, USER_AGENT.DESKTOP, False, HNDL.SEARCH, None),
        (HNDL.SEARCH_TOUCH, USER_AGENT.DESKTOP, True,  None,        'web4:phone'),
        (HNDL.SEARCH_SMART, USER_AGENT.DESKTOP, True,  None,        'granny_exp:phone'),
    ])
    @TSoY.yield_test
    def test_desktop_mobile_redir_blocked(self, query, path, template, ua, disable_redirects, location):
        """
        SERP-46893 - Флаг для отключения всех редиректов
        """
        query.SetDomain(TLD.RU)
        query.SetUserAgent(ua)
        query.SetPath(path)
        if disable_redirects:
            query.SetFlags({
                "disable_redirects": 1
            })
            query.SetDumpFilter(resp=[CTXS.INIT, CTXS.BLENDER_TEMPLATE_DATA])
        else:
            query.SetRequireStatus(302)

        resp = yield query

        if disable_redirects:
            ctxs = resp.GetCtxs()
            assert ctxs['device_config'][-1]['template_name'] == template
            assert ctxs["template_data"][0]["name"] == template
        else:
            assert resp.GetLocation().path == location

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-46893')
    @pytest.mark.parametrize(("platform", "path"), [
        (PLATFORM.DESKTOP, HNDL.SEARCH),
        (PLATFORM.TOUCH, HNDL.SEARCH_TOUCH),
        (PLATFORM.SMART, HNDL.SEARCH_SMART),
    ])
    @pytest.mark.parametrize("gid, tld",  [
        pytest.param(143, TLD.UA, marks=pytest.mark.xfail(reason="SEARCH-11856")),  # Киев
        (157,   TLD.BY),    # Минск
        (162,   TLD.KZ),    # Алматы
    ])
    @TSoY.yield_test
    def test_multidomain_gid_redir_blocked(self, platform, path, query, gid, tld):
        """
        SERP-46893 - Флаг для отключения всех редиректов
        """
        query.SetQueryType(platform)
        query.SetDomain(TLD.RU)
        query.SetYandexGid(gid)
        query.ReqYpCookie.set_ygu(0)
        query.SetNoAuth()
        query.SetRequireStatus(302)

        resp = yield query

        assert resp.GetLocation().path == path
        assert ParseDomain(resp.GetLocation().hostname)['tld'] == tld
        assert resp.GetLocation().scheme == 'https'

    @pytest.mark.ticket('SERP-46893')
    @pytest.mark.parametrize(("platform"), [
        PLATFORM.DESKTOP,
        PLATFORM.TOUCH,
        PLATFORM.SMART,
    ])
    @pytest.mark.parametrize("gid, tld",  [
        (143,   TLD.UA),    # Киев
        (157,   TLD.BY),    # Минск
        (162,   TLD.KZ),    # Алматы
    ])
    @TSoY.yield_test
    def test_multidomain_gid_redir_blocked_with_disable_redirects_flag(self, platform, query, gid, tld):
        """
        SERP-46893 - Флаг для отключения всех редиректов
        """
        query.SetQueryType(platform)
        query.SetDomain(TLD.RU)
        query.SetYandexGid(gid)
        query.SetFlags({
            'disable_redirects': 1
        })
        query.SetRequireStatus(200)

        yield query

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-46893')
    @pytest.mark.parametrize(("platform"), [
        PLATFORM.DESKTOP,
        PLATFORM.TOUCH,
        PLATFORM.SMART,
    ])
    @pytest.mark.parametrize("tld", [
        pytest.param(TLD.UA, marks=pytest.mark.xfail(reason="SEARCH-11856")),
        TLD.BY,
        TLD.KZ
    ])
    @TSoY.yield_test
    def test_multidomain_https_redir_blocked(self, query, platform, tld):
        """
        SERP-46893 - Флаг для отключения всех редиректов
        Проверяем, что без флага есть редирект на https
        """
        query.SetQueryType(platform)
        query.SetDomain(tld)
        query.SetNoAuth()
        query.SetScheme('http')
        query.SetRequireStatus(302)

        resp = yield query

        assert ParseDomain(resp.GetLocation().hostname)['tld'] == tld
        assert resp.GetLocation().scheme == 'https'

    @pytest.mark.ticket('SERP-46893')
    @pytest.mark.parametrize(("platform"), [
        PLATFORM.DESKTOP,
        PLATFORM.TOUCH,
        PLATFORM.SMART,
    ])
    @pytest.mark.parametrize("tld", [
        TLD.UA,
        TLD.BY,
        TLD.KZ
    ])
    @TSoY.yield_test
    def test_multidomain_https_redir_blocked_with_disable_redirects_flag(self, query, platform, tld):
        """
        SERP-46893 - Флаг для отключения всех редиректов
        """
        query.SetQueryType(platform)
        query.SetDomain(tld)
        query.SetNoAuth()
        query.SetScheme('http')
        query.SetFlags({
            'disable_redirects': 1
        })
        query.SetRequireStatus(200)

        yield query

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize(("tld"), [
        TLD.RU,
        TLD.KZ,
        TLD.COM,
        TLD.COMTR
    ])
    @TSoY.yield_test
    def test_set_uid_on_redir(self, query, tld):
        """
        SERP-36125 - Выставлять yandex_uid новым пользователям до (или вместе) с 302 редиректом
        Проверяем, что кука выставляется и для RU
        Для чего-то из КУБ(для), так как там работает МДА
        COM, COMTR проверяем, так как может быть разная логика для разных доменов
        """
        query.SetScheme('http')
        query.SetDomain(tld)
        query.SetRequireStatus(302)

        resp = yield query

        assert 'yandexuid' in resp.GetSetCookie()

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-38524')
    @pytest.mark.parametrize(("ua", "my", "path", "code", "location"), [
        # /search/
        (USER_AGENT.DESKTOP, None,      HNDL.SEARCH,            200, None),
        (USER_AGENT.DESKTOP, None,      HNDL.SEARCH_PAD,        200, None),
        (USER_AGENT.DESKTOP, None,      HNDL.SEARCH_TOUCH,      302, HNDL.SEARCH),
        (USER_AGENT.DESKTOP, None,      HNDL.SEARCH_SMART,      302, HNDL.SEARCH),
        # /yandsearh
        (USER_AGENT.DESKTOP, None,      HNDL.YANDSEARCH,        200, None),
        (USER_AGENT.DESKTOP, None,      HNDL.PADSEARCH,         200, None),
        (USER_AGENT.DESKTOP, None,      HNDL.TOUCHSEARCH,       302, HNDL.YANDSEARCH),
        (USER_AGENT.DESKTOP, None,      HNDL.MSEARCH,           302, HNDL.YANDSEARCH),
        # /search/pad
        (USER_AGENT.PAD,     None,      HNDL.SEARCH,            200, None),
        (USER_AGENT.PAD,     None,      HNDL.SEARCH_PAD,        200, None),
        (USER_AGENT.PAD,     None,      HNDL.SEARCH_TOUCH,      302, HNDL.SEARCH),
        (USER_AGENT.PAD,     None,      HNDL.SEARCH_SMART,      302, HNDL.SEARCH),
        (USER_AGENT.PAD,     L10N.FULL, HNDL.SEARCH,            200, None),
        (USER_AGENT.PAD,     L10N.FULL, HNDL.SEARCH_PAD,        200, None),
        # /padsearch
        (USER_AGENT.PAD,     None,      HNDL.YANDSEARCH,        200, None),
        (USER_AGENT.PAD,     None,      HNDL.PADSEARCH,         200, None),
        (USER_AGENT.PAD,     None,      HNDL.TOUCHSEARCH,       302, HNDL.YANDSEARCH),
        (USER_AGENT.PAD,     None,      HNDL.MSEARCH,           302, HNDL.YANDSEARCH),
        (USER_AGENT.PAD,     L10N.FULL, HNDL.YANDSEARCH,        200, None),
        (USER_AGENT.PAD,     L10N.FULL, HNDL.PADSEARCH,         200, None),
        # search/touch/
        (USER_AGENT.TOUCH,   None,      HNDL.SEARCH,            302, HNDL.SEARCH_TOUCH),
        (USER_AGENT.TOUCH,   None,      HNDL.SEARCH_PAD,        302, HNDL.SEARCH_TOUCH),
        (USER_AGENT.TOUCH,   None,      HNDL.SEARCH_TOUCH,      200, None),
        (USER_AGENT.TOUCH,   None,      HNDL.SEARCH_SMART,      302, HNDL.SEARCH_TOUCH),
        (USER_AGENT.TOUCH,   L10N.FULL, HNDL.SEARCH,            200, None),
        (USER_AGENT.TOUCH,   L10N.FULL, HNDL.SEARCH_PAD,        200, None),
        # /touchsearch
        (USER_AGENT.TOUCH,   None,      HNDL.YANDSEARCH,        302, HNDL.TOUCHSEARCH),
        (USER_AGENT.TOUCH,   None,      HNDL.PADSEARCH,         302, HNDL.TOUCHSEARCH),
        (USER_AGENT.TOUCH,   None,      HNDL.TOUCHSEARCH,       200, None),
        (USER_AGENT.TOUCH,   None,      HNDL.MSEARCH,           302, HNDL.TOUCHSEARCH),
        (USER_AGENT.TOUCH,   L10N.FULL, HNDL.YANDSEARCH,        200, None),
        (USER_AGENT.TOUCH,   L10N.FULL, HNDL.PADSEARCH,         200, None),
        # /search/smart/
        (USER_AGENT.SMART,   None,      HNDL.SEARCH,            302, HNDL.SEARCH_SMART),
        (USER_AGENT.SMART,   None,      HNDL.SEARCH_PAD,        302, HNDL.SEARCH_SMART),
        (USER_AGENT.SMART,   None,      HNDL.SEARCH_TOUCH,      200, None),
        (USER_AGENT.SMART,   None,      HNDL.SEARCH_SMART,      200, None),
        (USER_AGENT.SMART,   L10N.FULL, HNDL.SEARCH,            200, None),            # XXX here is http-only granny
        (USER_AGENT.SMART,   L10N.FULL, HNDL.SEARCH_SMART,      302, HNDL.SEARCH),     # XXX here is two-staged redirect on http-only granny
        # msearch/
        (USER_AGENT.SMART,   None,      HNDL.YANDSEARCH,        302, HNDL.MSEARCH),
        (USER_AGENT.SMART,   None,      HNDL.PADSEARCH,         302, HNDL.MSEARCH),
        (USER_AGENT.SMART,   None,      HNDL.TOUCHSEARCH,       200, None),
        (USER_AGENT.SMART,   None,      HNDL.MSEARCH,           200, None),
        (USER_AGENT.SMART,   L10N.FULL, HNDL.YANDSEARCH,        200, None),            # XXX here is http-only granny
        (USER_AGENT.SMART,   L10N.FULL, HNDL.MSEARCH,           302, HNDL.YANDSEARCH),  # XXX here is two-staged redirect on http-only granny
        # sitesearch
        (USER_AGENT.DESKTOP, None,      HNDL.SEARCH_SITE,       200, None),
        (USER_AGENT.PAD,     None,      HNDL.SEARCH_SITE,       200, None),
        (USER_AGENT.TOUCH,   None,      HNDL.SEARCH_SITE,       200, None),
        (USER_AGENT.SMART,   None,      HNDL.SEARCH_SITE,       200, None),
        # sitesearch_old
        (USER_AGENT.DESKTOP, None,      HNDL.SEARCH_SITE_OLD,   200, None),
        (USER_AGENT.PAD,     None,      HNDL.SEARCH_SITE_OLD,   200, None),
        (USER_AGENT.TOUCH,   None,      HNDL.SEARCH_SITE_OLD,   200, None),
        (USER_AGENT.SMART,   None,      HNDL.SEARCH_SITE_OLD,   200, None),
        # searchapp:   SERP-52131
        (USER_AGENT.SEARCHAPP_ANDROID, None, HNDL.SEARCHAPP, 200, None),
        (USER_AGENT.SEARCHAPP_IOS,     None, HNDL.SEARCHAPP, 200, None),
        # /blogs
        (USER_AGENT.DESKTOP, None,      HNDL.BLOGS_SEARCH,       200, None),
        (USER_AGENT.TOUCH,   None,      HNDL.BLOGS_SEARCH_TOUCH, 200, None),
        (USER_AGENT.PAD,     None,      HNDL.BLOGS_SEARCH_PAD,   200, None),
        (USER_AGENT.DESKTOP, None,      HNDL.BLOGS,              302, HNDL.BLOGS_SEARCH),
        # (USER_AGENT.DESKTOP, None,   HNDL.BLOGS_SEARCH_RSS,   302, HNDL.BLOGS_SEARCH),
        (USER_AGENT.TOUCH,   None,      HNDL.BLOGS,              302, HNDL.BLOGS_SEARCH_TOUCH),
        (USER_AGENT.TOUCH,   None,      HNDL.BLOGS_SEARCH,       302, HNDL.BLOGS_SEARCH_TOUCH),
        # /search/ads
        (USER_AGENT.TOUCH,   None,      HNDL.SEARCH_ADS,          302, HNDL.SEARCH_ADS_TOUCH),
        (USER_AGENT.PAD,     None,      HNDL.SEARCH_ADS,          200, None),
        (USER_AGENT.TOUCH,   None,      HNDL.SEARCH_ADS_TOUCH,    200, None),
        (USER_AGENT.PAD,     None,      HNDL.SEARCH_ADS_PAD,      200, None),
        # /search/direct
        (USER_AGENT.TOUCH,   None,      HNDL.SEARCH_DIRECT,       302, HNDL.SEARCH_DIRECT_TOUCH),
        (USER_AGENT.PAD,     None,      HNDL.SEARCH_DIRECT,       200, None),
        (USER_AGENT.TOUCH,   None,      HNDL.SEARCH_DIRECT_TOUCH, 200, None),
        (USER_AGENT.PAD,     None,      HNDL.SEARCH_DIRECT_PAD,   200, None)
    ])
    @pytest.mark.parametrize(("tld"), [
        TLD.RU,
        # UA,
        # KZ,
        # BY,
    ])
    @TSoY.yield_test
    def test_path_redirects(self, query, ua, my, path, code, location, tld):
        query.SetDomain(tld)
        query.SetFlags({'serp3_granny_https': 0})
        query.SetUserAgent(ua)
        query.SetPath(path)
        query.SetRegion(REGION_BY_TLD[tld])
        yabs_paths = [HNDL.SEARCH_ADS, HNDL.SEARCH_ADS_TOUCH, HNDL.SEARCH_ADS_PAD, HNDL.SEARCH_DIRECT, HNDL.SEARCH_DIRECT_TOUCH, HNDL.SEARCH_DIRECT_PAD]
        if path in yabs_paths:
            query.SetParams({'searchid': 2244093, 'srcskip': 'SAAS_DJ_SETUP'})
        else:
            query.SetParams({'searchid': 2244093})
        if my:
            query.SetCookies({
                'my': COOKIEMY_PREFER_SERP[my]
            })
        query.SetRequireStatus(code)

        resp = yield query

        if code == 302:
            l = resp.GetLocation()
            if l.hostname is not None:
                pd = ParseDomain(l.hostname)
                assert pd['tld'] == tld
            assert l.path == location
