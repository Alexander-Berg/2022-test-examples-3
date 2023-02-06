# -*- coding: utf-8 -*-

import os
import time
import urllib

import mock
import pytest
from nose_parameterized import parameterized

from test.base import DiskTestCase

from mpfs.common.static.codes import RESOURCE_NOT_FOUND
from mpfs.core.address import ResourceId
from mpfs.core.factory import get_resources_by_resource_ids, get_resource_by_resource_id
from mpfs.core.filesystem.dao.resource import PhotounlimDAO
from mpfs.core.lenta.utils import is_resource_visible_for_user
from mpfs.core.services.lenta_loader_service import LentaLoaderService
from mpfs.dao.session import Session
from mpfs.core.filesystem.dao.folder import FolderDAOItem
from test.base_suit import FakeRequest
from test.common.sharing import CommonSharingMethods
from test.conftest import setup_queue2, INIT_USER_IN_POSTGRES
from test.helpers.stubs.services import KladunStub, PreviewerStub
from test.parallelly.times_suit import fake_time_with_increment


class LentaTestCase(DiskTestCase):
    def setup_method(self, method):
        super(LentaTestCase, self).setup_method(method)

    def teardown_method(self, method):
        self.remove_uploaded_files()
        super(LentaTestCase, self).teardown_method(method)

    def test_list_by_path(self):
        ts = int(time.time())
        FILE_NUMBER = 3
        for i in range(FILE_NUMBER):
            self.upload_file(self.uid, '/disk/test%s.jpg' % i, media_type='image')

        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'path': '/disk', 'mtime_gte': str(ts)})

        assert isinstance(resp, list)
        assert len(resp) == FILE_NUMBER + 1  # файлы + сама папка

    def test_list_by_surogat_resource_id_for_root_folder(self):
        ts = int(time.time())
        FILE_NUMBER = 3
        file_id = '/disk'
        for i in range(FILE_NUMBER):
            self.upload_file(self.uid, '/disk/test%s.jpg' % i, media_type='image')

        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'resource_id': '%s:%s' % (self.uid, file_id),
                                                 'mtime_gte': str(ts)})

        assert isinstance(resp, list)
        assert len(resp) == FILE_NUMBER + 1  # файлы + сама папка

    def test_list_by_resource_id(self):
        ts = int(time.time())
        FILE_NUMBER = 3
        FOLDER_PATH = '/disk/folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': FOLDER_PATH})
        resp = self.json_ok('info', {'uid': self.uid, 'path': FOLDER_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']
        for i in range(FILE_NUMBER):
            self.upload_file(self.uid, FOLDER_PATH + '/test%s.jpg' % i, media_type='image')

        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'resource_id': str(ResourceId(self.uid, file_id)),
                                                 'mtime_gte': str(ts)})

        assert isinstance(resp, list)
        assert len(resp) == FILE_NUMBER + 1  # файлы + сама папка

    def test_list_by_resource_id_and_mtime_with_lte_and_gte(self):
        FILE_NUMBER = 3
        FOLDER_PATH = '/disk/folder'

        ts = int(time.time())

        self.json_ok('mkdir', {'uid': self.uid, 'path': FOLDER_PATH})
        resp = self.json_ok('info', {'uid': self.uid, 'path': FOLDER_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']
        for i in range(FILE_NUMBER):
            self.upload_file(self.uid, FOLDER_PATH + '/test%s.jpg' % i, media_type='image')

        resp = self.json_ok('lenta_block_list', {
            'uid': self.uid,
            'modify_uid': self.uid,
            'resource_id': str(ResourceId(self.uid, file_id)),
            'mtime_gte': str(ts),
            'mtime_lte': str(ts + 100500),
            'amount': str(1),
            'media_type': 'image',
            'meta': 'total_results_count,file_id'
        })

        assert isinstance(resp, list)
        assert len(resp) == 1 + 1  # 1 файл (amount) + сама папка

    def test_media_type_filter(self):
        VIDEO_NAME_TEMPLATE = '/disk/test%s.avi'
        VIDEOS_NUMBER = 3
        VIDEOS_PATHS = [VIDEO_NAME_TEMPLATE % i for i in range(VIDEOS_NUMBER)]
        for path in VIDEOS_PATHS:
            self.upload_file(self.uid, path, media_type='video')

        IMAGE_NAME_TEMPLATE = '/disk/test%s.jpg'
        IMAGES_NUMBER = 3
        IMAGES_PATHS = [IMAGE_NAME_TEMPLATE % i for i in range(IMAGES_NUMBER)]
        for path in IMAGES_PATHS:
            self.upload_file(self.uid, path, media_type='image')

        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'path': '/disk', 'mtime_gte': str(0),
                                                 'media_type': 'image', 'meta': 'media_type'})
        assert len(resp) == IMAGES_NUMBER + 1
        for r in resp[1:]:
            assert r['meta']['media_type'] == 'image'

    def test_pagination(self):
        FILE_NUMBER = 5
        OLD_FILES = 2
        FILE_NAME_TEMPLATE = '/disk/test%s.jpg'
        FILES_PATHS = [FILE_NAME_TEMPLATE % i for i in range(FILE_NUMBER)]
        OLD_FILES_PATHS = FILES_PATHS[:OLD_FILES]
        NEW_FILES_PATHS = FILES_PATHS[OLD_FILES:]

        for path in OLD_FILES_PATHS:
            self.upload_file(self.uid, path, media_type='image')

        time.sleep(1)
        TS = int(time.time())

        with mock.patch('time.time', fake_time_with_increment()):
            for path in NEW_FILES_PATHS:
                self.upload_file(self.uid, path, media_type='image')

        AMOUNT = 2
        # запрашиваем 2 элемента
        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'path': '/disk', 'mtime_gte': str(TS),
                                                 'media_type': 'image', 'amount': AMOUNT})
        assert len(resp) == AMOUNT + 1
        for r in resp[1:]:
            assert r['mtime'] >= TS
            assert r['path'] in NEW_FILES_PATHS[-AMOUNT:]  # сортируются в обратном порядке от новых к старым (во frontend'e)

        # запрашиваем оставшийся 1 элемент
        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'path': '/disk', 'mtime_gte': str(TS),
                                                 'media_type': 'image', 'amount': AMOUNT, 'offset': AMOUNT})
        assert len(resp) == FILE_NUMBER - OLD_FILES - AMOUNT + 1
        for r in resp[1:]:
            assert r['mtime'] >= TS
            assert r['path'] in NEW_FILES_PATHS[:-AMOUNT]

        # запрашиваем amount=0
        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'path': '/disk', 'mtime_gte': str(TS),
                                                 'media_type': 'image', 'amount': '0'})
        assert len(resp) == 1

    def test_total_results_count(self):
        FILE_NUMBER = 5
        OLD_FILES = 2
        FILE_NAME_TEMPLATE = '/disk/test%s.jpg'
        FILES_PATHS = [FILE_NAME_TEMPLATE % i for i in range(FILE_NUMBER)]
        OLD_FILES_PATHS = FILES_PATHS[:OLD_FILES]
        NEW_FILES_PATHS = FILES_PATHS[OLD_FILES:]

        for path in OLD_FILES_PATHS:
            self.upload_file(self.uid, path, media_type='image')

        time.sleep(1)
        TS = int(time.time())

        for path in NEW_FILES_PATHS:
            self.upload_file(self.uid, path, media_type='image')

        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'path': '/disk', 'meta': 'total_results_count',
                                                 'mtime_gte': str(TS), 'amount': 2})

        assert 'total_results_count' in resp[0].get('meta', {})
        assert resp[0]['meta']['total_results_count'] == FILE_NUMBER - OLD_FILES

    def test_mtime_interval_filter_error(self):
        # магический метод json_error сам проверят, что вернулась ошибка,
        # поэтому тут нет ни одного ассерта
        self.json_error('lenta_block_list', {'uid': self.uid, 'path': '/disk', 'meta': 'total_results_count',
                                             'mtime_gte': '1', 'mtime_lte': '0', 'amount': 2})

    def test_order_from_new_to_old(self):
        """Протестировать что файлы идут в порядке от новых к старым (по mtime)."""
        first_file_path = '/disk/test1.jpg'
        self.upload_file(self.uid, first_file_path, media_type='image')
        second_file_path = '/disk/test2.jpg'
        self.upload_file(self.uid, second_file_path, media_type='image')
        third_file_path = '/disk/test3.jpg'
        self.upload_file(self.uid, third_file_path, media_type='image')

        response = self.json_ok('info', {'uid': self.uid, 'path': first_file_path})
        first_file_mtime = response.get('mtime')
        response = self.json_ok('info', {'uid': self.uid, 'path': second_file_path})
        second_file_mtime = response.get('mtime')
        response = self.json_ok('info', {'uid': self.uid, 'path': third_file_path})
        third_file_mtime = response.get('mtime')

        response = self.json_ok('lenta_block_list',
                                {'uid': self.uid, 'path': '/disk', 'mtime_gte': '100500'})

        mtimes = [r['mtime'] for r in response[1:]]  # первой идет сама папка
        assert mtimes == [third_file_mtime, second_file_mtime, first_file_mtime]

    def test_iteration_key_for_non_empty_rest(self):
        """Протестировать корректность iteration_key в случае когда есть еще файлы по заданному критерию."""
        first_file_path = '/disk/test1.jpg'
        self.upload_file(self.uid, first_file_path, media_type='image')
        second_file_path = '/disk/test2.jpg'
        self.upload_file(self.uid, second_file_path, media_type='image')
        third_file_path = '/disk/test3.jpg'
        self.upload_file(self.uid, third_file_path, media_type='image')

        response = self.json_ok('info', {'uid': self.uid, 'path': third_file_path})
        third_file_mtime = response.get('mtime')

        response = self.json_ok('lenta_block_list', {'uid': self.uid, 'path': '/disk', 'mtime_gte': '100500', 'amount': 1, 'meta': ''})
        # содержится в meta папки, то есть в нулевом элементе ответа
        iteration_key = response[0]['meta']['iteration_key']
        assert iteration_key == '%s/%s' % (third_file_mtime, 1)

    def test_iteration_key_has_priority_over_mtime_lte(self):
        """Протестировать запрос с передачей параметра iteration_key."""
        # iteration_key имеет преимущество перед mtime_lte и offset
        first_file_path = '/disk/test1.jpg'
        self.upload_file(self.uid, first_file_path, media_type='image')
        second_file_path = '/disk/test2.jpg'
        self.upload_file(self.uid, second_file_path, media_type='image')
        time.sleep(1)  # т.к. mtime у нас хранится как смещение в секундах
        third_file_path = '/disk/test3.jpg'
        self.upload_file(self.uid, third_file_path, media_type='image')

        response = self.json_ok('info', {'uid': self.uid, 'path': first_file_path})
        first_file_mtime = response.get('mtime')
        response = self.json_ok('info', {'uid': self.uid, 'path': second_file_path})
        second_file_mtime = response.get('mtime')
        response = self.json_ok('info', {'uid': self.uid, 'path': third_file_path})
        third_file_mtime = response.get('mtime')

        assert first_file_mtime <= second_file_mtime < third_file_mtime

        # mtime_lte в запросе имеет mtime первого файла, то есть по идее должен вернуться
        # только он, но iteration_key его переопределяет и будут возвращены 2 файла
        response = self.json_ok(
            'lenta_block_list',
            {
                'uid': self.uid,
                'path': '/disk',
                'mtime_gte': '100500',
                'mtime_lte': str(first_file_mtime),
                'iteration_key': '%s/%s' % (second_file_mtime, 0)  # offset=0
            }
        )
        assert len(response) == len(['/disk', first_file_path, second_file_path])
        mtimes = [r['mtime'] for r in response[1:]]  # первой идет сама папка
        assert mtimes == [second_file_mtime, first_file_mtime]

    def test_iteration_key_for_empty_rest(self):
        """Протестировать корректность iteration_key в случае
        когда больше не остается файлов по заданному критерию."""
        first_file_path = '/disk/test1.jpg'
        self.upload_file(self.uid, first_file_path, media_type='image')

        response = self.json_ok('info', {'uid': self.uid, 'path': first_file_path})
        first_file_mtime = response.get('mtime')
        response = self.json_ok(
            'lenta_block_list',
            {
                'uid': self.uid,
                'path': '/disk',
                'mtime_gte': '100500',
                'amount': 1,
                'meta': '',
                'mtime_lte': str(first_file_mtime)
            }
        )
        # больше файлов нет подпадающих под выборку, поэтому iteration_key должен быть равен None
        iteration_key = response[0]['meta']['iteration_key']
        assert iteration_key is None

    def test_users_in_response_1(self):
        """Проверить включение пользовательской информации в ответ при переданном ключе `meta`."""
        file_path = '/disk/test1.jpg'
        self.upload_file(self.uid, file_path, media_type='image')

        response = self.json_ok('info', {'uid': self.uid, 'path': file_path})
        file_mtime = response.get('mtime')
        response = self.json_ok(
            'lenta_block_list',
            {
                'uid': self.uid,
                'path': '/disk',
                'mtime_gte': '100500',
                'amount': 1,
                'meta': '',
                'mtime_lte': str(file_mtime),
                'include_users': 1
            }
        )
        assert 'users' in response[0]['meta']
        for key in ('firstname', 'lastname', 'login', 'sex', 'uid'):
            assert key in response[0]['meta']['users'][0]

    def test_users_in_response_2(self):
        """Проверить отсутствие пользовательской информации в ответе при НЕпереданном ключе `meta`."""
        file_path = '/disk/test1.jpg'
        self.upload_file(self.uid, file_path, media_type='image')

        response = self.json_ok('info', {'uid': self.uid, 'path': file_path})
        file_mtime = response.get('mtime')
        response = self.json_ok(
            'lenta_block_list',
            {
                'uid': self.uid,
                'path': '/disk',
                'mtime_gte': '100500',
                'amount': 1,
                'mtime_lte': str(file_mtime),
            }
        )
        assert 'meta' not in response[0]
        assert 'users' not in response[0].get('meta', {})

    def test_users_in_response_3(self):
        """Проверить включение пользовательской информации в ответе при переданном поле users в meta."""
        file_path = '/disk/test1.jpg'
        self.upload_file(self.uid, file_path, media_type='image')

        response = self.json_ok('info', {'uid': self.uid, 'path': file_path})
        file_mtime = response.get('mtime')
        response = self.json_ok(
            'lenta_block_list',
            {
                'uid': self.uid,
                'path': '/disk',
                'mtime_gte': '100500',
                'amount': 1,
                'meta': 'users',
                'mtime_lte': str(file_mtime),
            }
        )
        assert 'users' in response[0]['meta']
        for key in ('firstname', 'lastname', 'login', 'sex', 'uid'):
            assert key in response[0]['meta']['users'][0]

    def test_lenta_block_list_with_non_existing_media_type_filter(self):
        """Проверить что при передаче некорректного медиа типа получим пятисоточку."""
        file_path = '/disk/test.jpg'
        self.upload_file(self.uid, file_path, media_type='image')

        response = self.json_ok('info', {'uid': self.uid, 'path': file_path})
        file_mtime = response.get('mtime')
        self.json_error(
            'lenta_block_list',
            {
                'uid': self.uid,
                'path': '/disk',
                'mtime_gte': '100500',
                'amount': 1,
                'mtime_lte': str(file_mtime),
                'media_type': 'image,non_existing_media_type'
            },
            code=201
        )

    def test_lenta_block_list_with_lenta_media_type_other(self):
        """Проверить что при передаче среди медиа типов значения other
        оно развернется в набор соответствующих медиа типов."""
        with mock.patch(
            'mpfs.core.base.lenta_block_list',
            return_value=None
        ) as mock_lenta_block_list:
            _, response = self.do_request(
                'lenta_block_list',
                {
                    'uid': self.uid,
                    'path': '/disk',
                    'mtime_gte': '100500',
                    'media_type': 'other',
                }
            )
            assert mock_lenta_block_list.called
            args, kwargs = mock_lenta_block_list.call_args
            (req,) = args
            media_type_str = req.media_type
            assert len(media_type_str.split(',')) == 15

    def test_lenta_block_list_file_resource(self):
        """Проверить что при передаче файла вместо папки получим ошибку."""
        file_path = '/disk/test.jpg'
        self.upload_file(self.uid, file_path, media_type='image')

        response = self.json_ok('info', {'uid': self.uid, 'path': file_path})
        file_mtime = response.get('mtime')
        self.json_error(
            'lenta_block_list',
            {
                'uid': self.uid,
                'path': file_path,
                'mtime_gte': '100500',
                'mtime_lte': str(file_mtime),
            },
            code=70
        )

    def test_lenta_notified_about_empty_block_without_iteration_key(self):
        """Проверить что Лента оповещена о пустом блоке в ленте пользователя (ключ итерации НЕ передан)."""
        first_file_path = '/disk/test1.jpg'
        self.upload_file(self.uid, first_file_path, media_type='image')

        args = {
            'uid': self.uid,
            'path': '/disk',
            'mtime_gte': '0',  # точно таких файлов не найдется
            'amount': 1,
            'meta': '',
            'mtime_lte': '9'
        }
        qs = urllib.urlencode(args)
        with mock.patch.dict(FakeRequest.environ, {'QUERY_STRING': qs}, clear=True):
            with mock.patch.object(LentaLoaderService, 'delete_empty_block') as mock_delete_empty_block:
                self.json_ok('lenta_block_list', args)
                assert mock_delete_empty_block.called
                mock_delete_empty_block.assert_called_with(qs)

    def test_lenta_notified_about_empty_block_with_iteration_key(self):
        """Проверить что Лента НЕ оповещена о пустом блоке в ленте пользователя (ключ итерации передан)."""
        first_file_path = '/disk/test1.jpg'
        self.upload_file(self.uid, first_file_path, media_type='image')

        args = {
            'uid': self.uid,
            'path': '/disk',
            'mtime_gte': '0',  # точно таких файлов не найдется
            'amount': 1,
            'meta': '',
            'mtime_lte': '9',
            'iteration_key': '100500/0'
        }
        qs = urllib.urlencode(args)
        with mock.patch.dict(FakeRequest.environ, {'QUERY_STRING': qs}, clear=True):
            with mock.patch.object(LentaLoaderService, 'delete_empty_block') as mock_delete_empty_block:
                self.json_ok('lenta_block_list', args)
                assert not mock_delete_empty_block.called

    def test_modify_uid_filter_common(self):
        """Проверяем, что modify_uid == owner_uid никак не влияет на обычные файлы"""
        ts = int(time.time())
        FILE_NUMBER = 3
        FOLDER_PATH = '/disk/folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': FOLDER_PATH})
        resp = self.json_ok('info', {'uid': self.uid, 'path': FOLDER_PATH, 'meta': 'file_id'})
        file_id = resp['meta']['file_id']
        for i in range(FILE_NUMBER):
            self.upload_file(self.uid, FOLDER_PATH + '/test%s.jpg' % i, media_type='image')

        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'resource_id': str(ResourceId(self.uid, file_id)),
                                                 'mtime_gte': str(ts), 'modify_uid': self.uid})

        assert isinstance(resp, list)
        assert len(resp) == FILE_NUMBER + 1  # файлы + сама папка

    def test_lenta_block_list_searches_only_in_disk_and_photounlim(self):
        """Проверить что поиск ведется только в /disk."""
        # https://st.yandex-team.ru/CHEMODAN-32534
        folder_path = '/disk/sub_folder'
        file_path = os.path.join(folder_path, 'test.jpg')
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        self.upload_file(self.uid, file_path, media_type='image')

        response = self.json_ok('info', {'uid': self.uid, 'path': folder_path, 'meta': ''})
        raw_resource_id = response['meta']['resource_id']
        with mock.patch('mpfs.core.factory.get_resources_by_resource_ids',
                        wraps=get_resources_by_resource_ids) as mock_search_func:
            self.json_ok(
                'lenta_block_list',
                {
                    'uid': self.uid,
                    'resource_id': raw_resource_id,
                    'mtime_gte': '100500',
                    'amount': 1,
                }
            )
            assert mock_search_func.called
            args, kwargs = mock_search_func.call_args
            assert 'enable_service_ids' in kwargs
            assert kwargs['enable_service_ids'] == ('/disk', '/photounlim')  # /trash отсутствует

    @parameterized.expand([
        ({'amount': 1}, 2, 2),
        ({}, 2, 3)
    ])
    @mock.patch.dict('mpfs.config.settings.lenta', {'max_results_count': 2})
    def test_lenta_block_list_max_limit_with_amount_less_than_limit(
        self, additional_params, expected_total_results_count, expected_results_count
    ):
        folder_path = '/disk/sub_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        self.upload_file(self.uid, os.path.join(folder_path, 'test_1.jpg'), media_type='image')
        self.upload_file(self.uid, os.path.join(folder_path, 'test_2.jpg'), media_type='image')
        self.upload_file(self.uid, os.path.join(folder_path, 'test_3.jpg'), media_type='image')
        self.upload_file(self.uid, os.path.join(folder_path, 'test_4.jpg'), media_type='image')

        params = {
            'uid': self.uid,
            'path': folder_path,
            'mtime_gte': '100500',
            'meta': 'total_results_count'
        }
        params.update(additional_params)

        result = self.json_ok('lenta_block_list', params)
        total_results_count = result[0]['meta']['total_results_count']
        assert total_results_count == expected_total_results_count
        assert len(result) == expected_results_count

    def test_filtering_photounlim(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/photounlim/dir'})
        self.upload_file(self.uid, '/disk/test.jpg')
        self.upload_file(self.uid, '/photounlim/test.jpg')
        self.upload_file(self.uid, '/disk/dir/test.jpg')
        self.upload_file(self.uid, '/photounlim/dir/test.jpg')

        disk_rid = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': 'resource_id'})['meta']['resource_id']
        photo_rid = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/dir', 'meta': 'resource_id'})['meta']['resource_id']

        # по resource_id
        self.json_ok('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'resource_id': self.uid + ':/disk', 'filter_photounlim': 1})
        self.json_ok('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'resource_id': disk_rid, 'filter_photounlim': 1})
        self.json_error('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'resource_id': self.uid + ':/photounlim', 'filter_photounlim': 1}, code=RESOURCE_NOT_FOUND)
        self.json_error('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'resource_id': photo_rid, 'filter_photounlim': 1}, code=RESOURCE_NOT_FOUND)

        self.json_ok('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'resource_id': self.uid + ':/photounlim', 'filter_photounlim': 0})
        self.json_ok('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'resource_id': photo_rid, 'filter_photounlim': 0})

        # по пути
        self.json_ok('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'path': '/disk', 'filter_photounlim': 1})
        self.json_ok('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'path': '/disk/dir', 'filter_photounlim': 1})
        self.json_error('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'path': '/photounlim', 'filter_photounlim': 1}, code=RESOURCE_NOT_FOUND)
        self.json_error('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'path': '/photounlim/dir', 'filter_photounlim': 1}, code=RESOURCE_NOT_FOUND)

        self.json_ok('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'path': '/photounlim', 'filter_photounlim': 0})
        self.json_ok('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'path': '/photounlim/dir', 'filter_photounlim': 0})

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='postgres test')
    def test_lenta_block_list_for_disk_folder_with_file_id_in_postgres(self):
        self.upload_file(self.uid, '/disk/xxx.txt')

        session = Session.create_from_uid(self.uid)
        cursor = session.execute("SELECT encode(id, 'hex') as id FROM disk.folders WHERE uid=:uid AND name=:name",
                                 {'uid': self.uid, 'name': 'disk'})
        folder_obj = FolderDAOItem.create_from_pg_data(cursor.fetchone())

        self.json_ok('lenta_block_list', {
            'uid': self.uid, 'mtime_gte': 0, 'resource_id': self.uid + ':' + folder_obj.file_id, 'filter_photounlim': 1
        })

    @parameterized.expand([
        (True, False, False, '1.jpg'),
        (False, True, True, '2.jpg'),
        (True, True, True, '2.jpg'),
        (True, True, False, '1.jpg'),
        (True, False, True, '1.jpg'),
        (False, True, False, '2.jpg'),
    ])
    def test_get_lenta_block_by_photostream_path(self, camera_dir_filled, photounlim_dir_filled, is_photounlim_on, correct_found_file):
        self.json_ok('mksysdir', {'uid': self.uid, 'type': 'photostream'})
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})

        if camera_dir_filled:
            self.upload_file(self.uid, '/disk/Фотокамера/1.jpg')
        if photounlim_dir_filled:
            self.upload_file(self.uid, '/photounlim/2.jpg')
        if not is_photounlim_on:
            self.json_ok('disable_unlimited_autouploading', {'uid': self.uid})

        result = self.json_ok('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'path': '/photostream', 'filter_photounlim': 0})
        assert 2 == len(result)

        assert result[-1]['name'] == correct_found_file

    @parameterized.expand([
        (True, False),
        (False, True),
        (True, True),
        (False, False),
    ])
    def test_get_lenta_block_returns_empty_block_if_one_of_folder_is_missing(self, camera_folder_exists, is_photounlim_on):
        if camera_folder_exists:
            self.json_ok('mksysdir', {'uid': self.uid, 'type': 'photostream'})
        if is_photounlim_on:
            self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        result = self.json_ok('lenta_block_list', {'uid': self.uid, 'mtime_gte': 0, 'path': '/photostream', 'filter_photounlim': 0})
        assert len(result) == 1


class LentaSharedFoldersTestCase(CommonSharingMethods):
    FOLDER_PATH = '/disk/folder'
    FILE_NUMBER = 3
    gid = None
    owner_folder_file_id = None
    owner_folder_resource_id = None
    guest_folder_file_id = None
    guest_folder_resource_id = None
    start_upload_files_ts = None
    """Таймстэмп начала загрузки файлов."""

    def __init__(self, *args, **kwargs):
        super(LentaSharedFoldersTestCase, self).__init__(*args, **kwargs)
        self.OWNER_UID = self.uid_1
        self.GUEST_UID = self.uid

    def setup_method(self, method):
        super(LentaSharedFoldersTestCase, self).setup_method(method)
        self.create_users_and_folder()

    def create_users_and_folder(self):
        # создаём юзверей
        self.create_user(self.OWNER_UID)
        self.create_user(self.GUEST_UID)

        # создаём ОП
        self.json_ok('mkdir', {'uid': self.OWNER_UID, 'path': self.FOLDER_PATH})

    def create_group_and_upload_files(self):
        folder_info = self.json_ok('info', {'uid': self.OWNER_UID, 'path': self.FOLDER_PATH, 'meta': 'file_id'})
        self.owner_folder_file_id = folder_info['meta']['file_id']
        self.owner_folder_resource_id = ResourceId(self.OWNER_UID, self.owner_folder_file_id)
        self.gid = self.create_group(uid=self.OWNER_UID, path=self.FOLDER_PATH)

        # без этой строчки не работет, как бы не казалось что она тут лишнаяя
        self.xiva_subscribe(self.uid_1)

        # приглашаем гостя и активируем инвайт
        invite_hash = self.invite_user(owner=self.OWNER_UID, uid=self.GUEST_UID, email=self.email,
                                       path=self.FOLDER_PATH, ext_gid=self.gid)
        self.activate_invite(uid=self.GUEST_UID, hash=invite_hash)
        folder_info = self.json_ok('info', {'uid': self.GUEST_UID, 'path': self.FOLDER_PATH, 'meta': 'file_id'})
        self.guest_folder_file_id = folder_info['meta']['file_id']
        self.guest_folder_resource_id = ResourceId(self.GUEST_UID, self.guest_folder_file_id)

        # заливаем файло, сохранив предварительно время после которого оно было загружено
        self.start_upload_files_ts = int(time.time())
        for i in range(self.FILE_NUMBER):
            self.upload_file(self.OWNER_UID, self.FOLDER_PATH + '/test%s.jpg' % i, media_type='image')

    def teardown_method(self, method):
        self.remove_uploaded_files()
        super(LentaSharedFoldersTestCase, self).teardown_method(method)

    def test_list_shared_folder_by_path_as_guest(self):
        self.create_group_and_upload_files()

        resp = self.json_ok('lenta_block_list', {'uid': self.GUEST_UID, 'path': self.FOLDER_PATH,
                                                 'mtime_gte': str(self.start_upload_files_ts)})
        assert isinstance(resp, list)
        assert len(resp) == self.FILE_NUMBER + 1  # файлы + сама папка

    def test_list_shared_folder_by_resource_id_as_guest(self):
        self.create_group_and_upload_files()

        resp = self.json_ok('lenta_block_list', {'uid': self.GUEST_UID, 'resource_id': str(self.guest_folder_resource_id),
                                                 'mtime_gte': str(self.start_upload_files_ts)})
        assert isinstance(resp, list)
        assert len(resp) == self.FILE_NUMBER + 1  # файлы + сама папка

    def test_modify_uid_filter(self):
        self.create_group_and_upload_files()

        time.sleep(1)
        num_files = 2
        self.start_upload_files_ts = int(time.time())
        for uid in (self.GUEST_UID, self.OWNER_UID):
            for i in range(num_files):
                self.upload_file(uid, '%s/%s_%s' % (self.FOLDER_PATH, uid, i))

        for uid in (self.GUEST_UID, self.OWNER_UID):
            resp = self.json_ok('lenta_block_list', {'uid': self.GUEST_UID, 'resource_id': str(self.guest_folder_resource_id),
                                                    'mtime_gte': str(self.start_upload_files_ts), 'modify_uid': uid,
                                                    'meta': 'modify_uid'})
            assert len(resp) == 1 + num_files
            for item in resp[1:]:
                assert item['meta']['modify_uid'] == uid

    def test_with_subfolder(self):
        self.json_ok('mkdir', {'uid': self.OWNER_UID, 'path': self.FOLDER_PATH + '/subfolder'})
        self.create_group_and_upload_files()

        resp = self.json_ok('lenta_block_list', {'uid': self.GUEST_UID,
                                                 'modify_uid': self.OWNER_UID,
                                                 'amount': '1',
                                                 'media_type': 'image',
                                                 'mtime_gte': str(self.start_upload_files_ts),
                                                 'meta': 'total_results_count,file_id',
                                                 'resource_id': str(self.guest_folder_resource_id),
                                                 'mtime_lte': str(self.start_upload_files_ts + 1000),
                                                 })

        assert isinstance(resp, list)
        assert len(resp) == 1 + 1  # 1 файл (amount=1) + сама папка

        assert resp[1]['name'] != 'subfolder'
        assert resp[1]['type'] != 'dir'

    def test_album_creation_from_lenta_content_block_with_items_from_shared_folder_with_different_paths(self):
        self.create_group_and_upload_files()  # p
        self.upload_file(self.GUEST_UID, self.FOLDER_PATH + '/sfgsd.jpg', media_type='image')
        self.json_ok('mkdir', {'uid': self.GUEST_UID, 'path': '/disk/inner'})
        self.json_ok('move', {'uid': self.GUEST_UID, 'src': self.FOLDER_PATH, 'dst': '/disk/inner/folder'})

        body = {
            "files_count": 1,
            "media_type": "image",
            "folder_id": self.guest_folder_resource_id.serialize(),
            "type": "content_block",
            "modifier_uid": self.GUEST_UID,
            "mfrom": 0,
        }

        response = self.json_ok('lenta_block_public_link', {'uid': self.GUEST_UID}, json=body)

        self.json_ok('public_album_check', {'public_key': response['public']['public_key']})
        album_items = self.json_ok('public_album_items_list', {'public_key': response['public']['public_key']})
        assert len(album_items) == 1

        body = {
            "files_count": 3,
            "media_type": "image",
            "folder_id": self.owner_folder_resource_id.serialize(),
            "type": "content_block",
            "modifier_uid": self.OWNER_UID,
            "mfrom": 0,
        }

        response = self.json_ok('lenta_block_public_link', {'uid': self.OWNER_UID}, json=body)

        self.json_ok('public_album_check', {'public_key': response['public']['public_key']})
        album_items = self.json_ok('public_album_items_list', {'public_key': response['public']['public_key']})
        assert len(album_items) == 3


class LentaCreateAlbumFromBlockTestCase(DiskTestCase):
    endpoint = 'lenta_create_album_from_block'

    def test_default_behavior(self):
        self.upload_file(self.uid, '/disk/test1.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/test2.jpg', media_type='image')

        title = u'Сан-Франциско'
        args = {
            'uid': self.uid,
            'path': '/disk',
            'mtime_gte': '0',
            'meta': '',
            'title': title
        }
        stid = '100500'
        with KladunStub():
            with PreviewerStub(album_preview_stid=stid):
                response = self.json_ok(self.endpoint, args)
                assert 'id' in response
                assert 'title' in response
                assert response['title'] == title
                assert 'social_cover_stid' in response
                assert response['social_cover_stid'] == stid
                assert 'layout' in response
                assert response['layout'] == 'waterfall'  # default
                assert 'flags' in response
                assert response['flags'] == ['show_tags']
                assert 'items' in response
                assert len(response['items']) == 2

                f1, f2 = response['items']
                assert 'object' in f1
                assert 'ctime' in f1['object']
                assert 'etime' in f1['object']
                assert 'id' in f1['object']
                assert 'meta' in f1['object']
                assert 'mtime' in f1['object']
                assert 'name' in f1['object']
                assert 'path' in f1['object']
                assert 'utime' in f1['object']

    def test_too_big_block(self):
        """Протестировать, что при переданном слишком большом блоке должна быть возвращена 409 ошибка (код 205)."""
        self.create_user(self.uid, noemail=True)
        path = '/disk/test_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.upload_file(self.uid, '/disk/test_folder/test1.jpg')
        self.upload_file(self.uid, '/disk/test_folder/test2.jpg')
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with mock.patch('mpfs.core.lenta.logic.create_album_from_block.MAX_BLOCK_SIZE_TO_CREATE_ALBUM_FOR', 1):
            args = {
                'uid': self.uid,
                'path': path,
                'mtime_gte': '0',
                'meta': '',
                'amount': 3,
                'title': u'Сан-Франциско'
            }
            self.json_error(self.endpoint, args, code=205)

    def test_empty_block(self):
        """Протестировать, что при переданном пустом блоке должна быть возвращена 404 ошибка (код 206)."""
        self.create_user(self.uid, noemail=True)
        path = '/disk/test_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with mock.patch('mpfs.core.lenta.logic.create_album_from_block.MAX_BLOCK_SIZE_TO_CREATE_ALBUM_FOR', 1):
            args = {
                'uid': self.uid,
                'path': path,
                'mtime_gte': '0',
                'meta': '',
                'title': u'Сан-Франциско'
            }
            self.json_error(self.endpoint, args, code=206)


class LentaUserInitTestCase(DiskTestCase):
    def test_user_init_event_history_log_muted(self):
        """
        Проверяем, что при инициализации пользователя в лог истории событий ни чего не пишется,
        чтоб не создавались всякие блоки в Ленте.
        """
        with mock.patch('mpfs.engine.process.get_event_history_log') as get_event_history_log_mock:
            self.json_ok('user_init', opts={'uid': self.uid})
            get_event_history_log_mock.assert_not_called()

            # Везде кроме юзер инита история должна работать и стэйт рубильника отключения логирования после юзер инита
            # должен сбрасываться.
            self.json_ok('mkdir', opts={'uid': self.uid, 'path': '/disk/test_folder'})
            assert len(get_event_history_log_mock.mock_calls) > 0


class ResourceVisibilityForUsersTestCase(CommonSharingMethods):

    def test_common_resource(self):
        self.json_ok('user_init', {'uid': self.user_1.uid})

        self.upload_file(self.uid, '/disk/1.jpg')
        resource_id = self.json_ok(
            'info', {'uid': self.uid, 'path': '/disk/1.jpg', 'meta': 'resource_id'})['meta']['resource_id']

        resource = get_resource_by_resource_id(self.uid, ResourceId.parse(resource_id))
        assert is_resource_visible_for_user(resource, self.uid)
        assert not is_resource_visible_for_user(resource, self.user_1.uid)

    def test_shared_resource(self):
        self.json_ok('user_init', {'uid': self.user_1.uid})
        self.json_ok('user_init', {'uid': self.user_3.uid})

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/shared'})
        self.create_group(path='/disk/shared')
        hash_ = self.invite_user(uid=self.user_1.uid, email=self.user_1.email, path='/disk/shared')
        self.activate_invite(uid=self.user_1.uid, hash=hash_)

        self.upload_file(self.uid, '/disk/shared/1.jpg')
        resource_id = self.json_ok(
            'info', {'uid': self.uid, 'path': '/disk/shared/1.jpg', 'meta': 'resource_id'})['meta']['resource_id']

        resource = get_resource_by_resource_id(self.uid, ResourceId.parse(resource_id))
        assert is_resource_visible_for_user(resource, self.uid)
        assert is_resource_visible_for_user(resource, self.user_1.uid)
        assert not is_resource_visible_for_user(resource, self.user_3.uid)

        resource = get_resource_by_resource_id(self.user_1.uid, ResourceId.parse(resource_id))
        assert is_resource_visible_for_user(resource, self.uid)
        assert is_resource_visible_for_user(resource, self.user_1.uid)
        assert not is_resource_visible_for_user(resource, self.user_3.uid)


class LentaBlockPublicLinkTestCase(DiskTestCase):
    def test_content_block_album_publication(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder', 'meta': ''})['meta']['resource_id']
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test2.jpg', media_type='image')

        body = {
            "files_count": 2,
            "media_type": "image",
            "folder_id": resource_id,
            "type": "content_block",
            "modifier_uid": self.uid,
            "mfrom": 0,
        }
        response = self.json_ok('lenta_block_public_link', {'uid': self.uid}, json=body)

        # проверка форматера альбома. Путь добавляется только в нём
        assert response['cover']['object']['path']

        self.json_ok('public_album_check', {'public_key': response['public']['public_key']})
        album_items = self.json_ok('public_album_items_list', {'public_key': response['public']['public_key']})
        assert len(album_items) == 2

    def test_content_block_file_publication(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder', 'meta': ''})['meta']['resource_id']
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')

        body = {
            "files_count": 1,
            "media_type": "image",
            "folder_id": resource_id,
            "type": "content_block",
            "modifier_uid": self.uid,
            "mfrom": 0,
        }
        response = self.json_ok('lenta_block_public_link', {'uid': self.uid}, json=body)

        self.json_ok('public_album_check', {'public_key': response['public']['public_key']})
        album_items = self.json_ok('public_album_items_list', {'public_key': response['public']['public_key']})
        assert len(album_items) == 1

    def test_content_block_album_max_size(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder', 'meta': ''})['meta']['resource_id']
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test2.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test3.jpg', media_type='image')

        body = {
            "files_count": 3,
            "media_type": "image",
            "folder_id": resource_id,
            "type": "content_block",
            "modifier_uid": self.uid,
            "mfrom": 0,
        }
        with mock.patch('mpfs.core.lenta.logic.lenta_block_public_link.MAX_BLOCK_SIZE_TO_CREATE_ALBUM_FOR', 2):
            response = self.json_ok('lenta_block_public_link', {'uid': self.uid}, json=body)

        self.json_ok('public_album_check', {'public_key': response['public']['public_key']})
        album_items = self.json_ok('public_album_items_list', {'public_key': response['public']['public_key']})
        assert len(album_items) == 2

    @parameterized.expand([
        ('photo_remind_block'),
        ('photo_selection_block'),
    ])
    def test_album_publication(self, block_type):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test2.jpg', media_type='image')
        resource_id_1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test1.jpg', 'meta': ''})['meta']['resource_id']
        resource_id_2 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test2.jpg', 'meta': ''})['meta']['resource_id']

        body = {
            "resource_ids": [
                resource_id_1,
                resource_id_2,
            ],
            "type": block_type,
        }
        response = self.json_ok('lenta_block_public_link', {'uid': self.uid}, json=body)

        self.json_ok('public_album_check', {'public_key': response['public']['public_key']})
        album_items = self.json_ok('public_album_items_list', {'public_key': response['public']['public_key']})
        assert len(album_items) == 2

    @parameterized.expand([
        ('photo_remind_block'),
        ('photo_selection_block'),
    ])
    def test_file_publication(self, block_type):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test1.jpg', 'meta': ''})['meta']['resource_id']

        body = {
            "resource_ids": [
                resource_id,
            ],
            "type": block_type,
        }
        response = self.json_ok('lenta_block_public_link', {'uid': self.uid}, json=body)

        self.json_ok('public_album_check', {'public_key': response['public']['public_key']})
        album_items = self.json_ok('public_album_items_list', {'public_key': response['public']['public_key']})
        assert len(album_items) == 1

    @parameterized.expand([
        ('photo_remind_block'),
        ('photo_selection_block'),
    ])
    def test_album_max_size(self, block_type):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test2.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test3.jpg', media_type='image')
        resource_id_1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test1.jpg', 'meta': ''})['meta']['resource_id']
        resource_id_2 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test2.jpg', 'meta': ''})['meta']['resource_id']
        resource_id_3 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test3.jpg', 'meta': ''})['meta']['resource_id']

        body = {
            "resource_ids": [
                resource_id_1,
                resource_id_2,
                resource_id_3,
            ],
            "type": block_type,
        }
        with mock.patch('mpfs.core.lenta.logic.lenta_block_public_link.MAX_BLOCK_SIZE_TO_CREATE_ALBUM_FOR', 2):
            response = self.json_ok('lenta_block_public_link', {'uid': self.uid}, json=body)

        self.json_ok('public_album_check', {'public_key': response['public']['public_key']})
        album_items = self.json_ok('public_album_items_list', {'public_key': response['public']['public_key']})
        assert len(album_items) == 2

    @parameterized.expand([
        ('photo_remind_block'),
        ('photo_selection_block'),
    ])
    def test_album_name_generation(self, block_type):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image', file_data={'etime': '2018-06-01T12:00:00Z'})
        self.upload_file(self.uid, '/disk/folder/test2.jpg', media_type='image', file_data={'etime': '2018-05-15T12:00:00Z'})
        resource_id_1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test1.jpg', 'meta': ''})['meta']['resource_id']
        resource_id_2 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test2.jpg', 'meta': ''})['meta']['resource_id']

        body = {
            "resource_ids": [
                resource_id_1,
                resource_id_2,
            ],
            "type": block_type,
        }
        response = self.json_ok('lenta_block_public_link', {'uid': self.uid}, json=body)
        assert response['title'] == u'15 мая 2018 – 1 июня 2018'

    @parameterized.expand([
        ('photo_remind_block'),
        ('photo_selection_block'),
    ])
    def test_album_name_generation_does_not_use_same_date_twice(self, block_type):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image', file_data={'etime': '2018-05-15T13:00:00Z'})
        self.upload_file(self.uid, '/disk/folder/test2.jpg', media_type='image', file_data={'etime': '2018-05-15T12:00:00Z'})
        resource_id_1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test1.jpg', 'meta': ''})['meta']['resource_id']
        resource_id_2 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test2.jpg', 'meta': ''})['meta']['resource_id']

        body = {
            "resource_ids": [
                resource_id_1,
                resource_id_2,
            ],
            "type": block_type,
        }
        response = self.json_ok('lenta_block_public_link', {'uid': self.uid}, json=body)
        assert response['title'] == u'15 мая 2018'

    @parameterized.expand([
        ('photo_remind_block'),
        ('photo_selection_block'),
    ])
    def test_album_name_generation_new_year_2018(self, block_type):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image', file_data={'etime': '2018-06-01T12:00:00Z'})
        self.upload_file(self.uid, '/disk/folder/test2.jpg', media_type='image', file_data={'etime': '2018-05-15T12:00:00Z'})
        resource_id_1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test1.jpg', 'meta': ''})['meta']['resource_id']
        resource_id_2 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test2.jpg', 'meta': ''})['meta']['resource_id']

        body = {
            "resource_ids": [
                resource_id_1,
                resource_id_2,
            ],
            "type": block_type,
            "icon_type": "new_year_2018_magic_wand"
        }
        response = self.json_ok('lenta_block_public_link', {'uid': self.uid}, json=body)
        assert response['title'] == u'2018 год в фотографиях'
