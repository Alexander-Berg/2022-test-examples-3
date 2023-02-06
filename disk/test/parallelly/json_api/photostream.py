# -*- coding: utf-8 -*-

import datetime
import re
import time
import urlparse
import mock
import pytest

from lxml import etree
from collections import defaultdict
from nose_parameterized import parameterized

from test.parallelly.json_api.base import CommonJsonApiTestCase

import mpfs.engine.process
from mpfs.common.util import from_json
from mpfs.core.address import Address
from mpfs.core.operations import manager

from mpfs.core.services.notifier_service import NotifierService
from test.helpers.stubs.services import SearchIndexerStub, KladunStub, PassportStub
from test.helpers.utils import check_task_called
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.engine.queue2.celery import BaseTask
from test.conftest import INIT_USER_IN_POSTGRES


db = CollectionRoutedDatabase()


class PhotostreamBrowseJsonApiTestCase(CommonJsonApiTestCase):
    mobile_headers = {'Yandex-Cloud-Request-ID': 'ios-123'}

    def test_photostream(self):
        self.json_ok('user_init', {'uid': self.uid_1, 'locale': 'en'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_with_file'})
        self.upload_file(self.uid, '/disk/folder_with_file/file.txt')

        opts = {
            'uid': self.uid_1,
            'path': '/photostream/file.jpg',
        }
        self.json_error('store', opts, code=118)

        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/file.txt',
            'meta': '',
        }
        listing_result = self.json_ok('info', opts)

        opts = {
            'uid': self.uid_1,
            'path': '/photostream/file.jpg',
            'size': listing_result['meta']['size'],
            'md5': listing_result['meta']['md5'],
            'sha256': listing_result['meta']['sha256'],
        }
        with self.patch_mulca_is_file_exist(func_resp=True):
            result = self.json_ok('store', opts)
            self.assertTrue(result['status'] == 'hardlinked')

        self.upload_file(self.uid_1, '/disk/Camera Uploads/file.jpg')

        opts = {
            'uid': self.uid_1,
            'path': '/photostream/file.jpg',
            'size': 1,
            'md5': '1' * 32,
        }
        result = self.json_ok('store', opts)

        operation = manager.get_operation(str(self.uid_1), result['oid'])
        path = operation.data['new_path']

        address = Address(path)
        self.assertTrue(int(address.name.split('file_')[1].split('.jpg')[0]))
        result = self.json_ok('astore', opts)
        self.assertTrue(result['oid'])

    def test_photostream_CHEMODAN_9635(self):
        """
            CHEMODAN-9635
        """
        open_url_data = {}
        with KladunStub() as kladun_mocks:
            self.upload_file(self.uid, '/photostream/file.jpg', open_url_data=open_url_data)
            self.upload_file(self.uid, '/photostream/file.jpg', open_url_data=open_url_data)
            kladun_args, _ = kladun_mocks.upload_to_disk_post_request.call_args

        RE = self.uid + u':/disk/Фотокамера/file_\d+.jpg'
        path = kladun_args[0]['path']
        self.assertNotEqual(re.match(RE, path), None)

        vals = defaultdict(list)
        found = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    diff_tag = etree.fromstring(each['pure_data'])
                    if diff_tag.tag == 'diff':
                        for op in diff_tag.iterfind('op'):
                            vals[notified_uid].append(etree.tostring(op, pretty_print=True))
                            if notified_uid == self.uid:
                                if op.get('key').startswith(u'/disk/Фотокамера/file_'):
                                    found = True
                            else:
                                self.fail('wrong uid')
        self.assertTrue(found)

    def test_photostream_CHEMODAN_10017(self):
        """
            CHEMODAN-10017
        """
        self.run_000_user_check(self.uid_3)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/Фотокамера',
            'meta': ''
        }
        self.json_error('info', opts)

        open_url_data = {}
        file_data = {'mimetype': 'image/jpg'}
        self.upload_file(self.uid_3, '/photostream/file.jpg', open_url_data=open_url_data, file_data=file_data)

        result = self.json_ok('info', opts)
        self.assertEqual(result['meta']['folder_type'], 'photostream')

        vals = defaultdict(list)
        found = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    diff_tag = etree.fromstring(each['pure_data'])
                    if diff_tag.tag == 'diff':
                        for op in diff_tag.iterfind('op'):
                            vals[notified_uid].append(etree.tostring(op, pretty_print=True))
                            if notified_uid == self.uid_3:
                                found = True
                            else:
                                self.fail('wrong uid')
        self.assertEqual(len(vals[self.uid_3]), 4, vals[self.uid])
        self.assertTrue(found)

    def test_store_photostream_mtime_ignored(self):
        """Проверить что при автозагрузке файла в фотострим игнорируется `mtime` передаваемый с клиента."""
        response = self.json_ok('store', {
            'uid': self.uid,
            'path': '/photostream/my_summer.jpg',
            'use_https': 1,
            'mtime': 100500,
            'size': 4929690,
            'md5': '76836a14bd5ce7be785242748a72cb57',
            'sha256': '613afedd95e9c47e45d3af2eff25bb0925e394306e6a76629f5ef3f09afa5a0c'
        })
        operation_id = response['oid']
        operation = manager.get_operation(self.uid, operation_id)
        assert operation.type == 'store'
        assert operation.subtype == 'photostream'
        assert 'mtime' not in operation.data['changes']

    def test_rm_check_search_version(self):
        """При удалении в поиск должна улетать актуальная версия

        https://st.yandex-team.ru/CHEMODAN-31066
        """
        file_data = {
            'mimetype': 'image/jpg',
        }

        get_user_version = lambda u: self.json_ok('user_info', {'uid': u})['version']
        get_push_version = lambda m: m.call_args[0][0][0]['version']

        def assert_correct_push_version(uid, func):
            prev_ver = get_user_version(uid)
            with SearchIndexerStub() as stub:
                func()
                after_ver = get_user_version(uid)
                push_ver = get_push_version(stub.push_change)
            assert prev_ver < after_ver
            assert prev_ver < push_ver
            assert after_ver == push_ver

        self.upload_file(self.uid, '/disk/2.jpg', file_data=file_data)
        self.upload_file(self.uid, '/photostream/1.jpg', file_data=file_data)

        assert_correct_push_version(self.uid, lambda: self.json_ok('rm',
                                                                   {'uid': self.uid,
                                                                    'path': '/disk/2.jpg'}))
        assert_correct_push_version(self.uid, lambda: self.json_ok('rm',
                                                                   {'uid': self.uid,
                                                                    'path': '/disk/Фотокамера/1.jpg'}))

        self.upload_file(self.uid, '/disk/Фотокамера/2.jpg', file_data=file_data)
        assert_correct_push_version(self.uid, lambda: self.json_ok('rm',
                                                                   {'uid': self.uid,
                                                                    'path': '/disk/Фотокамера/2.jpg'}))

    def test_32_gb_auto_upload_bonus_is_singleton(self):
        """Проверить что после повторной загрузки в фотострим не выдастся лишняя услуга."""
        date_ = datetime.date(*(2017, 4, 4))  # middle
        timestamp = time.mktime(date_.timetuple())

        with mock.patch('time.time', return_value=timestamp):
            for i in range(2):
                md5 = str(i) * 32
                sha256 = str(i) * 64
                self.upload_file(
                    self.uid,
                    '/photostream/test_32_gb_auto_upload_bonus_%i.jpg' % i,
                    file_data={'mimetype': 'image/jpg', 'size': '100500', 'md5': md5, 'sha256': sha256},
                    headers=self.mobile_headers,
                )
        result = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1'})
        cnt = sum([1 for s in result if s['name'] == '32_gb_autoupload'])
        assert cnt == 1

    @parameterized.expand([
        ((2017, 4, 3), True),  # start
        ((2017, 5, 1), True),  # middle
        ((2017, 7, 4), True),  # end
        ((2017, 4, 2), False),  # before start
        ((2017, 7, 5), False),  # after end
    ])
    def test_32_gb_auto_upload_bonus_corner_cases(self, today, add):
        u"""Проверить корректность выдачи места в разные даты во время действия/недействия акции."""
        date_ = datetime.date(*today)
        timestamp = time.mktime(date_.timetuple())

        with mock.patch('time.time', return_value=timestamp):
            self.upload_file(
                self.uid,
                '/photostream/test_32_gb_auto_upload_bonus.jpg',
                file_data={'mimetype': 'image/jpg', 'size': '100500'},
                headers=self.mobile_headers
            )

        result = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1'})
        services = dict([(s['name'], s) for s in result])
        if add:
            assert '32_gb_autoupload' in services
            assert services['32_gb_autoupload']['names']['ru'] == u'Мобильная автозагрузка'
            assert not services['32_gb_autoupload']['expires']
        else:
            assert '32_gb_autoupload' not in services

    def test_32_gb_auto_upload_notification_added_to_notifier(self):
        """Проверить что нотификация отправляется в нотифаер через асинхронную таску и переданы правильные параметры."""
        date_ = datetime.date(*(2017, 5, 4))
        timestamp = time.mktime(date_.timetuple())

        fake_apply_async = check_task_called(
            BaseTask.apply_async,
            'mpfs.core.job_handlers.notifier.handle_notifier_add_notification'
        )

        with mock.patch.object(BaseTask, 'apply_async', fake_apply_async) as mocked_notifier_add_notification:
            with mock.patch('time.time', return_value=timestamp):
                self.upload_file(
                    self.uid,
                    '/photostream/test_32_gb_auto_upload_bonus.jpg',
                    file_data={'mimetype': 'image/jpg', 'size': '100500'},
                    headers=self.mobile_headers
                )
                assert mocked_notifier_add_notification.called
                assert mocked_notifier_add_notification.call_count == 1
                kwargs = mocked_notifier_add_notification.call_args
                assert kwargs['uid'] == self.uid
                assert kwargs['_type'] == 'congratulations_2017_32gb'
                assert kwargs['service'] == 'disk'
                assert kwargs['actor'] == 'ya_disk'

    def test_32_gb_auto_upload_notifier_url_params(self):
        date_ = datetime.date(*(2017, 5, 4))
        timestamp = time.mktime(date_.timetuple())

        with mock.patch.object(
            NotifierService, 'open_url'
        ) as mocked_notifier_service_open_url:
            with mock.patch('time.time', return_value=timestamp):
                self.upload_file(
                    self.uid,
                    '/photostream/test_32_gb_auto_upload_bonus.jpg',
                    file_data={'mimetype': 'image/jpg', 'size': '100500'},
                    headers=self.mobile_headers
                )
                assert mocked_notifier_service_open_url.called
                args, kwargs = mocked_notifier_service_open_url.call_args
                (url,) = args
                parsed_url = urlparse.urlparse(url)
                assert parsed_url.path == '/notifier/add-notification'
                query = parsed_url.query
                parsed_query = urlparse.parse_qs(query)
                assert parsed_query['type'] == ['congratulations_2017_32gb']
                assert parsed_query['uid'] == [self.uid]
                assert parsed_query['actor'] == ['ya_disk']
                assert parsed_query['service'] == ['disk']

                assert 'pure_data' in kwargs
                data = kwargs['pure_data']  # percent-encoded

                data = urlparse.parse_qs(data)
                [meta] = data['meta']
                meta = from_json(meta)

                assert 'action' in meta
                assert 'mobile-link' in meta

                assert 'action' in meta['action']
                assert meta['action']['action'] == 'GO_TO_TUNE_PAGE'
                assert 'type' in meta['action']
                assert meta['action']['type'] == 'action'

                assert 'type' in meta['mobile-link']
                assert meta['mobile-link']['type'] == 'link'
                assert 'link' in meta['mobile-link']
                assert 'en_link' in meta['mobile-link']
                assert 'uk_link' in meta['mobile-link']
                assert 'tr_link' in meta['mobile-link']
                assert 'ru_link' in meta['mobile-link']

    @parameterized.expand([
        ('common.login', False),
        ('promo-auto-upload-32-gb-user-a1', True),
        ('promo.auto.upload.32.gb.user.a1', True),
    ])
    def test_32_gb_auto_upload_test_user(self, login, add):
        with PassportStub(userinfo={'login': login}):
            with mock.patch('time.time', return_value=1.0):
                self.upload_file(
                    self.uid,
                    '/photostream/test_32_gb_auto_upload_bonus.jpg',
                    file_data={'mimetype': 'image/jpg', 'size': '100500'},
                    headers=self.mobile_headers
                )

        result = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1'})
        services = {s['name'] for s in result}
        if add:
            assert '32_gb_autoupload' in services
        else:
            assert '32_gb_autoupload' not in services

    @parameterized.expand([
        ('ios', True),
        ('andr', True),
        ('wp', True),
        ('rest_ios', True),
        ('rest_andr', True),
        ('mpfs', False),
        ('lnx', False),
        ('web', False),
        ('rest', False),
        ('dav', False),
        ('win', False)
    ])
    def test_32_gb_auto_upload_only_for_mobile(self, ycrid_prefix, add):
        date_ = datetime.date(*(2017, 5, 4))
        timestamp = time.mktime(date_.timetuple())
        with mock.patch(
            'mpfs.core.job_handlers.notifier.handle_notifier_add_notification',
            return_value=None
        ) as mocked_notifier_add_notification:
            with mock.patch('time.time', return_value=timestamp):
                headers = {'Yandex-Cloud-Request-ID': '%s-123' % ycrid_prefix}
                self.upload_file(
                    self.uid,
                    '/photostream/test_32_gb_auto_upload_bonus.jpg',
                    file_data={'mimetype': 'image/jpg', 'size': '100500'},
                    headers=headers
                )

        result = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1'})
        services = {s['name'] for s in result}
        if add:
            assert '32_gb_autoupload' in services
        else:
            assert '32_gb_autoupload' not in services

    def test_store_remove_store_same_file(self):
        store_path, real_path = '/photostream/some.jpg', u'/disk/Фотокамера/some.jpg'
        self.upload_file(
            self.uid, store_path, file_data={'mimetype': 'image/jpg'}
        )
        file_info = self.json_ok('info', {'uid': self.uid, 'path': real_path, 'meta': 'sha256,md5,size'})
        sha256, md5, size = file_info['meta']['sha256'], file_info['meta']['md5'], file_info['meta']['size']

        self.json_ok('rm', {'uid': self.uid, 'path': real_path})

        self.upload_file(
            self.uid, store_path, file_data={'mimetype': 'image/jpg', 'sha256': sha256, 'md5': md5, 'size': size}
        )
