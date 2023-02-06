# -*- coding: utf-8 -*-

import re
import copy
import urlparse
import base64

import os
import pytest

from report.const import *
from report.functional.web.base import BaseFuncTest

@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestRedirects(BaseFuncTest):
    """
    Проверяем все редиректы в репорте.
    Только статус ответа и простые проверки валидности ответа.

    По мотивам тикета SERP-28157 и Аква-пака SERP Redirects
    """

    def test_search_wrong_url(self, query):
        query.set_url('/search/wrong')
        resp = self.request(query, require_status=302)
        assert resp.headers['location'][0].startswith('/search/?')

    @pytest.mark.parametrize("url", [
        '/cgi-bin/yandsearch',
        '/yandpage',
        '/largesearch',
    ])
    def test_search_obsolete(self, query, url):
        query.set_url(url)
        query.remove_params('text')
        resp = self.request(query, require_status=302)
        assert resp.headers['location'][0].startswith('/search/')

    @pytest.mark.parametrize("url", [
        '/familysearch',
        '/schoolsearch',
        '/search/school',
    ])
    def test_search_school(self, query, url):
        query.set_url(url)
        query.remove_params('text')
        resp = self.request(query, require_status=302)
        assert resp.headers['location'][0].startswith('/search/family')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("url", "ua", "params", "require_status"), [
        ('/search', USER_AGENT_DESKTOP, None, 302),
        ('/search/pad', USER_AGENT_PAD, None, 200),
        ('/search/smart', USER_AGENT_SMART, None, 302),
        ('/search/site', USER_AGENT_DESKTOP, { 'searchid': 2244093 }, 200),
    ])
    def test_search_no_slash(self, query, ua, url, params, require_status):
        query.set_url(url)
        query.set_user_agent(ua)
        query.set_params(params)
        query.add_params({'no-tests': 1})
        resp = self.request(query, require_status=require_status)
        if require_status in [302]:
            assert resp.headers['location'][0].startswith(url + '/')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_host_localhost(self, query):
        """
        SERP-31744 - Если в поле хост указать localhost, то мы получаем редирект без location
        Правильное поведение - location должен присутствовать хоть в каком-то виде
        """
        query.set_https(False)
        query.set_host('localhost')
        resp = self.request(query, require_status=302)
        assert 'location' in resp.headers
        assert resp.headers['location'][0].startswith('https://localhost/search/?text=')

    def test_advanced(self, query):
        """
        WEBREPORT-714 - закрываем /search/advanced
        раньше редиректили /advanced.html на /search/advanced
        """
        query.set_url('/advanced.html')

        resp = self.request(query, require_status=404)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-46893')
    @pytest.mark.parametrize(("device"), [
        DESKTOP,
        TOUCH,
        SMART,
    ])
    @pytest.mark.parametrize("gid, tld",  [
        (143,   'ua'),    # Киев
        (157,   'by'),    # Минск
        (162,   'kz'),    # Алматы
    ])
    def test_multidomain_gid_redir_blocked(self, device, query, gid, tld):
        """
        SERP-46893 - Флаг для отключения всех редиректов
        """
        query.set_query_type(device)
        query.set_host('ru')
        query.set_noauth()
        query.headers.cookie.set_yandex_gid(gid)

        resp = self.request(query, require_status=302)
        location = resp.headers['location'][0]
        assert location.startswith('https://yandex.'+tld+'/search/')

        query.headers.set_custom_headers({'X-Yandex-Internal-Flags': base64.b64encode(b'{"disable_redirects":1}')})
        self.request(query)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-46893')
    @pytest.mark.parametrize(("device"), [
        DESKTOP,
        TOUCH,
        SMART,
    ])
    @pytest.mark.parametrize("tld", [
        UA,
        BY,
        KZ
    ])
    def test_multidomain_https_redir_blocked(self, query, device, tld):
        """
        SERP-46893 - Флаг для отключения всех редиректов
        """
        query.set_host(tld)
        query.set_noauth()
        query.set_https(False)

        resp = self.request(query, require_status=302)
        assert resp.headers['location'][0].startswith('https://yandex.' + tld + '/search/?text=')

        query.headers.set_custom_headers({'X-Yandex-Internal-Flags': base64.b64encode(b'{"disable_redirects":1}')})
        self.request(query)

    @pytest.mark.parametrize("tld", [
        UA,
        BY,
        KZ
    ])
    def test_multidomain_no_redir(self, query, tld):
        """
        Проверяем редиректы для фичи - мультидомена
        https://wiki.yandex-team.ru/passport/mda/intro

        По мотивам тикета
        https://st.yandex-team.ru/SERP-34554
        проверяем, что редиректа нет, если передавался специальный параметр
        """
        query.add_params({'rdpass': '1'})
        query.set_host(tld)
        query.reset_auth()

        self.request(query)

    def base_nomda_test(self, query, query_type, tld, coo, expect_redir):
        query.set_query_type(query_type)
        query.set_host(tld)
        query.set_params({ "lr" : REGION_BY_TLD[tld] })
        query.add_flags({'infect_mda': 0})
        #query.headers.set_forward_for(IP[region])

        query.reset_auth()
        if coo:
            old_cookie = str(query.headers.cookie)
            query.headers.set_raw_cookie(old_cookie + '; ' + coo)

        resp = self.request(query, require_status=302 if expect_redir else 200)

        if expect_redir:
            location = resp.headers.get_one('location')
            assert location.startswith(expect_redir)

    @pytest.mark.skip(reason="SERP-71839,SERP-57279")
    @pytest.mark.parametrize(("query_type"), [
        DESKTOP,
        PAD,
    ])
    @pytest.mark.parametrize(("tld", "coo", "expect_redir"), [
        #(RU,  "",      None),
        #(RU,  "mda=0", None),
        #(UA,  "",      RU),
        #(UA,  "mda=0", UA)
        (UA,  "",      'https://pass.yandex.ua/'),
        (UA,  "mda=0", None),
    ])
    def test_nomda_desktop(self, query, query_type, tld, coo, expect_redir):
        self.base_nomda_test(query, query_type, tld, coo, expect_redir)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize("tld", [
        UA,
        BY,
        KZ
    ])
    def test_multidomain_rdpass_https(self, query, tld):
        """
        Проверяем редиректы для фичи - мультидомена
        https://wiki.yandex-team.ru/passport/mda/intro

        По мотивам тикета
        https://st.yandex-team.ru/SERP-34747
        проверяем, что если пришли по http, то нас правильно редиректит на https
        """
        query.add_params({'rdpass': '1'})
        query.set_host(tld)
        query.set_noauth()
        query.set_https(False)

        resp = self.request(query, require_status=302)
        assert resp.headers['location'][0].startswith('https://yandex.' + tld + '/search/?text=')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize(("url", "result"), [
        ('beta.yandex.', 'yandex.'),
        ('www.beta.yandex.', 'yandex.'),
        ('site.beta.yandex.', 'site.yandex.'),
        ('site.www.beta.yandex.', 'site.yandex.'),
        ('beta.zelo.serp.yandex.', 'zelo.serp.yandex.'),
        ('www.beta.zelo.serp.yandex.', 'zelo.serp.yandex.'),
        ('site.beta.zelo.serp.yandex.', 'site.zelo.serp.yandex.'),
        ('site.www.beta.zelo.serp.yandex.', 'site.zelo.serp.yandex.'),
    ])
    @pytest.mark.parametrize(("tld"), [
        RU,
        COM,
        COMTR,
        UA,
        KZ,
        BY,
        #UZ
    ])
    @pytest.mark.parametrize(("https"), [
        True,
        False
    ])
    def test_is_beta_redir(self, query, url, result, tld, https):
        query.set_https(https)
        query.set_host(url + tld)
        query.set_url('/')

        resp = self.request(query, require_status=302)
        pref = 'https' if https else 'http'
        assert resp.headers['location'][0].startswith(pref + '://' + result + tld)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize(("tld"), [
        RU,
        KZ,
        COM,
        COMTR
    ])
    def test_set_uid_on_redir(self, query, tld):
        """
        SERP-36125 - Выставлять yandex_uid новым пользователям до (или вместе) с 302 редиректом
        Проверяем, что кука выставляется и для RU
        Для чего-то из КУБ(для), так как там работает МДА
        COM, COMTR проверяем, так как может быть разная логика для разных доменов
        """

        query.set_https(False)
        query.set_host(tld)

        resp = self.request(query, require_status=302)
        cookie = '; '.join(resp.headers['set-cookie'])

        #конвертим значения в словарь
        cookie = dict([val.split('=') for val in cookie.split(';')])
        assert ' yandexuid' in cookie or 'yandexuid' in cookie

    @pytest.mark.skip(reason="RUNTIMETESTS-114")
    @pytest.mark.parametrize(("path"), [
        SEARCH_TOUCH,
        SEARCH,
        SEARCH_PAD,
        SEARCH_SMART,
        YANDSEARCH,
        SEARCHAPP
    ])
    @pytest.mark.parametrize(("lr"), [
        REGION_BY_TLD[RU],
        REGION_BY_TLD[UA],
        REGION_BY_TLD[KZ],
        REGION_BY_TLD[BY],
        #REGION_BY_TLD[UZ],
    ])
    @pytest.mark.parametrize(("tld"), [
        RU, COM, COMTR, UA, KZ, BY, #UZ
    ])
    @pytest.mark.parametrize(("https"), [
        True, False
    ])
    def test_no_ajax_redir(self, query, tld, path, lr, https):
        """
        SERP-40592 - Исключить любые редиректы для AJAX запросов
        """
        query.set_https(https)
        query.set_host(tld)
        query.set_url(path)
        query.set_params({
            'text': TEXT,
            'callback' : 'c305338619620',
            'yu' : YANDEXUID,
            'lr' : lr,
            'ajax' : '{}'
        })

        self.request(query)

    @pytest.mark.skip(reason="RUNTIMETESTS-114")
    @pytest.mark.ticket('SERP-43695')
    @pytest.mark.parametrize(("flag_value", "status"), [
        (1, 200),
        (0, 302),
    ])
    def test_noredirect_from_com(self, query, flag_value, status):
        query.headers.set_custom_headers({"X-LaaS-Answered": "1"})
        query.set_flags({'noredirect_com': flag_value})
        query.set_host(COM)
        query.set_region(REGION[RU_MOSCOW])
        self.request(query, require_status=status)

    @pytest.mark.ticket('SERP-48094')
    @pytest.mark.parametrize(("host", "lr", "region", "status"), [
        (RU, REGION_BY_TLD[RU], REGION[RU_MOSCOW], 200),
        (FR, REGION_BY_TLD[FR], REGION[RU_MOSCOW], 302),
        (FR, REGION_BY_TLD[RU], REGION[UZ_TASHKENT], 302),
        (UA, REGION_BY_TLD[UA], REGION[RU_MOSCOW], 200),
        (COM, REGION_BY_TLD[COM], REGION[RU_MOSCOW], 200),
        (COMTR, REGION_BY_TLD[COMTR], REGION[RU_MOSCOW], 200)
    ])
    def test_lr_redirects(self, query, host, lr, region, status):
        query.set_host(host)
        query.set_region(region)
        query.set_params({
            'text' : TEXT,
            'lr': lr
        })

        self.request(query, require_status=status)


    @pytest.mark.ticket('SERP-45071')
    @pytest.mark.parametrize(("flag_by_cgi"), [
        True, False,
    ])
    @pytest.mark.parametrize(("tld", "lr", "expected_tld"), [
        (RU, 213, RU),
        (RU, 157, BY)
    ])
    @pytest.mark.parametrize(("https", "flag"), [
        (False, 'disable_https'),
        (True, None)
    ])
    def test_tld_by_lr(self, query, flag_by_cgi, tld, lr, expected_tld, https, flag):
        query.set_flag(flag)

        if flag_by_cgi:
            # этот флаг полноценно работает ТОЛЬКО как cgi-параметр
            query.add_params({'no_geo_domain_redirect': 1})
        else:
            # передача его через настоящий механизм флагов, функциональность работает наполовину,
            # отключает редирект, но tld остается кривой
            query.set_flag('no_geo_domain_redirect')

        query.replace_params({'lr': lr})
        query.set_host(tld)
        query.set_https(https)

        resp = self.json_request(query)

        if flag_by_cgi:
            assert resp.data['reqdata']['tld'] == expected_tld
        else:
            assert resp.data['reqdata']['tld'] == tld

    @pytest.mark.ticket('SERP-47795')
    @pytest.mark.parametrize(("https"), [
        True, False
    ])
    def test_tld_by_lr_with_disable_redirects(self, query, https):

        query.headers.set_custom_headers({'X-Yandex-Internal-Flags': base64.b64encode(b'{"disable_redirects":1}')})

        query.replace_params({'lr': 157})
        query.set_host(RU)
        query.set_https(https)

        resp = self.json_request(query)

        assert resp.data['reqdata']['tld'] == BY

    @pytest.mark.ticket('SERP-46391')
    @pytest.mark.parametrize(("tld"), [
        RU,
#        COM,
        COMTR,
        UA,
        KZ,
        BY,
        UZ,
        COMGE,
        FR,
    ])
    def test_rdat_tld(self, query, tld):
        query.set_host(tld)
        resp = self.json_request(query)
        assert resp.data['reqdata']['tld'] == 'ua' if tld == FR else tld

    @pytest.mark.ticket('SERP-47842')
    @pytest.mark.parametrize( ("report", "ua", "distr_pwa", "header_name", "header_exists", "header_value"), [
        (   SEARCH_TOUCH, USER_AGENT_TOUCH,   0, "Service-Worker-Allowed", False, ""  ),
        (   SEARCH_TOUCH, USER_AGENT_TOUCH,   1, "Service-Worker-Allowed", True,  "/search/touch/"  ),
        (   SEARCH,       USER_AGENT_DESKTOP, 1, "Service-Worker-Allowed", False, ""  ),
        (   SEARCH,       USER_AGENT_DESKTOP, 0, "Service-Worker-Allowed", False, ""  ),
        (   SEARCH_PAD,   USER_AGENT_PAD,     1, "Service-Worker-Allowed", False, ""  ),
        (   SEARCH_PAD,   USER_AGENT_PAD,     0, "Service-Worker-Allowed", False, ""  ),
        (   SEARCH_SMART, USER_AGENT_SMART,   1, "Service-Worker-Allowed", False, ""  ),
        (   SEARCH_SMART, USER_AGENT_SMART,   0, "Service-Worker-Allowed", False, ""  )
    ])
    def test_distr_pwa_header(self, query, report, ua, distr_pwa, header_name, header_exists, header_value):
        query.set_url(report + "?text=test")
        query.set_https(True)
        query.set_flag({ "distr_pwa" : distr_pwa })
        query.set_user_agent(ua)

        resp = self.request(query, require_status=200)

        assert (header_name in resp.headers) == header_exists
        if (header_exists):
            assert header_value in resp.headers[header_name]
