# -*- coding: utf-8 -*-

import os
import pytest
from copy import deepcopy
import json
import zlib
import base64

from jsonschema import ValidationError
from report.functional.web.base import BaseFuncTest
from runtime_tests.util.predef.handler.server.http import SimpleConfig, SimpleDelayedConfig
from runtime_tests.util.predef.http.response import raw_ok, service_unavailable
from report.const import *
from runtime_tests.report.base import ReportBackendResponse

@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestTemplates(BaseFuncTest):
    def set_app_host(self, query, app_host):
        skip = 0 if app_host else 1
        in_app_host = 1 if app_host else 0
        query.add_flags({
            'app_host_srcskip': { 'PRE_SEARCH_TEMPLATE_DATA': skip, 'POST_SEARCH_TEMPLATE_DATA': skip },
            'output_in_app_host': in_app_host
        })

    def test_serp(self, query):
        query.set_url('/search/')
        self.json_test(query)

    def test_blogs_serp(self, query):
        query.set_url('/blogs/search')
        self.json_test(query)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-46072')
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_serp_unanswer(self, query):
        """
        Проверяем репорт по json-схеме с пустой выдачей
        """
        query.add_flag({'srcrwr': {'APP_HOST': 'localhost:1'}})
        resp = self.json_test(query)
        data = resp.data
        assert data['searchdata']['err_text']
        assert data['searchdata']['err_code'] in [15, 21]

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(('is_external', 'content'), (
        (False, '{'),
        (True, '<!DOCTYPE html>'),
    ))
    def test_export_external(self, query, is_external, content):
        """
        Проверяем недоступность параметра export=json из внешней сети
        """
        query.set_external(is_external)
        query.set_params({'export': 'json'})
        resp = self.request(query)
        assert resp.content.startswith(content)

    def test_broken_yp(self, query):
        SIZE_sz = '%231502456097%2Est_browser_s%2E20'
        query.headers.cookie.yp.set_sz(SIZE_sz)
        resp = self.json_request(query)
        assert resp.data['reqdata']['ycookie']['yp']['sz'] == SIZE_sz

    def test_cookies_empty(self, query):
        """
        Проверяем репорт по json-схеме с пустыми куками
        """
        query.headers.set_raw_cookie('')
        resp = self.json_test(query)
        assert resp.data['reqdata']['cookie'] == {}

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_cookies_all(self, query):
        """
        Проверяем репорт по json-схеме с всеми возможными куками
        """
        query.headers.set_raw_cookie('valid=valid; привет=привет; Cookie_check=1; ' +
            'yandex_gid=2; yandexuid=3; yandex_login=4; i-m-not-a-hacker=5; ' +
            'fyandex=6; Session_id=7; searchuid=8; fuid01=9; fuid888=10; ' +
            'my=11; yp=12; ys=13; ud=15; b=16; z=18_18; L=17; z=18; ' +
            'mda=19')

        resp = self.json_test(
            query,
            sources=[
                (
                    'APP_HOST',
                    json.dumps([{
                            "meta": {},
                            "name": "BLACKBOX",
                            "results": [
                                {
                                    'age': 2,
                                    'auth': {
                                        'allow_plain_text': True,
                                        'have_password': True,
                                        'partner_pdd_token': False,
                                        'password_verification_age': 2,
                                        'secure': True,
                                    },
                                    'connection_id': 's:1490115741950:mtcRMn_YaogIBAAAuAYCKg:52',
                                    'dbfields': {
                                        'subscription.suid.668': '',
                                        'subscription.suid.669': '',
                                    },
                                    'display_name': {
                                        'avatar': {
                                            'default': '0/0-0',
                                            'empty': True,
                                        },
                                        'name': 'ezhi',
                                    },
                                    'error': 'OK',
                                    'expires_in': 7775998,
                                    'have_hint': True,
                                    'have_password': True,
                                    'karma': {'value': 0, },
                                    'karma_status': {'value': 6000, },
                                    'login': 'ezhi',
                                    'phones': [
                                        {
                                            'attributes': {'102': '+79260000000', },
                                            'id': 10000000,
                                        },
                                    ],
                                    'regname': 'ezhi',
                                    'session_fraud': 0,
                                    'status': {
                                        'id': '0',
                                        'value': 'VALID',
                                    },
                                    'ttl': 5,
                                    'type': 'blackbox',
                                    'uid': {
                                        'hosted': False,
                                        'lite': False,
                                        'value': '12345',
                                    },
                                },
                            ]
                    }])
                ),
            ]
        )

        assert resp.data['reqdata']['cookie'] == {'valid': 'valid', 'Cookie_check': '1',
            'yandex_gid': '2', 'yandexuid': '3', 'yandex_login': '4', 'i-m-not-a-hacker': '5',
            'fyandex': '6', 'Session_id': '7', 'searchuid': '8', 'fuid01': '9', 'fuid888': '10',
            'my': '11', 'yp': '12', 'ys': '13', 'ud': '15', 'b': ['16'], 'L': '17',
            'z': ['18_18','18'], 'mda': '19'}

    @pytest.mark.skip(reason="RUNTIMETESTS-114")
    @pytest.mark.parametrize(('i18n'), (
        'ru', 'uk', 'kk', 'tt',
    ))
    def test_lang_switcher(self, query, i18n):
        query.set_host(UA)
        query.set_region(REGION_BY_TLD[RU])
        query.headers.cookie.set_my(COOKIEMY_LANG[i18n])
        query.add_params({'lr': REGION_BY_TLD[UA]})
        query.headers.set_custom_headers({'X-Laas-Answered': 1})
        resp = self.json_test(query)
        data = resp.data
        assert data['langSwitcher']
        assert data['langSwitcher']['current']['code'] == i18n

    def test_lang_switcher_empty(self, query):
        resp = self.json_test(query)
        assert not resp.data['langSwitcher']

    def test_search_doc_with_mime(self, query, schema_path):
        query.replace_params({'text': 'test mime:pdf site:ahmetakcay.com'})
        query.set_query_type(DESKTOP)
        resp = self.json_test(query)
        with open(schema_path) as f:
            schema = validate_json(f.read())
            if resp.data['searchdata']['docs']:
                for doc in resp.data['searchdata']['docs']:
                    if doc['snippets'].get('main', {}).get('type') == 'generic':
                        try:
                            if self.validate_json_scheme(doc, deepcopy(schema)):
                                return
                        except ValidationError as e:
                            print e
                pytest.fail('No mime=pdf snippets found')
            else:
                pytest.skip("No search docs at all")

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_rdat_app_version(self, query):
        """
        SERP-36011 - Не могу найти cgidata в выдаче с флагом export=json
        Смотрим, что параметр присутствует
        SERP-35756 - Прокидывать до верстки в rdat параметр и значение app_version
        Проверяем, что параметр должен приходит в cgidata
        """
        query.add_params({'app_version': '317'})
        query.set_query_type(PAD)

        resp = self.json_test(query)

        assert resp.data['cgidata']['args']['app_version'][0] == '317'

    @pytest.mark.ticket('SERP-37057')
    def test_header_tablet(self, query):
        """
        SERP-37057 - Добавить новый флаг appsearch_header_tablet=1 для планшетных приложений я.поиск
        В случае наличия хедера X-Yandex-Flags: appsearch-header-tablet=1 пробрасываем на выдачу appsearch-header-tablet=1
        """
        query.headers.set_custom_headers({'X-Yandex-Flags': 'appsearch-header-tablet=1'})

        resp = self.json_test(query)

        assert resp.data['reqdata']['flags']['appsearch_header_tablet'] == 1

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-37191')
    def test_header_tablet_for_apad(self, query):
        """
        SERP-37191 - Для UA планшетного приложения ЯндексПоиск включать флаг в rdat appsearch_header_tablet=1
        для юзер-агентов apad не редиректить на тачи и выставлять appsearch_header_tablet=1
        """
        query.set_query_type(PAD)
        query.set_user_agent(USER_AGENT_PAD_YANDEX)

        resp = self.json_test(query)

        assert resp.data['reqdata']['flags']['appsearch_header_tablet'] == 1

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-37191')
    def test_header_tablet_for_apad_no(self, query):
        """
        SERP-37191 - Для UA планшетного приложения ЯндексПоиск включать флаг в rdat appsearch_header_tablet=1
        для юзер-агентов apad не редиректить на тачи и выставлять appsearch_header_tablet=1
        """
        query.set_query_type(PAD)
        query.set_user_agent(USER_AGENT_PAD_ANDROID_4_3)

        resp = self.json_test(query)

        assert 'appsearch_header_tablet' not in resp.data['reqdata']['flags']

    def test_allowed_params(self, query):
        """
        Тестируем что репорт фильтрует параметры по белому списку
        """
        ALLOWED_PARAMS = ['brorich', 'broload']

        query.set_params(dict([ (x, x) for x in ALLOWED_PARAMS + ['__garbage_param__']]))
        resp = self.json_test(query)
        args = resp.data['cgidata']['args']

        assert '__garbage_param__' not in args

        for p in ALLOWED_PARAMS:
            assert p in args
            assert len(args[p]) == 1
            assert args[p][0] == p

    @pytest.mark.ticket('SERP-46712')
    def test_internal_flags(self, query):
        """
        SERP-46712 - Заголовок для добавления внутренних параметров в запросы
        """
        query.headers.set_custom_headers({'X-Yandex-Internal-Flags': base64.b64encode(b'{"tmplrwr":{"qqq":112}}')})
        resp = self.json_test(query)
        assert resp.data['reqdata']['flags']['tmplrwr'] == {'qqq': 112}, "flag set from X-Yandex-Internal-Flags header"

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-40963')
    def test_touch_ucbrowser_ru(self, query):
        """
        SERP-40963 - перенос touchsearch на перловый репорт
        """
        query.set_query_type(TOUCH)
        query.set_user_agent(USER_AGENT_UCBROWSER)

        resp = self.json_test(query)

        assert resp.data['reqdata']['flags']['direct_page'] == 3895, \
            "direct_page flag is not set to 3895 for UCBrowser/RU"

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-40963')
    def test_touch_ucbrowser_comtr(self, query):
        """
        SERP-40963 - перенос touchsearch на перловый репорт
        """
        query.set_query_type(TOUCH)
        query.set_user_agent(USER_AGENT_UCBROWSER)
        query.set_host(COMTR)
        query.add_params({'lr': 11508})

        resp = self.json_test(query)

        assert 'direct_page' not in resp.data['reqdata']['flags'], \
            "direct_page flag is set for UCBrowser/COMTR"

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_touch_no_geo_head(self, query):
        """
        SERP-42396 Отключить геошапку на тачах ("Показаны результаты для Москвы")
        """
        query.set_query_type(TOUCH)
        query.add_params({'lr': 21})

        resp = self.json_test(query)

        assert resp.data['localization']['top'] == 0


    @pytest.mark.parametrize("can_app_host, query_type, url_path, prms, template", [
        ######################
        ## НЕ ПОД АППХОСТОМ ##
        ######################
        ## (0, DESKTOP, '/search/pre',                { "tmplrwr": "web4:suggest" }, 'suggest:desktop'),
        ## (0, PAD,     '/search/pad/pre',            { "tmplrwr": "web4:web_exp" }, 'web_exp:pad'),
        ## (0, TOUCH,   '/search/touch/pre',          { "tmplrwr": "web4:granny" },  'granny:phone'),

        ## (0, DESKTOP, '/turbo',                     {  }, 'turbo:desktop'),
        ## (0, TOUCH,   '/turbo',                     {  }, 'turbo:phone'),

        ## (0, DESKTOP, '/chat',                      {  }, 'chat:phone'),
        ## (0, TOUCH,   '/chat',                      {  }, 'chat:phone'),
        ## (0, DESKTOP, '/search/pre',                { }, 'web4:desktop'),
        ## (0, PAD,     '/search/pad/pre',            { }, 'web4:pad'),
        ## (0, TOUCH,   '/search/touch/pre',          { }, 'web4:phone'),
        ## (0,GRANNY,  '/search/pre',                { }, 'granny:desktop'),

        ## (0, DESKTOP, '/search/site/',              { }, 'sitesearch_search:desktop'),
        ## (0, PAD,     '/search/site/',              { }, 'sitesearch_search:desktop'),
        ## (0, TOUCH,   '/search/site/',              { }, 'sitesearch_search:desktop'),
        ## (0, GRANNY,  '/search/site/',              { }, 'sitesearch_search:desktop'),

        ## (0, DESKTOP, '/search/opensearch.xml',     { }, 'opensearch:desktop'),
        ## (0, PAD,     '/search/opensearch.xml',     { }, 'opensearch:desktop'),
        ## (0, TOUCH,   '/search/opensearch.xml',     { }, 'opensearch:desktop'),
        ## (0, GRANNY,  '/search/opensearch.xml',     { }, 'opensearch:desktop'),

        ## (0, DESKTOP, '/search/redir_warning',      { 'url' : 'http://ya.ru' }, 'redir_warning:desktop'),
        ## (0, PAD,     '/search/redir_warning',      { 'url' : 'http://ya.ru' }, 'redir_warning:desktop'),
        ## (0, TOUCH,   '/search/redir_warning',      { 'url' : 'http://ya.ru' }, 'redir_warning:desktop'),
        ## (0, GRANNY,  '/search/redir_warning',      { 'url' : 'http://ya.ru' }, 'redir_warning:desktop'),

        ## (0, DESKTOP, '/search/adult',              { 'url' : 'pornhub.com' }, 'infected:desktop'),
        ## (0, PAD,     '/search/adult',              { 'url' : 'pornhub.com' }, 'infected:pad'),
        ## (0, TOUCH,   '/search/adult',              { 'url' : 'pornhub.com' }, 'infected:phone'),
        ## (0, GRANNY,  '/search/adult',              { 'url' : 'pornhub.com' }, 'infected:desktop'),

        ## (0, DESKTOP, '/search/customize',          {  }, 'customize:desktop'),
        ## (0, PAD,     '/search/customize',          {  }, 'customize:desktop'),
        ## (0, TOUCH,   '/search/customize',          {  }, 'customize:desktop'),
        ## (0, GRANNY,  '/search/customize',          { },  'customize:desktop'),

        ## (0, DESKTOP, '/search/pre',                { "template": "granny:phone" }, 'granny:phone'),
        ## (0, PAD,     '/search/pad/pre',            { "template": "web4:desktop" }, 'web4:desktop'),
        ## (0, TOUCH,   '/search/touch/pre',          { "template": "web4:desktop" }, 'web4:desktop'),

        ## (0, DESKTOP, '/search/pre',                { "template": "granny:phone", "tmplrwr": "granny:suggest" }, 'suggest:phone'),
        ## (0, PAD,     '/search/pad/pre',            { "template": "web4:desktop", "tmplrwr": "web4:web_exp" }, 'web_exp:desktop'),
        ## (0, TOUCH,   '/search/touch/pre',          { "template": "web4:desktop", "tmplrwr": "web4:granny" }, 'granny:desktop'),
        ## (0, DESKTOP, '/search/pre',                { "template": "granny:phone", "tmplrwr": "granny:phone:suggest" }, 'suggest:phone'),
        ## (0, PAD,     '/search/pad/pre',            { "template": "template_id:device_id", "tmplrwr": "template_id:device_id:web_exp:desktop" }, 'web_exp:desktop'),

        ## (0, DESKTOP, '/search/pre',                { "template": "web4:desktop", "tmplrwr": "web4::web_exp:desktop" }, 'web_exp:desktop'),
        ## (0, PAD,     '/search/pad/pre',            { "template": "web4:pad",     "tmplrwr": "web4::web_exp:desktop" }, 'web_exp:desktop'),
        ## (0, TOUCH,   '/search/touch/pre',          { "template": "web4:phone",   "tmplrwr": "web4::web_exp:desktop" }, 'web_exp:desktop'),

        ## (0, DESKTOP, '/search/pre',                { "template": "web4:desktop", "tmplrwr": "web4::web_exp:" }, 'web_exp:desktop'),
        ## (0, PAD,     '/search/pad/pre',            { "template": "web4:pad",     "tmplrwr": "web4::web_exp:" }, 'web_exp:pad'),
        ## (0, TOUCH,   '/search/touch/pre',          { "template": "web4:phone",   "tmplrwr": "web4::web_exp:" }, 'web_exp:phone'),

        ## (0, DESKTOP, '/search/pre',                { "template": "web4:desktop", "tmplrwr": "web4:::" }, 'web4:desktop'),
        ## (0, PAD,     '/search/pad/pre',            { "template": "web4:pad",     "tmplrwr": "web4:::" }, 'web4:pad'),
        ## (0, TOUCH,   '/search/touch/pre',          { "template": "web4:phone",   "tmplrwr": "web4:::" }, 'web4:phone'),

        ## (0, DESKTOP, '/search/pre',                { "template": "web4:desktop", "tmplrwr": ":pad:web_exp:desktop" }, 'web4:desktop'),
        ## (0, PAD,     '/search/pad/pre',            { "template": "web3:pad",     "tmplrwr": ":pad:web_exp:desktop" }, 'web_exp:desktop'),
        ## (0, TOUCH,   '/search/touch/pre',          { "template": "granny:phone", "tmplrwr": ":pad:web_exp:desktop" }, 'granny:phone'),

        ## (0, DESKTOP, '/search/pre',                { "template": "web4:desktop", "tmplrwr": ":pad:web_exp:" }, 'web4:desktop'),
        ## (0, PAD,     '/search/pad/pre',            { "template": "web3:pad",     "tmplrwr": ":pad:web_exp:" }, 'web_exp:pad'),
        ## (0, TOUCH,   '/search/touch/pre',          { "template": "granny:phone", "tmplrwr": ":pad:web_exp:" }, 'granny:phone'),

        ## (0, DESKTOP, '/search/pre',                { "template": "web4:desktop", "tmplrwr": ":pad::" }, 'web4:desktop'),
        ## (0, PAD,     '/search/pad/pre',            { "template": "web4:pad",     "tmplrwr": ":pad::" }, 'web4:pad'),
        ## (0, TOUCH,   '/search/touch/pre',          { "template": "web4:phone",   "tmplrwr": ":pad::" }, 'web4:phone'),

        ###################
        ## ПОД АППХОСТОМ ##
        ###################
        (1, DESKTOP, '/search/',                   { "template": "granny:phone" }, 'granny:phone'),
        (1, PAD,     '/search/pad/',               { "template": "web4:desktop" }, 'web4:desktop'),
        (1, TOUCH,   '/search/touch/',             { "template": "web4:desktop" }, 'web4:desktop'),

        (1, DESKTOP, '/search/',                   { }, 'web4:desktop'),
        (1, PAD,     '/search/pad/',               { }, 'web4:pad'),
        (1, TOUCH,   '/search/touch/',             { }, 'web4:phone'),
        (1, GRANNY,  '/search/',                   { }, 'granny:desktop'),
        (1, DESKTOP, '/search/',                   { "template": "granny:phone", "tmplrwr": "granny:suggest" }, 'suggest:phone'),
        (1, PAD,     '/search/pad/',               { "template": "web4:desktop", "tmplrwr": "web4:web_exp" }, 'web_exp:desktop'),
        (1, TOUCH,   '/search/touch/',             { "template": "web4:desktop", "tmplrwr": "web4:granny" }, 'granny:desktop'),
        (1, DESKTOP, '/search/',                   { "template": "granny:phone", "tmplrwr": "granny:phone:suggest" }, 'suggest:phone'),
        (1, PAD,     '/search/pad/',               { "template": "template_id:device_id", "tmplrwr": "template_id:device_id:web_exp:desktop" }, 'web_exp:desktop'),

        (1, DESKTOP, '/search/',                   { "template": "web4:desktop", "tmplrwr": "web4::web_exp:desktop" }, 'web_exp:desktop'),
        (1, PAD,     '/search/pad/',               { "template": "web4:pad",     "tmplrwr": "web4::web_exp:desktop" }, 'web_exp:desktop'),
        (1, TOUCH,   '/search/touch/',             { "template": "web4:phone",   "tmplrwr": "web4::web_exp:desktop" }, 'web_exp:desktop'),

        (1, DESKTOP, '/search/',                   { "template": "web4:desktop", "tmplrwr": "web4::web_exp:" }, 'web_exp:desktop'),
        (1, PAD,     '/search/pad/',               { "template": "web4:pad",     "tmplrwr": "web4::web_exp:" }, 'web_exp:pad'),
        (1, TOUCH,   '/search/touch/',             { "template": "web4:phone",   "tmplrwr": "web4::web_exp:" }, 'web_exp:phone'),

        (1, DESKTOP, '/search/',                   { "template": "web4:desktop", "tmplrwr": "web4:::" }, 'web4:desktop'),
        (1, PAD,     '/search/pad/',               { "template": "web4:pad",     "tmplrwr": "web4:::" }, 'web4:pad'),
        (1, TOUCH,   '/search/touch/',             { "template": "web4:phone",   "tmplrwr": "web4:::" }, 'web4:phone'),

        (1, DESKTOP, '/search/',                   { "template": "web4:desktop", "tmplrwr": ":pad:web_exp:desktop" }, 'web4:desktop'),
        (1, PAD,     '/search/pad/',               { "template": "web3:pad",     "tmplrwr": ":pad:web_exp:desktop" }, 'web_exp:desktop'),
        (1, TOUCH,   '/search/touch/',             { "template": "granny:phone", "tmplrwr": ":pad:web_exp:desktop" }, 'granny:phone'),

        (1, DESKTOP, '/search/',                   { "template": "web4:desktop", "tmplrwr": ":pad:web_exp:" }, 'web4:desktop'),
        (1, PAD,     '/search/pad/',               { "template": "web3:pad",     "tmplrwr": ":pad:web_exp:" }, 'web_exp:pad'),
        (1, TOUCH,   '/search/touch/',             { "template": "granny:phone", "tmplrwr": ":pad:web_exp:" }, 'granny:phone'),

        (1, DESKTOP, '/search/',                   { "template": "web4:desktop", "tmplrwr": ":pad::" }, 'web4:desktop'),
        (1, PAD,     '/search/pad/',               { "template": "web4:pad",     "tmplrwr": ":pad::" }, 'web4:pad'),
        (1, TOUCH,   '/search/touch/',             { "template": "web4:phone",   "tmplrwr": ":pad::" }, 'web4:phone'),

        (1, DESKTOP, '/search/',                   { "tmplrwr": "web4:suggest" }, 'suggest:desktop'),
        (1, PAD,     '/search/pad/',               { "tmplrwr": "web4:web_exp" }, 'web_exp:pad'),
        (1, TOUCH,   '/search/touch/',             { "tmplrwr": "web4:granny" },  'granny:phone'),
        (1, TOUCH,   '/search/touch/',             { "tmplrwr": "web4:phone:granny_exp:phone" },  'granny_exp:phone'),

    ])
    @pytest.mark.skip(reason='Do not work. Use integration test')
    def test_template_name(self, query, can_app_host, query_type, url_path, prms, template):
        # no all templates work under app_host
        query.set_url(url_path)
        query.set_user_agent(USERAGENT_BY_TYPE[query_type])
        query.set_flags({'json_template_external': 'by_name', 'json_template': 1, 'serp3_granny_https': 1})
        query.set_params({'text':TEXT, 'searchid': '2053249', 'web': '0'})
        query.set_internal() # XXX hack to make internal cgi param 'template' work
        query.add_params(prms)

        ctxs = self.json_dump_ctxs(query)
        # D({"DEVICE_CONFIG": ctxs['device_config']})

        assert ctxs['device_config'][-1]['template_name'] == template


    @pytest.mark.skipif(True, reason="WEBREPORT-409")
    @pytest.mark.parametrize("query_type, url_path, prms, template", [
        (DESKTOP, '/search/catalogsearch',      {  }, 'sitesearch_search:desktop'),
        (PAD,     '/search/catalogsearch',      {  }, 'sitesearch_search:pad'),
        (TOUCH,   '/search/catalogsearch',      {  }, 'sitesearch_search:phone'),
        (GRANNY,  '/search/catalogsearch',      {  }, 'sitesearch_search:desktop'),
    ])
    def test_template_name_catalogsearch(self, query, query_type, url_path, prms, template):
        TEMPLATE_SOURCE_NAME = 'TEMPLATE'
        query.set_url(url_path)
        query.set_user_agent(USERAGENT_BY_TYPE[query_type])
        query.set_flags({'json_template_external': 'by_name', 'json_template': 1, 'serp3_granny_https': 1})
        query.set_params({'text':TEXT, 'searchid': '2053249', 'web': '0'})
        query.set_params({'dump': 'eventlog', 'dump_source_request': TEMPLATE_SOURCE_NAME})
        query.set_internal() # XXX hack to make internal cgi param 'template' work
        query.add_params(prms)

        # skip first symbol due to " in beggining
        def starts_with_timestamp(string):
            TIMESTAMP_STR_LENGTH = 16
            return len(string) >= TIMESTAMP_STR_LENGTH + 1 and string[1:TIMESTAMP_STR_LENGTH + 1].isdigit()

        def get_source_request_from_response(resp, source_name):
            for line in resp.content.splitlines():
                stripped_line = line.strip()
                if not starts_with_timestamp(stripped_line):
                    continue
                splitted_line = stripped_line.split('\\t')
                event_type = splitted_line[2]
                if event_type == 'TSourceRequest' and splitted_line[3] == source_name:
                    assert splitted_line[-1][-1] == ','
                    splitted_line[-1] = splitted_line[-1][:-1]

                    escaped_json_request_string = '"' + '\\t'.join(splitted_line[4:])
                    escaped_json_request = json.loads(escaped_json_request_string)
                    json_request = json.loads(escaped_json_request)
                    yield json_request['answers']

        resp = self.request(query)
        flag = 0
        for request in get_source_request_from_response(resp, TEMPLATE_SOURCE_NAME):
            flag = 1
            j = get_app_host_context(request, ttype='template_data')
            assert len(j) == 1
            assert j[0]["name"] == template

        assert flag, 'No request was made to {}'.format(TEMPLATE_SOURCE_NAME)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-45229')
    @pytest.mark.parametrize(('app_host'), [False, True])
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_renderer_fatal_errors(self, query, app_host):
        query.set_flags({'json_template_external': 1, 'srcrwr': {'TEMPLATES': 'localhost:1'}})
        self.set_app_host(query, app_host)
        resp = self.request(query, validate_content=False)
        #TODO assert resp.status == 500
        assert FATAL_ERROR_MARKER in resp.content

    @pytest.mark.skipif(True, reason="SERP-60410")
    @pytest.mark.parametrize(('app_host'), [False, True])
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_renderer_retries(self, query, app_host):
        templates = SimpleDelayedConfig(response=service_unavailable(), response_delay=5)
        query.set_flag('json_template_external')
        self.set_app_host(query, app_host)
        resp = self.request(query, validate_content=False, source=('TEMPLATES', templates))
        assert resp.content
        assert FATAL_ERROR_MARKER in resp.content
        assert resp.source.count >= 2

    @pytest.mark.unstable
    @pytest.mark.ticket('SERP-42564')
    def test_familysearch_for_china(self, query):
        query.set_flag('json_template_external')
        query.add_params({'lr': 134})
        resp = self.request(query, source='TEMPLATES')

        assert resp.source.requests[0]['reqdata']['prefs']['afamily'] == 2
        assert resp.source.requests[0]['reqdata']['prefs']['family'] == 1
        assert resp.source.requests[0]['reqdata']['prefs']['is_family'] == 2

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize(('url', 'user_agent'), (
        ('/search/pre', USER_AGENT_DESKTOP),
        ('/search/pad/pre', USER_AGENT_PAD),
        ('/search/touch/pre', USER_AGENT_TOUCH),
    ))
    def test_renderer_pre(self, query, url, user_agent):
        # This test will be updted after flags release
        # https://st.yandex-team.ru/FEI-17241
        query.set_url(url)
        query.set_flag('json_template_external')
        query.set_user_agent(user_agent)

        content = '<html></html>'
        resp = self.request(query, sources=[
            ('TEMPLATES', content),
            ('APP_HOST_TEMPLATES'),
        ])

        if resp.sources['TEMPLATES']:
             assert resp.sources['TEMPLATES'].count == 1
             assert resp.content == content
             assert resp.sources['TEMPLATES'].data['entry'] == 'pre-search'
        elif resp.sources['APP_HOST_TEMPLATES']:
             assert resp.sources['APP_HOST_TEMPLATES'].count == 1

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize('url', (
        "/search/prefetch.txt",
        "/search/yandcache.js",
        "/search/padcache.js",
        "/search/touchcache.js",
        "/prefetch.txt",
        "/yandcache.js",
        "/padcache.js",
        "/touchcache.js"
    ))
    @pytest.mark.parametrize('query_type', (DESKTOP, PAD, TOUCH, SMART, TV))
    def test_prefetch(self, query, query_type, url):
        query.set_query_type(query_type)
        query.set_url(url)
        resp = self.request(query)
        assert resp.content

    @pytest.mark.parametrize(('url', 'user_agent'), (
        ('/search/', USER_AGENT_DESKTOP),
        ('/search/pad/', USER_AGENT_PAD),
        ('/search/touch/', USER_AGENT_TOUCH),
    ))
    def test_export_pre(self, query, url, user_agent):
        query.set_url(url)
        query.set_user_agent(user_agent)

        query.set_internal()
        query.set_params({'export': 'json_pre'})

        resp = self.request(query)

        export_pre = json.loads(resp.content)
        assert export_pre['entry'] == 'pre-search'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(('url', 'query_type', 'forever_on_top'), (
        ('/search/', DESKTOP, True),
        ('/search/pad/', PAD, True),
        ('/search/touch/', TOUCH, True),
        ('/search/result', DESKTOP, False),
    ))
    def test_foreverdata(self, query, url, query_type, forever_on_top):
        query.set_query_type(query_type)
        query.set_url(url) #no search/result in standard types

        query.set_internal()
        query.set_params({'export': 'json', 'promo': 'nomooa', 'foreverdata': '244708688'})

        resp = self.request(query)

        export = json.loads(resp.content)

        if forever_on_top:
            assert 'forever' in export
            forever = export['forever']
        else:
            assert 'foreverdata' in export['app_host']
            forever = export['app_host']['foreverdata']

        assert forever
        assert 'data' in forever
