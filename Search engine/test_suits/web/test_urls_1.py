# -*- coding: utf-8 -*-

from util.tsoy import TSoY
import pytest
from util.const import TEXT, XML_TEXT, HNDL, GEO, USER_AGENT, TLD, REGION, PLATFORM, CTXS
from urllib.parse import unquote_plus
import json


class TestUrls_1():
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
    @TSoY.yield_test
    def test_blablabla(self, query):
        query.SetPath(HNDL.BLABLABLA)
        query.SetRequireStatus(require_status=[404])

        yield query

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-50278')
    @pytest.mark.parametrize(('tld', 'match'), [
        (TLD.BY, '# yandex.by'),
        (TLD.COM, '# yandex.com'),
        (TLD.COMGE, '# yandex.ru'),
        (TLD.COMTR, '# yandex.com.tr'),
        (TLD.FR, '# yandex.fr'),
        (TLD.KZ, '# yandex.kz'),
        # ('yandex.net', '# yandex.net'),
        (TLD.RU, '# yandex.ru'),
        pytest.param(TLD.UA, '# yandex.ua', marks=pytest.mark.xfail(reason="RUNTIMETESTS-143")),
        (TLD.UZ, '# yandex.*')
    ])
    @TSoY.yield_test
    def test_robots_txt(self, query, tld, match):
        query.SetDomain(tld)
        query.SetPath(HNDL.ROBOTSTXT)
        query.ResetParams()
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.text.startswith(match)

    @pytest.mark.parametrize("tld", [
        TLD.RU,
        TLD.COM,
        TLD.COMTR
    ])
    @TSoY.yield_test
    def _test_search_adult(self, query, tld):
        query.SetPath(HNDL.SEARCH_ADULT)
        query.SetParams({'url': 'www.xvideos.com'})
        query.SetDomain(tld)
        query.SetRequireStatus(200)

        resp = yield query

        CONTAINS_ADULT_MATERIAL = {
            TLD.RU: 'содержит материалы «для взрослых»',
            TLD.COM: 'contains adult material',
            TLD.COMTR: '"yetişkin" içeriği barındırıyor'
        }

        assert CONTAINS_ADULT_MATERIAL[tld] in resp.text

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("tld", [
        TLD.RU,
        TLD.COM,
        TLD.COMTR
    ])
    @TSoY.yield_test
    def test_search_redir_warning(self, query, tld):
        query.SetPath(HNDL.SEARCH_REDIRWARNING)
        query.SetParams({'url': 'http://ya.ru'})
        query.SetDomain(tld)
        query.SetRequireStatus(200)

        resp = yield query

        THE_LINK_YOU_ARE_ABOUT_TO = {
            TLD.RU: (
                'Вы пытаетесь перейти по ссылке, которая устарела '
                'или ведёт на сайт, угрожающий безопасности компьютера.'
            ),
            TLD.COM: (
                'The link you are about to follow is either outdated '
                'or may harm your computer.'
            ),
            TLD.COMTR: (
                'Eski ya da sizi, bilgisayarınız için tehlike oluşturabilecek bir '
                'siteye yönlendiren bir bağlantıya tıklamak üzeresiniz.'
            )
        }
        assert THE_LINK_YOU_ARE_ABOUT_TO[tld] in resp.text

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("tld", [
        TLD.RU,
        TLD.COM,
        TLD.COMTR
    ])
    @TSoY.yield_test
    def test_search_redir_warning_error(self, query, tld):
        query.SetPath(HNDL.SEARCH_REDIRWARNING)
        query.SetDomain(tld)
        query.SetRequireStatus(require_status=[404])

        yield query

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-50947')
    @pytest.mark.parametrize("tld", [
        TLD.RU,
        TLD.COM,
        TLD.COMTR
    ])
    @pytest.mark.parametrize("path", [
        HNDL.SEARCH_PREFETCH,
        HNDL.PREFETCH
    ])
    @TSoY.yield_test
    def test_static_prefetch(self, query, path, tld):
        query.SetPath(path)
        query.SetDomain(tld)
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.headers.get('cache-control') == 'private, max-age=21600'
        assert resp.headers.get('content-type') == 'text/plain; charset=utf-8'
        assert len(resp.text) > 10

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SERP-50947')
    @pytest.mark.parametrize("tld", [
        TLD.RU,
        TLD.COM,
        TLD.COMTR
    ])
    @pytest.mark.parametrize("path", [
        HNDL.YANDCACHE,
        HNDL.SEARCH_YANDCACHE,
        HNDL.TOUCHCACHE,
        HNDL.SEARCH_TOUCHCACHE,
        HNDL.PADCACHE,
        HNDL.SEARCH_PADCACHE
    ])
    @TSoY.yield_test
    def test_static_cache(self, query, path, tld):
        query.SetPath(path)
        query.SetDomain(tld)
        query.SetRequireStatus(200)

        resp = yield query

        assert resp.headers.get('cache-control') == 'private, max-age=21600'
        assert resp.headers.get('content-type') == 'application/javascript; charset=utf-8'
        assert len(resp.text) > 10

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("tld", [
        TLD.RU,
        TLD.COM,
        TLD.COMTR
    ])
    @TSoY.yield_test
    def test_search_opensearch_xml(self, query, tld):
        query.SetPath(HNDL.SEARCH_OPENSEARCHXML)
        query.SetDomain(tld)
        query.SetRequireStatus(200)

        resp = yield query

        if not resp.text:
            raise AssertionError("Empty response, should be XML here")

        # reponse should be valid xml content
        assert '<?xml' in resp.text

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.xfail(reason='Реально потеряли ручку')
    @pytest.mark.parametrize("tld", [
        TLD.RU,
        # TLD.COM,
        # TLD.COMTR
    ])
    @TSoY.yield_test
    def test_search_site_opensearch_xml(self, query, tld):
        query.SetPath(HNDL.SEARCH_SITE_OPENSEARCHXML)
        query.SetDomain(tld)
        query.SetNoAuth()
        query.SetRequireStatus(200)

        resp = yield query

        assert 'xml' in resp.text

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize(("tld", "region"), [
        (TLD.RU, REGION[GEO.RU_MOSCOW]),
        (TLD.COM, REGION[GEO.USA]),
        (TLD.COMTR, REGION[GEO.COMTR_ISTANBUL])
    ])
    @pytest.mark.parametrize("path", [
        HNDL.SEARCH,
        HNDL.SEARCH_FAMILY,
        HNDL.SEARCH_XML,
    ])
    @TSoY.yield_test
    def test_search_desktop(self, path, query, tld, region):
        """
        Поиски, работающие для desktop user-agent
        """
        query.SetPath(path)
        query.SetDomain(tld)
        query.SetRegion(region)
        query.SetInternal()
        require_status = 302 if region == TLD.COM and path != HNDL.SEARCH_XML else 200
        query.SetRequireStatus(require_status)

        resp = yield query

        if require_status == 302:
            assert resp.GetLocation().path == HNDL.SEARCH
            assert resp.GetLocation().scheme == 'https'
            assert resp.GetLocationTld() == TLD.RU
        elif path == HNDL.SEARCH_XML:
            assert XML_TEXT in resp.text
        else:
            assert TEXT in resp.text

    @pytest.mark.parametrize(("tld", "region"), [
        (TLD.RU, REGION[GEO.RU_MOSCOW]),
        (TLD.COM, REGION[GEO.USA]),
        (TLD.COMTR, REGION[GEO.COMTR_ISTANBUL])
    ])
    @TSoY.yield_test
    def test_search_pad(self, query, tld, region):
        query.SetPath(HNDL.SEARCH_PAD)
        query.SetDomain(tld)
        query.SetRegion(region)
        query.SetUserAgent(USER_AGENT.PAD)
        require_status = 302 if region == TLD.COM else 200
        query.SetRequireStatus(require_status)

        resp = yield query

        if require_status == 302:
            assert resp.GetLocation().path == HNDL.SEARCH
            assert resp.GetLocation().scheme == 'https'
        else:
            assert TEXT in resp.text

    @pytest.mark.parametrize(("tld", "region"), [
        (TLD.RU, REGION[GEO.RU_MOSCOW]),
        (TLD.COM, REGION[GEO.USA]),
        (TLD.COMTR, REGION[GEO.COMTR_ISTANBUL])
    ])
    @TSoY.yield_test
    def test_search_smart(self, query, tld, region):
        query.SetPath(HNDL.SEARCH_SMART)
        query.SetDomain(tld)
        query.SetRegion(region)
        query.SetUserAgent(USER_AGENT.SMART)
        require_status = 200 if region != TLD.COM else 302
        query.SetRequireStatus(require_status)

        resp = yield query

        if require_status == 200:
            assert TEXT in resp.text
        else:
            assert resp.GetLocation().path == HNDL.SEARCH
            assert resp.GetLocation().scheme == 'https'

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("tld", [
        TLD.RU,
        # TLD.COMTR
    ])
    @TSoY.yield_test
    def test_search_site(self, query, tld):
        query.SetQueryType(PLATFORM.SITESEARCH)
        query.SetDomain(tld)
        query.SetRequireStatus(200)

        resp = yield query

        assert TEXT in resp.text

    @pytest.mark.parametrize(('tld'), [
        (TLD.RU),
        (TLD.KZ),
    ])
    @TSoY.yield_test
    def test_search_wizardsjson(self, query, tld):
        """
        SERP-35959 - ручка https://yandex.kz/search/wizardsjson отдаёт редирект при пустых куках
        Один из случаев - колдунщик цвета
        """
        query.SetDomain(tld)
        query.SetPath(HNDL.SEARCH_WIZARDSJSON)
        query.SetParams({
            'type': 'colors',
            'text': 'ff00ff'
        })
        query.SetNoAuth()
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert 'current' in js[0].keys()
        assert 'next' in js[0].keys()
        assert 'prev' in js[0].keys()
        assert js[0]['isHSV'] == 0
        assert js[0]['isHEX'] == 1
        assert js[0]['isRGB'] == 0
        assert 'value_rgb' in js[0]['current'].keys()

    @TSoY.yield_test
    def test_search_wizardsjson_postal_codes(self, query):
        """
        Postal codes response
        """
        query.SetPath(HNDL.SEARCH_WIZARDSJSON)
        query.SetParams({
            'type': 'postal_codes',
            'text': 'Россия, Москва, улица Перерва, 12',
            'geo': 213,
            'timeout': 999999,
            'nocache': 'da',
            'waitall': 999999
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        data_keys = js[0].keys()
        assert 'address' in data_keys
        assert 'source' in data_keys
        assert 'selected_region_ll' in data_keys
        assert 'selected_region_spn' in data_keys
        assert 'selected_region' in data_keys
        assert 'obj' in data_keys
        assert 'mode' in data_keys

    @pytest.mark.ticket('SERP-63685')
    @pytest.mark.ticket('SERP-78547')
    @pytest.mark.parametrize(('path'), [
        HNDL.SEARCH_RESULT,
        HNDL.SEARCH_RESULT_TOUCH
    ])
    @TSoY.yield_test
    def test_search_result_geov(self, query, path):
        """
        Раньше это был тест test_adresa_geolocation
        Потом по таску SERP-78547 ручку /adresa-geolocation перевели на /search/result?type=geov
        """
        query.SetPath(path)
        query.SetParams({
            'type': 'geo',
            'callback': 'jQuery18308019359641513826_1424963064584',
            'ajax': '{"points-request":{}}',
            'proto': 'map',
            'text': 'кафе',
            'lr': '213',
            'sll': '37.80687345507813,55.700171135082236',
            'sspn': '0.09441375732423296%2C0.029082679992470162',
            '_': '1424963077700',
        })
        query.SetAjax()

        resp = yield query
        js = resp.json()

        assert 'assets' in js.keys()
        assert 'serp' in js.keys()
        assert 'cnt' in js.keys()

    @pytest.mark.parametrize(("text", "wizard_type"), [
        ("погода в москве", "weather"),
        ("котики", "video")
    ])
    @TSoY.yield_test
    def test_search_result_handler(self, query, text, wizard_type):
        query.SetPath(HNDL.SEARCH_RESULT)
        query.SetParams({
            'text': text,
            'type': wizard_type,
            'format': 'json'
        })
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert 'error' not in js.keys()
        assert 'docs' in js.keys()

    @TSoY.yield_test
    def test_search_result_empty_response(self, query):
        """
        SEARCHPRODINCIDENTS-2827: [27.10.17] PROD-WEB / report:
        пятисотки изнанки после выкатки report-core в sas и man (SAS,MAN)
        """
        query.SetPath(HNDL.SEARCH_RESULT)
        query.SetParams({
            'text': 'sadfasdfasdfasdfasd',
            'type': 'geo',
            'format': 'json'
        })
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert 'error' in js.keys()
        assert js['error'] == 'No result'
        assert js['type'] == 'report'
        assert 'docs' not in js.keys()

    @pytest.mark.ticket('SERP-91039')
    @TSoY.yield_test
    def test_post_search_direct_preview_handler(self, query):
        query.SetPath(HNDL.SEARCH_DIRECTPREVIEW)
        query.SetMethod('POST')
        query.SetDomain(TLD.RU)
        query.ResetParams()

        adv_val = "{}{}{}{}{}{}{}{}{}{}{}{}{}{}{}{}{}{}".format(
            '%7B%22body%22%3A+%22%5Cu041f%5Cu0435%5Cu0440%5Cu0432%5Cu044b%5Cu043c+%5Cu043f%5Cu044f%5Cu0442%5Cu0438+%5Cu043f%5Cu043e%5Cu043a%5Cu0443',
            '%5Cu043f%5Cu0430%5Cu0442%5Cu0435%5Cu043b%5Cu044f%5Cu043c+-+%5Cu0441%5Cu043a%5Cu0438%5Cu0434%5Cu043a%5Cu0430.+%5Cu0417%5Cu0432%5Cu043e',
            '%5Cu043d%5Cu0438%5Cu0442%5Cu0435%21%22%2C+%22working_time%22%3A+%22%22%2C+%22domain%22%3A+%22www.gtool.ru%22%2C+%22vcard_url%22%3A+',
            '%22https%3A%2F%2Fdna.yandex.ru%3A3000%2Fregistered%2Fmain.pl%3Fcmd%3DshowContactInfo%26cid%3D12624197%26bid%3D875309997%22%2C+',
            '%22sitelinks%22%3A+%5B%7B%22url%22%3A+%22http%3A%2F%2Fwww.gtool.ru%2Fdostavka-i-oplata%2F%22%2C+%22text%22%3A+%22%22%2C+',
            '%22no_redirect_url%22%3A+%22http%3A%2F%2Fwww.gtool.ru%2Fdostavka-i-oplata%2F%22%2C+%22title%22%3A+%22%5Cu0414%5Cu043e',
            '%5Cu0441%5Cu0442%5Cu0430%5Cu0432%5Cu043a%5Cu0430+%5Cu0438+%5Cu043e%5Cu043f%5Cu043b%5Cu0430%5Cu0442%5Cu0430%22%7D%2C+',
            '%7B%22url%22%3A+%22http%3A%2F%2Fwww.gtool.ru%2Fcatalog%2F%22%2C+%22text%22%3A+%22%22%2C+%22no_redirect_url%22%3A+%22http%3A%2F%2Fwww.gtool.ru',
            '%2Fcatalog%2F%22%2C+%22title%22%3A+%22%5Cu041a%5Cu0430%5Cu0442%5Cu0430%5Cu043b%5Cu043e%5Cu0433+%5Cu043f%5Cu0440%5Cu043e%5Cu0434%5Cu0443%5Cu043a',
            '%5Cu0446%5Cu0438%5Cu0438%22%7D%2C+%7B%22url%22%3A+%22http%3A%2F%2Fwww.gtool.ru%2Fcontact%2F%22%2C+%22text%22%3A+%22%22%2C+%22no_redirect_url%22',
            '%3A+%22http%3A%2F%2Fwww.gtool.ru%2Fcontact%2F%22%2C+%22title%22%3A+%22%5Cu041a%5Cu043e%5Cu043d%5Cu0442%5Cu0430%5Cu043a%5Cu0442%5Cu044b%22%7D%2C+',
            '%7B%22url%22%3A+%22http%3A%2F%2Fwww.gtool.ru%2Frepair%2F%22%2C+%22text%22%3A+%22%22%2C+%22no_redirect_url%22%3A+%22http%3A%2F%2Fwww.gtool.ru%2Frepair',
            '%2F%22%2C+%22title%22%3A+%22%5Cu0421%5Cu0435%5Cu0440%5Cu0432%5Cu0438%5Cu0441%22%7D%5D%2C+%22subtype%22%3A+%22%22%2C+%22url%22%3A+%22http%3A%2F',
            '%2Fwww.gtool.ru%2Fcatalog%2Fborfrezy%2F%22%2C+%22title%22%3A+%2222+%5Cu2013+%5Cu0411%5Cu043e%5Cu0440%5Cu0444%5Cu0440%5Cu0435%5Cu0437%5Cu044b+',
            '%5Cu0432+%5Cu043d%5Cu0430%5Cu043b%5Cu0438%5Cu0447%5Cu0438%5Cu0438%22%2C+%22age%22%3A+%2218%2B%22%2C+%22search_prefs%22%3A+%22%22%2C+%22add_info',
            '%22%3A+%7B%22callouts_list%22%3A+%5B%5D%2C+%22type%22%3A+%22callouts%22%7D%2C+%22phone%22%3A+%22%22%2C+%22warning%22%3A+%22%22%2C+%22fav_domain',
            '%22%3A+%22www.gtool.ru%22%2C+%22position%22%3A+%22%22%2C+%22region%22%3A+%22%22%2C+%22path%22%3A+%22www.gtool.ru%2F%22%2C+%22path_url%22%3A+%22',
            'http%3A%2F%2Fwww.gtool.ru%2Fcatalog%2Fborfrezy%2F%22%2C+%22bid%22%3A+%22875309997%22%7D'
        )
        query.SetData({
            'format': 'json',
            'text': '1',
            'type': 'yabs_proxy',
            'adv': unquote_plus(adv_val)
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert "error" not in js
        assert "docs" in js
        assert "sitelinks" in js["docs"][0]
        assert js["docs"][0]["sitelinks"] == json.loads(unquote_plus(adv_val))["sitelinks"]

    @TSoY.yield_test
    def test_search_direct_preview_handler(self, query):
        query.SetPath(HNDL.SEARCH_DIRECTPREVIEW)
        query.SetParams({
            'lr': '213',
            'adv': '{"domain":"ford-favorit.ru","subway_station":"","call_url":"http://yabs.yandex.ru/count/","warning":"","sitelinks":[{"url":"http://yabs.yandex.ru/count/","no_redirect_url":"http://ford-favorit.ru/","title":"1"},{"url":"http://yabs.yandex.ru/count/","no_redirect_url":"http://ford-favorit.ru/","title":"2"},{"url":"http://yabs.yandex.ru/count/","no_redirect_url":"http://ford-favorit.ru/","title":"3"},{"url":"","no_redirect_url":"","title":""}],"ratings":{"market_rating":"0"},"no_redirect_url":"http://ford-favorit.ru/","title":"title","working_time":"пн-вс 9:00-22:00","body":"body","vcard_url":"http://yabs.yandex.ru/count/","bid":"319763563","phone":" 7 (495) 786-25-25","block_favicon":"0","order_type":"1","url":"http://yabs.yandex.ru/count/","region":"Москва","search_prefs":{},"fav_domain":"ford-favorit.ru","debug":"","age":""}',  # noqa
            'text': '1',
            'type': 'yabs_proxy',
            'format': 'json'
        })
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert "error" not in js
        assert "docs" in js
        assert "sitelinks" in js["docs"][0]
        assert len(js["docs"][0]["sitelinks"]) == 4

    @TSoY.yield_test
    def test_search_direct_preview_touch_handler(self, query):
        query.SetPath(HNDL.SEARCH_DIRECTPREVIEW_TOUCH)
        query.SetParams({
            'lr': '213',
            'adv': '{"test":"1"}',
            'text': '1',
            'type': 'yabs_proxy',
            'format': 'json'
        })
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert "error" not in js
        assert "docs" in js
        assert "test" in js["docs"][0]

    @pytest.mark.ticket('RUNTIMETESTS-120')
    @TSoY.yield_test
    def test_search_direct_preview_touch_device(self, query):
        query.SetDumpFilter(resp=[CTXS.INIT_HANDLER])
        query.SetPath(HNDL.SEARCH_DIRECTPREVIEW_TOUCH)
        query.SetParams({
            'lr': '213',
            'adv': '{"test":"1"}',
            'text': '1',
            'type': 'yabs_proxy',
            'format': 'json'
        })
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert "touch" == ctxs['device_config'][-1]['device']
        assert "direct-preview:phone" == ctxs['device_config'][-1]['template_name']

    @TSoY.yield_test
    def test_search_direct_preview_pad_handler(self, query):
        query.SetPath(HNDL.SEARCH_DIRECTPREVIEW_PAD)
        query.SetParams({
            'lr': '213',
            'adv': '{"test":"1"}',
            'text': '1',
            'type': 'yabs_proxy',
            'format': 'json'
        })
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert "error" not in js
        assert "docs" in js
        assert "test" in js["docs"][0]

    @TSoY.yield_test
    def test_search_wizardsjson_color_general_hsv(self, query):
        query.SetDomain(TLD.RU)
        query.SetPath(HNDL.SEARCH_WIZARDSJSON)
        query.SetParams({
            'type': 'colors',
            'text': '336 16 22',
            'is_hsv': '1',
            'user_input': '1',
            'layoutLang': 'ru'
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert 'current' in js[0].keys()
        assert 'next' in js[0].keys()
        assert 'prev' in js[0].keys()
        assert js[0]['isHSV'] == 1
        assert js[0]['isHEX'] == 0
        assert js[0]['isRGB'] == 0

    @TSoY.yield_test
    def test_search_wizardsjson_color_general_hex(self, query):
        query.SetDomain(TLD.RU)
        query.SetPath(HNDL.SEARCH_WIZARDSJSON)
        query.SetParams({
            'type': 'colors',
            'text': '123456',
            'user_input': '1',
            'layoutLang': 'ru'
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert 'current' in js[0].keys()
        assert 'next' in js[0].keys()
        assert 'prev' in js[0].keys()
        assert js[0]['isHSV'] == 0
        assert js[0]['isHEX'] == 1
        assert js[0]['isRGB'] == 0

    @TSoY.yield_test
    def test_search_wizardsjson_color_segment_next(self, query):
        query.SetDomain(TLD.RU)
        query.SetPath(HNDL.SEARCH_WIZARDSJSON)
        query.SetParams({
            'type': 'colors',
            'text': '905d5d',
            'segment': 'next',
            'layoutLang': 'ru'
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert 'segment' in js.keys()
        assert 'data' in js.keys()

    @TSoY.yield_test
    def test_search_wizardsjson_color_segment_prev(self, query):
        query.SetDomain(TLD.RU)
        query.SetPath(HNDL.SEARCH_WIZARDSJSON)
        query.SetParams({
            'type': 'colors',
            'text': 'c34d0a',
            'segment': 'prev',
            'layoutLang': 'ru'
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert 'segment' in js.keys()
        assert 'data' in js.keys()

    @TSoY.yield_test
    def test_search_wizardsjson_autoregions_city(self, query):
        query.SetDomain(TLD.RU)
        query.SetPath(HNDL.SEARCH_WIZARDSJSON)
        query.SetParams({
            'type': 'suggestfacts2',
            'text': 'Барнаул',
            'subtype': 'autoregions',
            'timeout': 999999,
            'nocache': 'da',
            'waitall': 999999
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        for key in ("norm_query", "question", "text", "type", "voiceInfo"):
            assert key in js[0].keys()

    @TSoY.yield_test
    def test_search_wizardsjson_autoregions_region(self, query):
        query.SetDomain(TLD.RU)
        query.SetPath(HNDL.SEARCH_WIZARDSJSON)
        query.SetParams({
            'type': 'suggestfacts2',
            'text': 'Новосибирская область',
            'subtype': 'autoregions',
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        for key in ("norm_query", "question", "text", "type", "voiceInfo"):
            assert key in js[0].keys()

    @TSoY.yield_test
    def test_search_wizardsjson_autoregions_number(self, query):
        query.SetDomain(TLD.RU)
        query.SetPath(HNDL.SEARCH_WIZARDSJSON)
        query.SetParams({
            'type': 'suggestfacts2',
            'text': '77',
            'subtype': 'autoregions',
            'timeout': 999999,
            'nocache': 'da',
            'waitall': 999999
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        for key in ("norm_query", "question", "text", "type", "voiceInfo"):
            assert key in js[0].keys()

    # new cuurency converter. old /search/wizardsjson
    @pytest.mark.ticket('WZRD-2266')
    @TSoY.yield_test
    def test_search_result_currency_converter(self, query):
        query.SetDomain(TLD.RU)
        query.SetPath(HNDL.SEARCH_RESULT)
        query.SetParams({
            'text': 'eur 1 в 1',
            'type': 'currency_converter',
            "currency_data": "eyJGcm9tIjoiZXVyIiwiVG8iOiJydWIiLCJBbW91bnQiOiIxIn0=",
            "format": "json"
        })
        query.SetRequireStatus(200)

        resp = yield query
        js = resp.json()

        assert "docs" in js and len(js["docs"]) and "data" in js["docs"][0] and "charts" in js["docs"][0]["data"]
        for key in ("allTime", "month", "year"):
            assert key in js["docs"][0]["data"]["charts"]
            assert len(js["docs"][0]["data"]["charts"][key])
