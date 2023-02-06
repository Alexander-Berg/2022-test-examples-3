# -*- coding: utf-8 -*-

import os
import pytest
import re

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestHeaders(BaseFuncTest):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-44826')
    @pytest.mark.parametrize(("flag_value"), [
        ('100'),
    ])
    @pytest.mark.parametrize(("tld", "ip"), [
        (RU, RU_MOSCOW),
        (COM, W_OSLO),
        (COMTR, COMTR_ISTANBUL),
        (UA, UA_KIEV),
        (KZ, KZ_ASTANA),
        (BY, BY_MINSK),
        #(UZ, UZ_TASHKENT)
    ])
    @pytest.mark.ticket('WEBREPORT-855')
    def test_hsts_flag_ok(self, query, flag_value, tld, ip):
        query.set_host(tld)
        query.headers.set_forward_for(IP[ip])

        resp = self.request(query)

        assert resp.headers.get_one('X-Yandex-STS') == '1'
        assert resp.headers.get_one('X-Yandex-STS-Plus') == '1'
        assert resp.headers.get_one('Strict-Transport-Security') == 'max-age=1'

    @pytest.mark.parametrize(("tld", "ip"), [
        (RU, RU_MOSCOW),
        (COM, W_OSLO),
        (COMTR, COMTR_ISTANBUL),
        (UA, UA_KIEV),
        (KZ, KZ_ASTANA),
        (BY, BY_MINSK),
        #(UZ, UZ_TASHKENT)
    ])
    @pytest.mark.ticket('RUNTIMETESTS-21')
    def test_double_headers(self, query, tld, ip):
        query.set_host(tld)
        query.headers.set_forward_for(IP[ip])

        resp = self.request(query)
        ignore_list = ['x-content-type-options', 'set-cookie', 'expires', 'x-yandex-sts-plus', 'x-yandex-items-count', 'cache-control']
        for hName in resp.headers.get_names():
            if hName not in ignore_list:
                assert resp.headers.get_one(hName)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-48817')
    @pytest.mark.parametrize(("ua", "is_external", "expect"), [
        (USER_AGENT_TOUCH, True, 'DENY'),
        (USER_AGENT_TOUCH, False, None),
        (USER_AGENT_TIZEN_SEARCHAPP, True, None),
        (USER_AGENT_TIZEN_SEARCHAPP, False, None)
    ])
    def test_frame_options(self, query, ua, is_external, expect):
        query.set_query_type(TOUCH)
        query.set_user_agent(ua)
        query.set_external(is_external)

        resp = self.request(query)

        if expect is None:
            assert 'X-Frame-Options' not in resp.headers
        else:
            assert resp.headers.get_one('X-Frame-Options') == expect

    def assert_csp_header(self, header, project):
        data = str.strip(header)
        assert data.startswith('report-uri https://yandex.ru/csp?from=' + project + '&reqid=')

    @pytest.mark.skipif(True, reason="SERP-62753")
    @pytest.mark.ticket('SERP-56957')
    @pytest.mark.parametrize(("enable_csp"), [(1), (0)])
    @pytest.mark.parametrize(("url", "user_agent", "project"), [
        (SEARCH,            USER_AGENT_DESKTOP,     'web4:desktop'),
        (SEARCH,            USER_AGENT_GRANNY,      'granny:desktop'),
        (SEARCH_TOUCH,      USER_AGENT_TOUCH,       'web4:phone'),
        (SEARCH_PAD,        USER_AGENT_PAD,         'web4:pad'),
    ])
    def test_csp_headers_from(self, query, url, user_agent, project, enable_csp):
#        query.set_flag('serp3_granny_https')
        query.set_flags({'enable_csp': enable_csp})
        query.set_url(url)
        query.set_user_agent(user_agent)
        query.set_yandexuid()

        resp = self.request(query)

        if enable_csp in [0]:
            assert 'content-security-policy' not in resp.headers
        else :
            assert 'content-security-policy' in resp.headers
            self.assert_csp_header(resp.headers.get_one('content-security-policy'), project)
        assert 'content-security-policy-report-only' not in resp.headers

    @pytest.mark.skipif(True, reason="SERP-62753")
    @pytest.mark.ticket('SERP-36940', 'SERP-36119', 'SERP-37808', 'SERP-56957')
    def test_csp_headers_desktop_report_only(self, query):
        """
        SERP-36119 - блокирующий CSP
        SERP-36940 : Исправления в CSP для Safari по http
        SERP-56957: CSP: переезд на верстку, репорт
        Проверяем хедер content-security-policy-report-only
        """
        query.set_flags({'csp_report_only': 1})
        query.set_yandexuid()

        resp = self.request(query)

        assert 'content-security-policy' not in resp.headers
        assert 'content-security-policy-report-only' in resp.headers
        self.assert_csp_header(resp.headers.get_one('content-security-policy-report-only'), 'web4:desktop')

    @pytest.mark.skipif(True, reason="SERP-62753")
    @pytest.mark.ticket('SERP-56957')
    def test_csp_headers_desktop_disabled(self, query):
        """
        SERP-56957: CSP: переезд на верстку, репорт
        enable_csp=0 выключает CSP
        """
        query.set_flags({'enable_csp': 0})
        query.set_yandexuid()

        resp = self.request(query)

        assert 'content-security-policy' not in resp.headers
        assert 'content-security-policy-report-only' not in resp.headers

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-48084')
    @pytest.mark.parametrize(("url", "is_exists", "origin"), [
        ('/search/suggest-history', False, "yandex.ru"),
        ('/search/suggest-history', True, "https://yandex.ru"),
        ('/search/suggest-history', False, "https://byandex.ru"),
        ('/search/', False, None),
    ])
    def test_cors(self, query, url, is_exists, origin):
        if origin:
            query.set_custom_headers({"Origin": origin})
        query.set_host(RU)
        query.set_url(url)

        resp = self.request(query)

        if is_exists:
            assert resp.headers.get_one('Access-Control-Allow-Origin') == origin
            assert resp.headers.get_one('Access-Control-Allow-Credentials') == 'true'
        else:
            assert not resp.headers.get_one('Access-Control-Allow-Origin')
            assert not resp.headers.get_one('Access-Control-Allow-Credentials')

    @pytest.mark.parametrize(("lite"), [
        (0),
        (1),
        (None),
    ])
    @pytest.mark.parametrize(("granoff"), [
        (1),
        (None),
    ])
    def test_granoff(self, query, lite, granoff):
        query.set_host(RU)
        query.set_url(SEARCH_TOUCH)
        query.set_user_agent(USER_AGENT_TOUCH)
        query.set_yandexuid()
        start_time = str(int(time.time()))
        query.headers.set_custom_headers({'X-Start-Time': '{}000000'.format(start_time)})
        if not(lite is None):
            query.set_params({'lite': lite})
        if not(granoff is None):
            query.headers.cookie.set_ys("ys=granoff." + start_time)

        resp = self.request(query)
        granny = filter(lambda(x): x.startswith("ys=granoff."), resp.headers.get_all('Set-Cookie'))
        # print resp.headers.get_all('Set-Cookie')

        if lite == 0 and not granoff:
            assert granny == ['ys=granoff.' + start_time + '; path=/; domain=.yandex.ru']
        else:
            assert granny == []

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-55292')
    def test_searchapp_content_type(self, query):

        query.set_host(RU)
        query.set_url(SEARCHAPP)
        query.set_user_agent(USER_AGENT_SEARCHAPP_ANDROID)
        query.set_internal()

        resp = self.request(query)

        assert re.match('multipart/related; ?boundary=Asrf456BGe4h; ?charset=utf-8', resp.headers.get_one('Content-Type'))

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-56212')
    def test_searchapp_serp_only_content_type(self, query):

        query.set_host(RU)
        query.set_url(SEARCHAPP)
        query.set_user_agent(USER_AGENT_SEARCHAPP_ANDROID)
        query.set_params({'serp_only': "1"})

        resp = self.request(query)

        assert resp.headers.get_one('Content-Type') == 'text/html; charset=utf-8'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-61529')
    @pytest.mark.parametrize(("referer", "expect"), [
        ('https://yandex.ru/something', None),
        ('https://ya.ru/', None),
        ('http://notyandex.ru/', 'DENY'),
        ('http://not-yandex.ru/', 'DENY'),
        ('http://some.not-yandex.ru/', 'DENY'),
        ('https://hamster.yandex.ru/', None),
        ('https://yandex.ua/', 'DENY'),
        (None, 'DENY')
    ])
    def test_frames_for_referer(self, query, referer, expect):
        if referer is not None:
            query.headers['Referer'] = referer
        query.set_external(1)

        resp = self.request(query)

        if expect is None:
            assert 'X-Frame-Options' not in resp.headers
        else:
            assert resp.headers.get_one('X-Frame-Options') == expect
