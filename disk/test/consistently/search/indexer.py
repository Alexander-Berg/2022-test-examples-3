# -*- coding: utf-8 -*-
import cjson
import datetime
import os
import random
import time
import urlparse
from contextlib import contextmanager

import pytest
import mock

from test.base import DiskTestCase, time_machine
from test.base_suit import set_up_open_url, tear_down_open_url
from test.common.sharing import CommonSharingMethods
from test.conftest import collected_data
from test.fixtures import users
from test.helpers.stubs.services import SearchIndexerStub, DiskSearchStub
from test.helpers.stubs.manager import StubsManager
from test.helpers.stubs.resources.users_info import empty_userinfo

from mpfs.config import settings
from mpfs.core.address import Address, ResourceId
from mpfs.core.bus import Bus
from mpfs.core.filesystem.indexer import DiskDataIndexer
from mpfs.core.services.index_service import SearchIndexer
from mpfs.core.services.search_service import DiskSearch
from mpfs.common.static import codes
from mpfs.core.factory import get_resource
from mpfs.core.social.share import Group
from test.helpers.stubs.smart_services import DiskGeoSearchSmartMockHelper
from nose_parameterized import parameterized



@contextmanager
def mock_search_indexer_push_change():
    result = list()
    prev = SearchIndexer.push_change
    try:
        SearchIndexer.push_change = lambda self, *args, **kwargs:  result.append(args[0])
        yield result
    finally:
        SearchIndexer.push_change = prev


@contextmanager
def mock_search_indexer_open_url():
    result = list()
    prev = SearchIndexer.open_url
    try:
        SearchIndexer.open_url = lambda self, *args: result.append(args[0])
        yield result
    finally:
        SearchIndexer.open_url = prev


class CommonIndexerMixin(DiskTestCase):

    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {SearchIndexerStub})

    uids = [users.usr_1.uid,
            users.user_1.uid,
            users.user_3.uid]

    file_data = {
        "meta": {
            "file_mid": "1000003.yadisk:89031628.249690056312488962060095667221",
            "digest_mid": "1000005.yadisk:89031628.3983296384177350807526090116783",
            "md5": "83e5cd52e94e3a41054157a6e33226f7",
            "sha256": "4355a46b19d348dc2f57c046f8ef63d4538ebb936000f3c9ee954a27460dd865",
        },
        "size": 10000,
        "mimetype": "text/plain",
    }

    def setup_method(self, method):
        """
        Выбираем рандомный тестовый uid (чтобы не пересекаться с другими агентами)
        Вычищаем поисковый индексатор
        Создаем тестовую папку и пару файлов
        """
        super(CommonIndexerMixin, self).setup_method(method)

        self.uid = random.choice(self.uids)
        self.run_000_user_check()

        self.clean_search_database()

        foldaddr = Address.Make(self.uid, '/disk/filesystem test folder').id
        Bus().mkdir(self.uid, foldaddr)

        faddr = Address.Make(self.uid, '/disk/filesystem test file').id
        Bus().mkfile(self.uid, faddr, data=self.file_data)

        time.sleep(2)

        faddr = Address.Make(self.uid, '/disk/filesystem test folder/test').id
        Bus().mkfile(self.uid, faddr, data=self.file_data)

    def teardown_method(self, method):
        DiskSearch().delete(self.uid)

    def clean_search_database(self):
        docs = []
        for doc_id in DiskSearch().get_all_documents_for_user(self.uid):
            docs.append({
                'action': 'delete',
                "file_id": doc_id,
                "uid": int(self.uid),
                'version': 999999999999999999,
                'operation': 'rm',
            })
        SearchIndexer().push_change(docs)


class SearchIndexerCoreTestCase(CommonIndexerMixin):
    def test_search_send_modify(self):
        """
        Проверяем, что пуши индексатору уходят
        """
        out_requests = set_up_open_url()
        faddr = Address.Make(self.uid, '/disk/filesystem test folder/testissimo').id
        file = Bus().mkfile(self.uid, faddr, data=self.file_data)
        tear_down_open_url()

        self.assertTrue(collected_data['queue_put']['a'][1]['type'] == 'search')
        search_indexed = False
        for k, v in out_requests.iteritems():
            if k.startswith(self.search_url):
                search_indexed = True
                self.assertTrue('?id=' in k)

                qs_params = urlparse.parse_qs(urlparse.urlparse(v[0]['args'][0]).query)
                assert 'resource_id' in qs_params
                assert qs_params['resource_id'][0] == file.resource_id.serialize()
                assert 'version' in qs_params
                assert int(qs_params['version'][0]) == file.version
                assert 'metric' not in qs_params

        self.assertTrue(search_indexed)

    def test_search_send_modify_system_folder(self):
        """
        Проверяем, что пуши про системные папки уходят с folder_type
        """
        def check_search_index(requests, folder_path=None,  folder_type=None):
            result = False
            self.assertTrue(collected_data['queue_put']['a'][1]['type'] == 'search')

            for k, v in requests.iteritems():
                if k.startswith(self.search_url):
                    result = True
                    self.assertTrue('?id=' in k)

                    qs_params = urlparse.parse_qs(urlparse.urlparse(v[0]['args'][0]).query)
                    assert 'resource_id' in qs_params
                    assert qs_params['resource_id']
                    assert 'version' in qs_params
                    assert qs_params['version']

            return result

        # обычный каталог, folder_type = None
        out_requests = set_up_open_url()
        args = {'uid': self.uid, 'path': '/disk/just_dir'}
        self.json_ok('mkdir', args)
        tear_down_open_url()

        search_indexed = check_search_index(out_requests, folder_path='/disk/just_dir', folder_type=None)
        self.assertTrue(search_indexed)

        # системный каталог, folder_type = photostream
        out_requests = set_up_open_url()
        args = {'uid': self.uid, 'type': 'photostream'}
        self.json_ok('mksysdir', args)
        tear_down_open_url()

        search_indexed = check_search_index(out_requests, folder_path=u'/disk/Фотокамера', folder_type='photostream')
        self.assertTrue(search_indexed)

    def test_indexer_mill(self):
        """
        Проверяем, что пуши уходят порционно для массовых операций
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/t'})
        limit = settings.indexer['cumulative_operations_items_limit']
        for i in xrange(limit + 2):
            faddr = Address.Make(self.uid, '/disk/t/t_%i' % i).id
            Bus().mkfile(self.uid, faddr, data=self.file_data)

        for i, op in enumerate(('copy', 'move')):
            with mock_search_indexer_push_change() as data:
                self.json_ok(op, {'uid': self.uid, 'src': '/disk/t', 'dst': '/disk/cp%i' % i})
                # отправляем в два захода
                self.assertEqual(len(data), 2)
                # в первой отправке limit + 1 данных
                self.assertEqual(len(data[0]), limit + 1)
        self.clean_search_database()

    def test_user_action_param(self):
        """
        Проставление query string параметра operation
        """
        with mock_search_indexer_open_url() as data:
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/t'})
            for i in xrange(2):
                faddr = Address.Make(self.uid, '/disk/t/t_%i' % i).id
                Bus().mkfile(self.uid, faddr, data=self.file_data)
            self.json_ok('copy', {'uid': self.uid, 'src': '/disk/t', 'dst': '/disk/cp'})
            for url in data:
                self.assertIn('action=', url)
        self.clean_search_database()

    def test_search_reindex(self):
        """
        Проверем полную переиндексацию
        Убеждаемся, что отправили и отправили ровно столько, сколько надо
        """
        # получаем общее число ресурсов
        opts = {'uid': self.uid}
        diff = self.json_ok('diff', opts)
        resource_ids_from_diff = {ResourceId(self.uid, x['fid']).serialize() for x in diff['result']}
        total_amount = diff['amount']
        files_by_name = {x['key']: x for x in diff['result']}

        # индексируем
        out_requests = set_up_open_url()
        DiskDataIndexer().search_reindex(self.uid, index_body=0)
        tear_down_open_url()

        # ползаем по запросам к индексатору
        search_indexed = False
        total_count = 0
        pushed_resource_ids = set()
        for k, v in out_requests.iteritems():
            if k.startswith(self.search_url):
                self.assertTrue('?id=' in k)
                search_indexed = True

                for item in v:
                    total_count += 1
                    qs_params = urlparse.parse_qs(urlparse.urlparse(item['args'][0]).query)
                    assert 'resource_id' in qs_params
                    pushed_resource_ids.add(qs_params['resource_id'][0])
                    assert 'version' in qs_params
                    assert int(qs_params['version'][0]) <= int(diff['version'])


        # проверяем сам факт отправки
        self.assertTrue(search_indexed)

        assert resource_ids_from_diff == pushed_resource_ids

        # количество отпрвленных элементов = 4
        # это три созданных нами элемента + дефолтная Музыка
        self.assertEqual(total_count, total_amount)

    def test_search_reindex_with_trash(self):
        """
        Проверка переиндексации с учетом треша
        """
        # получаем общее число ресурсов
        opts = {'uid': self.uid}

        # махинации с корзиной
        faddr = Address.Make(self.uid, '/disk/filesystem test folder/test').id
        trash_append_result = Bus().trash_append(self.uid, faddr)
        trashed_resource_id = ResourceId(self.uid, trash_append_result['this']['meta']['file_id']).serialize()

        diff = self.json_ok('diff', opts)
        total_amount = diff['amount']

        # индексируем
        out_requests = set_up_open_url()
        DiskDataIndexer().search_reindex(self.uid, index_body=0)
        tear_down_open_url()

        # ползаем по запросам к индексатору
        disk_indexed = False
        total_count = 0
        trash_indexed = False
        for k, v in out_requests.iteritems():
            if k.startswith(self.search_url):
                self.assertTrue('?id=' in k)
                disk_indexed = True

                for item in v:
                    total_count += 1

                    qs_params = urlparse.parse_qs(urlparse.urlparse(item['args'][0]).query)
                    assert 'resource_id' in qs_params
                    if qs_params['resource_id'][0] == trashed_resource_id:
                        trash_indexed = True
                    assert 'version' in qs_params
                    assert int(qs_params['version'][0]) <= int(diff['version'])

        # проверяем факт отправки
        self.assertTrue(disk_indexed)

        # проверяем факт отправки из треша
        self.assertTrue(trash_indexed)

        # количество отпрвленных элементов = 4
        # это три созданных нами элемента + дефолтная Музыка. Прибавляем 1 так как один из файлов удалили
        self.assertEqual(total_count, total_amount + 1)

    def test_search_reindex_with_photounlim(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.upload_file(self.uid, '/photounlim/0.jpg')
        photosliced_resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/0.jpg', 'meta': ''})['meta']['resource_id']

        opts = {'uid': self.uid}
        diff = self.json_ok('diff', opts)
        total_amount = diff['amount']

        # индексируем
        out_requests = set_up_open_url()
        DiskDataIndexer().search_reindex(self.uid, index_body=0)
        tear_down_open_url()

        # ползаем по запросам к индексатору
        disk_indexed = False
        total_count = 0
        photounlim_indexed = False
        for k, v in out_requests.iteritems():
            if k.startswith(self.search_url):
                self.assertTrue('?id=' in k)
                disk_indexed = True

                for item in v:
                    total_count += 1

                    qs_params = urlparse.parse_qs(urlparse.urlparse(item['args'][0]).query)
                    assert 'resource_id' in qs_params
                    if qs_params['resource_id'][0] == photosliced_resource_id:
                        photounlim_indexed = True
                    assert 'version' in qs_params
                    if qs_params['resource_id'][0] != photosliced_resource_id:
                        assert int(qs_params['version'][0]) <= int(diff['version'])

        # проверяем факт отправки
        self.assertTrue(disk_indexed)

        # проверяем факт отправки из треша
        self.assertTrue(photounlim_indexed)

        # количество отпрвленных элементов = 4 + 2 фотоамлимных не попадают в дифф
        # это три созданных нами элемента + дефолтная Музыка + 2 фотоанлимных ресурса
        self.assertEqual(total_count, total_amount + 2)

    def test_search_reindex_with_mediatype(self):
        """
        Проверем полную переиндексацию с медиатипом
        """
        # загружаем пару файлов
        self.upload_file(self.uid, '/disk/video1.vid', media_type='video')
        self.upload_file(self.uid, '/disk/video2.vid', media_type='video')

        # получаем общее число ресурсов
        diff = self.json_ok('diff', {'uid': self.uid})
        video_resources = filter(lambda x: x.get('media_type') == 'video', diff['result'])
        video_resource_ids_from_diff = {ResourceId(self.uid, x['fid']).serialize() for x in video_resources}
        video_amount = len(video_resources)

        # индексируем
        out_requests = set_up_open_url()
        DiskDataIndexer().search_reindex(self.uid, index_body=0, mediatype=['video'])
        tear_down_open_url()

        # ползаем по запросам к индексатору
        disk_indexed = False
        total_count = 0
        pushed_resource_ids = set()
        for k, v in out_requests.iteritems():
            if k.startswith(self.search_url):
                assert '?id=' in k
                disk_indexed = True

                for item in v:
                    total_count += 1
                    qs_params = urlparse.parse_qs(urlparse.urlparse(item['args'][0]).query)
                    assert 'resource_id' in qs_params
                    pushed_resource_ids.add(qs_params['resource_id'][0])
                    assert 'version' in qs_params
                    assert int(qs_params['version'][0]) <= int(diff['version'])

        # проверяем факт отправки
        assert disk_indexed

        # отправили пуши для всех ресурсов
        assert video_resource_ids_from_diff == pushed_resource_ids

        # проверяем факт совпадения количества
        assert video_amount == total_count

    def test_sending_metric_for_photoslice_repairing(self):
        path = '/disk/1.jpg'
        file_id = '1' * 64
        search_open_mocker = mock.patch(
            'mpfs.core.services.search_service.SearchDB.open_url',
            return_value='{"hitsCount":1,"hitsArray":[{"key":"%s","id":"%s","etime":1,"ctime":1,"mimetype":"image/jpeg"}]}' % (
            path, file_id))
        feature_toggle_mock = mock.patch('mpfs.core.base.FEATURE_NOTIFY_PHOTOSLICE_INDEX_WHEN_FILE_NOT_FOUND', True)
        photoslice_notification_mock = mock.patch(
            'mpfs.core.job_handlers.indexer.INDEXER_PHOTOSLICE_NOTIFICATION_ON_INDEXER_SIDE', True)

        out_requests = set_up_open_url()
        with search_open_mocker, \
                feature_toggle_mock, \
                photoslice_notification_mock:
            self.json_error('info', {'uid': self.uid, 'path': path}, code=71)
        tear_down_open_url()
        request_data = [v for k, v in out_requests.iteritems() if k.startswith(self.search_url)][0]
        assert request_data
        for i in request_data:
            qs_params = urlparse.parse_qs(urlparse.urlparse(i['args'][0]).query)
            assert qs_params['metric'][0] == '404_info_repair'


class IndexerInterfaceTestCase(CommonIndexerMixin):
    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {DiskSearchStub})

    def test_new_search_does_not_send_key_for_searches_in_disk_root(self):
        open_url_data = set_up_open_url()
        self.json_ok('new_search', {'uid': self.uid, 'path': '/disk', 'query': 'my_file'})
        tear_down_open_url()
        assert 'key' not in urlparse.parse_qs(urlparse.urlparse(open_url_data.keys()[0]).query)

    def test_new_search_sends_key_for_searches_in_trash_root(self):
        open_url_data = set_up_open_url()
        self.json_ok('new_search', {'uid': self.uid, 'path': '/trash', 'query': 'my_file'})
        tear_down_open_url()
        assert urlparse.parse_qs(urlparse.urlparse(open_url_data.keys()[0]).query)['key'] == ['/trash/*']

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_query(self):
        """
        Тупое тестирование запроса
        Возвращает четыре элемента
        - /disk, потому что так надо
        - /disk/filesystem test folder, потому что test в названии
        - /disk/filesystem test file, потому что test  в названии
        - /disk/filesystem test folder/test сами знаете почему
        """
        opts = {
            'uid': self.uid,
            'path': Address.Make(self.uid, '/disk').id,
            'query': 'test',
            'meta': 'mediatype',
        }
        result = self.json_ok('new_search', opts)['results']

        self.assertEqual(len(result), 4)

        found_file = False
        for item in result:
            if item['type'] == 'file':
                self.assertTrue('mediatype' in item['meta'])
                found_file = True

        self.assertTrue(found_file)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_query_with_sort(self):
        """
        Тестируем сортировку
        Файлы отличаются по mtime, потому что мы делали двухсекундную задержку для них
        """
        # desc
        opts = {
            'uid': self.uid,
            'path': Address.Make(self.uid, '/disk').id,
            'query': 'test',
            'sort': 'mtime',
            'order': '0',
        }
        result = self.json_ok('new_search', opts)['results']

        lower_element = result[3]
        upper_element = result[2]

        self.assertTrue(lower_element['mtime'] > upper_element['mtime'])

        # asc
        opts = {
            'uid': self.uid,
            'path': Address.Make(self.uid, '/disk').id,
            'query': 'test',
            'sort': 'mtime',
            'order': '1',
        }
        result = self.json_ok('new_search', opts)['results']

        lower_element = result[3]
        upper_element = result[2]

        self.assertTrue(lower_element['mtime'] <= upper_element['mtime'])

    @pytest.mark.xfail(reason="https://st.yandex-team.ru/CHEMODAN-13196")
    def test_query_with_paging_amount(self):
        """Проверяем параметр amount.

        В ответе должно быть amount + 1 элементов (+ корневой элемент для поиска).

        """
        opts = {
            'uid': self.uid,
            'path': Address.Make(self.uid, '/disk').id,
            'query': 'test',
            'sort': 'mtime',
            'amount': '2',
            'offset': '0'
        }

        result_0_2 = self.json_ok('new_search', opts)['results']
        self.assertEqual(len(result_0_2), 3)

    @pytest.mark.xfail(reason="https://st.yandex-team.ru/CHEMODAN-13196")
    def test_query_with_paging_offset(self):
        """Проверяем параметр offset."""
        opts = {
            'uid': self.uid,
            'path': Address.Make(self.uid, '/disk').id,
            'query': 'test',
            'sort': 'mtime',
            'amount': '2',
            'offset': '2'
        }
        result_2_4 = self.json_ok('new_search', opts)['results']
        self.assertEqual(len(result_2_4), 2)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_query_trash(self):
        """
        Помещаем файл в корзину и ищем его там
        """
        opts = {
            'uid': self.uid,
            'path': Address.Make(self.uid, '/disk/filesystem test folder/test').id
        }

        self.json_ok('trash_append', opts)

        opts = {
            'uid': self.uid,
            'path': Address.Make(self.uid, '/trash').id,
            'query': 'test',
            'meta': '',
        }
        result = self.json_ok('new_search', opts)['results']

        found_file = False
        for item in result:
            if item['type'] == 'file':
                self.assertTrue('mediatype' in item['meta'])
                self.assertEqual(item['meta']['original_parent_id'], u'/disk/filesystem test folder/')
                self.assertEqual(item['path'], u'/trash/test')
                found_file = True

        self.assertTrue(found_file)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_query_strange_symbols(self):
        """
        Делаем запросы со странными символами
        Убеждаемся, что они работают, хотя ничего и не ищут
        """
        opts = {
            'uid': self.uid,
            'path': Address.Make(self.uid, '/disk').id,
            'query': '%s' % "  #\(   ){}[]'?+:!-^  ",
        }
        result = self.json_ok('new_search', opts)['results']
        self.assertTrue(result is not None)
        self.assertTrue(len(result) > 0)

        opts = {
            'uid': self.uid,
            'path': Address.Make(self.uid, '/disk').id,
            'query': '%s' % "*",
        }
        result = self.json_ok('new_search', opts)['results']
        self.assertTrue(result is not None)
        self.assertTrue(len(result) > 0)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_query_with_autocorrection(self):
        '''
        Проверяем работу автокорректора запросов

        Сначала делаем неправильный запрос с ним, смотрим:
        - запрос был модифицирован
        - результаты есть

        Далее делаем неправильный запрос с форсом, смотрим:
        - запрос не был модифицирован
        - результатов нет
        '''
        mistyped_text = "testt"
        correct_text = "test"

        opts = {
            'uid': self.uid,
            'path': Address.Make(self.uid, '/disk').id,
            'query': mistyped_text
        }

        result_usual = self.json_ok('new_search', opts)
        self.assertEqual(result_usual['query'], correct_text)
        self.assertGreater(len(result_usual["results"]), 1)

        opts = {
            'uid': self.uid,
            'path': Address.Make(self.uid, '/disk').id,
            'query': mistyped_text,
            'force': 1
        }

        result_force = self.json_ok('new_search', opts)
        self.assertEqual(result_force['query'], mistyped_text)
        self.assertEqual(len(result_force["results"]), 1)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-74846')
    def test_do_not_send_pushes_if_nothing_to_push(self):
        # В некоторых ситуация отправляли пустой список docs в индексатор. Например, при удалении пользователя из-за
        # отсутствия file_id у /disk в пуш улетит пустой список
        # https://st.yandex-team.ru/CHEMODAN-42703

        # удаляем заранее созданные файлы, они нам не нужны, оставляем только /disk
        for path in ('/disk/filesystem test file', '/disk/filesystem test folder/test', '/disk/filesystem test folder'):
            self.json_ok('rm', {'uid': self.uid, 'path': path})

        with mock.patch('mpfs.core.user.common.CommonUser.is_b2b', return_value=True):
            out_requests = set_up_open_url()
            self.json_ok('async_user_remove', {'uid': self.uid})
            tear_down_open_url()
        assert not [x for x in out_requests.keys() if urlparse.urlparse(settings.services['search_indexer']['base_url']).hostname in x]


class SendingResourceIdToIndexerForBulkIndexerOperationsTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(SendingResourceIdToIndexerForBulkIndexerOperationsTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.xiva_subscribe(self.uid)

        self.create_user(self.uid_1)
        self.xiva_subscribe(self.uid_1)

        self.create_user(self.uid_3)
        self.xiva_subscribe(self.uid_3)

        self.shared_folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.shared_folder_path })
        self.gid = self.create_group(path=self.shared_folder_path )
        hash_ = self.invite_user(uid=self.uid_1, email=self.email_1, path=self.shared_folder_path )
        self.activate_invite(uid=self.uid_1, hash=hash_)
        self.upload_file(self.uid, path=self.shared_folder_path + '/test.jpg')

    def test_rm_folder_with_shared_by_guest(self):
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/contains_shared_folder'})
        self.json_ok('move', {'uid': self.uid_1, 'src': self.shared_folder_path, 'dst': '/disk/contains_shared_folder/shared_folder'})

        correct_path_to_rid = self._get_path_to_resource_id_for_resources_in_disk_root(self.uid_1)

        with mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexing_data', wraps=DiskDataIndexer().indexing_data) as index_mock:
            self.json_ok('rm', {'uid': self.uid_1, 'path': '/disk/contains_shared_folder'})
            path_to_rid = {x[0][0]['id']: x[0][0].get('resource_id') for x in index_mock.call_args_list}

        assert path_to_rid == correct_path_to_rid

    def test_accept_invite(self):
        with mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexing_data', wraps=DiskDataIndexer().indexing_data) as index_mock:
            hash_ = self.invite_user(uid=self.uid_3, email=self.email_3, path=self.shared_folder_path)
            self.activate_invite(uid=self.uid_3, hash=hash_)
            path_to_rid = {x[0][0]['id']: x[0][0].get('resource_id') for x in index_mock.call_args_list}

        correct_path_to_rid = self._get_path_to_resource_id_for_resources_in_disk_root(self.uid_3)
        assert path_to_rid == correct_path_to_rid

    def test_guest_leaves_shared_folder(self):
        correct_path_to_rid = self._get_path_to_resource_id_for_resources_in_disk_root(self.uid_1)
        with mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexing_data', wraps=DiskDataIndexer().indexing_data) as index_mock:
            self.json_ok('rm', {'uid': self.uid_1, 'path': self.shared_folder_path})
            path_to_rid = {x[0][0]['id']: x[0][0].get('resource_id') for x in index_mock.call_args_list}

        assert path_to_rid == correct_path_to_rid

    def test_rm_from_owner_dir_of_deleted_user_sends_resource_id(self):
        # Очень странный случай, что при выполнении rm в ОП удаленного в Паспорте пользователя не отправляем resource_id
        # https://st.yandex-team.ru/CHEMODAN-42703
        with empty_userinfo(self.uid), \
                mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexing_data', wraps=DiskDataIndexer().indexing_data) as index_mock:
            self.json_ok('rm', {'uid': self.uid_1, 'path': self.shared_folder_path + '/test.jpg'})
        assert 'resource_id' in index_mock.call_args[0][0]

    def test_upload_with_force_from_owner_dir_of_deleted_user_sends_resource_id(self):
        # Очень странный случай, что при выполнении rm в ОП удаленного в Паспорте пользователя не отправляем resource_id
        # https://st.yandex-team.ru/CHEMODAN-42703
        with empty_userinfo(self.uid), \
                mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexing_data', wraps=DiskDataIndexer().indexing_data) as index_mock:
            self.upload_file(self.uid_1, path=self.shared_folder_path + '/test.jpg')
        assert 'resource_id' in index_mock.call_args[0][0]

    def test_owner_unshare_folder_for_several_uids_and_one_moved_shared_folder(self):
        correct_path_to_rid_owner = self._get_path_to_resource_id_for_resources_in_disk_root(self.uid)
        correct_path_to_rid_guest_1 = self._get_path_to_resource_id_for_resources_in_disk_root(self.uid_1)

        hash_ = self.invite_user(uid=self.uid_3, email=self.email_3, path=self.shared_folder_path)
        self.activate_invite(uid=self.uid_3, hash=hash_)
        mover_path = '/disk/shared_move'
        self.json_ok('move', {'uid': self.uid_3, 'src': self.shared_folder_path, 'dst': mover_path})

        correct_path_to_rid_guest_2 = self._get_path_to_resource_id_for_resources_in_disk_root(self.uid_3)

        with mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexing_data', wraps=DiskDataIndexer().indexing_data) as index_mock:
            self.json_ok('share_unshare_folder', {'uid': self.uid, 'gid': self.gid})
            path_to_rid_owner = {x[0][0]['id']: x[0][0].get('resource_id') for x in
                                 [i for i in index_mock.call_args_list if i[0][0]['uid'] == self.uid]}
            path_to_rid_guest_1 = {x[0][0]['id']: x[0][0].get('resource_id') for x in
                                   [i for i in index_mock.call_args_list if i[0][0]['uid'] == self.uid_1]}
            path_to_rid_guest_2 = {x[0][0]['id']: x[0][0].get('resource_id') for x in
                                   [i for i in index_mock.call_args_list if i[0][0]['uid'] == self.uid_3]}

        assert path_to_rid_owner == correct_path_to_rid_owner
        assert path_to_rid_guest_1 == correct_path_to_rid_guest_1
        assert path_to_rid_guest_2 == correct_path_to_rid_guest_2

    def test_trash_drop_all(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_to_delete'})
        self.upload_file(self.uid, path='/disk/folder_to_delete/file_to_delete')
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/folder_to_delete'})['this']['id']

        res = self.json_ok('info', {'uid': self.uid, 'path': trash_path, 'meta': 'resource_id'})
        correct_path_to_rid = {res['path']: res['meta']['resource_id']}
        res = self.json_ok(
            'info', {'uid': self.uid, 'path': os.path.join(trash_path, 'file_to_delete'), 'meta': 'resource_id'}
        )
        correct_path_to_rid.update({res['path']: res['meta']['resource_id']})

        with mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexing_data', wraps=DiskDataIndexer().indexing_data) as index_mock:
            self.json_ok('trash_drop_all', {'uid': self.uid})
            path_to_rid = {x[0][0]['id']: x[0][0].get('resource_id') for x in index_mock.call_args_list}

        assert path_to_rid == correct_path_to_rid

    def test_trash_drop_element_subfolder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_to_delete'})
        self.upload_file(self.uid, path='/disk/folder_to_delete/file_to_delete_1')
        self.upload_file(self.uid, path='/disk/folder_to_delete/file_to_delete_2')
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/folder_to_delete'})['this']['id']

        res = self.json_ok('info', {'uid': self.uid, 'path': trash_path, 'meta': 'resource_id'})
        correct_path_to_rid = {res['path']: res['meta']['resource_id']}
        res = self.json_ok('info', {'uid': self.uid, 'path': os.path.join(trash_path, 'file_to_delete_1'), 'meta': 'resource_id'})
        correct_path_to_rid.update({res['path']: res['meta']['resource_id']})
        res = self.json_ok('info', {'uid': self.uid, 'path': os.path.join(trash_path, 'file_to_delete_2'), 'meta': 'resource_id'})
        correct_path_to_rid.update({res['path']: res['meta']['resource_id']})

        with mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexing_data', wraps=DiskDataIndexer().indexing_data) as index_mock:
            self.json_ok('trash_drop', {'uid': self.uid, 'path': trash_path})
            path_to_rid = {x[0][0]['id']: x[0][0].get('resource_id') for x in index_mock.call_args_list}

        assert path_to_rid == correct_path_to_rid

    def test_trash_drop_element_single_file(self):
        self.upload_file(self.uid, path='/disk/file_to_delete')
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/file_to_delete'})['this']['id']

        res = self.json_ok('info', {'uid': self.uid, 'path': trash_path, 'meta': 'resource_id'})
        correct_path_to_rid = {res['path']: res['meta']['resource_id']}

        with mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexing_data', wraps=DiskDataIndexer().indexing_data) as index_mock:
            self.json_ok('trash_drop', {'uid': self.uid, 'path': trash_path})
            path_to_rid = {x[0][0]['id']: x[0][0].get('resource_id') for x in index_mock.call_args_list}

        assert path_to_rid == correct_path_to_rid

    def _get_path_to_resource_id_for_resources_in_disk_root(self, uid):
        correct_path_to_rid = {}
        json = {'iteration_key': ''}
        while True:
            res = self.json_ok('snapshot', {'uid': uid}, json=json)
            correct_path_to_rid.update({x['path']: x['meta']['resource_id'] for x in res['items']})
            if not res['iteration_key']:
                break
            json = {'iteration_key': res['iteration_key']}

        # snapshot используется только для того, чтобы получить соержимое диска. В индексатор /disk никогда не
        # отправляем, поэтому выкидываем его оттуда
        correct_path_to_rid.pop('/disk', None)
        return correct_path_to_rid


class IndexerFakeVersionForSharedResourcesTestCase(CommonSharingMethods):

    def create_folder_with_resources(self):
        self.create_user(self.uid)
        self.xiva_subscribe(self.uid)

        self.create_user(self.uid_1)
        self.xiva_subscribe(self.uid_1)

        self.create_user(self.uid_3)
        self.xiva_subscribe(self.uid_3)

        self.shared_folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.shared_folder_path})

        self.upload_file(self.uid, path=self.shared_folder_path + '/test1.jpg')

        self.shared_subfolder_path = self.shared_folder_path + '/subfolder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.shared_subfolder_path})
        self.upload_file(self.uid, path=self.shared_folder_path + '/subfolder/test2.jpg')

    def test_versions_for_file_in_shared_folder_increases_after_reinvite_to_folder(self):
        self.create_folder_with_resources()

        self.gid = self.create_group(path=self.shared_folder_path)
        hash_ = self.invite_user(uid=self.uid_1, email=self.email_1, path=self.shared_folder_path)

        with SearchIndexerStub() as index_service:
            self.activate_invite(uid=self.uid_1, hash=hash_)

            group = Group.find(gid=self.gid, link=False, group=True)
            group_link_version_1 = group.get_group_link_base_version(self.uid_1)

            for call_args in index_service.push_change.call_args_list:
                indexer_data = call_args[0][0]
                for push in indexer_data:
                    resource = get_resource(push['uid'], push['id'])
                    if str(push['uid']) == str(self.uid):
                        assert 'shared_folder_version' not in push
                    else:
                        if push['id'] == self.shared_folder_path:
                            assert 'shared_folder_version' not in push
                            continue

                        assert push['shared_folder_version'] == int(resource.version) + group_link_version_1

        self.leave_group(self.uid_1, self.gid)
        self.json_error('info', {'uid': self.uid_1, 'path': self.shared_folder_path}, code=codes.RESOURCE_NOT_FOUND)

        hash_ = self.invite_user(uid=self.uid_1, email=self.email_1, path=self.shared_folder_path)
        with SearchIndexerStub() as index_service:
            self.activate_invite(uid=self.uid_1, hash=hash_)

            group = Group.find(gid=self.gid, link=False, group=True)
            group_link_version_2 = group.get_group_link_base_version(self.uid_1)
            assert group_link_version_2 > group_link_version_1

            for call_args in index_service.push_change.call_args_list:
                indexer_data = call_args[0][0]
                for push in indexer_data:
                    resource = get_resource(push['uid'], push['id'])
                    if str(push['uid']) == str(self.uid):
                        assert 'shared_folder_version' not in push
                    else:
                        if push['id'] == self.shared_folder_path:
                            assert 'shared_folder_version' not in push
                            continue

                        assert push['shared_folder_version'] == int(resource.version) + group_link_version_2

    def test_pushes_for_creating_resources_in_shared_folder(self):
        self.create_folder_with_resources()

        self.gid = self.create_group(path=self.shared_folder_path)
        hash_ = self.invite_user(uid=self.uid_1, email=self.email_1, path=self.shared_folder_path)

        self.activate_invite(uid=self.uid_1, hash=hash_)

        group = Group.find(gid=self.gid, link=False, group=True)
        group_link_version = group.get_group_link_base_version(self.uid_1)

        with SearchIndexerStub() as index_service:
            self.upload_file(self.uid_1, path=self.shared_folder_path + '/test2.jpg')
            for call_args in index_service.push_change.call_args_list:
                indexer_data = call_args[0][0]
                for push in indexer_data:
                    resource = get_resource(push['uid'], push['id'])
                    if str(push['uid']) == str(self.uid):
                        assert 'shared_folder_version' not in push
                    else:
                        assert push['shared_folder_version'] == int(resource.version) + group_link_version

    def test_pushes_for_moved_shared_folder(self):
        self.create_folder_with_resources()

        self.gid = self.create_group(path=self.shared_folder_path)
        hash_ = self.invite_user(uid=self.uid_1, email=self.email_1, path=self.shared_folder_path)

        self.activate_invite(uid=self.uid_1, hash=hash_)
        new_shared_folder_address = Address.Make(self.uid_1, '/disk/f1/f2/f3/f4/f5')
        for parent in sorted(new_shared_folder_address.get_parents(), key=lambda a: a.id):
            if parent.is_root or parent.is_storage:
                continue
            self.json_ok('mkdir', {'uid': self.uid_1, 'path': parent.path})
        self.json_ok(
            'async_move',
            {'uid': self.uid_1, 'src': self.shared_folder_path, 'dst': new_shared_folder_address.path}
        )

        group = Group.find(gid=self.gid, link=False, group=True)
        group_link_version = group.get_group_link_base_version(self.uid_1)

        with SearchIndexerStub() as index_service:
            self.upload_file(self.uid_1, path=new_shared_folder_address.path + '/test2.jpg')
            for call_args in index_service.push_change.call_args_list:
                indexer_data = call_args[0][0]
                for push in indexer_data:
                    resource = get_resource(push['uid'], push['id'])
                    if str(push['uid']) == str(self.uid):
                        assert 'shared_folder_version' not in push
                    else:
                        assert push['shared_folder_version'] == int(resource.version) + group_link_version

    def test_pushes_for_several_participants(self):
        self.create_folder_with_resources()

        self.gid = self.create_group(path=self.shared_folder_path)
        hash_1 = self.invite_user(uid=self.uid_1, email=self.email_1, path=self.shared_folder_path)
        hash_2 = self.invite_user(uid=self.uid_3, email=self.email_3, path=self.shared_folder_path)

        self.activate_invite(uid=self.uid_1, hash=hash_1)
        with time_machine(datetime.datetime.now() + datetime.timedelta(seconds=1)):
            self.activate_invite(uid=self.uid_3, hash=hash_2)

        group = Group.find(gid=self.gid, link=False, group=True)
        group_link_version_1 = group.get_group_link_base_version(self.uid_1)
        group_link_version_2 = group.get_group_link_base_version(self.uid_3)

        assert group_link_version_1 != group_link_version_2

        with SearchIndexerStub() as index_service:
            self.upload_file(self.uid_1, path=self.shared_folder_path + '/test2.jpg')
            for call_args in index_service.push_change.call_args_list:
                indexer_data = call_args[0][0]
                for push in indexer_data:
                    resource = get_resource(push['uid'], push['id'])
                    if str(push['uid']) == str(self.uid):
                        assert 'shared_folder_version' not in push
                    elif str(push['uid']) == str(self.uid_1):
                        assert push['shared_folder_version'] == int(resource.version) + group_link_version_1
                    elif str(push['uid']) == str(self.uid_3):
                        assert push['shared_folder_version'] == int(resource.version) + group_link_version_2
                    else:
                        self.fail('unexpected push')

    def test_pushes_for_async_move_inside_shared_folder(self):
        self.create_folder_with_resources()
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.shared_subfolder_path + '/dir-1'})

        self.gid = self.create_group(path=self.shared_folder_path)
        hash_ = self.invite_user(uid=self.uid_1, email=self.email_1, path=self.shared_folder_path)

        self.activate_invite(uid=self.uid_1, hash=hash_)

        group = Group.find(gid=self.gid, link=False, group=True)
        group_link_version = group.get_group_link_base_version(self.uid_1)

        with SearchIndexerStub() as index_service:
            new_subfolder_path = self.shared_folder_path + '/new_path'
            self.json_ok(
                'async_move',
                {'uid': self.uid, 'src': self.shared_subfolder_path, 'dst': new_subfolder_path}
            )

            for call_args in index_service.push_change.call_args_list:
                indexer_data = call_args[0][0]
                for push in indexer_data:
                    resource = get_resource(push['uid'], push['id'])
                    if str(push['uid']) == str(self.uid):
                        assert 'shared_folder_version' not in push
                    else:
                        assert push['shared_folder_version'] == int(resource.version) + group_link_version

    @parameterized.expand((
        ('all_params',
         {
            'uid': u'128280859',
            'distance': u'1000',
            'latitude': u'55.0',
            'longitude': u'37.0',
            'end_date': '0',
            'count_lost_results': '1',
            'amount': u'20',
            'meta': '',
            'offset': u'0',
            'skip_overdraft_check': u'1',
            'preview_size': u'S',
            'start_date': '0',
            'preview_crop': u'0'
         }),
        ('without_coordinates',
         {
            'uid': u'128280859',
            'end_date': '0',
            'count_lost_results': '1',
            'amount': u'20',
            'meta': '',
            'offset': u'0',
            'skip_overdraft_check': u'1',
            'preview_size': u'S',
            'start_date': '0',
            'preview_crop': u'0'
         })))
    def test_geo_search(self, name, query):
        self.upload_file(self.uid, '/disk/test.jpg')
        with DiskGeoSearchSmartMockHelper.mock():
            DiskGeoSearchSmartMockHelper.add_to_index(self.uid, '/disk/test.jpg')
            response = self.json_ok('geo_search', query)
            assert len([x for x in response['results'] if x['type'] == 'file']) == 1
