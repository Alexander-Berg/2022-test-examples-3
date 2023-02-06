# -*- coding: utf-8 -*-

from util.tsoy import TSoY
import pytest
from util.const import HNDL, TLD, PLATFORM, CTXS


class TestUrls_2():
    """
    Проверяем все url на которые отвечает web-репорт.

    Полные проверки на околостатику. Только позитивные кейсы на все остальное.
    Статус ответа и простые проверки валидности ответа.
    + невалидный урл.

    По мотивам тикета SERP-25619

    URLS:
    Полные проверки:
        Служебные: - only RKUB
            /search/v
            /search/vl
            /search/versions
            /search/all-supported-params
            /search/all-supported-flags
            /search/check_condition
            /search/tail-log
            /search/viewconfig
            /search/checkconfig

        Предупреждения:
            /search/adult
            /search/infected
            /search/redir_warning

        Статика:
            /search/yandcache.js
            /search/touchcache.js
            /search/padcache.js
            /search/prefetch.txt

        Почти статика:
            /search/opensearch.xml
            /search/site/opensearch.xml - разбираемся о том, какие регионы нужны - SERP-3037

    Только позитивыне кейсы:
        Поиск:
            /search/
            /search/family
            /search/pad/
            /search/xml         -   в xml поиске нет редиректов. Совсем
            /search/touch/
            /search/smart/
            /search/site/       -   only RKUB Поиск для сайта https://site.yandex.ru/
                                    разбираемся о том, какие регионы нужны - SERP-30371

        Разные ручки:
            /admin                  -   ручка админского управления
            /search/storeclick      -   сохранение в находки, POST запрос TODO: тест
            /search/wizard          -   only RKUB колдунщик горячей воды, который может внедряться во фреймы
            /search/wizardsjson     -   only RKUB колдунщики, обязательное поле - type
            /search/auto-regions
            /search/adresa-geolocation - TODO: тест на какой-то(кроме пустого) ответ
            /search/hotwater        -   only RKUB
            /search/inforequest     -   кука релевантности
            /search/wmpreview       -   #url на внешний сервис не в конфиге
            /search/video[/touch|pad] -   ручки дозагрузки данных для колдунщика Видео

    """

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-44761')
    @pytest.mark.ticket('SERP-44367')
    @pytest.mark.parametrize(("path", "platform"), [
        (HNDL.SEARCH_PRE, PLATFORM.DESKTOP),
        (HNDL.SEARCH_PAD_PRE, PLATFORM.PAD),
        (HNDL.SEARCH_TOUCH_PRE, PLATFORM.TOUCH)
    ])
    @TSoY.yield_test
    def test_search_pre(self, query, path, platform):
        query.SetQueryType(platform)
        query.SetPath(path)
        query.SetRequireStatus(200)

        yield query

    @TSoY.yield_test
    def test_lr29(self, query):
        query.SetPath(HNDL.SEARCH)
        query.SetParams({
            'text': (
                '%D1%81%D1%82%D1%80%D0%BE%D0%B8%D1%82%D0%B5%D0%BB%D1%8C%'
                'D1%81%D1%82%D0%B2%D0%BE%20%D0%B4%D0%BE%D0%BC%D0%BE%D0%B2'
            ),
            'lr': '29',
        })
        query.SetRequireStatus(200)

        yield query

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-50277')
    @pytest.mark.parametrize(('ento', 'on'), [
        ('0oCghydXc1Mjg1NRgCQg7QvNCw0LTQvtC90L3QsPS_JLg', True),
        ('0oCghydXc1Mjg1NRgEQgdtYWRvbm5hCNvw4A', True),
        ('Helloasjkdbasydgaisdgasdknas7dta71y23871h', False)
    ])
    @pytest.mark.parametrize("tld", [
        TLD.RU,
        TLD.COMTR
    ])
    @TSoY.yield_test
    def test_entitysearch(self, query, ento, on, tld):
        # делаем запрос https://yandex.ru/search/?lr=213&text=%D1%84%D0%B8%D0%BB%D1%8C%D0%BC%D1%8B+2000
        # жмем на "Больше фильмов" отправляется запрос на HNDL.SEARCH_ENTITY
        query.SetPath(HNDL.SEARCH_ENTITY)
        query.SetDomain(tld)
        query.SetParams({
            'format': 'json',
            'ento': ento
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert 'entity' == js['type']
        assert 'error' not in js.keys()
        assert ('display_options' in js.keys()) == on

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-54392')
    @pytest.mark.ticket('SERP-56089')
    @pytest.mark.parametrize(("path"), [
        (HNDL.TURBO),
        (HNDL.CHAT)
    ])
    @TSoY.yield_test
    def test_turbo(self, query, path):
        query.SetPath(path)
        query.SetRequireStatus(200)

        yield query

    @pytest.mark.ticket('SERP-60141')
    @TSoY.yield_test
    def test_json_proxy(self, query):
        query.SetQueryType(PLATFORM.JSON_PROXY)
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA, CTXS.INIT_HTTP_RESPONSE])
        query.SetInternal()
        # no need check status when use json_dump_response. Instead of this we must check status in the http_response
        query.SetRequireStatus(200)

        resp = yield query
        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        http_resp = resp.GetCtxs()['http_response']
        assert len(http_resp) and "status_code" in http_resp[0] and http_resp[0]["status_code"] == 200

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-63584')
    @TSoY.yield_test
    def test_json_proxy_time_mode(self, query):
        query.SetQueryType(PLATFORM.JSON_PROXY)
        query.SetParams({
            'act': 'time'
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert 'time' in js
        assert js['time'] > 1521647856
