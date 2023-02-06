# -*- coding: utf-8 -*-
import urlparse

import time

from datetime import timedelta

from mpfs.common.static import tags
from mpfs.common.util import from_json
from mpfs.config import settings
from mpfs.platform.v1.case.exceptions import DocviewerExpiredUrlError, DocviewerInvalidFileUrlError, \
    DocviewerSignatureMismatchError, DocviewerYandexuidRequiredError
from mpfs.platform.v1.case.handlers import DocviewerYabrowserUrlService

from test.parallelly.api.disk.base import DiskApiTestCase

PLATFORM_DOCVIEWER_LINK_ALLOWED_CLIENT_IDS = settings.platform['docviewer']['link_allowed_client_ids']
PLATFORM_DOCVIEWER_FILE_DATA_ALLOWED_CLIENT_IDS = settings.platform['docviewer']['file_data_allowed_client_ids']


class DocviewerYabrowserTestCase(DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    url_service = DocviewerYabrowserUrlService()

    def test_roundtrip(self):
        with self.specified_client(id=PLATFORM_DOCVIEWER_LINK_ALLOWED_CLIENT_IDS[0]):
            resp = self.client.request('GET', 'case/docviewer/ya-browser/docviewer-link?mds_key=1234&file_name=док.doc',
                                       headers={'cookie': 'yandexuid=222'})

        assert resp.status_code == 200
        json = from_json(resp.content)
        assert json['method'] == 'GET'
        assert json['templated'] is False

        host, params = json['href'].split('?', 1)
        file_url = urlparse.parse_qs(params)['url'][0]

        with self.specified_client(id=PLATFORM_DOCVIEWER_FILE_DATA_ALLOWED_CLIENT_IDS[0]):
            resp = self.client.request('GET', 'case/docviewer/ya-browser/file-data?file_url=' + file_url,
                                       headers={'cookie': 'yandexuid=222'})

        assert resp.status_code == 200
        json = from_json(resp.content)
        assert json['mds_key'] == '1234'

    def test_expired_link_returns_410(self):
        encrypted = self.url_service.encrypt_mds_key_with_time('1234', time.time() - timedelta(days=1, seconds=1).total_seconds())
        signature = self.url_service.sign_with_yandexuid(encrypted, '222')
        file_url = self.url_service.format_yabrowser_url(encrypted, signature)

        with self.specified_client(id=PLATFORM_DOCVIEWER_FILE_DATA_ALLOWED_CLIENT_IDS[0]):
            resp = self.client.request('GET', 'case/docviewer/ya-browser/file-data?file_url=' + file_url,
                                       headers={'cookie': 'yandexuid=222'})

        assert resp.status_code == 410
        json = from_json(resp.content)
        assert json['error'] == DocviewerExpiredUrlError.__name__

    def test_signature_mismatch_returns_400(self):
        encrypted = self.url_service.encrypt_mds_key_with_time('1234')
        signature = self.url_service.sign_with_yandexuid(encrypted, '111')
        file_url = self.url_service.format_yabrowser_url(encrypted, signature)

        with self.specified_client(id=PLATFORM_DOCVIEWER_FILE_DATA_ALLOWED_CLIENT_IDS[0]):
            resp = self.client.request('GET', 'case/docviewer/ya-browser/file-data?file_url=' + file_url,
                                       headers={'cookie': 'yandexuid=222'})

        assert resp.status_code == 400
        json = from_json(resp.content)
        assert json['error'] == DocviewerSignatureMismatchError.__name__

    def test_invalid_yabrowser_url_returns_400(self):
        encrypted = self.url_service.encrypt_mds_key_with_time('1234')
        signature = self.url_service.sign_with_yandexuid(encrypted, '222')
        file_url = self.url_service.format_yabrowser_url(encrypted, signature)
        file_url = file_url.replace('browser', 'bowser')

        with self.specified_client(id=PLATFORM_DOCVIEWER_FILE_DATA_ALLOWED_CLIENT_IDS[0]):
            resp = self.client.request('GET', 'case/docviewer/ya-browser/file-data?file_url=' + file_url,
                                       headers={'cookie': 'yandexuid=222'})

        assert resp.status_code == 400
        json = from_json(resp.content)
        assert json['error'] == DocviewerInvalidFileUrlError.__name__

    def test_missing_yandexuid_returns_400(self):
        encrypted = self.url_service.encrypt_mds_key_with_time('1234')
        signature = self.url_service.sign_with_yandexuid(encrypted, '222')
        file_url = self.url_service.format_yabrowser_url(encrypted, signature)
        file_url = file_url.replace('browser', 'bowser')

        with self.specified_client(id=PLATFORM_DOCVIEWER_FILE_DATA_ALLOWED_CLIENT_IDS[0]):
            resp = self.client.request('GET', 'case/docviewer/ya-browser/file-data?file_url=' + file_url,
                                       headers={'cookie': 'notyandexuid=222'})

        assert resp.status_code == 400
        json = from_json(resp.content)
        assert json['error'] == DocviewerYandexuidRequiredError.__name__
