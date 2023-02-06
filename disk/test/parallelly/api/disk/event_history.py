# -*- coding: utf-8 -*-
import mock

from hamcrest import assert_that, equal_to, is_, matches_regexp
from nose_parameterized import parameterized

from test.parallelly.api.disk.base import DiskApiTestCase
from mpfs.common.static import tags
from mpfs.common.util import from_json, to_json
from mpfs.core.services.event_history_search_service import EventHistorySearchService
from test.base_suit import UserTestCaseMixin


class ClusterizeSearchProxyHandlerTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL

    def setup_method(self, method):
        super(ClusterizeSearchProxyHandlerTestCase, self).setup_method(method)
        self.create_user(uid=self.uid)

    @parameterized.expand([('iOS', False), ('android 7.0.0', False), ('windows', True), ('mac', True)])
    def test_clusterize_does_not_return_milliseconds_for_mobile(self, os, expect_milliseconds):
        mock_response = {
            "hitsCount": 2,
            "hitsArray": [
                {
                    "size": 2,
                    "group": ["FS_MKDIR|128280859|/disk/|DIRECTORY", "16903"],
                    "max": 1486000922002,
                    "min": 1483232461001,
                    "counters": {"resource_type": {"directory": 2}},
                    "merged_docs": [
                        {
                            "id": "FS_MKDIR|128280859|/disk/|DIRECTORY|Музыка|1460479846",
                            "version": "1460936621326",
                            "event_type": "fs-mkdir",
                            "event_class": "fs",
                            "group_key": "FS_MKDIR|128280859|/disk/|DIRECTORY",
                            "event_timestamp": "1483232461001",
                            "target_path": "/disk/Музыка/",
                            "target_folder": "/disk/",
                            "resource_type": "directory",
                            "resource_file_id": "7a49ee9781fe1324306544637ae3c6abd2efd9205a0b1d7a78fd3f0090dbe5ff",
                            "owner_uid": "128280859",
                            "user_uid": "128280859"
                        },
                        {
                            "id": "FS_MKDIR|128280859|/disk/|DIRECTORY|test_group|1460479846",
                            "version": "1460936621335",
                            "event_type": "fs-mkdir",
                            "event_class": "fs",
                            "group_key": "FS_MKDIR|128280859|/disk/|DIRECTORY",
                            "event_timestamp": "1486000922002",
                            "platform": "mpfs",
                            "target_path": "/disk/test_group/",
                            "target_folder": "/disk/",
                            "resource_type": "directory",
                            "resource_file_id": "c6061135697d4e2ed832328b5aa11f0b78404505dc5c1c3fddc03934aab1212f",
                            "owner_uid": "128280859",
                            "user_uid": "128280859"
                        }
                    ]
                }
            ]
        }

        with mock.patch.object(EventHistorySearchService, 'open_url', return_value=(200, to_json(mock_response), {})):
            resp = self.client.get('disk/event-history/clusterize?tz_offset=0', uid=self.uid,
                                   headers={'User-AGENT': 'Yandex.Disk {"os":"%s"}' % os})
            assert_that(resp.status_code, is_(equal_to(200)))
            json = from_json(resp.content)

        if expect_milliseconds:
            assert_that(json['groups'][0]['events'][0]['event_date'], is_(equal_to('2017-01-01T01:01:01.001000+00:00')))
            assert_that(json['groups'][0]['events'][1]['event_date'], is_(equal_to('2017-02-02T02:02:02.002000+00:00')))
            assert_that(json['groups'][0]['group']['min_date'], is_(equal_to('2017-01-01T01:01:01.001000+00:00')))
            assert_that(json['groups'][0]['group']['max_date'], is_(equal_to('2017-02-02T02:02:02.002000+00:00')))
            # page_load_date может не содержать миллисекунд, не проверяем
        else:
            assert_that(json['groups'][0]['events'][0]['event_date'], is_(equal_to('2017-01-01T01:01:01+00:00')))
            assert_that(json['groups'][0]['events'][1]['event_date'], is_(equal_to('2017-02-02T02:02:02+00:00')))
            assert_that(json['groups'][0]['group']['min_date'], is_(equal_to('2017-01-01T01:01:01+00:00')))
            assert_that(json['groups'][0]['group']['max_date'], is_(equal_to('2017-02-02T02:02:02+00:00')))
            assert_that(json['page_load_date'], matches_regexp(r'\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d\+\d\d:\d\d'))

    def test_clusterize_does_not_fail_without_time_fields_for_mobile_requests(self):
        mock_response = {
            "hitsCount": 2,
            "hitsArray": [
                {
                    "size": 2,
                    "group": ["FS_MKDIR|128280859|/disk/|DIRECTORY", "16903"],
                    "max": 1486000922002,
                    "min": 1483232461001,
                    "counters": {"resource_type": {"directory": 2}},
                    "merged_docs": [
                        {
                            "id": "FS_MKDIR|128280859|/disk/|DIRECTORY|Музыка|1460479846",
                            "version": "1460936621326",
                            "event_type": "fs-store",
                            "event_class": "fs",
                            "group_key": "FS_MKDIR|128280859|/disk/|DIRECTORY",
                            "event_timestamp": "1483232461001",
                            "target_path": "/disk/Музыка/",
                            "target_folder": "/disk/",
                            "resource_type": "directory",
                            "resource_file_id": "7a49ee9781fe1324306544637ae3c6abd2efd9205a0b1d7a78fd3f0090dbe5ff",
                            "owner_uid": "128280859",
                            "user_uid": "128280859"
                        },
                        {
                            "id": "FS_MKDIR|128280859|/disk/|DIRECTORY|test_group|1460479846",
                            "version": "1460936621335",
                            "event_type": "fs-mkdir",
                            "event_class": "fs",
                            "group_key": "FS_MKDIR|128280859|/disk/|DIRECTORY",
                            "event_timestamp": "1486000922002",
                            "platform": "mpfs",
                            "target_path": "/disk/test_group/",
                            "target_folder": "/disk/",
                            "resource_type": "directory",
                            "resource_file_id": "c6061135697d4e2ed832328b5aa11f0b78404505dc5c1c3fddc03934aab1212f",
                            "owner_uid": "128280859",
                            "user_uid": "128280859"
                        }
                    ]
                }
            ]
        }

        with mock.patch.object(EventHistorySearchService, 'open_url', return_value=(200, to_json(mock_response), {})):
            resp = self.client.get('disk/event-history/clusterize?tz_offset=0&fields=groups.events.path,groups.events.user_id,groups.events.id,groups.events.resource,groups.group.event_group_key,groups.total_count,total_count', uid=self.uid,
                                   headers={'User-AGENT': 'Yandex.Disk {"os":"android 7.0.0"}'})
        assert_that(resp.status_code, is_(equal_to(200)))

    def test_lenta_block_in_events(self):
        mock_response = {
            'hitsCount': 3,
            'hitsArray': [
                {
                    'size': 3,
                    'group': ['FS_MKDIR|128280859|/disk/|DIRECTORY', '16903'],
                    'max': 1486000922002,
                    'min': 1483232461001,
                    'counters': {'resource_type': {'directory': 3}},
                    'merged_docs': [
                        {
                            'id': 'FS_MKDIR|128280859|/disk/|DIRECTORY|Музыка|1460479846',
                            'version': '1460936621325',
                            'event_type': 'fs-store',
                            'entity_type': 'private_resource',
                            'event_class': 'fs',
                            'group_key': 'FS_MKDIR|128280859|/disk/|DIRECTORY',
                            'event_timestamp': '1483232461001',
                            'target_path': '/disk/Музыка/',
                            'target_folder': '/disk/',
                            'resource_type': 'directory',
                            'entity_id': '7a49ee9781fe1324306544637ae3c6abd2efd9205a0b1d7a78fd3f0090dbe5ff',
                            'owner_uid': '128280859',
                            'user_uid': '128280859'
                        },
                        {
                            'id': 'FS_MKDIR|128280859|/disk/|DIRECTORY|Музыка|1460479846',
                            'version': '1460936621326',
                            'event_type': 'comment-add',
                            'entity_type': 'public_resource',
                            'event_class': 'fs',
                            'group_key': 'FS_MKDIR|128280859|/disk/|DIRECTORY',
                            'event_timestamp': '1483232461001',
                            'target_path': '/disk/Музыка/',
                            'target_folder': '/disk/',
                            'resource_type': 'directory',
                            'entity_id': '7a49ee9781fe1324306544637ae3c6abd2efd9205a0b1d7a78fd3f0090dbe5ff',
                            'owner_uid': '128280859',
                            'user_uid': '128280859'
                        },
                        {
                            'id': 'FS_MKDIR|128280859|/disk/|DIRECTORY|test_group|1460479846',
                            'version': '1460936621335',
                            'event_type': 'comment-add',
                            'entity_type': 'private_resource',
                            'event_class': 'fs',
                            'group_key': 'FS_MKDIR|128280859|/disk/|DIRECTORY',
                            'event_timestamp': '1486000922002',
                            'platform': 'mpfs',
                            'target_path': '/disk/test_group/',
                            'target_folder': '/disk/',
                            'resource_type': 'directory',
                            'entity_id': 'c6061135697d4e2ed832328b5aa11f0b78404505dc5c1c3fddc03934aab1212f',
                            'owner_uid': '128280859',
                            'user_uid': '128280859',
                            'resource': {
                                'meta': {
                                    'media_type': 'image'
                                }
                            }
                        }
                    ]
                }
            ]
        }

        correct_lenta_block = {
            'uid': self.uid,
            'modify_uid': '128280859',
            'media_type': 'image',
            'mtime': '1486000922002',
            'type': 'private_resource',
            'resource_file_id': 'c6061135697d4e2ed832328b5aa11f0b78404505dc5c1c3fddc03934aab1212f',
        }

        with mock.patch.object(EventHistorySearchService, 'open_url', return_value=(200, to_json(mock_response), {})):
            resp = self.client.get('disk/event-history/clusterize?tz_offset=0', uid=self.uid)
        assert 'lenta_block' not in from_json(resp.content)['groups'][0]['events'][0]
        assert from_json(resp.content)['groups'][0]['events'][1]['lenta_block'] is None
        assert from_json(resp.content)['groups'][0]['events'][2]['lenta_block'] == correct_lenta_block
