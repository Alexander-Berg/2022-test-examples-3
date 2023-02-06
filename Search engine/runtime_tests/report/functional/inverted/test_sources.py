# -*- coding: utf-8 -*-

import re
import json
import urlparse
import time
import base64
import urllib

import os
import pytest

from runtime_tests.util.predef.handler.server.http import CloseConfig, SimpleDelayedConfig
from runtime_tests.util.predef.http.response import raw_ok, service_unavailable

from report.functional.web.base import BaseFuncTest
from report.const import *


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

@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestSourcesParams(BaseFuncTest):
    def _get_yabs_page_id_and_host(self, yabs_setup):
        for yabs in yabs_setup:
            if 'metahost' in yabs:
                pageID = yabs['metahost'][0].split('?')[0].split('/')[2]
                yabs_host = yabs['metahost'][0].split('?')[0].split(':')[1]
                return pageID, yabs_host
        return None, None

    def get_rearr_relev_snip_param_value_from_ctx(self, clt_ctx, name, param_name, unique=True):
        #берем все источники, куда передаются поля name
        sources_param = self.get_param_from_clt_ctx(clt_ctx, name)

        assert len(sources_param) > 0 #есть источники, где есть нужный переметр

        #выдераем из пар источник-поле только подполя с нужным именем
        param = [p for s in sources_param for p in s[1] if param_name in p]

        assert len(param) > 0 #хоть на какой-то источник эти подполя должны были передаться

        param = list(set(param))

        if unique:
            assert len(param) == 1 #везде одинаковые значения
            return param[0]
        else:
            return param

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("query_type, result", [
        (DESKTOP, 'report=www'),
        (SMART, 'report=www-smart'),
        (PAD, 'report=www-tablet'),
    ])
    def test_sources_snip(self, query, query_type, result):
        """
        SERP-30830 - Передача вида репорта на базовый
        Проверяем, что в параметре snip есть вид репорта
        """
        query.set_query_type(query_type)
        noapache_setup = self.get_noapache_setup(query)
        report = self.get_rearr_relev_snip_param_value_from_ctx(noapache_setup['client_ctx'], 'snip', 'report', unique=False)

        assert result in report

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("query_type, result", [
        #(DESKTOP, 'report=www'),
        #(SMART, 'report=www-smart'),
        (PAD, 'report=www-tablet'),
    ])
    def test_report_type_global_deskpad(self, query, query_type, result):
        """
        SERP-36530: Пробрасывать тип репорта для ноапача
        Проверяем, что в глобальном контексте есть тип репорта
        """
        query.set_query_type(query_type)
        noapache_setup = self.get_noapache_setup(query)

        assert result in ';'.join(noapache_setup['global_ctx'].get('snip', []))

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-51364')
    @pytest.mark.parametrize(('query_type', 'flags', 'report', 'granny') , [
        (DESKTOP, {},  'web', 0),
        (TOUCH,   {},  'web', 0),
        (TOUCH,   { 'template': 'granny_exp:phone' },  'web', 1),
    ])
    def test_report_type_global_rearr(self, query, flags, query_type, report, granny):
        """
        SERP-51364: Пробрасывать тип репорта для ноапача
        Проверяем, что в rearr глобального контекста есть тип репорта
        """

        # wrong gateway using
        query.set_query_type(query_type)
        query.set_flags(flags)
        noapache_setup = self.get_noapache_setup(query)

        rearr = ';'.join(noapache_setup['global_ctx'].get('rearr', []))
        assert ('report=' + report) in rearr
        assert ('granny=' + str(granny)) in rearr

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-47817')
    @pytest.mark.parametrize('query_type', (
        DESKTOP,
        PAD,
        TOUCH,
    ))
    def test_waitall(self, query, query_type):
        query.set_query_type(query_type)
        query.set_flags({'srcparams':'=waitall=1'})

        noapache_setup = self.get_noapache_setup(query)

        assert 'waitall' in noapache_setup['global_ctx']
        assert 'waitall' in noapache_setup['client_ctx']['WEB']

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize('query_type', (
        DESKTOP,
        PAD,
        TOUCH,
    ))
    def test_uatraits(self, query, query_type):
        query.set_query_type(query_type)

        noapache_setup = self.get_noapache_setup(query)

        assert 'uatraits' in noapache_setup['global_ctx']
        uatraits = json.loads(noapache_setup['global_ctx']['uatraits'][0])

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize('url', [
        SEARCH,
        # SEARCH_XML
    ])
    def test_web_rearr(self, query, url):
        """
        Проверяем флаг web_rearr
        Он добавляет(на самом деле затирает параметр с соответствующим именем) параметры в rearr
        """
        TEST_PARAMETER = "fon=gamgy_test_parameter"
        query.set_internal()
        query.set_url(url)
        query.set_flags({"web_rearr": TEST_PARAMETER})
        query.add_params({'json_dump': "search.app_host.sources.(_.name =~ 'NOAPACHE_SETUP')"})

        noapache_setup = self.get_noapache_setup(query)
        rearr = noapache_setup['client_ctx']['WEB']['rearr'][0]

        assert TEST_PARAMETER in rearr

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-50835')
    def test_app_host_expboxes(self, query):
        """
        Проверяем, что в контексте AppHost'а появился источник EXPBOXES
        """
        query.headers['X-Yandex-ExpBoxes'] = '39580,0,-1';
        query.headers['X-Yandex-ExpFlags'] = 'WwogIHsKICAgICJIQU5ETEVSIjogIlJFUE9SVCIsIAogICAgIkNPTlRFWFQiOiB7CiAgICAgICJSRVBPUlQiOiB7CiAgICAgICAgInRlc3RpZCI6IFsKICAgICAgICAgICIzOTE3MCIKICAgICAgICBdCiAgICAgIH0sIAogICAgICAiTUFJTiI6IHsKICAgICAgICAic291cmNlIjogewogICAgICAgICAgIlBFT1BMRVBfU0FBUyI6IHsKICAgICAgICAgICAgInJlbGV2IjogWwogICAgICAgICAgICAgICJmb3JtdWxhPXBwbF9yYW5raW5nXzE2MjkwNSIKICAgICAgICAgICAgXQogICAgICAgICAgfSwgCiAgICAgICAgICAiUEVPUExFIjogewogICAgICAgICAgICAicmVsZXYiOiBbCiAgICAgICAgICAgICAgImZvcm11bGE9cHBsX3JhbmtpbmdfMTYyOTA1IgogICAgICAgICAgICBdCiAgICAgICAgICB9CiAgICAgICAgfQogICAgICB9CiAgICB9CiAgfQpd';
        query.set_params({ "json_dump" : "app_host" })
        resp = self.json_request(query)
        assert 'expboxes' in resp.data['app_host']

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_yabs_sz(self, query):
        """
        SERP-33219 - Прокинуть в БК инфу о размере экрана пользователя
        Есть кука в которой есть данные о размере экрана
        """
        SIZE = '50:50:1.1'

        query.headers.cookie.yp.set_sz(SIZE)
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        assert yabs_setup[0]['sz'] == [SIZE]

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-45890')
    def test_yabs_szm_instead_sm(self, query):
        SIZE_sz = '50:50:1.1'
        SIZE_szm = '1:600x1000:0x0'
        query.headers.cookie.yp.set_sz(SIZE_sz)
        query.headers.cookie.yp.set_szm(SIZE_szm)
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        assert yabs_setup[0]['sz'] == [SIZE_sz]

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_yabs_no_sz(self, query):
        """
        SERP-33219 - Прокинуть в БК инфу о размере экрана пользователя
        Есть куки нет, то ничего не прокидываем
        """
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        assert not yabs_setup[0].get('sz')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_direct_page_not_empty(self, query):
        """
        SERP-32762 - Не распространять действие флага direct_page на пустые запросы
        не пустой запрос - применяем флаг
        """
        query.set_flags({'direct_page': '199'})
        query.add_params({'text': 'test'})

        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0

        pageID = yabs_setup[0]['metahost'][0].split('/')[-1][:-1]

        assert pageID == '199'

    @pytest.mark.ticket('SERP-43429')
    @pytest.mark.parametrize(('query_type','path'), [(DESKTOP, ''), (PAD, 'pad/')])
    def test_yabs_referer_touch1(self, query, query_type, path):
        referer = 'http://[^/]*?yandex\.ru/something'
        query.headers['Referer'] = "http://yandex.ru/something"
        query.set_query_type(query_type)

        for yabs_setup in self.json_dump_context(query, ['yabs_setup']):
            headers = yabs_setup['_HttpHeaders'][0]
            assert re.search('Referer: https://[^/]*?yandex\.ru/search/{path}'.format(path=re.escape(path)), headers)
            assert re.search('X-YaBS-Rereferer: {referer}'.format(referer=referer), headers)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-43429')
    @pytest.mark.parametrize(('path'), ['/search/touch/', '/touchsearch'])
    def test_yabs_referer_touch2(self, query, path):
        query.set_query_type(TOUCH)
        query.set_url(path)

        for yabs_setup in self.json_dump_context(query, ['yabs_setup']):
            headers = yabs_setup['_HttpHeaders'][0]
            assert re.search('Referer: https?://[^/]+/yandsearch', headers)
            assert 'X-YaBS-Rereferer: ' not in headers  # SERP-43810

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-45982')
    def test_yabs_banner_ua_touch(self, query):
        custom_ua = 'this+is+my+custom+banner+ua'
        query.set_query_type(TOUCH)
        query.add_params({'banner_ua': custom_ua})

        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        headers = yabs_setup[0]['_HttpHeaders'][0]

        assert headers.find('User-Agent:') >= 0
        # first header is used by the source
        assert headers.find('User-Agent:') == headers.find('User-Agent: '+custom_ua)

    @pytest.mark.ticket('SERP-45982')
    def test_yabs_bpage_touch(self, query):
        query.set_query_type(TOUCH)
        query.add_params({'bpage': 888})

        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        pageID, yabs_host = self._get_yabs_page_id_and_host(yabs_setup)

        assert pageID == '888'
        assert yabs_host == 'yabs.yandex.ru'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(('query_type'), [
        SMART,
    ])
    def test_yabs_no_on_com(self, query, query_type):
        """
        SERP-33713 - Директ на yandex.com на /search/ и /search/pad/
        проверяем, что на всем остальном рекламы нет
        """
        query.set_query_type(query_type)
        query.add_params({'lr': REGION_BY_TLD[COM]})
        query.set_host(COM)

        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) == 0

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_pron_srcparams_duplicate_bug(self, query):
        """
        SERP-46017 - Дублировались параметры в запросах в источники
        Проверяем, что параметр не дублируется
        """
        query.set_internal()
        query.add_params({'srcparams': '=pron=filterbanall'})
        noapache_setup = self.get_noapache_setup(query)
        assert 1 == noapache_setup['client_ctx']['ENTITYSEARCH']['pron'].count('filterbanall')
        assert 1 == noapache_setup['client_ctx']['GEOV']['pron'].count('filterbanall')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_srcparams_metapool(self, query):
        """
        SERP-34708 - Унести режим сбора prs за логин
        Если пользователь залогинен - то параметр пробрасывается
        """
        query.set_flag('prs_logged')
        query.set_internal()
        query.add_params({'srcparams': 'WEB:metapool=factors:textinfo:wrap_report'})
        query.set_auth()

        noapache_setup = self.get_noapache_setup(query)
        assert noapache_setup['client_ctx']['WEB']['metapool'][0] == 'factors:textinfo:wrap_report'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_srcparams_metapool_nologin_part(self, query):
        """
        SERP-34708 - Унести режим сбора prs за логин
        Если нет factors или wrap_report, то прокидываем в любом случае
        """
        query.set_flag('prs_logged')
        query.set_internal()
        query.add_params({'srcparams': 'WEB:metapool=factors:textinfo'})

        noapache_setup = self.get_noapache_setup(query)
        assert noapache_setup['client_ctx']['WEB']['metapool'][0] == 'factors:textinfo'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_srcparams_metapool_noflag(self, query):
        """
        SERP-34708 - Унести режим сбора prs за логин
        Без флага, проверяем, что все работает и без залогина
        """
        query.set_internal()
        query.add_params({'srcparams': 'WEB:metapool=factors:textinfo:wrap_report'})

        noapache_setup = self.get_noapache_setup(query)
        assert noapache_setup['client_ctx']['WEB']['metapool'][0] == 'factors:textinfo:wrap_report'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_noapache_no_pag(self, query):
        """
        SERP-36592 - Открыть параметр pag= снаружи
        Проверить, что параметр работает без залогина
        (по изменению группировки в источнике WEB)
        """

        query.add_params({'pag': ''})
        noapache_setup = self.get_noapache_setup(query)
        self.check_grouping(noapache_setup['client_ctx']['WEB']['g'], re.compile(r'^1\.d\.'))

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-48925')
    @pytest.mark.parametrize(('query_type'), [
        DESKTOP,
        TOUCH,
        PAD
    ])
    def test_noapache_faf_adr(self, query, query_type):
        query.replace_params({'faf': 'adr'})
        query.set_query_type(query_type)
        noapache_setup = self.get_noapache_setup(query)
        rearr = ';'.join(noapache_setup['global_ctx']['rearr'])
        assert 'scheme_Local/BlenderFmls/ExpFlags/adresa_always_first=1' in rearr

    @pytest.mark.ticket('SERP-37323')
    @pytest.mark.parametrize(('faf', 'result', 'is_work'), [
    ('afs', 'scheme_Local/BlenderFmls/ExpFlags/afisha_always_first=1', False),
    ('adr', 'scheme_Local/BlenderFmls/ExpFlags/adresa_always_first=1', True),
    ('adrmr', 'scheme_Local/RightColumnTransport/Restrictions={"GEOV":{"LeaveAsText":1, "Pre":"1"}}', True),
    ('trf', 'scheme_Local/BlenderFmls/ExpFlags/traffic_jams_always_first=1', False),
    ('wea', 'scheme_Local/BlenderFmls/ExpFlags/weather_always_first=1', False),
    ])
    def test_noapache_fa_desktop(self, query, faf, result, is_work):
        """
        SERP-37323 - Параметры faf faa доступные извне, для проброски rearr - ов
        Проверяем, что для десктопа работают только adr & adrmr
        """
        query.replace_params({'faf': faf})
        query.set_query_type(DESKTOP)
        noapache_setup = self.get_noapache_setup(query)

        rearr = ';'.join(noapache_setup['global_ctx']['rearr'])

        assert (result in rearr) == is_work

    def base_test_noapache_group_size(self, query, result = None):
        noapache_setup = self.get_noapache_setup(query)

        ok = False
        for g in noapache_setup['client_ctx']['WEB']['g']:
            params = g.split('.')
            if params[1] == 'd' and params[-2:] == result:
                ok = True
                break
        assert ok, noapache_setup['client_ctx']['WEB']['g']

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-36789')
    def test_noapache_group_size(self, query):
        """
        SERP-36789 - Сделать флаг dup_group_size и max_dup_group_count
        Проверяем кастомные значения флагов
        """
        query.set_flags({'dup_group_size': 0, 'max_dup_group_count': 1})
        self.base_test_noapache_group_size(query, result=['0','1'])

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-36789', 'SERP-37803')
    def test_noapache_group_size_default(self, query):
        """
        SERP-36789 - Сделать флаг dup_group_size и max_dup_group_count
        Проверяем дефолтные значения флага
        """
        self.base_test_noapache_group_size(query, result=['0', '0'])

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-37337')
    @pytest.mark.parametrize(('flag', 'source'),[
        ('enable_quick_dev',  'QUICK_DEV')
    ])
    def test_noapache_quick_more(self, query, flag, source):
        """
        SERP-37337 - Отправлять нагрузку на тестовые контуры QUICK_DEV и QUICK_STG по флагу
        по флагу enable_quick_dev вместе с запросами в QUICK добавлялась нагрузка на QUICK_DEV
        по флагу enable_quick_stg вместе с запросами в QUICK добавлялась нагрузка на QUICK_STG
        """
        query.set_flag(flag)
        query.replace_params({'text': 'Путин'})
        noapache_setup = self.get_noapache_setup(query)

        assert 'QUICK' in noapache_setup['client_ctx']
        assert source in noapache_setup['client_ctx']

    @pytest.mark.skip()
    @pytest.mark.ticket('SERP-45205')
    @pytest.mark.parametrize('path', [
        '/search/'
    ])
    def test_geov_external_params(self, query, path):
        uri, oid = 'ymapsbm1://org?oid=1388360881', '1388360881'
        query.set_params({'text': 'шоколадница', 'uri': uri, 'oid': oid})
        query.set_url(path)
        if path != '/search/':
            query.set_ajax()
            contexts = self.with_noapache(query)[0]
        else:
            contexts = self.get_noapache_setup(query)
        geov = contexts['client_ctx']['GEOV'] if 'GEOV' in contexts['client_ctx'] else contexts['client_ctx']['GEOV_SLOW']
        assert geov
        assert geov['uri'] == [uri]
        assert geov['relev_result_ids'] == [oid]

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-44415')
    def test_noapache_uuid_to_yabs_no_uuid(self, query):
        query.replace_params({'text': 'холодильник'})
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        assert not yabs_setup[0].get('uuid')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-37705', 'SERP-44415')
    def test_noapache_uuid_to_yabs(self, query):
        """
        Прокидывать uuid в БК для Поискового Приложения
        Проверяем, что uuid есть
        """
        UUID = "0002431eafb4aa76b98c856224226e4d"
        query.add_params({'uuid': UUID})
        query.set_query_type(PADAPP)
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        assert yabs_setup[0]['uuid'] == [UUID]

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-37705', 'SERP-44415')
    def test_noapache_uuid_to_yabs_pad(self, query):
        """
        Прокидывать uuid в БК для Поискового Приложения
        Проверяем, что uuid нет, так как не YandexSearch
        """
        UUID = "0002431eafb4aa76b98c856224226e4d"
        query.add_params({'uuid': UUID})
        query.set_query_type(PAD)
        query.set_user_agent(USER_AGENT_PAD_ANDROID_4_3)

        for yabs_setup in self.json_dump_context(query, ['yabs_setup']):
            assert yabs_setup['uuid'] == [UUID]

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-43690', 'RUNTIMETESTS-11')
    def test_yabs_encoding(self, query):
        query.set_flag('yabs_text_from_wizard')
        query.replace_params({'text': 'BAYRAK DİREĞİ', 'noreask': 1})
        assert self.source_param(query, 'YABS', 'text') == u'bayrak direği'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-44651')
    def test_yandexuid_in_entitysearch(self, query):
        uid = '5401345411472567123'
        query.set_yandexuid(uid)
        assert self.source_param(query, 'ENTITYSEARCH', 'yandexuid') == uid

    @pytest.mark.skipif(True, reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-46970')
    def test_myreqid_flag(self, query):
        test_reqid='this-is-a-test-reqid-1234'
        query.set_flags({ 'myreqid': test_reqid })
        resp = self.json_request(query)
        assert resp.data['reqdata']['reqid'] == test_reqid

    @pytest.mark.skipif(True, reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-46970')
    def test_myreqid_param(self, query):
        test_reqid='this-is-a-test-reqid-1234'
        query.add_params({'myreqid': test_reqid})
        resp = self.json_request(query)
        assert resp.data['reqdata']['reqid'] == test_reqid

    @pytest.mark.ticket('SERP-46979')
    def test_yandexuid_param(self, query):
        test_uid = ''.join(['1112', str(int(time.time()))])
        query.add_params({'yandexuid': test_uid})
        resp = self.json_request(query)
        assert resp.data['reqdata']['ruid'] == test_uid
