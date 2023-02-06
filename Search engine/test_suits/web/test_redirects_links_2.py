# -*- coding: utf-8 -*-

from util.tsoy import TSoY
import pytest
from util.const import HNDL, REGION_BY_TLD, TLD, CTXS, YANDEXUID, REGION, GEO


class TestRedirectsLinks_2():
    @pytest.mark.parametrize(("path"), [
        HNDL.SEARCH_TOUCH,
        HNDL.SEARCH,
        HNDL.SEARCH_PAD,
        HNDL.SEARCH_SMART,
        HNDL.YANDSEARCH,
        HNDL.SEARCHAPP
    ])
    @pytest.mark.parametrize(("lr"), [
        REGION_BY_TLD[TLD.RU],
        pytest.param(REGION_BY_TLD[TLD.UA], marks=pytest.mark.xfail(reason="RUNTIMETESTS-143")),
        REGION_BY_TLD[TLD.KZ],
        REGION_BY_TLD[TLD.BY],
        # REGION_BY_TLD[TLD.UZ],
    ])
    @pytest.mark.parametrize(("tld"), [
        TLD.RU, TLD.COM, TLD.COMTR, pytest.param(TLD.UA, marks=pytest.mark.xfail(reason="RUNTIMETESTS-143")), TLD.KZ, TLD.BY,  # TLD.UZ
    ])
    @pytest.mark.parametrize(("https"), [
        True, False
    ])
    @TSoY.yield_test
    def test_no_ajax_redir(self, query, tld, path, lr, https):
        """
        SERP-40592 - Исключить любые редиректы для AJAX запросов
        """
        query.SetScheme('https' if https else 'http')
        query.SetDomain(tld)
        query.SetPath(path)
        query.SetParams({
            'callback': 'c305338619620',
            'yu': YANDEXUID,
            'lr': lr,
            'ajax': '{}'
        })
        query.SetRequireStatus(200)

        yield query

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-43695')
    @pytest.mark.parametrize(("flag_value", "status"), [
        (1, 200),
        (0, 302),
    ])
    @TSoY.yield_test
    def test_noredirect_from_com(self, query, flag_value, status):
        query.SetRwrHeaders({"X-LaaS-Answered": "1"})
        query.SetFlags({
            'noredirect_com': flag_value
        })
        query.SetDomain(TLD.COM)
        query.SetRegion(REGION[GEO.RU_MOSCOW])
        query.SetRequireStatus(status)

        yield query

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-48094')
    @pytest.mark.parametrize(("host", "lr", "region", "status"), [
        (TLD.RU, REGION_BY_TLD[TLD.RU], REGION[GEO.RU_MOSCOW], 200),
        (TLD.FR, REGION_BY_TLD[TLD.FR], REGION[GEO.RU_MOSCOW], 302),
        (TLD.FR, REGION_BY_TLD[TLD.RU], REGION[GEO.UZ_TASHKENT], 302),
        pytest.param(TLD.UA, REGION_BY_TLD[TLD.UA], REGION[GEO.RU_MOSCOW], 200, marks=pytest.mark.xfail(reason="SEARCH-11856")),
        (TLD.COM, REGION_BY_TLD[TLD.COM], REGION[GEO.RU_MOSCOW], 200),
        (TLD.COMTR, REGION_BY_TLD[TLD.COMTR], REGION[GEO.RU_MOSCOW], 200)
    ])
    @TSoY.yield_test
    def test_lr_redirects(self, query, host, lr, region, status):
        query.SetDomain(host)
        query.SetRegion(region)
        query.SetLr(lr)
        query.SetRequireStatus(status)

        yield query

    @pytest.mark.ticket('SERP-45071')
    @pytest.mark.parametrize(("flag_by_cgi"), [
        True, False,
    ])
    @pytest.mark.parametrize(("tld", "lr", "expected_tld"), [
        (TLD.RU, 213, TLD.RU),
        (TLD.RU, 157, TLD.BY)
    ])
    @pytest.mark.parametrize(("https", "flag"), [
        (False, 'disable_https'),
        (True, None)
    ])
    @TSoY.yield_test
    def test_tld_by_lr(self, query, flag_by_cgi, tld, lr, expected_tld, https, flag):
        query.SetDumpFilter(resp=[CTXS.REPORT])
        query.SetFlags({
            flag: 1
        })

        if flag_by_cgi:
            # этот флаг полноценно работает ТОЛЬКО как cgi-параметр
            query.SetParams({
                'no_geo_domain_redirect': 1
            })
        else:
            # передача его через настоящий механизм флагов, функциональность работает наполовину,
            # отключает редирект, но tld остается кривой
            query.SetFlags({
                'no_geo_domain_redirect': 1
            })

        query.SetLr(lr)
        query.SetDomain(tld)
        query.SetScheme('https' if https else 'http')

        resp = yield query
        ctxs = resp.GetCtxs()

        if flag_by_cgi:
            assert ctxs['report'][0]['tld'] == expected_tld
        else:
            assert ctxs['report'][0]['tld'] == tld
