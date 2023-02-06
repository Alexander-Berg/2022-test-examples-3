# -*- coding: utf-8 -*-
import mock
import re

from lxml import etree
from nose_parameterized import parameterized

from mpfs.metastorage.mongo.collections.base import UserIndexCollection
from mpfs.common.static import tags
from mpfs.common.util import to_json
from mpfs.config import settings
from mpfs.core.user.base import User
from mpfs.core.services.event_history_search_service import EventHistorySearchService

from test.base import CommonDiskTestCase
from test.parallelly.billing.base import BaseBillingTestCase
from test.parallelly.api.disk.base import DiskApiTestCase

MOBILE_APP_ID = settings.platform['mobile_apps_ids'][0]

TOO_EARLY_TIMESTAMP = 1512629999  # 7 декабря 2017 г., 09:59:59
START_TIMESTAMP = 1512630000  # 7 декабря 2017 г., 10:00:00


class UnlimitedAutouploadStatusTestCase(DiskApiTestCase, BaseBillingTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        DiskApiTestCase.setup_method(self, method)
        BaseBillingTestCase.setup_method(self, method)

    @parameterized.expand([
        ('too_early', TOO_EARLY_TIMESTAMP, 404),
        ('first_day', START_TIMESTAMP, 200),
    ])
    def test_valid_dates(self, case_name, mock_date, code):
        u"""Проверяем ответ ручки, возвращающей статус акции, в зависимости от текущей даты"""
        with mock.patch('time.time', return_value=mock_date):
            resp = self.client.head('case/disk/unlimited-autoupload/status')
            assert resp.status_code == code


class UnlimitedAutouploadActivationTestCase(CommonDiskTestCase, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    def test_activate_unlimited_autoupload(self):
        with self.specified_client(id=MOBILE_APP_ID, uid=self.uid):
            resp = self.client.request(
                'PUT', 'case/disk/unlimited-autoupload/set-state', data={'activate': True})
        UserIndexCollection.reset()
        assert resp.status_code == 200
        assert User(self.uid).is_unlimited_autouploading_enabled()

    def test_deactivate_unlimited_autoupload(self):
        with self.specified_client(id=MOBILE_APP_ID, uid=self.uid):
            self.client.request(
                'PUT', 'case/disk/unlimited-autoupload/set-state', data={'activate': True})
            resp = self.client.request(
                'PUT', 'case/disk/unlimited-autoupload/set-state', data={'activate': False})
        UserIndexCollection.reset()
        assert resp.status_code == 200
        assert not User(self.uid).is_unlimited_autouploading_enabled()

    def test_photounlim_supported_in_event_history(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        event_history_mock = {
            'hitsArray': [
                {
                    'counters': {
                        'resource_media_type': {
                            'image': 1
                        },
                        'resource_type': {
                            'file': 1
                        }
                    },
                    'group': [
                        'FS_STORE_PHOTOUNLIM|4004843271|store|photounlim|/photounlim/|IMAGE|IMG_961812313213.JPG|1509709404',
                        '17473'
                    ],
                    'max': 1509709404000,
                    'merged_docs': [
                        {
                            'event_class': 'fs',
                            'event_timestamp': '1509709404000',
                            'event_type': 'fs-store-photounlim',
                            'group_key': 'FS_STORE_PHOTOUNLIM|4004843271|store|photounlim|/photounlim/|IMAGE|IMG_961812313213.JPG|1509709404',
                            'id': 'FS_STORE_PHOTOUNLIM|4004843271|store|photounlim|/photounlim/|IMAGE|IMG_961812313213.JPG|1509709404',
                            'owner_uid': '4004843271',
                            'platform': 'ios',
                            'resource_file_id': '109aa83db39ce8f69e9a534ed1e1747957dfb8762b3174168b3cf211e2329344',
                            'resource_media_type': 'image',
                            'resource_type': 'file',
                            'store_subtype': 'photounlim',
                            'store_type': 'store',
                            'target_folder': '/photounlim/',
                            'target_path': '/photounlim/test.jpg',
                            'user_uid': self.uid,
                            'version': '1509709444490'
                        }
                    ],
                    'min': 1509709404000,
                    'size': 1
                },
            ],
            'hitsCount': 1
        }

        mpfs_mock = [{
            u'ctime': 1509726600, u'name': u'test.jpg', u'etime': 1509737400,
            u'meta': {u'file_id': u'109aa83db39ce8f69e9a534ed1e1747957dfb8762b3174168b3cf211e2329344',
                      u'etime': 1509737400}, u'mtime': 1509726600, u'path': u'/photounlim/test.jpg', u'type': u'file',
            u'id': u'/photounlim/test.jpg', u'utime': 1509726600
        }]

        self.upload_file(self.uid, '/photounlim/test.jpg')
        with mock.patch.object(EventHistorySearchService, 'open_url', return_value=(200, to_json(event_history_mock), {})), \
                self.specified_client(scopes=['yadisk:all'], uid=self.uid), \
                mock.patch('mpfs.core.services.mpfsproxy_service.MpfsProxy.bulk_info_by_resource_ids', return_value=mpfs_mock):
            resp = self.client.get('disk/event-history/clusterize?tz_offset=0&event_type=fs-store-photounlim', uid=self.uid)
        assert resp.status_code == 200

    def test_resource_info_understands_photounlim_paths(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.upload_file(self.uid, '/photounlim/test.jpg')
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid):
            resp = self.client.get('disk/resources', uid=self.uid, query={'path': 'photounlim:/test.jpg'})
            assert resp.status_code == 200

    def test_photounlim_upload_push_notification(self):
        open_url_data = {}
        type_new_push_sent = False
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.upload_file(self.uid, '/photounlim/test.jpg', open_url_data=open_url_data)
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                self.assertEqual(notified_uid, self.uid)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    if optag.tag == 'diff' and optag.find('op').get('type') == 'new':
                        self.assertEqual(optag.find('op').get('folder'), '/photounlim/')
                        self.assertEqual(optag.find('op').get('key'), '/photounlim/test.jpg')
                        type_new_push_sent = True
        self.assertTrue(type_new_push_sent)
