# -*- coding: utf-8 -*-

import base64
import time
import re
import pytest
import json
from util.tsoy import TSoY
from util.const import PLATFORM, HNDL, TLD, TEXT, REGION_BY_TLD, USER_AGENT, CTXS

from search.idl.meta_pb2 import TReport

"""
    Тесты источников

    Репорт должен отвечать 200 OK если:
        a) Хост источника не заресолвился
        б) Хост источника ресетит
        в) Отвечает мусором
        г) HTTP ошибки
        д) Часть ответа - TODO

    Источники в которые Репорт ходит сам:
        http://yandex.ru/search/viewconfig?name=request.json

    Источники в которые Репорт ходит через UPPER
        http://yandex.ru/search/viewconfig
        искать примерно такое:

        <SearchSource>
        ServerDescr <NAME>
        ...
        </SearchSource>
"""


class TestSources():
    def _get_yabs_page_id_and_host(self, yabs_setup):
        for yabs in yabs_setup:
            if 'metahost' in yabs:
                pageID = yabs['metahost'][0].split('?')[0].split('/')[2]
                yabs_host = yabs['metahost'][0].split('?')[0].split(':')[1]
                return pageID, yabs_host
        return None, None

    def get_param_from_clt_ctx(self, clt_ctx, param):
        if clt_ctx is None:
            return []
        res = []
        for source in clt_ctx:
            if clt_ctx[source] is None:
                continue
            if param in clt_ctx[source]:
                res.append((source, ';'.join(clt_ctx[source][param]).split(';')))
        return res

    def get_rearr_relev_snip_param_value_from_ctx(self, clt_ctx, name, param_name, unique=True):
        # берем все источники, куда передаются поля name
        sources_param = self.get_param_from_clt_ctx(clt_ctx, name)

        assert len(sources_param) > 0  # есть источники, где есть нужный переметр

        # выдераем из пар источник-поле только подполя с нужным именем
        param = [p for s in sources_param for p in s[1] if param_name in p]

        assert len(param) > 0  # хоть на какой-то источник эти подполя должны были передаться

        param = list(set(param))

        if unique:
            assert len(param) == 1  # везде одинаковые значения
            return param[0]
        else:
            return param

    @pytest.mark.parametrize("headers, gsmop_expect", [
        ({
            'X-Forwarded-For': '0000:0000:0000:0000:0000:ffff:b03b:0000',
            'X-Forwarded-For-Y': '0000:0000:0000:0000:0000:ffff:b03b:0000',
            'X-Real-IP': '0000:0000:0000:0000:0000:ffff:b03b:0000'
        }, ['gsmop=TELE2', 'gsmop=t2 mobile llc']),
        ({'X-Forwarded-For': '213.87.126.0', 'X-Forwarded-For-Y': '213.87.126.0', 'X-Real-IP': '213.87.126.0'}, ['gsmop=MTS'],),
        ({'X-Forwarded-For': '213.87.126.0'}, ['gsmop=MTS'],),
        ({'X-Real-IP': 'asddsa'}, ['gsmop=0'],),
        ({'X-Real-IP': '::1'}, ['gsmop=0'],),
        ({'X-Real-IP': '1.2.3.4'}, ['gsmop=0'],),
        ({'X-Forwarded-For': '1.2.3.4'}, ['gsmop=0'],),
        ({'X-Forwarded-For': '127.0.0.1'}, ['gsmop=0'],),
        ({'X-Forwarded-For': '::1'}, ['gsmop=0'],),
        ({'X-Forwarded-For': 'asddsa'}, ['gsmop=0'],),
        ({'X-Real-IP': '5.144.64.0', 'X-Forwarded-For': '1.2.3.4'}, ['gsmop=0'],),
    ])
    @TSoY.yield_test
    def test_sources_rearr_gsmop(self, query, headers, gsmop_expect):
        """
        SERP-29157 Пробрасывать флажок мобильных операторов на средний
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetRwrHeaders(headers)
        query.SetInternal()
        query.SetRequireStatus(200)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        gsmop_got = self.get_rearr_relev_snip_param_value_from_ctx(ctxs['noapache_setup'][0]['client_ctx'], 'rearr', 'gsmop')
        assert gsmop_got in gsmop_expect

    @pytest.mark.parametrize("query_type, result", [
        (PLATFORM.DESKTOP, 'report=www'),
        (PLATFORM.SMART, 'report=www-smart'),
        (PLATFORM.PAD, 'report=www-tablet'),
    ])
    @TSoY.yield_test
    def test_sources_snip(self, query, query_type, result):
        """
        SERP-30830 - Передача вида репорта на базовый
        Проверяем, что в параметре snip есть вид репорта
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetQueryType(query_type)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]
        report = self.get_rearr_relev_snip_param_value_from_ctx(noapache_setup['client_ctx'], 'snip', 'report', unique=False)

        assert result in report

    @pytest.mark.parametrize("query_type, result", [
        (PLATFORM.DESKTOP, 'report=www'),
        (PLATFORM.SMART, 'report=www-smart'),
        (PLATFORM.PAD, 'report=www-tablet'),
    ])
    @TSoY.yield_test
    def test_report_type_global_deskpad(self, query, query_type, result):
        """
        SERP-36530: Пробрасывать тип репорта для ноапача
        Проверяем, что в глобальном контексте есть тип репорта
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetQueryType(query_type)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        assert result in ';'.join(noapache_setup['global_ctx'].get('snip', []))

    @pytest.mark.ticket('SERP-51364')
    @pytest.mark.parametrize(('query_type', 'flags', 'report', 'granny'), [
        (PLATFORM.DESKTOP, {},  'web', 0),
        (PLATFORM.TOUCH,   {},  'web', 0),
        (PLATFORM.TOUCH,   {'template': 'granny_exp:phone'},  'web', 1),
    ])
    @TSoY.yield_test
    def test_report_type_global_rearr(self, query, flags, query_type, report, granny):
        """
        SERP-51364: Пробрасывать тип репорта для ноапача
        Проверяем, что в rearr глобального контекста есть тип репорта
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetQueryType(query_type)
        query.SetFlags(flags)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        rearr = ';'.join(noapache_setup['global_ctx'].get('rearr', []))
        assert ('report=' + report) in rearr
        assert ('granny=' + str(granny)) in rearr

    @pytest.mark.ticket('SERP-47817')
    @pytest.mark.parametrize('query_type', (
        PLATFORM.DESKTOP,
        PLATFORM.PAD,
        PLATFORM.TOUCH,
    ))
    @TSoY.yield_test
    def test_waitall(self, query, query_type):
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetQueryType(query_type)
        query.SetFlags({'srcparams': '=waitall=1'})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        assert 'waitall' in noapache_setup['global_ctx']
        assert 'waitall' in noapache_setup['client_ctx']['WEB']

    @pytest.mark.parametrize('query_type', (
        PLATFORM.DESKTOP,
        PLATFORM.PAD,
        PLATFORM.TOUCH,
    ))
    @TSoY.yield_test
    def test_uatraits(self, query, query_type):
        query.SetQueryType(query_type)
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        assert 'uatraits' in noapache_setup['global_ctx']
        json.loads(noapache_setup['global_ctx']['uatraits'][0])

    @pytest.mark.parametrize('url', [
        HNDL.SEARCH
    ])
    @TSoY.yield_test
    def test_web_rearr(self, query, url):
        """
        Проверяем флаг web_rearr
        Он добавляет(на самом деле затирает параметр с соответствующим именем) параметры в rearr
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        TEST_PARAMETER = "FACTORS:textinfo:WRAP_REPORT"
        query.SetInternal()
        query.SetPath(url)
        query.SetFlags({"web_rearr": TEST_PARAMETER})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        rearr = noapache_setup['client_ctx']['WEB']['rearr'][0]
        assert TEST_PARAMETER in rearr

    @pytest.mark.ticket('SERP-50835')
    @TSoY.yield_test
    def test_app_host_expboxes(self, query):
        """
        Проверяем, что в контексте AppHost'а появился источник EXPBOXES
        """
        query.SetDumpFilter(resp=[CTXS.WEB_SEARCH])
        query.SetRwrHeaders({
            'X-Yandex-ExpBoxes': '39580,0,-1',
            'X-Yandex-ExpFlags': '{}{}{}{}'.format(
                'WwogIHsKICAgICJIQU5ETEVSIjogIlJFUE9SVCIsIAogICAgIkNPTlRFWFQiOiB7CiAgICAgICJSRVBPUlQiOiB7CiAgICAgICAgInRlc3RpZCI6IFsKICAgICAgICAgICIzOTE3MCIKICAgICAgIC',
                'BdCiAgICAgIH0sIAogICAgICAiTUFJTiI6IHsKICAgICAgICAic291cmNlIjogewogICAgICAgICAgIlBFT1BMRVBfU0FBUyI6IHsKICAgICAgICAgICAgInJlbGV2IjogWwogICAgICAgICAgICAg',
                'ICJmb3JtdWxhPXBwbF9yYW5raW5nXzE2MjkwNSIKICAgICAgICAgICAgXQogICAgICAgICAgfSwgCiAgICAgICAgICAiUEVPUExFIjogewogICAgICAgICAgICAicmVsZXYiOiBbCiAgICAgICAgIC',
                'AgICAgImZvcm11bGE9cHBsX3JhbmtpbmdfMTYyOTA1IgogICAgICAgICAgICBdCiAgICAgICAgICB9CiAgICAgICAgfQogICAgICB9CiAgICB9CiAgfQpd'
            )
        })
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert 'expboxes' in ctxs

    @pytest.mark.ticket('SERP-31286')
    @pytest.mark.parametrize(('query_type', 'result'), [
        (PLATFORM.DESKTOP, '56'),
        (PLATFORM.PAD, '56'),
        (PLATFORM.SMART, '69'),
    ])
    @TSoY.yield_test
    def test_yabs_title_length(self, query, query_type, result):
        """
        SERP-31286: Отправлять в БК сигнал об ожидаемой длине тайтла для десктопа и планшета
        Для смарта
        """
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetQueryType(query_type)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        yabs_setup = ctxs['yabs_setup'][0]

        if result:
            assert yabs_setup.get('title-length-limit') == [result]
        else:
            assert not yabs_setup.get('title-length-limit')

    @TSoY.yield_test
    def test_yabs_sz(self, query):
        """
        SERP-33219 - Прокинуть в БК инфу о размере экрана пользователя
        Есть кука в которой есть данные о размере экрана
        """
        SIZE = '50:50:1.1'
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.ReqYpCookie.set_sz(SIZE)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        yabs_setup = ctxs['yabs_setup'][0]

        assert len(yabs_setup) > 0
        assert yabs_setup['sz'] == [SIZE]

    @pytest.mark.ticket('SERP-45890')
    @TSoY.yield_test
    def test_yabs_szm_instead_sm(self, query):
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        SIZE_sz = '50:50:1.1'
        SIZE_szm = '1:600x1000:0x0'
        query.ReqYpCookie.set_sz(SIZE_sz)
        query.ReqYpCookie.set_szm(SIZE_szm)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        yabs_setup = ctxs['yabs_setup'][0]

        assert len(yabs_setup) > 0
        assert yabs_setup['sz'] == [SIZE_sz]

    @TSoY.yield_test
    def test_yabs_no_sz(self, query):
        """
        SERP-33219 - Прокинуть в БК инфу о размере экрана пользователя
        Есть куки нет, то ничего не прокидываем
        """
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        yabs_setup = ctxs['yabs_setup'][0]

        assert not yabs_setup.get('sz')

    @pytest.mark.parametrize("query_type, screen_size_szm, snip_width_expected,  tablet_grid_snip_width", [
        (PLATFORM.DESKTOP, '1:600x1000:0x0',  'snip_width=536', 0),
        (PLATFORM.TOUCH,   '8:1000x666:0x0',  'snip_width=492', 0),
        (PLATFORM.SMART,   '9:9999x9999:0x0', 'snip_width=536', 0),
        (PLATFORM.PAD,     '10:613x1:0x0',    'snip_width=536', 0),
        (PLATFORM.PAD,     '11:614x1:0x0',    'snip_width=536', 0),
        (PLATFORM.PAD,     '12:769x1:0x0',    'snip_width=536', 1),
        (PLATFORM.PAD,     '13:767x1:0x0',    'snip_width=536', 1),
        (PLATFORM.DESKTOP, '10:613x1:0x0',    'snip_width=536', 0),
        (PLATFORM.SMART,   '12:769x1:0x0',    'snip_width=536', 1),
        (PLATFORM.DESKTOP, '13:767x1:0x0',    'snip_width=536', 1),
    ])
    @TSoY.yield_test
    def test_snip_width(self, query, query_type, screen_size_szm, snip_width_expected, tablet_grid_snip_width):
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetQueryType(query_type)
        query.SetFlags({
            'tablet_grid_snip_width': +tablet_grid_snip_width
        })
        query.ReqYpCookie.set_szm(screen_size_szm)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        snip_width = self.get_rearr_relev_snip_param_value_from_ctx(noapache_setup['client_ctx'], 'snip', 'snip_width')
        assert snip_width == snip_width_expected

    @pytest.mark.parametrize("tld", [
        TLD.RU,
        pytest.param(TLD.UA, marks=pytest.mark.xfail(reason="SEARCH-11856")),
        TLD.KZ,
        TLD.BY,
        TLD.COMTR,
        TLD.COM
    ])
    @pytest.mark.parametrize("query_type, expected", [
        (PLATFORM.PAD, {TLD.RU: 172351, TLD.UA: 172352, TLD.KZ: 172353, TLD.BY: 172354, TLD.COMTR: 888, TLD.COM: 888}),
        (PLATFORM.DESKTOP, {TLD.RU: 172351, TLD.UA: 172352, TLD.KZ: 172353, TLD.BY: 172354, TLD.COMTR: 888, TLD.COM: 888}),
    ])
    @TSoY.yield_test
    def test_yabs_adblock_enabled(self, query, query_type, tld, expected):
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetFlags({'direct_page': 888})
        query.SetQueryType(query_type)
        query.SetParams({'lr': REGION_BY_TLD[tld]})
        query.SetDomain(tld)
        query.ReqYpCookie.set_los(1)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        yabs_setup = ctxs['yabs_setup']

        assert len(yabs_setup) > 0
        pageID, yabs_host = self._get_yabs_page_id_and_host(yabs_setup)
        assert pageID == str(expected[tld])

    @pytest.mark.parametrize("tld", [
        TLD.RU,
        pytest.param(TLD.UA, marks=pytest.mark.xfail(reason="SEARCH-11856")),
        TLD.KZ,
        TLD.BY,
        TLD.COMTR,
        TLD.COM
    ])
    @pytest.mark.parametrize("query_type, expected", [
        (PLATFORM.PAD, {TLD.RU: 172351, TLD.UA: 172352, TLD.KZ: 172353, TLD.BY: 172354, TLD.COMTR: 888, TLD.COM: 888}),
        (PLATFORM.DESKTOP, {TLD.RU: 172351, TLD.UA: 172352, TLD.KZ: 172353, TLD.BY: 172354, TLD.COMTR: 888, TLD.COM: 888}),
    ])
    @TSoY.yield_test
    def test_yabs_adblock_enabled_lost(self, query, query_type, tld, expected):
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetFlags({'direct_page': 888})
        query.SetQueryType(query_type)
        query.SetParams({'lr': REGION_BY_TLD[tld]})
        query.SetDomain(tld)
        query.ReqYpCookie.set_lost(1)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        yabs_setup = ctxs['yabs_setup']

        assert len(yabs_setup) > 0
        pageID, yabs_host = self._get_yabs_page_id_and_host(yabs_setup)
        assert pageID == str(expected[tld])

    @pytest.mark.soy_http('RUNTIMETESTS-96', 'BLNDR-5998')
    @TSoY.yield_test
    def test_direct_page_empty(self, query):
        """
        DMBACKEND-89 - Нет похода в БК при пустом запросе
        """
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetParams({
            'text': ''
        })
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert 'yabs_setup' not in ctxs

    @TSoY.yield_test
    def test_direct_page_not_empty(self, query):
        """
        SERP-32762 - Не распространять действие флага direct_page на пустые запросы
        не пустой запрос - применяем флаг
        """
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetFlags({'direct_page': '199'})
        query.SetParams({'text': 'test'})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        yabs_setup = ctxs['yabs_setup'][0]

        assert '199' == yabs_setup['metahost'][0].split('/')[-1][:-1]

    @pytest.mark.parametrize(('query_type', 'text', 'tld', 'result'), [
        (PLATFORM.PAD,       TEXT,     TLD.RU,     '2'),
        pytest.param(PLATFORM.PAD,       TEXT,     TLD.UA,     '63', marks=pytest.mark.xfail(reason="SEARCH-11856")),
        (PLATFORM.PAD,       TEXT,     TLD.KZ,     '113'),
        (PLATFORM.PAD,       TEXT,     TLD.BY,     '129'),
        (PLATFORM.PAD,       TEXT,     TLD.COMTR,  '165'),
        (PLATFORM.PAD,       TEXT,     TLD.COM,    '250'),
        (PLATFORM.PADAPP,    TEXT,     TLD.RU,     '849319'),
        pytest.param(PLATFORM.PADAPP,    TEXT,     TLD.UA,     '849319', marks=pytest.mark.xfail(reason="SEARCH-11856")),
        (PLATFORM.PADAPP,    TEXT,     TLD.KZ,     '849319'),
        (PLATFORM.PADAPP,    TEXT,     TLD.BY,     '849319'),
        (PLATFORM.PADAPP,    TEXT,     TLD.COMTR,  '849319'),
        (PLATFORM.PADAPP,    TEXT,     TLD.COM,    '849319'),
        # SERP-32497 - Переключить рекламные пейджи msearch на .ua .kz .by .com.tr
        # Проверяем, что уходит нужный pageID
        (PLATFORM.SMART,     TEXT,     TLD.RU,     '183'),
        pytest.param(PLATFORM.SMART,     TEXT,     TLD.UA,     '184', marks=pytest.mark.xfail(reason="SEARCH-11856")),
        # (PLATFORM.SMART,     TEXT,     TLD.KZ,     '185'),  # Language isn't allowed
        # (PLATFORM.SMART,     TEXT,     TLD.BY,     '186'),  # Language isn't allowed
        (PLATFORM.SMART,     TEXT,     TLD.COMTR,  '187'),
        # SERP-33713 - Директ на yandex.com на /search/ и /search/pad/
        # проверяем, что пейджи нужные
        (PLATFORM.DESKTOP,   TEXT,     TLD.COM,    '250'),
        (PLATFORM.DESKTOP,   TEXT,     TLD.COMTR,  '165'),
        (PLATFORM.DESKTOP,   TEXT,     TLD.RU,     '2'),
        pytest.param(PLATFORM.DESKTOP,   TEXT,     TLD.UA,     '63', marks=pytest.mark.xfail(reason="SEARCH-11856")),
        (PLATFORM.DESKTOP,   TEXT,     TLD.KZ,     '113'),
        (PLATFORM.DESKTOP,   TEXT,     TLD.BY,     '129'),
    ])
    @TSoY.yield_test
    def test_yabs_page_id(self, query, query_type, tld, result, text):
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetQueryType(query_type)
        query.SetParams({'text': text, 'lr': REGION_BY_TLD[tld]})
        query.SetDomain(tld)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        yabs_setup = ctxs['yabs_setup']

        assert len(yabs_setup) > 0
        pageID, yabs_host = self._get_yabs_page_id_and_host(yabs_setup)
        assert pageID == result
        if tld in [TLD.KZ, TLD.UA, TLD.BY]:  # or tld in [COMTR] and query_type == DESKTOP:
            assert yabs_host == 'yabs.yandex.' + tld
        else:
            assert yabs_host == 'yabs.yandex.ru'

    @pytest.mark.ticket('SERP-43429')
    @pytest.mark.parametrize(('query_type', 'path'), [(PLATFORM.DESKTOP, ''), (PLATFORM.PAD, 'pad/')])
    @TSoY.yield_test
    def test_yabs_referer_touch1(self, query, query_type, path):
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        referer = r'http://[^/]*?yandex\.ru/something'
        query.SetHeaders({'Referer': 'http://yandex.ru/something'})
        query.SetQueryType(query_type)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['yabs_setup']) > 0
        for yabs_setup in ctxs['yabs_setup']:
            headers = yabs_setup['_HttpHeaders'][0]
            assert re.search(r'Referer: https://[^/]*?yandex\.ru/search/{path}'.format(path=re.escape(path)), headers)
            assert re.search('X-YaBS-Rereferer: {referer}'.format(referer=referer), headers)

    @pytest.mark.ticket('SERP-43429')
    @pytest.mark.parametrize(('path'), [HNDL.SEARCH_TOUCH, HNDL.TOUCHSEARCH])
    @TSoY.yield_test
    def test_yabs_referer_touch2(self, query, path):
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetQueryType(PLATFORM.TOUCH)
        query.SetPath(path)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['yabs_setup']) > 0
        for yabs_setup in ctxs['yabs_setup']:
            headers = yabs_setup['_HttpHeaders'][0]
            assert re.search('Referer: https?://[^/]+/yandsearch', headers)
            assert 'X-YaBS-Rereferer: ' not in headers  # SERP-43810

    @pytest.mark.ticket('SERP-45982')
    @TSoY.yield_test
    def test_yabs_banner_ua_touch(self, query):
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        custom_ua = 'this+is+my+custom+banner+ua'
        query.SetQueryType(PLATFORM.TOUCH)
        query.SetParams({'banner_ua': custom_ua})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['yabs_setup']) > 0
        headers = ctxs['yabs_setup'][0]['_HttpHeaders'][0]
        assert headers.find('User-Agent:') >= 0
        # first header is used by the source
        assert headers.find('User-Agent:') == headers.find('User-Agent: '+custom_ua)

    @pytest.mark.ticket('SERP-45982')
    @TSoY.yield_test
    def test_yabs_bpage_touch(self, query):
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetQueryType(PLATFORM.TOUCH)
        query.SetParams({'bpage': 888})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['yabs_setup']) > 0
        pageID, yabs_host = self._get_yabs_page_id_and_host(ctxs['yabs_setup'])
        assert pageID == '888'
        assert yabs_host == 'yabs.yandex.ru'

    @pytest.mark.parametrize(('query_type'), [
        PLATFORM.SMART,
    ])
    @TSoY.yield_test
    def test_yabs_no_on_com(self, query, query_type):
        """
        SERP-33713 - Директ на yandex.com на /search/ и /search/pad/
        проверяем, что на всем остальном рекламы нет
        """
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetQueryType(query_type)
        query.SetParams({'lr': REGION_BY_TLD[TLD.COM]})
        query.SetDomain(TLD.COM)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert 'yabs_setup' not in ctxs

    @TSoY.yield_test
    def test_pron_srcparams_duplicate_bug(self, query):
        """
        SERP-46017 - Дублировались параметры в запросах в источники
        Проверяем, что параметр не дублируется
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetInternal()
        query.SetParams({'srcparams': '=pron=filterbanall'})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        assert 1 == noapache_setup['client_ctx']['ENTITYSEARCH']['pron'].count('filterbanall')
        assert 1 == noapache_setup['client_ctx']['GEOV']['pron'].count('filterbanall')

    @pytest.mark.soy_http('RUNTIMETESTS-130')
    @TSoY.yield_test
    def test_srcparams_metapool(self, query):
        """
        SERP-34708 - Унести режим сбора prs за логин
        Если пользователь залогинен - то параметр пробрасывается
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetFlags({'prs_logged': '1'})
        query.SetInternal()
        query.SetParams({'srcparams': 'WEB:metapool=factors:textinfo:wrap_report'})
        # query.set_auth()
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        assert noapache_setup['client_ctx']['WEB']['metapool'][0] == 'factors:textinfo:wrap_report'

    @TSoY.yield_test
    def test_srcparams_metapool_nologin_part(self, query):
        """
        SERP-34708 - Унести режим сбора prs за логин
        Если нет factors или wrap_report, то прокидываем в любом случае
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetFlags({'prs_logged': '1'})
        query.SetInternal()
        query.SetParams({'srcparams': 'WEB:metapool=factors:textinfo'})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        assert noapache_setup['client_ctx']['WEB']['metapool'][0] == 'factors:textinfo'

    @pytest.mark.soy_http('RUNTIMETESTS-130')
    @TSoY.yield_test
    def test_srcparams_metapool_noflag(self, query):
        """
        SERP-34708 - Унести режим сбора prs за логин
        Без флага, проверяем, что все работает и без залогина
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetInternal()
        query.SetParams({'srcparams': 'WEB:metapool=factors:textinfo:wrap_report'})
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        assert noapache_setup['client_ctx']['WEB']['metapool'][0] == 'factors:textinfo:wrap_report'

    @TSoY.yield_test
    def test_req_is_manual_1_gid_1_laas_1(self, query):
        """
        SERP-33871 - Проверка правильности reg_is_manual
        Проверяем заголовки laas
        Выставлено X-Region-Is-User-Choice и X-Region-City-Id
        передаем req_is_manual
        """
        query.SetRwrHeaders({
            'X-Region-Is-User-Choice': '1',
            'X-Region-City-Id': '213',
            'X-LaaS-Answered': '1'
        })
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP, CTXS.NOAPACHE])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['yabs_setup']) > 0
        assert ctxs['yabs_setup'][0]['reg_is_manual']
        assert 'rgm=1' in ctxs['noapache_setup'][0]['client_ctx']['WEB']['rearr'][0]

    @TSoY.yield_test
    def test_req_is_manual_0_gid_1_laas_0(self, query):
        """
        SERP-33871 - Проверка правильности reg_is_manual
        Проверяем заголовки laas
        Выставлено X-Region-City-Id, но X-Region-Is-User-Choice:0
        не передаем req_is_manual
        """
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP, CTXS.NOAPACHE])
        query.SetRwrHeaders({
            'X-Region-Is-User-Choice': '0',
            'X-Region-City-Id': '213',
            'X-LaaS-Answered': '1'
        })
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['yabs_setup']) > 0
        assert not ctxs['yabs_setup'][0].get('reg_is_manual')
        assert 'rgm=1' not in ctxs['noapache_setup'][0]['client_ctx']['WEB']['rearr'][0]

    @TSoY.yield_test
    def test_req_is_manual_0_gid_1_laas_none(self, query):
        """
        SERP-33871 - Проверка правильности reg_is_manual
        Проверяем заголовки laas
        Выставлено X-Region-City-Id, но отсутствует X-Region-Is-User-Choice
        не передаем req_is_manual
        """
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP, CTXS.NOAPACHE])
        query.SetRwrHeaders({
            'X-Region-City-Id': '213',
            'X-LaaS-Answered': '1'
        })
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['yabs_setup']) > 0
        assert not ctxs['yabs_setup'][0].get('reg_is_manual')
        assert 'rgm=1' not in ctxs['noapache_setup'][0]['client_ctx']['WEB']['rearr'][0]

    @TSoY.yield_test
    def test_req_is_manual_0_gid_0_laas_1(self, query):
        """
        SERP-33871 - Проверка правильности reg_is_manual
        Проверяем заголовки laas
        Игнорируем laas заголовки, если нет X-Region-City-Id
        (поведение как для случая без laas)
        """
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP, CTXS.NOAPACHE])
        query.SetRwrHeaders({
            'X-Region-Is-User-Choice': '0',
            'X-LaaS-Answered': '1'
        })
        query.SetYandexGid('213')
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['yabs_setup']) > 0
        assert not ctxs['yabs_setup'][0].get('reg_is_manual')
        assert 'rgm=1' not in ctxs['noapache_setup'][0]['client_ctx']['WEB']['rearr'][0]

    @pytest.mark.parametrize(('query_type', 'result'), [
        (PLATFORM.SMART, 'is_mobile=1'),
        (PLATFORM.PAD,   'is_desktop=1'),
        (PLATFORM.DESKTOP, 'is_desktop=1'),
    ])
    @TSoY.yield_test
    def test_report_type_rearr(self, query, query_type, result):
        """
        SERP-35722 - Для каждого репорта посылаем свое значение в rearr
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetQueryType(query_type)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        assert result in ';'.join(noapache_setup['client_ctx']['WEB']['rearr'])

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket("LAAS-1799")
    @pytest.mark.parametrize('mode', [True, False])
    @pytest.mark.parametrize(('pag', 're'), [
        (None, r'^1\.d\.'),
        ('u', r'^0\.\.')
    ])
    @TSoY.yield_test
    def test_noapache_pag(self, query, pag, re, mode):
        """
        SERP-36592 - Открыть параметр pag= снаружи
        Проверить, что параметр работает без залогина
        (по изменению группировки в источнике WEB)
        """
        query.SetParams({'pag': pag})
        query.SetMode(external=mode)
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetRequireStatus(200)

        resp = yield query
        noapache_setup = resp.GetCtxs()['noapache_setup'][0]

        assert [filter(lambda g: re.match(re, g), noapache_setup['client_ctx']['WEB']['g'])]

    @pytest.mark.ticket('SERP-48925')
    @pytest.mark.parametrize(('query_type'), [
        PLATFORM.DESKTOP,
        PLATFORM.TOUCH,
        PLATFORM.PAD
    ])
    @TSoY.yield_test
    def test_noapache_faf_adr(self, query, query_type):
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetParams({'faf': 'adr'})
        query.SetQueryType(query_type)
        query.SetRequireStatus(200)

        resp = yield query
        noapache_setup = resp.GetCtxs()['noapache_setup'][0]

        rearr = ';'.join(noapache_setup['global_ctx']['rearr'])
        assert 'scheme_Local/BlenderFmls/ExpFlags/adresa_always_first=1' in rearr

    @pytest.mark.ticket('SERP-37323')
    @pytest.mark.ticket('SEARCH-11086')
    @pytest.mark.parametrize('platform', [
        PLATFORM.DESKTOP,
        PLATFORM.PAD
    ])
    @pytest.mark.parametrize(('faf', 'result', 'is_work'), [
        ('afs', 'scheme_Local/BlenderFmls/ExpFlags/afisha_always_first=1', False),
        ('adr', 'scheme_Local/BlenderFmls/ExpFlags/adresa_always_first=1', True),
        ('adrmr', 'scheme_Local/RightColumnTransport/Restrictions={"GEOV":{"LeaveAsText":1, "Pre":"1"}}', True),
        ('trf', 'scheme_Local/BlenderFmls/ExpFlags/traffic_jams_always_first=1', False),
        ('wea', 'scheme_Local/BlenderFmls/ExpFlags/weather_always_first=1', False),
        (None, re.compile(r'^scheme_Local/BlenderFmls/ExpFlags/'), True),
    ])
    @TSoY.yield_test
    def test_noapache_fa_desktop_and_pad(self, query, faf, result, is_work, platform):
        """
        SERP-37323 - Параметры faf faa доступные извне, для проброски rearr - ов
        Проверяем, что для десктопа работают только adr & adrmr

        SEARCH-11086 - Переключаем падовую верстку на десктопную, в связи с чем
        объединяем тест для падов с десктопным
        """

        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetParams({'faf': faf})
        query.SetQueryType(platform)
        query.SetRequireStatus(200)

        resp = yield query
        noapache_setup = resp.GetCtxs()['noapache_setup'][0]

        rearr = ';'.join(noapache_setup['global_ctx']['rearr'])
        if faf is None:
            for value in rearr:
                assert result.search(value) is None
        else:
            assert (result in rearr) == is_work

    @pytest.mark.ticket('SERP-36789')
    @TSoY.yield_test
    def test_noapache_group_size(self, query):
        """
        SERP-36789 - Сделать флаг dup_group_size и max_dup_group_count
        Проверяем кастомные значения флагов
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetFlags({'dup_group_size': 0, 'max_dup_group_count': 1})
        query.SetRequireStatus(200)

        resp = yield query
        noapache_setup = resp.GetCtxs()['noapache_setup'][0]

        assert [filter(lambda g: re.match(r'^1\.d\.', g), noapache_setup['client_ctx']['WEB']['g'])]

    @pytest.mark.ticket('SERP-36789', 'SERP-37803')
    @TSoY.yield_test
    def test_noapache_group_size_default(self, query):
        """
        SERP-36789 - Сделать флаг dup_group_size и max_dup_group_count
        Проверяем дефолтные значения флага
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetRequireStatus(200)

        resp = yield query
        noapache_setup = resp.GetCtxs()['noapache_setup'][0]

        assert [filter(lambda g: re.match(r'^1\.d\.', g), noapache_setup['client_ctx']['WEB']['g'])]

    @pytest.mark.ticket('SERP-37337')
    @pytest.mark.parametrize(('flags', 'source'), [
        ({'enable_quick_dev': 1},  'QUICK_DEV')
    ])
    @TSoY.yield_test
    def test_noapache_quick_more(self, query, flags, source):
        """
        SERP-37337 - Отправлять нагрузку на тестовые контуры QUICK_DEV и QUICK_STG по флагу
        по флагу enable_quick_dev вместе с запросами в QUICK добавлялась нагрузка на QUICK_DEV
        по флагу enable_quick_stg вместе с запросами в QUICK добавлялась нагрузка на QUICK_STG
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetParams({
            'text': 'Путин'
        })
        query.SetFlags(flags)
        query.SetFlags({
            'disable_preclassifiers': 1
        })
        query.SetRequireStatus(200)

        resp = yield query
        noapache_setup = resp.GetCtxs()['noapache_setup'][0]

        assert 'QUICK' in noapache_setup['client_ctx']
        assert source in noapache_setup['client_ctx']

    @pytest.mark.ticket('SERP-37318')
    @pytest.mark.parametrize(('timestamp_delta', 'flag_value', 'result'), [
        (0,       None, True),             # дефолт, кука актуальна, все ок, координаты совпадают
        (60*57,   None, True),             # дефолт, кука актуальна, все ок, координаты совпадают
        (60*61,   None, True),             # дефолт, кука актуальна, все ок, координаты совпадают
        (60*57*2, None, True),             # дефолт, кука актуальна, все ок, координаты совпадают
        (60*61*2, None, False),            # дефолт, кука неактуальна, координаты не совпадают
        (30,      65,   True),             # выставляем время в секундах, кука актуальна
        (60,      50,   False),            # выставляем время в секундах, кука неактуальна
        (0,       -1,   False),            # флаг, выставленный в -1 - это игнорирование куки
        (60,      -1,   False),            # флаг, выставленный в -1 - это игнорирование куки
    ])
    @TSoY.yield_test
    def test_noapache_gpauto(self, query, timestamp_delta, flag_value, result):
        """
        SERP-37318 - Сделать под флагом разное время жизни координаты от gpauto
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetParams({'text': 'шоколадница'})

        if flag_value:
            query.SetFlags({'gpauto_ttl': str(flag_value)})

        lat = '55.715104'
        lon = '37.552804'
        query.SetYandexGid(213)
        query.SetRwrHeaders({
            'X-Forwarded-For': '78.108.195.43'
        })
        query.ReqYpCookie.set_gpauto(lat, lon, timestamp=(int(time.time()) - timestamp_delta))
        query.SetRequireStatus(200)

        resp = yield query
        noapache_setup = resp.GetCtxs()['noapache_setup'][0]

        assert 'GEOV' in noapache_setup['client_ctx']
        if result:
            # ll отсутствует в GEOV контексте
            # assert noapache_setup['client_ctx']['GEOV']['ll'][0] == ','.join((lon, lat))
            pass
        else:
            # ll отсутствует в GEOV контексте
            # assert 'll' not in noapache_setup['client_ctx']['GEOV']
            pass

    @TSoY.yield_test
    def test_noapache_gpauto_laas(self, query):
        """
        WEBREPORT-44 - проверить работу laas_geopoint
        """
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetParams({'text': 'шоколадница'})
        lat = '55.715104'
        lon = '37.552804'
        xrl = ','.join([lat, lon, "1"])
        query.SetYandexGid(213)
        query.SetRwrHeaders({
            "X-LaaS-Answered": "1",
            "X-Region-Location": xrl
        })
        query.SetRequireStatus(200)

        resp = yield query
        noapache_setup = resp.GetCtxs()['noapache_setup'][0]

        assert 'GEOV' in noapache_setup['client_ctx']
        assert noapache_setup['client_ctx']['GEOV']['middle_nearby_ull'][0] == ','.join((lon, lat))

    @pytest.mark.ticket('SERP-45205')
    @pytest.mark.parametrize('path', [
        HNDL.SEARCH
    ])
    @TSoY.yield_test
    def test_geov_external_params(self, query, path):
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        uri, oid = 'ymapsbm1://org?oid=1388360881', '1388360881'
        query.SetParams({'text': 'шоколадница', 'uri': uri, 'oid': 'b:' + oid})
        query.SetPath(path)
        query.SetRequireStatus(200)

        resp = yield query
        noapache_setup = resp.GetCtxs()['noapache_setup'][0]

        geov = noapache_setup['client_ctx']['GEOV'] if 'GEOV' in noapache_setup['client_ctx'] else noapache_setup['client_ctx']['GEOV_SLOW']
        assert geov
        assert geov['uri'] == [uri]
        assert geov['fixed_top'] == [oid]

    @pytest.mark.ticket('SERP-44415')
    @TSoY.yield_test
    def test_noapache_uuid_to_yabs_no_uuid(self, query):
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetParams({'text': 'холодильник'})
        query.SetRequireStatus(200)

        resp = yield query
        yabs_setup = resp.GetCtxs()['yabs_setup']

        assert len(yabs_setup) > 0
        assert not yabs_setup[0].get('uuid')

    @pytest.mark.ticket('SERP-37705', 'SERP-44415')
    @TSoY.yield_test
    def test_noapache_uuid_to_yabs(self, query):
        """
        Прокидывать uuid в БК для Поискового Приложения
        Проверяем, что uuid есть
        """
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        UUID = "0002431eafb4aa76b98c856224226e4d"
        query.SetParams({'uuid': UUID})
        query.SetQueryType(PLATFORM.PADAPP)
        query.SetRequireStatus(200)

        resp = yield query
        yabs_setup = resp.GetCtxs()['yabs_setup']

        assert len(yabs_setup) > 0
        assert yabs_setup[0]['uuid'] == [UUID]

    @pytest.mark.ticket('SERP-37705', 'SERP-44415')
    @TSoY.yield_test
    def test_noapache_uuid_to_yabs_pad(self, query):
        """
        Прокидывать uuid в БК для Поискового Приложения
        Проверяем, что uuid нет, так как не YandexSearch
        """
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        UUID = "0002431eafb4aa76b98c856224226e4d"
        query.SetParams({'uuid': UUID})
        query.SetQueryType(PLATFORM.PAD)
        query.SetUserAgent(USER_AGENT.PAD_ANDROID_4_3)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['yabs_setup']) > 0
        for yabs_setup in ctxs['yabs_setup']:
            assert yabs_setup['uuid'] == [UUID]

    @pytest.mark.ticket('SERP-43690')
    @TSoY.yield_test
    def test_yabs_encoding(self, query):
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetFlags({'yabs_text_from_wizard': 1})
        query.SetParams({'text': 'BAYRAK DİREĞİ', 'noreask': 1})
        query.SetRequireStatus(200)

        resp = yield query
        yabs_setup = resp.GetCtxs()['yabs_setup']

        assert len(yabs_setup) > 0
        assert yabs_setup[0]['text'] == [u'bayrak direği']

    @pytest.mark.ticket('SERP-44651')
    @TSoY.yield_test
    def test_yandexuid_in_entitysearch(self, query):
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        uid = '5401345411472567123'
        query.SetYandexUid(uid)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        noapache_setup = ctxs['noapache_setup'][0]

        assert noapache_setup['client_ctx']['ENTITYSEARCH']['yandexuid'] == [uid]

    @TSoY.yield_test
    def test_yandexuid_param(self, query):
        test_uid = ''.join(['1112', str(int(time.time()))])
        query.SetParams({'yandexuid': test_uid})
        query.SetDumpFilter(resp=[CTXS.BLENDER])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        template_data = ctxs['template_data'][0]['data']

        assert template_data['reqdata']['ruid'] == test_uid

    @TSoY.yield_test
    def test_word_stat(self, query):
        query.SetParams({'text': 'youtube'})
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert int(ctxs['blender_pre_search'][0]['app_host']['word_stat']['data']) != 0

    @TSoY.yield_test
    def test_request_extensions(self, query):
        query.SetParams({'text': 'youtube'})
        query.SetDumpFilter(resp=['REQUEST_EXTENSIONS_POST'])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        data = ctxs['request_extensions_meta'][0]['binary']
        report = TReport()
        report.ParseFromString(base64.b64decode(data))

        has_data = False
        for grouping in report.Grouping:
            for group in grouping.Group:
                for document in group.Document:
                    if getattr(document, 'ArchiveInfo', None):
                        for attr in document.ArchiveInfo.GtaRelatedAttribute:
                            if len(attr.Value.decode()):
                                has_data = True

        assert has_data
