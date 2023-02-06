# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import PLATFORM, IP, GEO, REGION, CTXS, HNDL


class TestLaasLbsAppHost():
    """
    1) Проверяем, что LaaS правильно определяет регион по IP адресу
    2) Проверяем, что тесты могут зарерайтить заголовок LaaS
    3) Проверяем работу LaaS с cellid
    4) Проверяем что для определенных ручек в запросе в laas используется ip из x-real-ip
    """
    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.ticket('SERP-65360')
    @TSoY.yield_test
    def test_laas_lbs_ip(self, query):
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetQueryType(PLATFORM.TOUCH)
        query.SetExternal(ip=IP[GEO.RU_IRKUTSK])
        # query.SetRegion(REGION[GEO.RU_MOSCOW])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        request = ctxs['request'][-1]

        assert request['headers']['x-laas-answered'] == '1'
        assert request['headers']['x-region-city-id'] == REGION[GEO.RU_IRKUTSK]

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.ticket('SERP-65360')
    @TSoY.yield_test
    def test_laas_lbs_rwr(self, query):
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetQueryType(PLATFORM.TOUCH)
        query.SetExternal(ip=IP[GEO.RU_IRKUTSK])
        query.SetRegion(REGION[GEO.RU_MOSCOW])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        request = ctxs['request'][-1]

        assert request['headers']['x-laas-answered'] == '1'
        assert request['headers']['x-region-city-id'] == REGION[GEO.RU_MOSCOW]

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.ticket('SERP-65361')
    @pytest.mark.parametrize('cellid, lr, gid, rstr, default, laas_real, real, selected, tuned', [
        ('bad_cellid', '63', '20097', '2', '63', '213', '213', '63', '213'),
        ('401,01,4466781,6182,-85', '63', '20097', '2', '63', '190', '190', '63', '190')
    ])
    @TSoY.yield_test
    def test_cellid(self, query, cellid, lr, gid, rstr, default, laas_real, real, selected, tuned):
        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetQueryType(PLATFORM.TOUCH)
        query.SetParams({
            'text': 'test',
            'lr': lr,
            'rstr': rstr,
            'cellid': cellid
        })
        query.SetExternal(ip=IP[GEO.RU_MOSCOW])
        query.SetYandexGid(gid)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        request = ctxs['request'][-1]
        region = ctxs['region'][-1]

        assert request['headers']['x-laas-answered'] == '1'
        assert request['headers']['x-region-city-id'] == laas_real
        assert region['laas_real']['id'] == laas_real
        assert region['real']['id'] == real
        assert region['selected']['id'] == selected
        assert region['tuned']['id'] == tuned
        assert region['default']['id'] == default

    @pytest.mark.ticket('SEARCH-11241')
    @pytest.mark.parametrize('path, use_real_ip', [
        (HNDL.SEARCH, False),
        # (HNDL.SEARCH_ADONLY, True), wait till adonly moved to apphost laas as well
        (HNDL.SEARCH_XML, True),
        (HNDL.XMLSEARCH, True),
    ])
    @TSoY.yield_test
    def test_real_ip(self, query, path, use_real_ip):
        REAL_IP = '8.8.8.8'
        FORWARDED_FOR = '1.2.3.4'

        query.SetDumpFilter(resp=[CTXS.INIT_PRE])
        query.SetPath(path)
        query.SetHeaders({
            'X-Real-Ip': REAL_IP,
            'X-Forwarded-For': FORWARDED_FOR
        })
        query.SetParams({
            'text': 'test',
        })
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        laas_request = ctxs['laas_request'][-1]
        # request = ctxs['resuest'][-1]

        def check_headers(item, real_ip):
            return item['headers']['x-forwarded-for'] == real_ip

        if use_real_ip:
            assert check_headers(laas_request, REAL_IP)
        else:
            assert not check_headers(laas_request, REAL_IP)
