# -*- coding: utf-8 -*-

import re
import json
import urllib

import os
import pytest

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestSettings(BaseFuncTest):
    @pytest.mark.unstable
    def test_numdoc(self, query):
        """
        Выставляем в куке настройку поиска по кол-ву документов - 30
        Проверяем, что за вычитом колдунщиков, вернулось правильное кол-во
        """
        n = 30
        query.headers.cookie.yp.set_sp('nd:' + str(n))
        resp = self.json_request(query)

        docs = self.get_docs(resp)

        assert len(docs) == n

    def test_kbd_default(self, query):
        """
        SERP-33790 - Выключить по умолчанию настройку клавиатурного управления
        Задаем запрос без кук - проверяем, что настройка в 0
        """
        resp = self.json_request(query)
        assert resp.data['reqdata']['prefs']['kbd'] == 0


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestSettingsTune(BaseFuncTest):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-32990', 'SERP-33944', 'SERP-33945')
    @pytest.mark.parametrize("login", [
        0,
        1,
    ])
    def test_tune_save(self, query, login):
        """
        SERP-33945
        сохранение настроек
        """


        query.set_method('POST')
        query.set_url('/search/customize')
        query.set_yandexuid()

        post_data = '&language=be&retpath=&t=&target=_blank&numdoc=20&banners=1&favicons=1&ajx=1&langs=any&family=1&noreask=0&person=1&suggest_personal_nav=1&save=xxx'
        query.set_post_params(post_data)

        # логин
        if login:
            query.set_auth()
        else:
            query.set_noauth()

        resp = self.request(query, require_status=302, source='TUNE')
#        assert resp.headers['location'][0].startswith('https://pass.yandex.ru/syncookie?')

        t = resp.source
        assert t.headers.get_one('host') == 'tune-internal.yandex.ru'

        # /api/lang/v1.1/save.xml?intl=be&sk=u8c602b271e6ad999b7202fc805b51609&json=1
        assert t.method == 'GET'
        assert t.path == '/api/lang/v1.1/save.xml'
        assert t.args.get('intl') == ['be']

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-32990', 'SERP-33944','SERP-33945')
    @pytest.mark.parametrize("login", [
        0,
        1,
    ])
    def test_tune_reset(self, query, login):
        """
        SERP-33945
        сброс настроек
        """

        query.set_method('POST')
        query.set_url('/search/customize')
        query.set_host('by')
        query.set_yandexuid()

        post_data = '&language=be&retpath=&t=&target=_blank&numdoc=20&banners=1&favicons=1&ajx=1&langs=any&family=1&noreask=0&person=1&suggest_personal_nav=1&reset_prefs=on'
        query.set_post_params(post_data)

        # логин
        if login:
            query.set_auth()
        else:
            query.set_noauth()

        resp = self.request(query, require_status=302, source='TUNE')

#        assert resp.headers['location'][0].startswith('https://pass.yandex.ru/syncookie?')

        t = resp.source
        assert t.headers.get_one('host') == 'tune-internal.yandex.by'

        # GET /api/lang/v1.1/save.xml?intl=ru&sk=ucd366f4c6c3460190e143511b2749127&json=1 HTTP/1.1
        assert t.method == 'GET'
        assert t.path == '/api/lang/v1.1/save.xml'
        assert t.args.get('intl') == ['ru']
