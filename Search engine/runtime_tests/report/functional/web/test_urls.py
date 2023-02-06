# -*- coding: utf-8 -*-

import os
import pytest

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestUrls(BaseFuncTest):
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

        Настройки поиска:
            /search/customize

        Разные ручки:
            /admin                  -   ручка админского управления
            /search/storeclick      -   сохранение в находки, POST запрос TODO: тест
            /search/wizard          -   only RKUB колдунщик горячей воды, который может внедряться во фреймы
            /search/wizardsjson     -   only RKUB колдунщики, обязательное поле - type
            /search/auto-regions
            /search/hotwater        -   only RKUB
            /search/inforequest     -   кука релевантности
            /search/wmpreview       -   #url на внешний сервис не в конфиге
            /search/video[/touch|pad] -   ручки дозагрузки данных для колдунщика Видео

    """

    def test_blablabla(self, query):
        query.set_url('/blablabla')
        self.request(query, require_status=404)

    @pytest.mark.ticket('SERP-50278')
    @pytest.mark.parametrize(('host', 'match'), [
        (BY, '# yandex.by'),
        (COM, '# yandex.com'),
        (COMGE, '# yandex.*'),
        (COMTR, '# yandex.com.tr'),
        (FR, '# yandex.*'),
        (KZ, '# yandex.kz'),
        ('yandex.net', '# yandex.net'),
        (RU, '# yandex.ru'),
        (UA, '# yandex.ua'),
        (UZ, '# yandex.*')
    ])
    def test_robots_txt(self, query, host, match):
        query.set_host(host)
        query.set_url('/robots.txt')
        query.set_params({})

        resp = self.request(query, require_status=200)

        assert resp.content.startswith(match)

    @pytest.mark.parametrize("region", [
        RU,
        COM,
        COMTR
    ])
    def _test_search_adult(self, query, region):
        query.set_url("/search/adult?url=www.xvideos.com")
        query.set_host(region)

        resp = self.request(query)

        CONTAINS_ADULT_MATERIAL = {
            RU: 'содержит материалы «для взрослых»',
            COM: 'contains adult material',
            COMTR: '"yetişkin" içeriği barındırıyor'
        }

        assert CONTAINS_ADULT_MATERIAL[region] in resp.content

    @pytest.mark.parametrize("region", [
        RU,
        COM,
        COMTR
    ])
    def _test_search_adult_error(self, query, region):
        query.set_url("/search/adult")
        query.set_host(region)
        self.request(query, require_status=404)

    @pytest.mark.parametrize("region", [
        RU,
        COM,
        COMTR
    ])
    def _test_search_infected(self, query, region):
        query.set_url("/search/infected?url=wmconvirus.narod.ru")
        query.set_host(region)

        resp = self.request(query)

        THE_SITE_MAY_BE_UNSAFE = {
            RU: 'Сайт <strong>wmconvirus.narod.ru</strong> может представлять угрозу',
            COM: 'The site <strong>wmconvirus.narod.ru</strong> may be un-safe',
            COMTR: '<strong>wmconvirus.narod.ru</strong> sitesi tehlikeli olabilir'
        }

        assert THE_SITE_MAY_BE_UNSAFE[region] in resp.content

    @pytest.mark.parametrize("region", [
        RU,
        COM,
        COMTR
    ])
    def _test_search_infected_error(self, query, region):
        """
        без параметров - получаем ошибку
        """
        query.set_url("/search/infected")
        query.set_host(region)

        self.request(query, require_status=404)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("region", [
        RU,
        COM,
        COMTR
    ])
    def test_search_redir_warning(self, query, region):
        query.set_url("/search/redir_warning?url=http://ya.ru")
        query.set_host(region)

        resp = self.request(query)

        THE_LINK_YOU_ARE_ABOUT_TO = {
            RU: (
                'Вы пытаетесь перейти по ссылке, которая устарела '
                'или ведёт на сайт, угрожающий безопасности компьютера.'
            ),
            COM: (
                'The link you are about to follow is either outdated '
                'or may harm your computer.'
            ),
            COMTR: (
                'Eski ya da sizi, bilgisayarınız için tehlike oluşturabilecek bir '
                'siteye yönlendiren bir bağlantıya tıklamak üzeresiniz.'
            )
        }

        assert THE_LINK_YOU_ARE_ABOUT_TO[region] in resp.content

    @pytest.mark.parametrize("region", [
        RU,
        COM,
        COMTR
    ])
    def test_search_redir_warning_error(self, query, region):
        query.set_url("/search/redir_warning")
        query.set_host(region)
        self.request(query, require_status=404)

    # SERP-50947
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("region", [
        RU,
        COM,
        COMTR
    ])
    @pytest.mark.parametrize("prefix", [
        "/search",
        ""
    ])
    def test_static_prefetch(self, query, prefix, region):
        query.set_url(prefix + '/prefetch.txt')
        query.set_host(region)

        resp = self.request(query)

        assert resp.headers['cache-control'][0] == 'private, max-age=21600'
#        assert resp.headers['content-type'][0] == 'text/plain; charset=utf-8'
        assert len(resp.content) > 10

    # SERP-50947
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("region", [
        RU,
        COM,
        COMTR
    ])
    @pytest.mark.parametrize("prefix", [
        "/search",
        ""
    ])
    @pytest.mark.parametrize("url", [
        "/yandcache.js",
        "/touchcache.js",
        "/padcache.js"
    ])
    def test_static_cache(self, query, prefix, url, region):
        query.set_url(prefix + url)
        query.set_host(region)

        resp = self.request(query)

        assert resp.headers['cache-control'][0] == 'private, max-age=21600'
        assert resp.headers['content-type'][0] == 'application/javascript; charset=utf-8'
        assert len(resp.content) > 10

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("region", [
        RU,
        COM,
        COMTR
    ])
    def test_search_opensearch_xml(self, query, region):
        query.set_url('/search/opensearch.xml')
        query.set_host(region)

        resp = self.request(query)
        if not resp.content:
            logging.error("Empty response, should be XML here")

        # reponse should be valid xml content
        assert '<?xml' in resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize("region", [
        RU,
        # COM,
        # COMTR
    ])
    def test_search_site_opensearch_xml(self, query, region):
        query.set_url('/search/site/opensearch.xml')
        query.set_host(region)

        resp = self.request(query)

        assert 'xml' in resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("host", "region"), [
        (RU, REGION[RU_MOSCOW]),
        (COM, REGION[USA]),
        (COMTR, REGION[COMTR_ISTANBUL])
    ])
    @pytest.mark.parametrize("url", [
        SEARCH,
        SEARCH_FAMILY,
        SEARCH_XML,
    ])
    def test_search_desktop(self, url, query, host, region):
        """
        Поиски, работающие для desktop user-agent
        """
        query.set_url(url)
        query.set_host(host)
        query.set_region(region)
        query.set_internal()

        resp = self.request(query, require_status=None)

        if region == COM and url != SEARCH_XML:
            assert resp.status == 302
            assert resp.headers['location'][0].startswith('https://yandex.ru/search/')
        else:
            assert resp.status == 200
            assert TEXT.encode('utf-8') in resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("host", "region"), [
        (RU, REGION[RU_MOSCOW]),
        (COM, REGION[USA]),
        (COMTR, REGION[COMTR_ISTANBUL])
    ])
    def test_search_pad(self, query, host, region):
        query.set_url(SEARCH_PAD)
        query.set_host(host)
        query.set_region(region)
        query.set_user_agent(USER_AGENT_PAD)

        resp = self.request(query, require_status=None)

        if region == COM:
            assert resp.status == 302
            assert resp.headers['location'][0].startswith('https://yandex.ru/search/')
        else:
            assert resp.status == 200
            assert TEXT.encode('utf-8') in resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("host", "region"), [
        (RU, REGION[RU_MOSCOW]),
        (COM, REGION[USA]),
        (COMTR, REGION[COMTR_ISTANBUL])
    ])
    def test_search_smart(self, query, host, region):
        query.set_url(SEARCH_SMART)
        query.set_host(host)
        query.set_region(region)
        query.set_user_agent(USER_AGENT_SMART)

        resp = self.request(query, require_status=None)

        if (region != COM):
            assert resp.status == 200
            assert TEXT.encode('utf-8') in resp.content
        else:
            assert resp.status == 302
            assert resp.headers['location'][0].startswith('https://yandex.ru/search/')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("region", [
        RU,
        # COMTR
    ])
    def test_search_site(self, query, region):
        query.set_query_type(SITESEARCH)
        query.set_host(region)

        resp = self.request(query)

        assert TEXT.encode('utf-8') in resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.unstable
    @pytest.mark.parametrize(
        ('tld'),
        [
            RU,
            KZ,
        ]
    )
    def test_search_wizardsjson(self, query, tld):
        """
        SERP-35959 - ручка https://yandex.kz/search/wizardsjson отдаёт редирект при пустых куках
        Один из случаев - колдунщик цвета
        """
        query.set_host(tld)
        query.set_url("/search/wizardsjson")
        query.set_params({'type': 'colors', 'text': 'ff00ff'})
        query.set_noauth()

        resp = self.request(query)

        assert '"value_rgb":["255","0","255"]' in resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_wizardsjson_postal_codes(self, query):
        """
        Postal codes response
        """
        query.set_url("/search/wizardsjson")
        query.set_params({
            'type': 'postal_codes',
            'text': 'Россия, Москва, улица Перерва, 12',
            'geo': 213,
            'timeout': 999999,
            'nocache': 'da',
            'waitall': 999999
        })
        resp = self.json_request(query, require_status=200)
        assert len(resp.data) > 0
        data_keys = resp.data[0].keys()
        assert 'address' in data_keys
        assert 'source' in data_keys
        assert 'selected_region_ll' in data_keys
        assert 'selected_region_spn' in data_keys
        assert 'selected_region' in data_keys
        assert 'obj' in data_keys
        assert 'mode' in data_keys

    @pytest.mark.skipif(True, reason='Closed for spring')
    def test_search_hotwater(self, query):
        query.set_url("/search/hotwater")
        query.set_params({'city': '213', 'address': TEXT})
        query.set_host(RU)

        resp = self.request(query)

        assert "Адрес не найден. Проверьте правильность написания" in resp.content

    @pytest.mark.skipif(not (os.environ.get('REPORT_INVERTED') == '1'), reason="Отключение тестов неинвертированной схемы")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("text", "wizard_type"), [
        ("погода в москве", "weather"),
        ("котики", "video")
    ])
    def test_search_result_handler(self, query, text, wizard_type):
        query.set_url("/search/result")
        query.set_params({
            'text': text,
            'type': wizard_type,
            'format': 'json'
        })

        query.set_host(RU)

        resp = self.json_request(query, require_status=200)

        assert 'error' not in resp.data.keys()
        assert 'docs' in resp.data.keys()

    @pytest.mark.skipif(not (os.environ.get('REPORT_INVERTED') == '1'), reason="Отключение тестов неинвертированной схемы")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_result_empty_response(self, query):
        """
        SEARCHPRODINCIDENTS-2827: [27.10.17] PROD-WEB / report:
        пятисотки изнанки после выкатки report-core в sas и man (SAS,MAN)
        """
        query.set_url("/search/result")
        query.set_params({
            'text': 'sadfasdfasdfasdfasd',
            'type': 'geo',
            'format': 'json'
        })
        query.set_host(RU)

        resp = self.json_request(query, require_status=200)

        assert 'error' in resp.data.keys()
        assert resp.data['error'] == 'No result'
        assert resp.data['type'] == 'report'
        assert 'docs' not in resp.data.keys()

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_direct_preview_touch_handler(self, query):
        query.set_url("/search/direct-preview/touch")
        query.set_params({
            'lr': '213',
            'adv': '{"test":"1"}',
            'text': '1',
            'type': 'yabs_proxy',
            'format': 'json'
        })
        query.set_host(RU)

        resp = self.json_request(query, require_status=200)

        assert 'error' not in resp.data.keys()
        assert 'docs' in resp.data.keys()

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_direct_preview_pad_handler(self, query):
        query.set_url("/search/direct-preview/pad")
        query.set_params({
            'lr': '213',
            'adv': '{"test":"1"}',
            'text': '1',
            'type': 'yabs_proxy',
            'format': 'json'
        })
        query.set_host(RU)

        resp = self.json_request(query, require_status=200)

        assert 'error' not in resp.data.keys()
        assert 'docs' in resp.data.keys()

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_wizardsjson_color_general_hsv(self, query):
        query.set_host(RU)
        query.set_url("/search/wizardsjson")
        query.set_params({
            'type': 'colors',
            'text': '336 16 22',
            'is_hsv': '1',
            'user_input': '1',
            'layoutLang': 'ru'
        })

        resp = self.json_request(query, require_status=200)

        assert 'current' in resp.data[0].keys()
        assert 'next' in resp.data[0].keys()
        assert 'prev' in resp.data[0].keys()
        assert resp.data[0]['isHSV'] == 1
        assert resp.data[0]['isHEX'] == 0
        assert resp.data[0]['isRGB'] == 0

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_wizardsjson_color_general_hex(self, query):
        query.set_host(RU)
        query.set_url("/search/wizardsjson")
        query.set_params({
            'type': 'colors',
            'text': '123456',
            'user_input': '1',
            'layoutLang': 'ru'
        })

        resp = self.json_request(query, require_status=200)

        assert 'current' in resp.data[0].keys()
        assert 'next' in resp.data[0].keys()
        assert 'prev' in resp.data[0].keys()
        assert resp.data[0]['isHSV'] == 0
        assert resp.data[0]['isHEX'] == 1
        assert resp.data[0]['isRGB'] == 0

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_wizardsjson_color_segment_next(self, query):
        query.set_host(RU)
        query.set_url("/search/wizardsjson")
        query.set_params({
            'type': 'colors',
            'text': '905d5d',
            'segment': 'next',
            'layoutLang': 'ru'
        })

        resp = self.json_request(query, require_status=200)

        assert 'segment' in resp.data.keys()
        assert 'data' in resp.data.keys()

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_wizardsjson_color_segment_prev(self, query):
        query.set_host(RU)
        query.set_url("/search/wizardsjson")
        query.set_params({
            'type': 'colors',
            'text': 'c34d0a',
            'segment': 'prev',
            'layoutLang': 'ru'
        })

        resp = self.json_request(query, require_status=200)

        assert 'segment' in resp.data.keys()
        assert 'data' in resp.data.keys()

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_wizardsjson_autoregions_city(self, query):
        query.set_host(RU)
        query.set_url("/search/wizardsjson")
        query.set_params({
            'type': 'suggestfacts2',
            'text': 'Барнаул',
            'subtype': 'autoregions',
            'timeout': 999999,
            'nocache': 'da',
            'waitall': 999999
        })
        # за данные отвечает nkireev@
        resp = self.json_request(query, require_status=200)

        for key in ("norm_query", "question", "text", "type", "voiceInfo"):
            assert key in resp.data[0].keys()

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_wizardsjson_autoregions_region(self, query):
        query.set_host(RU)
        query.set_url("/search/wizardsjson")
        query.set_params({
            'type': 'suggestfacts2',
            'text': 'Новосибирская область',
            'subtype': 'autoregions',
        })
        # за данные отвечает nkireev@
        resp = self.json_request(query, require_status=200)

        for key in ("norm_query", "question", "text", "type", "voiceInfo"):
            assert key in resp.data[0].keys()

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_wizardsjson_autoregions_number(self, query):
        query.set_host(RU)
        query.set_url("/search/wizardsjson")
        query.set_params({
            'type': 'suggestfacts2',
            'text': '77',
            'subtype': 'autoregions',
            'timeout': 999999,
            'nocache': 'da',
            'waitall': 999999
        })
        # за данные отвечает nkireev@
        resp = self.json_request(query, require_status=200)

        for key in ("norm_query", "question", "text", "type", "voiceInfo"):
            assert key in resp.data[0].keys()

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_search_wizardsjson_unitsconverter(self, query):
        query.set_host(RU)
        query.set_url("/search/wizardsjson")
        query.set_params({
            'text': '-',
            'fromamount': '1',
            'type': 'units_converter_graph',
            'from': 'USD',
            'to': 'RUR',
            'src': 'RUS',
        })

        resp = self.json_request(query, require_status=200)

        assert 'rates' in resp.data[0]['data']['graph_data']['graphs'].keys()

    @pytest.mark.skipif(True, reason="Temporaly removed")
    @pytest.mark.parametrize("region", [
        RU,
        COM,
        COMTR
    ])
    def test_search_poll_station(self, query, region):
        """
        Запрос без параметров - пустой ответ
        """
        query.set_url("/search/poll-stations")
        query.set_host(region)

        resp = self.request(query)

        assert "json={" in resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-44761')
    @pytest.mark.ticket('SERP-44367')
    @pytest.mark.parametrize(("path"), [
        ('/search/pre'),
        ('/search/pad/pre'),
        ('/search/touch/pre')
    ])
    def test_ajax_valid_yu(self, query, path):
        query.set_params(['callback', 'jQuery18305933488425875815_1423233158581', 'ajax', '{}', 'yu', YANDEXUID])
        query.set_yandexuid(YANDEXUID)
        query.set_url(path)

        resp = self.request(query)
        assert resp.content.startswith('jQuery18305933488425875815_1423233158581({')
        assert resp.content.count('jQuery18305933488425875815_1423233158581({') == 1
        assert 'JSONP security token invalid' not in resp.content

        query.set_yandexuid(888)  # wrong yandexuid
        resp = self.request(query)
        assert resp.content.startswith('jQuery18305933488425875815_1423233158581({')
        assert resp.content.count('jQuery18305933488425875815_1423233158581({') == 1
        assert resp.content.count(
            'jQuery18305933488425875815_1423233158581({ error: "JSONP security token invalid" })'
        ) == 1

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-45235')
    @pytest.mark.parametrize(("path"), [
        ('/search/pre'),
        ('/search/pad/pre'),
        ('/search/touch/pre')
    ])
    def test_ajax_valid_yp(self, query, path):
        query.set_params(['callback', 'jQuery18305933488425875815_1423233158581', 'ajax', '{}', 'yu', YANDEXUID])
        query.set_yandexuid(YANDEXUID)
        query.set_url(path)

        resp = self.request(query)
        assert resp.content.startswith('jQuery18305933488425875815_1423233158581({')
        assert resp.content.count('jQuery18305933488425875815_1423233158581({') == 1
        assert 'JSONP security token invalid' not in resp.content

        query.set_yandexuid(888)  # wrong yandexuid
        resp = self.request(query)
        assert resp.content.startswith('jQuery18305933488425875815_1423233158581({')
        assert resp.content.count('jQuery18305933488425875815_1423233158581({') == 1
        assert resp.content.count(
            'jQuery18305933488425875815_1423233158581({ error: "JSONP security token invalid" })'
        ) == 1

    def test_lr29(self, query):
        query.set_url('/search/')
        query.set_params({
            'text': (
                '%D1%81%D1%82%D1%80%D0%BE%D0%B8%D1%82%D0%B5%D0%BB%D1%8C%'
                'D1%81%D1%82%D0%B2%D0%BE%20%D0%B4%D0%BE%D0%BC%D0%BE%D0%B2'
            ),
            'lr': '29',
        })
        self.request(query)

    @pytest.mark.parametrize("https", [True, False])
    @pytest.mark.parametrize("url", [
        '/search/nahodki',
        '/search/nahodki/',
    ])
    @pytest.mark.parametrize("host", [
        RU,
        BY,
    ])
    def test_nahodki_redirect(self, query, url, host, https):
        """
        Поиски, работающие для desktop user-agent
        """
        query.set_url(url)
        query.set_host(host)
        query.set_https(https)

        resp = self.request(query, require_status=302)

        proto = 'https' if https else 'http'
        assert resp.headers['location'][0].startswith('{}://www.yandex.{}'.format(proto, host))
