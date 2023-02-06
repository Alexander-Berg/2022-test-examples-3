# -*- coding: utf-8 -*-

import re
import urllib

import os
import pytest

from report.functional.web.base import BaseFuncTest
from report.const import *
from urlparse import urlparse


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestLogs(BaseFuncTest):

    @classmethod
    @pytest.fixture(scope='class', autouse=True)
    def class_setup(cls, setup):
        cls.RETRIES = 1

    @pytest.fixture(scope='function')
    def sdch_dict(self, query):
        if not hasattr(self, '_sdch_dict'):
            query.set_flags({ 'enable_sdch': 1, 'output_in_app_host': 0, 'sdch_from_sandbox': 1 })
            query.headers.set_custom_headers({"Accept-Encoding": 'sdch'})
            query.set_user_agent(USER_AGENT_TOUCH)
            query.set_url(SEARCH_TOUCH)
            resp = self.json_request(query)

            d = resp.headers['Get-Dictionary'][0].replace('/search/sdch/', '')
            d = d.replace('.dict', '')
            self._sdch_dict = d
        return self._sdch_dict

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-36528')
    def test_no_content_post(self, query):
        """
        SERP-36528 - Запрос без тела, но с ненулевым content-length неправильно записывается в лог
        Задаем запрос, ждем таймаута,
        Смотрим в лог и проверяем, что там не 200
        """
        query.set_method('POST')
        query.headers.set_content_length(50)
        query.set_url(SEARCH_XML)

        resp = self.request(query, require_response=False)

        access = resp.access_log()
        assert access['url'].startswith(SEARCH_XML + '?text=' + urllib.quote(TEXT.encode('utf-8')).replace('%20', '+'))
        assert access['method'] == 'POST'
        assert access['status'] == '408'
        assert access['redirect'] == "-"

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-38781')
    def test_redirect_by_report(self, query):
        query.set_method('GET')
        query.path.set('/search/wrong')

        resp = self.request(query, require_response=False)

        access = resp.access_log()
        assert access['url'].startswith('/search/wrong')
        assert access['method'] == 'GET'
        assert access['status'] == '302'
        assert access['redirect'].startswith('/search/?')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-38781')
    def test_redirect_by_apache(self, query):
        query.set_method('GET')
        query.path.set('/search')

        resp = self.request(query, require_response=False)

        access = resp.access_log()
        assert access['url'].startswith('/search')
        assert access['method'] == 'GET'
        assert access['status'] == '302'
        assert access['redirect'].startswith("/search/?")

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_external_search_props(self, query):
        """
        SERP-36915 / SERP-35887 Closed Внешние search properties в perl-gateway
        Проверяем, что значение параметра search_props в perl-gateway пишется в reqans_log
        """
        query.set_query_type(GATEWAY_TOUCH)
        query.add_params({'search_props': 'test=12345,test2=23456'})

        resp = self.request(query)

        reqans = resp.reqans_log()
        assert 'GATEWAY:test=12345,test2=23456' in reqans['req']['search_props']


    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('WEBREPORT-401')
    @pytest.mark.parametrize("query_type", [
        #(DESKTOP),
        #(SMART),
        #(PAD),
        (GATEWAY)
    ])
    def test_testid_to_reqans(self, query, query_type):
        """
        WEBREPORT-401: Написать тест на запись reqans-log для верстки под флагами
        Проверяем, что test-id попадает в reqans лог в нужные места.
        Косвенно этот тест проверяет, что данные из app_host контекстов доезжают до reqans лога
        https://ab.yandex-team.ru/testid/298 - постоянно включенный эксперимент
        """
        query.set_method('GET')
        #query.path.set('/search/')
        query.set_query_type(query_type)
        query.headers['X-Yandex-Internal-Request'] = 1
        query.headers.set_custom_headers({
            "X-Yandex-ExpConfigVersion": "14721",
            "X-Yandex-ExpBoxes": "298,0,-1",
            "X-Yandex-ExpFlags": "W3siSEFORExFUiI6ICJSRVBPUlQiLCAiVEVTVElEIjogWyIyOTgiXSwgIkNPTlRFWFQiOiB7Ik1BSU4iOiB7IlVQUEVSIjp7InJlYXJyIjpbXX19fX1d"
        })

        query.add_params({ "text": "putin" })

        resp = self.request(query)

        reqans = resp.reqans_log()

        assert 'test-ids=298' in reqans['req']['search_props'].split(',')
        assert '298,0,-1' in reqans['req']['test-buckets'].split(';')
        assert '298' in reqans['req']['test-ids'].split(',')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-71165')
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_search_props_report_client_http_version(self, query):
        query.headers['X-Yandex-HTTP-Version'] = 'h2'
        resp = self.request(query)

        reqans = resp.reqans_log()
        assert 'client_http_version=h2' in reqans['req']['search_props']

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_no_empty_reqans(self, query):
        """
        SERP-36643 - Некорректные записи в реканс логе
        На любом запросе с неответами Верхнего в записях появлялось url=
        Проверка на то, что в reqans ничего нет при неответе noapache
        """
        resp = self.request(query)

        reqans = resp.reqans_log()
        urls = reqans['url']
        assert len(urls) == 0

    def stype_in_reqans_test(self, query, query_type, params, result, reqans_method):
        """
        SERP-36583 - значения stype в reqans_log
        """

        query.set_query_type(query_type)
        query.add_params(params)

        if self.is_blender_reqans_log(query):
            pytest.skip("Skip check reqans log, because blender_reqans_log=1")

        resp = self.request(query)

        reqans = getattr(resp, reqans_method)()
        assert reqans['req']['stype'] == result

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("query_type, result, params", [
        (DESKTOP, 'www', None),
        (SMART, 'www-smart', None),
        (PAD, 'www-tablet', None),
        (GATEWAY, 'gateway', None),
        pytest.mark.xfail(
            (SITESEARCH, 'sitesearch', {
                'text': TEXT, 'web': 0, 'l10n': 'ru', 'frame': 1, 'v': '2.0', 'searchid': 2010784,
                'topdoc': 'xdm_e%3Dhttp%253A%252F%252Fwww.sp-piter.ru%26xdm_c%3Ddefault5926%26xdm_p%3D1'
            }),
            reason='SERP-67197'
        ),
        pytest.mark.xfail(
            (PEOPLE, 'people', {'text': TEXT}),
            reason='SERP-61628'
        ),
    ])
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_stype_in_reqans(self, query, query_type, result, params):
        """
        SERP-36583 - значения stype в reqans_log
        """

        if params is None:
            params = {'text': TEXT}

        self.stype_in_reqans_test(query, query_type, params, result, 'reqans_log')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-42224')
    def test_abuse_link(self, query):
        resp = self.json_test(query)
        data = resp.data
        abuse_url = data["searchdata"]["abuse_link"]
        reqid = data["reqdata"]["reqid"]

        parsed_url = urlparse(abuse_url)

        assert parsed_url.path == '/search/abuse', 'Wrong abuse url path'

        query.set_url(parsed_url.path + '?' + parsed_url.query)
        query.set_method('POST')

        post_data = 'reqid='+ reqid
        post_data += '&lr=213'
        post_data += '&docid=docid1234'
        post_data += '&type=type1234'
        post_data += '&query=text'
        post_data += '&url-complaint=url-complaint1234'
        post_data += '&service=service1234'
        post_data += '&complaint-query=complaint-query1234'
        post_data += '&reason-complaint=reason-complaint1234'
        post_data += '&shows-urls=shows-urls1234'
        post_data += '&shows-related-queries=shows-related-queries1234'
        post_data += '&related-query-complaint=related-query-complaint1234'
        post_data += '&shows-related-images=shows-related-images1234'
        post_data += '&related-image-complaint=related-image-complaint1234'
        query.set_post_params(post_data)

        resp = self.request(query)

        abuse = SearchAbuseLog().get(resp.reqid)
        assert abuse[0] == 'tskv', 'The log line should start with tskv'
        assert abuse[1] == 'tskv_format=abuse_log', 'Second element in the log line should be tskv_format'

        allowed_args_and_values = post_data.split('&')
        allowed_args = []
        for l in allowed_args_and_values:
            allowed_args.append(str(l.split('=')[0]))

        mandatory_args = [
            'tskv',
            'tskv_format',
            'time-complaint',
            'ip',
            'uid',
            'yandex-login',
            'user-agent',
            'internal-network',
            'fuid',
            'serpid'
        ]

        args = set(allowed_args + mandatory_args)
        logs_args = []
        for l in abuse:
            logs_args.append(str(l.split('=')[0]))
        logs_args = set(logs_args)
        assert args.issubset(logs_args), 'One of allawed or mandatory argument is not in the logs'
        assert len(args) == len(logs_args), 'There is more arguments in logs than should be'

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-47680')
    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_msp_in_reqanslog(self, query):
        query.replace_params({'text': 'однокласники'})

        if self.is_blender_reqans_log(query):
            pytest.skip("Skip check reqans log, because blender_reqans_log=1")

        resp = self.request(query)

        reqans = resp.reqans_log()
        msp = reqans['req']['msp']

        assert re.match(r'1:Misspell:\d+:[^:]+:report:0', msp) != None

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-54274')
    @pytest.mark.ticket('SERP-63007')
    @pytest.mark.parametrize('path', (
        '/searchapp', '/searchapp/',
        '/searchapp/searchapp', '/searchapp/searchapp/'
    ))
    @pytest.mark.parametrize('method', (
        'GET', 'POST'
    ))
    def test_searchapp_logs(self, query, path, method):
        query.set_method(method)
        query.path.set(path)
        query.set_params({'text': 'sport', 'lr': 213})

        resp = self.request(query, require_response=False)

        access = resp.access_log()
        assert access['url'].startswith('/search/touch/?service=www.yandex&ui=webmobileapp.yandex&text=sport&lr=213')
        assert access['method'] == method
        assert access['status'] == '200'

