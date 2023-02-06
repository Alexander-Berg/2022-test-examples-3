# -*- coding: utf-8 -*-

import os
import json
import urlparse
import pytest
import re

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestXMLAuth(BaseFuncTest):
    def test_internal_auth(self, query):
        query.set_internal()
        query.set_url("/search/xml")

        resp = self.request(query, sources=['XML_AUTH', 'XML_AUTH_SLOW'])

        assert not resp.sources['XML_AUTH']
        assert not resp.sources['XML_AUTH_SLOW']

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_user_limits(self, query, static_file_content):
        query.set_url("/search/xml")
        query.add_params({'action': 'limits-info'})
        query.headers.set_forward_for_y('85.26.168.0')

        resp = self.request(query, source=('XML_AUTH_SLOW', static_file_content('user_limits.xml')))

        assert resp.source
        # TODO content

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-38005', 'SERP-36890')
    def test_rps_limit(self, query, static_file_content):
        query.set_url("/search/xml")
        query.headers.set_forward_for_y('85.26.168.0')

        resp = self.request(query, source=('XML_AUTH', static_file_content('user_auth.xml')))

        assert resp.source
        assert '<error code="55">' in resp.content


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestXML(BaseFuncTest):
    def test_https_xml_get_internal(self, query):
        """
        SERP-33523 Включить флаг enable_https_xmlsearch на внешнюю сеть
        Для GET из внутренней сети отдаем 200
        """
        query.set_internal()
        query.set_url(SEARCH_XML)
        query.set_https(False)
        self.request(query)

    def test_https_xml_post_internal(self, query):
        """
        SERP-33523 Включить флаг enable_https_xmlsearch на внешнюю сеть
        Для POST из внутренней сети отдаем 200
        """
        query.set_internal()
        query.set_method('POST')
        query.set_post_params('SOMEDATA')
        query.set_https(False)
        query.set_url(SEARCH_XML)
        self.request(query)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_https_xml_get_external(self, query):
        """
        SERP-33523 Включить флаг enable_https_xmlsearch на внешнюю сеть
        Для GET из внешней сети отдаем 302
        """
        query.set_url(SEARCH_XML)
        query.set_https(False)

        resp = self.request(query, require_status=302)

        assert resp.headers['location'][0].startswith('https://yandex.ru/search/xml')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_https_xml_post_external(self, query):
        """
        SERP-33523 Включить флаг enable_https_xmlsearch на внешнюю сеть
        Для POST из внешней сети отдаем 307
        """
        query.set_method('POST')
        query.set_post_params('SOMEDATA')
        query.set_url(SEARCH_XML)
        query.set_https(False)

        resp = self.request(query, require_status=307)

        assert 'location' in resp.headers
        assert resp.headers['location'][0].startswith('https://yandex.ru/search/xml')

    def test_https_xml_slash(self, query):
        """
        SERP-36071 Выбор HTTPS протокола работает некорректно для xmlsearch
        Некорректный редирект для https и запроса https://yandex.by/search/xml/? на 404
        """
        query.set_method('POST')
        query.set_post_params('SOMEDATA')
        query.set_url(SEARCH_XML + '/')
        self.request(query)

    @pytest.mark.skipif(True, reason="XML работает по невывернутой схеме")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_xml_grouping(self, query):
        query.set_internal()
        query.set_url(SEARCH_XML + '/')
        query.add_params({ 'groupby': 'attr=d.mode=1.groups-on-page=38' })
        self.check_grouping(self.source_params(query, 'WEB', 'g', sources=['APP_HOST']), re.compile(r'^1\.d\.38\.1'))

    @pytest.mark.skipif(True, reason="XML работает по невывернутой схеме")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_xml_flat_grouping(self, query):
        """
        SERP-58385: Выровнять gta и параметры группировок в XML-ном репорте
        """
        query.set_internal()
        query.set_url(SEARCH_XML + '/')
        query.add_params({ 'xml_flat_grouping': 1, 'groupby': 'attr=d' })
        self.check_grouping(self.source_params(query, 'WEB', 'g', sources=['APP_HOST']), re.compile(r'^0\.\.10\.1.*'))


