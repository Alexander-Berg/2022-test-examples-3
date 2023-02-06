# -*- coding: utf-8 -*-

from util.tsoy import TSoY
import pytest
from util.const import HNDL, USER_AGENT, TLD, PLATFORM, CTXS
from util.helpers import ParseDomain
# import time


class TestRedirectsLinks_3():
    @pytest.mark.ticket('SERP-47795')
    @pytest.mark.parametrize(("https"), [
        True, False
    ])
    @TSoY.yield_test
    def test_tld_by_lr_with_disable_redirects(self, query, https):
        query.SetDumpFilter(resp=[CTXS.REPORT])
        query.SetFlags({
            "disable_redirects": 1
        })
        query.SetLr(157)
        query.SetDomain(TLD.RU)
        query.SetScheme('https' if https else 'http')

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['report'][0]['tld'] == TLD.BY

    @pytest.mark.ticket('SERP-46391')
    @pytest.mark.parametrize(("tld"), [
        TLD.RU,
        # TLD.COM,
        TLD.COMTR,
        pytest.param(TLD.UA, marks=pytest.mark.xfail(reason="SEARCH-11856")),
        TLD.KZ,
        TLD.BY,
        TLD.UZ,
        TLD.COMGE,
        TLD.FR,
    ])
    @TSoY.yield_test
    def test_rdat_tld(self, query, tld):
        query.SetDumpFilter(resp=[CTXS.REPORT])
        query.SetDomain(tld)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs['report'][0]['tld'] == TLD.UA if tld == TLD.FR else tld

    @pytest.mark.ticket('SERP-47842')
    @pytest.mark.parametrize(("report", "ua", "distr_pwa", "header_name", "header_exists", "header_value"), [
        (HNDL.SEARCH_TOUCH, USER_AGENT.TOUCH,   0, "Service-Worker-Allowed", False, ""),
        (HNDL.SEARCH_TOUCH, USER_AGENT.TOUCH,   1, "Service-Worker-Allowed", True,  "/search/touch/"),
        (HNDL.SEARCH,       USER_AGENT.DESKTOP, 1, "Service-Worker-Allowed", False, ""),
        (HNDL.SEARCH,       USER_AGENT.DESKTOP, 0, "Service-Worker-Allowed", False, ""),
        (HNDL.SEARCH_PAD,   USER_AGENT.PAD,     1, "Service-Worker-Allowed", False, ""),
        (HNDL.SEARCH_PAD,   USER_AGENT.PAD,     0, "Service-Worker-Allowed", False, ""),
        (HNDL.SEARCH_SMART, USER_AGENT.SMART,   1, "Service-Worker-Allowed", False, ""),
        (HNDL.SEARCH_SMART, USER_AGENT.SMART,   0, "Service-Worker-Allowed", False, "")
    ])
    @TSoY.yield_test
    def test_distr_pwa_header(self, query, report, ua, distr_pwa, header_name, header_exists, header_value):
        query.SetPath(report)
        query.SetParams({
            'text': 'test'
        })
        query.SetFlags({
            "distr_pwa": distr_pwa
        })
        query.SetUserAgent(ua)
        query.SetRequireStatus(200)

        resp = yield query

        assert (header_name in resp.headers) == header_exists
        if (header_exists):
            assert header_value in resp.headers[header_name]

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('WEBREPORT-448')
    @pytest.mark.parametrize(("target_platform", "target_path"), [
        (PLATFORM.TOUCH,   HNDL.SEARCH_TOUCH),
    ])
    @TSoY.yield_test
    def test_device_redirect(self, query, target_platform, target_path):
        query.SetQueryType(PLATFORM.SMART)
        query.SetFlags({
            "device_redirect": target_platform
        })
        query.SetRequireStatus(302)

        resp = yield query

        assert resp.GetLocation().path == target_path
        assert resp.GetLocationParams()['padp'] == [target_platform]

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("https", [True, False])
    @pytest.mark.parametrize("url", [
        HNDL.SEARCH_NAHODKI[:-1],
        HNDL.SEARCH_NAHODKI
    ])
    @pytest.mark.parametrize("tld", [
        TLD.RU,
        TLD.BY,
    ])
    @TSoY.yield_test
    def test_nahodki_redirect(self, query, url, tld, https):
        """
        Поиски, работающие для desktop user-agent
        """
        query.SetPath(url)
        query.SetDomain(tld)
        scheme = 'https' if https else 'http'
        query.SetScheme(scheme)
        query.SetRequireStatus(302)

        resp = yield query

        assert ParseDomain(resp.GetLocation().hostname)['tld'] == tld
        assert resp.GetLocation().scheme == scheme

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("tld", [
        TLD.RU,
        TLD.COM,
        TLD.COMTR
    ])
    @TSoY.yield_test
    def test_search_customize(self, query, tld):
        query.SetPath(HNDL.SEARCH_CUSTOMIZE[:-1])
        query.SetDomain(tld)
        query.SetRequireStatus(302)

        resp = yield query
        assert resp.GetLocation().path == '/tune/search/'
