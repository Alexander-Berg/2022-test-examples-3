# -*- coding: utf-8 -*-
import mock
import calendar

from hamcrest import assert_that, contains_inanyorder
from dateutil import relativedelta, tz, parser
from datetime import datetime

from test.parallelly.api.disk.base import DiskApiTestCase
from test.base import CommonDiskTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin
from mpfs.common.static import tags
from mpfs.common.util import from_json
from mpfs.config import settings
from mpfs.core.services.lenta_loader_service import LentaLoaderService

MOBILE_APP_ID = settings.platform['mobile_apps_ids'][0]


class GetAutouploadLentaBlockUrlHandlerTestCase(CommonDiskTestCase, UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    url = 'case/lenta/autoupload_block_url'

    def test_common(self):
        self.upload_file(self.uid, '/photostream/1.jpg', file_data={'size': 10})
        resp = self.client.get(self.url, uid=self.uid, query={'tld': 'ru'})
        assert resp.status_code == 200
        json_content = from_json(resp.content)

        assert_that(json_content.keys(), contains_inanyorder('date', 'total', 'block_url'))
        assert json_content['block_url'] == u'https://disk.yandex.ru/search-app'
        assert json_content['total'] == 1
        date = parser.parse(json_content['date'])
        max_dt = datetime.now(tz.tzlocal())
        # предполагается, что ручка отвечает быстрее 10 минут =)
        min_dt = max_dt - relativedelta.relativedelta(minutes=10)
        assert min_dt < date < max_dt

    def test_request_non_init_user(self):
        resp = self.client.get(self.url, uid='1234567890', query={'tld': 'ru'})
        assert resp.status_code == 404

    def test_no_tld(self):
        resp = self.client.get(self.url, uid=self.uid)
        assert resp.status_code == 400

    def test_unsupported_tld(self):
        self.upload_file(self.uid, '/photostream/1.jpg', file_data={'size': 10})
        resp = self.client.get(self.url, uid=self.uid, query={'tld': 'shaitan.tld'})
        assert resp.status_code == 400
        json_content = from_json(resp.content)
        assert 'description' in json_content
        assert '"shaitan.tld"' in json_content['description']
        assert 'message' in json_content
        assert json_content['error'] == 'DiskUnsupportedTLDError'
        assert 'date' in json_content
        assert 'total' in json_content
        assert json_content['block_url'] == 'https://disk.yandex.com/search-app'

    def test_no_photostream_folder(self):
        resp = self.client.get(self.url, uid=self.uid, query={'tld': 'ru'})
        assert resp.status_code == 404

    def test_empty_photostream_folder(self):
        self.json_ok('mksysdir', {'uid': self.uid, 'type': 'photostream'})
        resp = self.client.get(self.url, uid=self.uid, query={'tld': 'ru'})
        assert resp.status_code == 404

    def test_begin_date(self):
        now_dt = datetime.now(tz.tzlocal())
        two_days_before_dt = now_dt - relativedelta.relativedelta(days=2)
        two_days_before_ts = calendar.timegm(two_days_before_dt.utctimetuple())
        with mock.patch('time.time', return_value=two_days_before_ts):
            self.upload_file(self.uid, '/photostream/1.jpg', file_data={'size': 10})
        self.upload_file(self.uid, '/photostream/2.jpg', file_data={'size': 11})

        resp = self.client.get(self.url, uid=self.uid, query={'tld': 'ru'})
        assert resp.status_code == 200
        json_content = from_json(resp.content)
        assert json_content['total'] == 1

        three_days_before_dt = now_dt - relativedelta.relativedelta(days=3)
        resp = self.client.get(self.url, query={'begin_date': three_days_before_dt.isoformat(), 'tld': 'ru'}, uid=self.uid)
        assert resp.status_code == 200
        json_content = from_json(resp.content)
        assert json_content['total'] == 2

    def test_crop_max_date(self):
        now_dt = datetime.now(tz.tzlocal())
        two_months_before_dt = now_dt - relativedelta.relativedelta(months=2)
        two_months_before_ts = calendar.timegm(two_months_before_dt.utctimetuple())
        with mock.patch('time.time', return_value=two_months_before_ts):
            self.upload_file(self.uid, '/photostream/1.jpg', file_data={'size': 10})
        self.upload_file(self.uid, '/photostream/2.jpg', file_data={'size': 11})

        resp = self.client.get(self.url, uid=self.uid, query={'tld': 'ru'})
        assert resp.status_code == 200
        json_content = from_json(resp.content)
        assert json_content['total'] == 1

        # мы в любом случае обрезаем begin_date до 1-го месяца
        three_months_before_dt = now_dt - relativedelta.relativedelta(months=3)
        resp = self.client.get(self.url, query={'begin_date': three_months_before_dt.isoformat(), 'tld': 'ru'}, uid=self.uid)
        assert resp.status_code == 200
        json_content = from_json(resp.content)
        assert json_content['total'] == 1

    def test_sending_request_with_photostream_path_to_mpfs(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.upload_file(self.uid, '/photostream/1.jpg', file_data={'size': 10})

        with mock.patch('mpfs.core.base.lenta_block_list') as mpfs_mock:
            self.client.get(self.url, uid=self.uid, query={'tld': 'ru'})
        assert '/photostream' == mpfs_mock.call_args[0][0].path


class ExteranlGetAutouploadLentaBlockUrlHandlerTestCase(CommonDiskTestCase, UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/lenta/autoupload_block_url'

    def test_allow(self):
        with self.specified_client(scopes=['yadisk:all']):
            resp = self.client.get(self.url, uid=self.uid, query={'tld': 'ru'})
            assert resp.status_code == 404

    def test_deny(self):
        resp = self.client.get(self.url, uid=self.uid, query={'tld': 'ru'})
        assert resp.status_code == 401
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get(self.url, uid=self.uid, query={'tld': 'ru'})
            assert resp.status_code == 403


class LentaSelectionBlockInfoHandler(CommonDiskTestCase, UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/lenta/selection_block_info'
    block_body = '''{
        "id": "000000155540722915400010000001555407229211",
        "type": "photo_selection_block",
        "subtype": "cool_lenta",
        "best_resource_id": "50273844:65cd758cddcd61af9048abb04fd63e766eef7b4fc0426ac687c83f529853af46",
        "resource_ids": [
            "50273844:97f678b02c7c30d02a2f88b20dc7bcbbec54c31b9a5eb2f508adb1b99add7684",
            "50273844:1a0ad9dde7cca87a281ec666a0f23a17407c00ca54992044e64e8597498a383b",
            "50273844:debdb1494662373d1090d07b6ab0dc123153e175e47efe0a8a921416d471611c",
            "50273844:d287a3a0486c40a5be745a087a1996be8272e814908f30039c8af6f435fda57a",
            "50273844:07dd72e4f7ed2f9946c6699e92a90d861f4fb9120298921c34ad0ea02a3fba45",
            "50273844:99bb8dc181bec873c5574f45ec7ae39e3c8941f94ca6e82db6889b9dc201b715",
            "50273844:6cfd9be1b5c4dff77413681e63d22b86be6a8732fbd6a0ab68954e645fb97d65",
            "50273844:24b4be992e232a08d4260c6dd4826d9d97e7453ef09d26c4d954e19f25d2e442",
            "50273844:c47f894a00b815180fef7d41ce38e861c71111c851512c711b1f6267946a2978",
            "50273844:65cd758cddcd61af9048abb04fd63e766eef7b4fc0426ac687c83f529853af46",
            "50273844:bce8b5c51a505197a0aa555c106ddb118dcd62986f5128cf29a77252883f76d2",
            "50273844:8688e826c160eb4561b322cc9a122457d9757f0be0c9762b10b7fddca051610f",
            "50273844:26a4905252de21cdfe0b9b58083057c8cdb63fae17c0f66fd7031292a5d546f0",
            "50273844:fa3aee20bbdff574199c395170cc32210aa793ba0f3f17ec3cc44478994a89dc",
            "50273844:0aae458c662defb30553b759eb9469d73c5c6b6e4e06c2631d0e8f85bc705175",
            "50273844:bdbd06a407c71d926279b2d7cb672fafcc4ec179c3edbfaf83ebc807ba8d860a",
            "50273844:6c7c9a7671599324a51dc5ebb05bed67c631f568ecbe65976e2a00a6a12f76aa",
            "50273844:c1fba1330fdaca893b85bc6d82f6282e638d506e1f70bbf0b60854532e6e7b7e",
            "50273844:7d8181f74b57b50ead3765d2c7d9dfb1e28026916f19acb947b175a96657bf9d",
            "50273844:3d0a0655259c7f0125ef676249f48bfe82b2d648ed11d84f04c2c99d66b709a8"
        ],
        "photoslice_date": "2013-09-16T20:27:47.000Z",
        "title": "Осень 2013",
        "cover_title": "Осенняя подборка",
        "cover_subtitle": "Осень",
        "button_text": "Все фото за осень 2013"
    }'''

    def test_common(self):
        with mock.patch.object(LentaLoaderService, 'open_url', return_value=(200, self.block_body, {})) as lenta_stub:
            with self.specified_client(id=MOBILE_APP_ID, uid=self.uid):
                resp = self.client.get(self.url, uid=self.uid, query={'locale': 'ru', 'block_id': '123'})
            call_url = lenta_stub.call_args[0][0]
            assert 'uid=%s' % self.uid in call_url
            assert 'blockId=123' in call_url
            assert 'lang=ru' in call_url
        assert resp.status_code == 200
        assert 'id' in from_json(resp.content)


class LentaLentaReportBlockVisitHandler(CommonDiskTestCase, UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/lenta/report_block_visit'
    body = '{}'
    headers = {'user-agent': 'Yandex.Disk {"os":"android 9","device":"phone","src":"disk.mobile","vsn":"5.15.0-1013",'
                             '"id":"3c7caebe5877cbf7fa1e026d77fd5043","flavor":"prod",'
                             '"uuid":"4cc24a719d7c68a55ccb81bab68954e4"}'}

    def test_common(self):
        with mock.patch.object(LentaLoaderService, 'open_url', return_value=(200, self.body, {})) as lenta_stub:
            with self.specified_client(id=MOBILE_APP_ID, uid=self.uid):
                resp = self.client.get(self.url, uid=self.uid, query={'block_id': '123'}, headers=self.headers)
            call_url = lenta_stub.call_args[0][0]
            assert 'uid=%s' % self.uid in call_url
            assert 'blockId=123' in call_url
            assert 'os=android%209' in call_url
            assert resp.result == self.body
        assert resp.status_code == 200
