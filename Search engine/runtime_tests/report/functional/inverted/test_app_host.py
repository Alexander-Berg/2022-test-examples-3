# pphost_user_connection_flags_mergepphost_user_connection_flags_merge-*- coding: utf-8 -*-

import os
import pytest
import base64
import json

from report.functional.web.base import BaseFuncTest
from runtime_tests.util.predef.handler.server.http import SimpleDelayedConfig
from runtime_tests.util.predef.http.response import service_unavailable
from report.const import *

@pytest.mark.skip(reason="we do not use perl")
class TestAppHost(BaseFuncTest):
    def test_app_host_schema(self, query, schema_path_to_contexts):
        #SERP-38481 схема INIT контекста
        ctxs = self.json_dump_ctxs(query)
        for type in ctxs:
            if type in ['access', 'app_host_params', 'context_rewrite', 'device_config', 'device', 'experiments', 'exp_params', 'flags', 'http_request', 'log_access', 'noapache_setup', 'region', 'report', 'request', 'settings', 'true', 'user']:
                for ctx in ctxs[type]:
                    self.validate_context(ctx, os.path.join(schema_path_to_contexts, type + '.json'))

        resp = self.app_host_request(query)

    # SERP-50331
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize(("content_type", 'post_body_ascii', 'post_body_utf8'), [
        ['application/json', 'Тестовая строка', u'Тестовая строка'],
        ['application/json; charset=utf-8', 'Тестовая строка', u'Тестовая строка'],
    ])
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_unicode_post(self, query, content_type, post_body_ascii, post_body_utf8):
        query.set_method('POST')
        query.headers.set_content_type(content_type)
        query.set_url("/search/")
        query.set_post_params(post_body_ascii)
        resp = self.app_host_request(query)

        for ctx in resp.sources['APP_HOST'].requests[0].data:
            if (ctx['name'] == 'INIT'):
                for r in ctx['results']:
                    if (r['type'] == 'request'):
                        assert r['body'] == post_body_utf8

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(('url', 'params'), (
        ('/search/catalogsearch', { 'format': 'json', 'searchid': 198373, 'text': TEXT }),
        ('/search/entity', {'format': 'json'}),
        ('/search/suggest-history', {'format': 'json'}),
    ))
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_apphost_handlers(self, query, url, params):
        query.path.set(url=url, params=params)
        resp = self.request(query)
        assert resp.content

        query.set_url(url + '/')
        resp = self.request(query)
        assert resp.content

        query.set_params({'full-context': 1})
        resp = self.json_request(query)

    @pytest.mark.ticket('SERP-55994')
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_apphost_handlers_handler(self, query):
        query.path.set(url='/search/result', params={ 'json_dump': 'rdat.handler' })
        resp = self.json_request(query)
        assert resp.data['rdat.handler'] == 'YxWeb::MiscHandlers::AppHostExternal::result'

    @pytest.mark.ticket('SERP-53448')
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_app_host_handler_x_frame_options_null(self, query):
        query.set_internal()
        query.path.set(url="/search/turbo", params={'format': 'json'})
        resp = self.request(query)
        assert not resp.headers['X-Frame-Options']

    @pytest.mark.skipif(True, reason="SERP-67197")
    @pytest.mark.ticket('SERP-46282')
    @pytest.mark.parametrize(('code', 'params'), (
        (15, {'searchid': '2045692', 'text': 'персонаж', 'web': '0', 'lr': '213', 'constraintid': '3',
                                'within': '777', 'from_day': '19', 'from_month': '9', 'from_year': '2016', 'to_day': '19',
                                'to_month': '9', 'to_year': '2016'}),
        (2,  {'searchid': '2045692', 'text': '', 'web': '0', 'lr': '213', 'constraintid': '3',
                                'within': '777'}),
    ))
    def test_msg_no_results(self, query, code, params):
        query.set_params(params)
        query.set_query_type(SITESEARCH)
        resp = self.json_request(query, source='APP_HOST')

        assert resp.source
        assert resp.data['searchdata']['err_code'] == code

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-41471')
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_apphost_web_param(self, query):
        query.headers['X-Region-Id'] = '888'
        assert '"x-region-id":"888"' in self.source_param(query, 'WEB', 'app_host')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.skipif(True, reason="")
    def test_apphost_user_connection_flags_merge(self, query):
        query.add_flags({
            'app_host_srcskip': 'APP_HOST_PRE=0,TEMPLATE_LEGACY-PRE_SEARCH=0',
            'app_host_source': 'PRE=APP_HOST_WEB',
            'json_template_external': 'by_name'
        })
        query.headers.set_custom_headers({
            'X-Yandex-ExpBoxes': '1137597,1137597,1137597',
            'X-Yandex-ExpFlags': base64.b64encode(json.dumps([{
#                'CONDITION': 'app_host.user_connection.foo eq "bar"',
                'HANDLER': 'REPORT',
                'TESTID': [ '1137597' ],
                'CONTEXT': {
                    'MAIN': {
                        'REPORT': {
                            "template": "granny123:phone"
                        }
                    },
                },
            }])),
        })
        sources=[
            (
                'APP_HOST_WEB',
                json.dumps([
                    {
                        'name': "USER_CONNECTION",
                        'results': [
                            { 'type': 'user_connection', 'foo': 'bar' },
                        ]
                    }
                ])
            ),
            'TEMPLATES_LOWLOAD'
        ]
        query.set_internal()
        resp = self.request(
                query,
                require_status=None,
                sources=sources
            )
        assert resp.sources['TEMPLATES_LOWLOAD'].requests[0].path == '/granny123:phone'
        reqans = resp.reqans_log()
        found = re.search(r'test-ids=([\d ]+),', reqans['req']['search_props'])
        assert found
        assert '1137597' in found.group(1).split()
        resp = self.json_request(
                query,
                require_status=None,
                sources=sources
            )
        assert resp.data['reqdata']['flags']['template'] == 'granny123:phone'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-50304')
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_apphost_log_reqans(self, query):
        resp = self.request(
            query,
            require_status=None,
            sources=[
                (
                    'APP_HOST',
                    json.dumps([
                        {
                            'name': "USER_CONNECTION",
                            'results': [
                                { 'type': 'user_connection', 'foo': 'bar' },
                                { 'type': 'log_reqans', 'data': { '111uc_key': '111uc_val', '222uc_key': '222uc_val' } },
                            ]
                        }
                    ])
                )
            ]
        )
        reqans = resp.reqans_log()
        assert '111uc_key=111uc_val' in reqans['req']['search_props']
        assert '222uc_key=222uc_val' in reqans['req']['search_props']

    @pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_blackbox_valid(self, query):
        login = 'test-login'
        uid = 20000000

        resp = self.json_request(
            query,
            require_status=None,
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
                                        'name': login,
                                    },
                                    'error': 'OK',
                                    'expires_in': 7775998,
                                    'have_hint': True,
                                    'have_password': True,
                                    'karma': {'value': 0, },
                                    'karma_status': {'value': 6000, },
                                    'login': login,
                                    'phones': [
                                        {
                                            'attributes': {'102': '+79260000000', },
                                            'id': 10000000,
                                        },
                                    ],
                                    'regname': login,
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
                                        'value': uid,
                                    },
                                },
                            ]
                    }])
                ),
            ]
        )
        pas = resp.data['reqdata']['passport']
        assert pas['logged_in'] == 1, pas
        assert pas['cookieL']['uid'] == uid, pas
        assert pas['cookieL']['login'] == login, pas

    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_blackbox_need_reset(self, query):
        login = 'test-login'
        uid = 20000000

        resp = self.json_request(
            query,
            require_status=None,
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
                                        'name': login,
                                    },
                                    'error': 'OK',
                                    'expires_in': 7775998,
                                    'have_hint': True,
                                    'have_password': True,
                                    'karma': {'value': 0, },
                                    'karma_status': {'value': 6000, },
                                    'login': login,
                                    'phones': [
                                        {
                                            'attributes': {'102': '+79260000000', },
                                            'id': 10000000,
                                        },
                                    ],
                                    'regname': login,
                                    'session_fraud': 0,
                                    'status': {
                                        'id': '0',
                                        'value': 'NEED_RESET',
                                    },
                                    'ttl': 5,
                                    'type': 'blackbox',
                                    'uid': {
                                        'hosted': False,
                                        'lite': False,
                                        'value': uid,
                                    },
                                },
                            ]
                    }])
                ),
            ]
        )
        pas = resp.data['reqdata']['passport']
        assert pas['logged_in'] == 1, pas
        assert pas['cookieL']['uid'] == uid, pas
        assert pas['cookieL']['login'] == login, pas

    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_blackbox_social(self, query):
        """
        SERP-52313: Не приходит login при социальной авторизации
        """
        login = 'Dmitry Savvateev'
        uid = 482268923

        resp = self.json_request(
                query,
                require_status=None,
                sources=[
                    (
                        'APP_HOST',
                        json.dumps([{
                            "meta": {},
                            "name": "BLACKBOX",
                            "results": [
                                {
                                    'age' : 1857,
                                    'connection_id' : 'test',
                                    'display_name' : {
                                        'name' : 'Dmitry Savvateev',
                                    },
                                    'error' : 'OK',
                                    'expires_in' : 7774143,
                                    'karma' : {
                                        'value' : 0
                                        },
                                    'karma_status' : {
                                        'value' : 0
                                        },
                                    'login' : '',
                                    'phones' : [],
                                    'regname' : 'uid-c6brmgt7',
                                    'session_fraud' : 0,
                                    'status' : {
                                        'id' : 0,
                                        'value' : 'VALID'
                                        },
                                    'ttl' : 5,
                                    'type' : 'blackbox',
                                    'uid' : {
                                        'value' : 482268923
                                    }

                                },
                            ]
                    }])
                ),
            ]
        )
        pas = resp.data['reqdata']['passport']
        assert pas['logged_in'] == 1, pas
        assert pas['cookieL']['uid'] == uid, pas
        assert pas['cookieL']['login'] == login, pas

