# -*- coding: utf-8 -*-
import urllib2
import mock

from mpfs.common.errors import DjfsError
from mpfs.common.static import tags
from mpfs.common.util import from_json
from mpfs.config import settings
from mpfs.core.services.djfs_api_service import DjfsApiLegacyService

from test.parallelly.api.disk.base import DiskApiTestCase


PLATFORM_DOCVIEWER_LINK_ALLOWED_CLIENT_IDS = settings.platform['docviewer']['link_allowed_client_ids']
PLATFORM_DOCVIEWER_FILE_DATA_ALLOWED_CLIENT_IDS = settings.platform['docviewer']['file_data_allowed_client_ids']


class GetPreviewDownloadLinksTestCase(DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    uid = '123123123'
    djfs_resp_content = {
        'items': [{'href': 'http://downloader.dst.yandex.net/link%s' % i} for i in xrange(7)]
    }

    def test_get_download_links(self):
        with mock.patch.object(DjfsApiLegacyService, 'open_url', return_value=(200, self.djfs_resp_content, {})):
            resp = self.client.request(
                'GET',
                'case/disk/searchapp/nostalgia?uid=%(uid)s&locale=%(locale)s&image_size=%(image_size)s' % {
                    'uid': self.uid,
                    'locale': 'ru',
                    'image_size': '40x20',
                }
            )

        assert resp.status_code == 200
        json = from_json(resp.content)
        assert self.djfs_resp_content == json

    def test_200_on_service_error(self):
        with mock.patch.object(DjfsApiLegacyService, 'open_url', side_effect=RuntimeError()):
            resp = self.client.request(
                'GET',
                'case/disk/searchapp/nostalgia?uid=%(uid)s&locale=%(locale)s&image_size=%(image_size)s' % {
                    'uid': self.uid,
                    'locale': 'ru',
                    'image_size': '40x20',
                }
            )

            assert resp.status_code == 200
            json = from_json(resp.content)
            self.assertDictEqual(json, {'empty': True})

    def test_wrong_size_parameter(self):
        with mock.patch.object(DjfsApiLegacyService, 'open_url', return_value=(200, self.djfs_resp_content, {})):
            resp = self.client.request(
                'GET',
                'case/disk/searchapp/nostalgia?uid=%(uid)s&locale=%(locale)s&image_size=%(image_size)s' % {
                    'uid': self.uid,
                    'locale': 'ru',
                    'image_size': '400',
                }
            )

        assert resp.status_code == 400
        json = from_json(resp.content)
        assert json['error'] == 'FieldValidationError'

    def test_unsupported_locale(self):
        with mock.patch.object(DjfsApiLegacyService, 'open_url', return_value=(200, self.djfs_resp_content, {})):
            resp = self.client.request(
                'GET',
                'case/disk/searchapp/nostalgia?uid=%(uid)s&locale=%(locale)s&image_size=%(image_size)s' % {
                    'uid': self.uid,
                    'locale': 'cz',
                    'image_size': '40x20',
                }
            )

        assert resp.status_code == 400
        json = from_json(resp.content)
        assert json['error'] == 'FieldValidationError'

    def test_empty_block_for_searchapp_on_404(self):
        with mock.patch.object(DjfsApiLegacyService, 'open_url', return_value=(404, {}, {})):
            resp = self.client.request(
                'GET',
                'case/disk/searchapp/nostalgia?uid=%(uid)s&locale=%(locale)s&image_size=%(image_size)s' % {
                    'uid': self.uid,
                    'locale': 'ru',
                    'image_size': '40x20',
                }
            )

        assert resp.status_code == 200
        json = from_json(resp.content)
        self.assertDictEqual(json, {'empty': True})

    def test_proxy_code_for_web_on_404(self):
        with mock.patch.object(DjfsApiLegacyService, 'open_url', side_effect=DjfsError(data={'text': '', 'code': 404})):
            resp = self.client.request(
                'GET',
                'case/disk/searchapp/nostalgia/blocks?uid=%(uid)s&locale=%(locale)s' % {
                    'uid': self.uid,
                    'locale': 'ru',
                }
            )
        assert resp.status_code == 404
