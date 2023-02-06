# -*- coding: utf-8 -*-

import os
import pytest

from report.functional.web.base import BaseFuncTest
from report.const import *

TEXT_BY_TLD = {
    RU: 'Петров',
    COMTR: 'Zafer Sever'
}

PEOPLE_URL = {
    PEOPLE_PAD_SEARCH: '/people/search/pad',
    PAD: '/people/search/pad',
    DESKTOP: '/people/search',
    TOUCH: '/people/search/touch',
}

WEB_URL = {
    PAD: '/search/pad/',
    DESKTOP: '/search/'
}

DEPRECATED_URL = {
    PEOPLE_PAD_SEARCH: '/people/pad/search',
    PAD: '/people/pad',
    DESKTOP: '/people',
    TOUCH: '/people/touch',
}


@pytest.mark.skip(reason="RUNTIMETESTS-75")
class TestPeople(BaseFuncTest):
    """
    Поиск по людям
    """

    def set_adapter(self, query):
        query.set_internal()  # FIXME(mvel): REPORTINFRA-276
        # for http_adapter (REPORTINFRA-269)
        query.set_timeouts()
        query.set_http_adapter()

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    # @pytest.mark.skipif(True, reason="SERP-61605")
    def test_people_serp(self, query):
        query.set_url('/people/search')
        self.set_adapter(query)
        self.json_test(query)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-36862')
    def test_people_base_html(self, query):
        """
        SERP-36862 - Не показываются стили на выдаче ППЛ
        Проверяем, что был первый запрос в верстку и приехали стили и шапка
        (как минимум есть html тег)
        при включенном app_host
        """
        query.remove_params('text')
        query.set_url('/people')

        query.set_internal()  # FIXME(mvel): REPORTINFRA-276
        # for http_adapter (REPORTINFRA-269)
        query.set_timeouts()
        query.set_http_adapter()

        resp = self.request(query)
        assert '<!DOCTYPE html><html' in resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("tld", TEXT_BY_TLD.keys())
    @pytest.mark.parametrize("query_type", PEOPLE_URL.keys())
    def test_people_search(self, tld, query, query_type):
        """
        Доступность из внутренней сети
        Для всех все работает
        """
        query.set_host(tld)
        query.set_internal()
        query.set_url(PEOPLE_URL[query_type])
        query.set_params({'text': TEXT_BY_TLD[tld]})
        query.set_user_agent(USERAGENT_BY_TYPE[query_type])

        # for http_adapter (REPORTINFRA-269)
        query.set_timeouts()
        query.set_http_adapter()

        resp = self.request(query)

        assert TEXT_BY_TLD[tld] in resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("query_type", PEOPLE_URL.keys())
    def test_people_search_ru_external(self, query, query_type, tld=RU):
        """
        Доступность из внешней сети
        Для ru Все работает
        """
        query.set_host(tld)
        query.set_url(PEOPLE_URL[query_type])
        query.set_params({'text': TEXT_BY_TLD[tld]})
        query.set_user_agent(USERAGENT_BY_TYPE[query_type])

        query.set_internal()  # FIXME(mvel): REPORTINFRA-276
        # for http_adapter (REPORTINFRA-269)
        query.set_timeouts()
        query.set_http_adapter()

        resp = self.request(query)

        assert TEXT_BY_TLD[tld] in resp.content

    @pytest.mark.parametrize("query_type", [
        pytest.mark.xfail(
            PAD, reason='SERP-30501'),
        pytest.mark.xfail(
            TOUCH, reason='SERP-30501'),
        pytest.mark.xfail(
            DESKTOP, reason='REPORTINFRA-276')
    ])
    def test_people_search_com_tr_external(self, query, query_type, tld=COMTR):
        """
        Доступность из внешней сети
        Редиректы на большой поиск
        TODO: pad редиректит на /search, а не /search/pad
        """
        query.set_host(tld)
        query.set_url(PEOPLE_URL[query_type])
        query.set_params({'text': TEXT_BY_TLD[tld]})
        query.set_user_agent(USERAGENT_BY_TYPE[query_type])
        self.set_adapter(query)

        resp = self.request(query, require_status=302)

        assert resp.headers['location'][0].lower().startswith(WEB_URL[query_type] + '?text=zafer')

    @pytest.mark.xfail(reason='REPORTINFRA-276')
    @pytest.mark.parametrize("query_type", PEOPLE_URL.keys())
    def test_people_search_com_tr_external_empty_query(self, query, query_type, tld=COMTR):
        """
        Доступность из внешней сети
        Если пустые запросы, то редиректим на морду
        """

        query.remove_params('text')
        query.set_host(tld)
        query.set_url(PEOPLE_URL[query_type])
        query.set_user_agent(USERAGENT_BY_TYPE[query_type])
        self.set_adapter(query)

        resp = self.request(query, require_status=302)

        assert resp.headers['location'][0].startswith('/?')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("tld", TEXT_BY_TLD.keys())
    @pytest.mark.parametrize("query_type", PEOPLE_URL.keys())
    def test_people_deprecated_internal(self, query, query_type, tld):
        """
        Внутренняя сеть
        Вечные редиректы на соответствующий (новый) урл
        """

        query.set_host(tld)
        query.set_internal()
        query.set_url(DEPRECATED_URL[query_type])
        query.set_params({'text': TEXT_BY_TLD[tld]})
        query.set_user_agent(USERAGENT_BY_TYPE[query_type])
        self.set_adapter(query)

        resp = self.request(query, require_status=302)

        assert resp.headers['location'][0].startswith(PEOPLE_URL[query_type])

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("tld", TEXT_BY_TLD.keys())
    @pytest.mark.parametrize("query_type", PEOPLE_URL.keys())
    def test_people_deprecated(self, query, query_type, tld):
        query.set_host(tld)
        query.set_url(DEPRECATED_URL[query_type])
        query.set_params({'text': TEXT_BY_TLD[tld]})
        query.set_user_agent(USERAGENT_BY_TYPE[query_type])
        self.set_adapter(query)
        if tld == 'com.tr':
            pytest.xfail('REPORTINFRA-276')

        resp = self.request(query, require_status=302)

        expect = '/search/?' if tld == COMTR else PEOPLE_URL[query_type]
        assert resp.headers['location'][0].startswith(expect)


    @pytest.mark.skipif(os.environ.get('BETA_HOST') is not None, reason="Невозможно передать домен people.yandex.by в Яппи")
    @pytest.mark.parametrize("tld", [RU, COMTR, UA, BY, KZ])
    def test_people_redirect(self, query, tld):
        """
        SERP-37269 редиректы people.yandex.ru
        people.yandex.(ru|by|kz|ua) должен редиректить на
        yandex.(ru|by|kz|ua)/people
        """
        query.set_host('people.yandex.' + tld)
        self.set_adapter(query)
        resp = self.request(query, require_status=301)
        assert resp.headers['location'][0].startswith('https://yandex.' + tld + '/people')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("tld", TEXT_BY_TLD.keys())
    def test_people_search_pad(self, query, tld):
        query.set_host(tld)
        query.set_internal()
        query.set_url('/people/search/pad')
        query.set_params({'text': 'Petrov'})
        query.set_user_agent(USER_AGENT_PAD)

        # for http_adapter (REPORTINFRA-269)
        query.set_timeouts()
        query.set_http_adapter()

        resp = self.request(query)

        assert "Petrov" in resp.content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("tld", TEXT_BY_TLD.keys())
    def test_people_pad_internal(self, query, tld):
        query.set_internal()
        query.set_url('/people/pad')
        query.set_params({'text': TEXT_BY_TLD[tld]})
        query.set_user_agent(USER_AGENT_PAD)
        query.set_host(tld)
        self.set_adapter(query)
        resp = self.request(query, require_status=302)

        assert resp.headers['location'][0].startswith('/people/search/pad?')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("path", ["/people/search", "/people/search/"])
    @pytest.mark.parametrize("tld", TEXT_BY_TLD.keys())
    def test_people_no_redirect(self, query, tld, path):
        query.set_host(tld)
        query.set_internal()
        query.set_url(path)
        query.set_params({'text': 'Petrov'})
        self.set_adapter(query)

        self.request(query)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("tld, expect", [
        ('ru', '/people/search/pad?'),
        ('com.tr', '/search/?'),
    ])
    def test_people_pad_external(self, query, tld, expect):
        query.set_url('/people/pad')
        query.set_params({'text': TEXT_BY_TLD[tld]})
        query.set_user_agent(USER_AGENT_PAD)
        query.set_host(tld)
        self.set_adapter(query)
        if tld == 'com.tr':
            pytest.xfail('REPORTINFRA-276')

        resp = self.request(query, require_status=302)

        assert resp.headers['location'][0].startswith(expect)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('SERP-43127')
    @pytest.mark.parametrize("tld", TEXT_BY_TLD.keys())
    @pytest.mark.parametrize("query_type", PEOPLE_URL.keys())
    # @pytest.mark.skipif(True, reason="SERP-61605")
    def test_people_snippets(self, query, tld, query_type):
        query.set_host(tld)
        query.set_url(PEOPLE_URL[query_type])
        query.set_params({'text': TEXT_BY_TLD[tld]})
        query.set_user_agent(USERAGENT_BY_TYPE[query_type])
        self.set_adapter(query)

        resp = self.json_request(query)

        for doc in resp.data['searchdata']['docs']:
            full = doc['snippets']['full']
            if 'data' in full and 'Snippet' in full['data']:
                snippet = full['data']['Snippet']
                assert snippet['Brief'] or snippet['SerpBrief']
            else:
                assert full['type'] == 'social_snippet'
