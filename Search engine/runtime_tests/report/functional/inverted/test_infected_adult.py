# -*- coding: utf-8 -*-

import pytest
import json
import os

from report.const import *
from report.functional.web.base import BaseFuncTest
from runtime_tests.util.predef.handler.server.http import SimpleConfig
from runtime_tests.util.predef.http.response import raw_ok
from runtime_tests.util.predef.handler.server.http import ThreeModeHandler, ThreeModeConfig


DEPRICATED_INFECTED_DATA = """url=http://wmconvirus.narod.ru\r\nfilter=1100008	4000003	4001487	4003043	21000225	30000001	30000008	30000018	30003579	33000002	34000003	34000004	42000011	42000368	71000225\r\ninfected=2015-07-12 19:33:48	sophos_yamo	Yndx/MegaVirus	2015-04-10 00:29:00		;	2015-07-14 12:37:38	sophos	Yndx/MegaVirus	2013-09-09 14:10:18		;	2015-07-11 21:25:42	sophos_yamo	Yndx/MegaVirus	2013-08-23 15:02:24\r\n\r\n"""
INFECTED_SNIPPET_DATA="""url=http://pikachumobile.com\r\nfilter=\r\ninfected=2015-09-18 21:22:25	virustotal_avr	Yandex/MalAndroid	2015-02-18 15:22:13	;	2015-09-05 15:24:37	virustotal_bdr	Yandex/MalAndroid	2015-02-15 12:16:44	;	2015-09-18 21:22:23	sophos	Andr/VietSms-H	2015-02-18 15:22:04\r\n\r\n"""


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestInfectedAdultRedirects(BaseFuncTest):
    def base_infected_adult(self, query):
        return self.request(query, sources=[('SAFE_BROWSING', 'adult'), ('RESINFOD', DEPRICATED_INFECTED_DATA)])

    def base_test_infected(self, query, result):
        query.set_url('/infected')
        query.set_params({'url': 'sexetc.org', 'fmode': 'inject', 'infectedalert': 'yes', 'domredir': '1', 'mime': 'html' })

        resp = self.request(query, require_status=302)

        assert 'domredir' not in resp.headers['location'][0]
        assert 'yandex.' + result in resp.headers['location'][0]

        query.path.set(resp.headers['location'][0])
        query.set_host(result)

        self.base_infected_adult(query)


    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize(("tld", "ip", "tune", "result"), [
    #Логика для всех стран из КУБ по отношению к RU одинакова
    #Мы не можем зайти на RU из КУБ

    #редиректим сами на себя
    (RU,    RU_VLADIVOSTOK, RU_VLADIVOSTOK, RU),
    (COM,   W_SEATTLE,      W_SEATTLE,      COM),

    #редиректим с себя не на себя
    #в идеале все должны приходить на com, так что проверяем его
    (COM,    RU_VLADIVOSTOK, None,         RU),
    (COM,    KZ_KARAGANDA,   None,         KZ),
    (COM,    COMTR_IZMIR,    COMTR_IZMIR,  COMTR),

    #на всякий случай - кейс без com
    (RU,     W_SEATTLE,      None,         COM),

    #кейс с настройкой отличающейся от IP
    (COM,    COMTR_IZMIR,    BY_GOMEL,     BY),

    #проверяем на всякий случай
    (COM,    SIMFEROPOL,     SIMFEROPOL,   RU),
    ])
    def test_adult(self, query, tld, ip, tune, result):
        if ip:
            query.headers.set_forward_for_y(IP[ip])
            query.set_region(REGION[ip])

        if tune:
            query.headers.cookie.set_yandex_gid(REGION[tune])
            query.set_region(REGION[tune])

        query.set_host(tld)
        self.base_test_infected(query, result)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_adult_cr_ua_ru(self, query):
        """
        Проверяем крымскую куку.
        Сначала нас редиректит на RU, так как пришли из Симферополя, кука выставленна в RU, значит только один редирект
        """
        query.set_url('/adult')
        query.set_params({'url': 'sexetc.org', 'fmode': 'inject', 'domredir': '1', 'mime': 'html'})
        query.set_host(UA)
        query.headers.set_forward_for_y(IP[SIMFEROPOL])
        query.headers.cookie.set_yandex_gid(REGION[SIMFEROPOL])
        query.headers.cookie.yp.set_cr(RU.lower())

        resp = self.request(query, require_status=302)

        location = resp.headers['location'][0].split("/")
        url = "/" + location[3]
        host = location[2]

        assert 'domredir' not in url
        assert 'yandex.ru' == host

        query.path.set(url)
        query.set_host(RU)

        self.base_infected_adult(query)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_adult_cr_ru_ua(self, query):
        """
        Проверяем крымскую куку.
        Сначала нас редиректит на RU, так как пришли из Симферополя, кука выставленна в UA, поэтому потом нас редиректит на UA
        Итого - 2 редиректа
        """
        query.set_url('/adult')
        query.set_params({'url': 'sexetc.org', 'fmode': 'inject', 'domredir': '1', 'mime': 'html'})
        query.set_host(UA)
        query.headers.set_forward_for_y(IP[SIMFEROPOL])
        query.headers.cookie.set_yandex_gid(REGION[SIMFEROPOL])
        query.headers.cookie.yp.set_cr(UA.lower())

        resp = self.request(query, require_status=302)

        location = resp.headers['location'][0].split("/")
        url = "/" + location[3]
        host = location[2]

        assert 'domredir' not in url
        assert 'yandex.ru' == host

        query.path.set(url)
        query.set_host(RU)

        resp = self.request(query, require_status=302)

        location = resp.headers['location'][0].split("/")
        url = "/" + location[3]
        host = location[2]

        assert 'yandex.ua' == host

        query.path.set(url)
        query.set_host(UA)

        self.base_infected_adult(query)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize(("host", "region"), [
        (RU, REGION[RU_MOSCOW]),
        (COM, REGION[USA]),
        (COMTR, REGION[COMTR_ISTANBUL])
    ])
    def test_search_infected_error(self, query, host, region):
        """
        без параметров - получаем ошибку
        """
        query.set_url("/search/infected")
        query.set_host(host)
        query.set_region(region)

        resp = self.request(query, require_status=None)

        if region == COM:
            assert resp.status == 302
            assert resp.headers['location'][0].startswith('https://yandex.ru/search/')
        else:
            assert resp.status == 404

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize(("tld", "region"), [
        (RU, REGION[RU_SAINT_PETESBURG]),
        (COM, REGION[USA]),
        (COMTR, REGION[COMTR_IZMIR])
    ])
    def test_search_adult_error(self, query, tld, region):
        """
        без параметров - получаем ошибку
        """
        query.set_url("/search/adult")
        query.set_host(tld)
        query.set_region(region)

        resp = self.request(query, require_status=None)

        if region == COM:
            assert resp.status == 302
            assert resp.headers['location'][0].startswith('https://yandex.ru/search/adult?')
        else:
            assert resp.status == 404

INFECTED = """url=http://baby-example.asia\r\ninfected=2016-03-10 10:12:31	phishing	phishing	2014-12-23 14:11:37\r\n\r\n"""
INFECTED_DATA ={u'set_info_url': None, u'name': u'phishing', u'providers': [{u'date': u'2016-03-10 10:12:31', u'type_for_counter': u'other', u'name': u'phishing', u'provider': u'phishing'}], u'provider': u'phishing', u'date': u'2016-03-10 10:12:31', u'type_for_counter': u'other'}
INFECTED_ALERT = """url=http://wmconvirus.narod.ru\r\ninfected=2015-10-02 05:35:46	sophos	Yndx/MegaVirus	2013-09-09 14:10:18	;	2015-09-30 03:21:21	sophos_yava	Yndx/MegaVirus	2014-03-19 06:42:58	;	2015-09-30 07:04:28	sophos_yamo	Yndx/MegaVirus	2015-04-10 00:29:00\r\n\r\n"""
INFECTED_ALERT_DATA = {u'set_info_url': None, u'name': u'Yndx/MegaVirus', u'providers': [{u'date': u'2015-10-02 05:35:46', u'type_for_counter': u'other', u'name': u'Yndx/MegaVirus', u'provider': u'sophos'}, {u'date': u'2015-09-30 03:21:21', u'type_for_counter': u'other', u'name': u'Yndx/MegaVirus', u'provider': u'sophos_yava'}, {u'date': u'2015-09-30 07:04:28', u'type_for_counter': u'other', u'name': u'Yndx/MegaVirus', u'provider': u'sophos_yamo'}], u'provider': u'sophos', u'date': u'2015-10-02 05:35:46', u'type_for_counter': u'other'}

ResinfodOK_1 = """url=https://ya.ru\r\nfilter=2001039	3000017	4000020	4000091	4000565	4000573	4000596	4000936	4001025	4001345	4001456	4001463	4001514	4001562	4001598	4001684	4001813	4001871	4001910	4001994	4002034	4002214	4002229	4002281	4002374	4002574	4002582	4002626	4002628	4002700	4002707	4003208	4003812	4003819	9000289	9003804	9011887	9011938	11000029	12000029	13000001	14000029	15000029	16000002	17000029	18000029	19000029	20000003	21000382	21000958	22000004	22000099	24000289	24003804	24011887	24011938	25000000	26000029	29000029	30000001	30000002	30000003	30000004	30000005	30000006	30000007	30000008	30000009	30000018	30000019	30000020	30000028	30000037	30000039	30000043	30000049	30000066	30000072	30000088	30000090	30000095	30000096	30000099	30000102	30000105	30000111	30000199	30000233	30000245	30000247	30000263	30000289	30000311	30000333	30000398	30000437	30000460	30000519	30000552	30000631	30000668	30000671	30000679	30000682	30000691	30002865	30002866	30003202	30003281	30003349	30003579	30003804	30011887	30011938	33000002	33000003	33000011	33000013	33000029	34000001	34000003	34000004	34000005	35000001	35000003	35000004	35000029	36000001	36000002	36000003	36000029	37000002	38000003	38000029	39000001	39000002	39000029	40000001	40000029	41000001	41000002	41000003	42000003	42000004	42000008	42000009	42000011	42000013	42000146	42000165	42000179	42000190	42000207	42000274	42000292	42000368	42000385	42000444	42000449	43001632	49003804	49011887	49011938	51000382	51000958	59000029	61000029	71000001	71000002	71000007	71000038	71000047	71000050	71000076	71000142	71000213	71000225	71010926	71011212	71011318	81000006	81000213	99000029\r\ninfected=\r\n\r\n"""
ResinfodOK_2 = """url=http://ya.ru\r\ninfected=\r\n\r\n"""

class ResinfodOkHandler(ThreeModeHandler):
    def handle_prefix(self, raw_request, stream):
        resp = raw_ok(headers=[('Content-Type', 'text/plain'), ('content-length', len(ResinfodOK_1)),],
                      data=ResinfodOK_1)
        stream.write_response(resp)
        self.finish_response()

    def handle_first(self, raw_request, stream):
        resp = raw_ok(headers=[('Content-Type', 'text/plain'), ('content-length', len(ResinfodOK_2)),],
                      data=ResinfodOK_2)
        stream.write_response(resp)
        self.finish_response()

class ResinfodOkConfig(ThreeModeConfig):
    HANDLER_TYPE = ResinfodOkHandler


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestInfectedAdult(BaseFuncTest):
    @pytest.mark.parametrize("tld", [
        RU,
        COM,
        COMTR
    ])
    @pytest.mark.parametrize(('params', 'filename', 'backend_cfg'), [
        ({"lr": "213", "infectedalert": "yes", "url": "wmconvirus.narod.ru"}, "infected_infected_alert.json", 'alert'),
        ({"lr": "213", "url": "baby-example.asia"}, "infected.json", 'common'),
    ])
    @pytest.mark.unstable
    def test_infected_serp(self, query, tld, params, custom_schema_path, filename, backend_cfg):
        BACKEND_CFG={
            'alert': (INFECTED_ALERT, INFECTED_ALERT_DATA),
            'common': (INFECTED, INFECTED_DATA),
        }
        backend_data, resp_data = BACKEND_CFG[backend_cfg]

        query.set_url('/infected')
        if tld == COM:
            del params["lr"];
            params["lr"] = '87';
        query.set_params(params)
        query.set_host(tld)

        resp = self.json_request(query, source=('RESINFOD', backend_data))
        resp.validate_schema(custom_schema_path(filename))

        data = resp.data
        assert data["infected"] == resp_data
        assert data["url"] == 'http://' + params["url"] + '/'

    @pytest.mark.parametrize("tld", [
        RU,
        COM,
        COMTR
    ])
    @pytest.mark.unstable
    def test_infected_serp_no(self, query, tld, custom_schema_path):
        params = {"lr": REGION_BY_TLD[tld], "url":"ya.ru"}
        query.set_url('/infected')
        query.set_params(params)
        query.set_host(tld)

        resinfod = self.start_source(ResinfodOkConfig(prefix=1, first=10, second=0, response=raw_ok()), port=GenerateBackendPort(RESINFOD))

        resp = self.json_request(query)
        data = resp.data
        resp.validate_schema(custom_schema_path("infected_no_ya_ru.json"))
        assert not data["infected"]
        assert data["url"] == 'http://' + params["url"] + '/'
        assert data["template"]["filename"] == "infected/pages-desktop/infected/_infected.priv.js"


    @pytest.mark.parametrize(("tld", "region"), [
        (RU, REGION[RU_MOSCOW]),
        (COM, REGION[USA]),
        (COMTR, REGION[COMTR_ISTANBUL])
    ])
    @pytest.mark.parametrize("url", ("/adult", "/adult/"))
    def test_adult_serp(self, query, tld, region, custom_schema_path, url):
        params = {'lr': REGION_BY_TLD[tld], 'url': 'porno.com'}
        query.set_url(url)
        query.set_params(params)
        query.set_host(tld)
        query.set_region(region)

        DATA = 'adult'
        safe_browsing = self.start_source(SimpleConfig(response=raw_ok(headers=[('Content-Type', 'text/plain'),
                                                                ( 'X-Content-Type-Options', 'nosniff' ),
                                                                ( 'X-XSS-Protection', '1; mode=block'),
                                                                ('content-length', len(DATA)),
                                                                ('connection', 'close')
                                                              ], data=DATA)), port=GenerateBackendPort(SAFE_BROWSING))

        resp = self.json_request(query)
        data = resp.data
        resp.validate_schema(custom_schema_path("adult.json"))

        assert data["adult"]
        assert data["url"] == 'http://' + params["url"] + '/'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_adult_serp_no(self, query):
        # если сайт не "взрослый" то ручка возвращает 404
        query.set_url("/adult")
        query.set_params({'url': 'ya.ru'})
        resp = self.request(query, require_status=404)
        assert resp.headers['X-Yandex-ReqId']

    @pytest.mark.unstable
    def test_infected_snippet(self, query, schema_path):

        resinfod = self.start_source(SimpleConfig(response=raw_ok(headers=[('Content-Type', 'text/plain'),
                                                                        ('content-length', len(INFECTED_SNIPPET_DATA)),
                                                                      ], data=INFECTED_SNIPPET_DATA)), port=GenerateBackendPort(RESINFOD))

        query.set_url('/search/')
        query.set_params({"text": 'pikachumobile.com'})

        resp = self.json_test(query)
        infected_snippet = resp.data["searchdata"]["docs"][0]["snippets"]["full"]

        assert resinfod.state.accepted.value

        self.validate_json_data(infected_snippet, schema_path)
