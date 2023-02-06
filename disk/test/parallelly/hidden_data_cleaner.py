# -*- coding: utf-8 -*-

import contextlib
import copy
import random
import time
import uuid

import mock
import pytest

from mpfs.core.filesystem.cleaner.storage_duplicates import handle_storage_duplicate_clean
from test.base import DiskTestCase
from test.conftest import INIT_USER_IN_POSTGRES
from test.fixtures import users
from test.helpers.stubs.services import MulcaServiceStub

from mpfs.config import settings
from mpfs.core.filesystem.cleaner.hidden import (
    HiddenDataCleanerWorker, HiddenDataCleanerManager, HiddenDataCleanerToggle,
)
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.filesystem.resources.base import Resource
from mpfs.core.queue import QueueDispatcher
from mpfs.core.services import queller_service
from mpfs.dao.session import Session
from mpfs.metastorage.mongo.mapper import POSTGRES_USER_INFO_ENTRY
from mpfs.metastorage.mongo.util import compress_data
from mpfs.metastorage.postgres.query_executer import PGQueryExecuter

HIDDEN_DATA_CLEANER_WORKER_STIDS_IGNORE_LIST = settings.hidden_data_cleaner['worker']['stids_ignore_list']


class HiddenDataCleanerToggleTestCase(DiskTestCase):
    @staticmethod
    def config_patch(value):
        return mock.patch.object(HiddenDataCleanerToggle, 'config_toggle', value)

    def test_common(self):
        with self.config_patch(False):
            assert not HiddenDataCleanerToggle.is_enable()
        with self.config_patch(True):
            assert HiddenDataCleanerToggle.is_enable()


class HiddenDataCleanerManagerTestCase(DiskTestCase):
    @staticmethod
    def toggle_patch(value):
        return mock.patch.object(HiddenDataCleanerToggle, 'is_enable', return_value=value)

    @staticmethod
    def task_put_patch():
        return mock.patch.object(QueueDispatcher, 'put')

    @staticmethod
    def task_count_patch(num):
        return mock.patch.object(queller_service.QuellerService, 'get_tasks_count', return_value=num)

    def test_common(self):
        with self.toggle_patch(True):
            with self.task_count_patch(0):
                with self.task_put_patch() as insert_stub:
                    m = HiddenDataCleanerManager()
                    m.run()
                    assert insert_stub.call_count > 1
                    for call_args in insert_stub.call_args_list:
                        args, _ = call_args
                        self.assertIsInstance(args, tuple)
                        assert len(args) == 2
                        worker_path = 'mpfs.core.filesystem.cleaner.hidden.HiddenDataCleanerWorker'
                        assert args[1] == worker_path
                        self.assertIsInstance(args[0], dict)
                        assert 'shard' in args[0]
                        assert args[0]['shard'] in {'1', '2'}

    def test_has_active_workers(self):
        with self.toggle_patch(True):
            with self.task_count_patch(1):
                with self.task_put_patch() as insert_stub:
                    m = HiddenDataCleanerManager()
                    m.run()
                    assert insert_stub.call_count == 0

    def test_disable(self):
        with self.toggle_patch(False):
            with self.task_put_patch() as insert_stub:
                m = HiddenDataCleanerManager()
                m.run()
                assert not insert_stub.called


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='PG only')
class HiddenDataCleanerWorkerTestCase(DiskTestCase):
    FILES_NUM = 2
    DIRS_NUM = 3
    file_params = {
        'dtime': int(time.time()) - 1,
        'size': 20,
        'type_': 'file',
    }
    dir_params = {
        'type_': 'dir',
    }

    def setup_method(self, method):
        super(HiddenDataCleanerWorkerTestCase, self).setup_method(method)
        self.routed_db = CollectionRoutedDatabase()

    @staticmethod
    def worker_patch(enable, clean_delay, min_file_size):
        return contextlib.nested(
            HiddenDataCleanerManagerTestCase.toggle_patch(enable),
            mock.patch.object(HiddenDataCleanerWorker, 'CLEAN_DELAY', clean_delay),
            mock.patch.object(HiddenDataCleanerWorker, 'MIN_FILE_SIZE', min_file_size),
        )

    @staticmethod
    def random_with_N_digits(n):
        range_start = 10 ** (n - 1)
        range_end = (10 ** n) - 1
        return random.randint(range_start, range_end)

    def _uniq_stid(self):
        return '%d.yadisk:%d.%d' % (self.random_with_N_digits(3),
                                    self.random_with_N_digits(3),
                                    self.random_with_N_digits(3))

    def generate_raw_doc(self, uid=None, path=None, type_='file', dtime=None, size=0):
        uniq_str = lambda: uuid.uuid4().hex

        key = path if path else '/hidden/%s' % uniq_str()
        doc = {
            '_id': uniq_str(),
            'parent': '/hidden',
            'type': type_,
            'uid': uid if uid else self.uid,
            'dtime': dtime if dtime else int(time.time()),
            'version': long(time.time() * 100000),
            'key': key,
            'data': {
                # 'mimetype': 'application/octet-stream',
                # 'mt': 3,
                # 'visible': 1,
                # 'original_id': '/disk/control',
                # 'file_id': '5052437e68da14c7d2902b7d9570e01a4ab002a92dd52ac091b8a4b8e6583218',
                # 'mtime': 1387277864,
                # 'utime': 1387277864,
                'file_id': Resource.generate_file_id(self.uid, key)
            },
            'zdata': compress_data({}),
        }
        if type_ == 'file':
            doc.update({'hid': uniq_str()})
            stids = []
            for i, t in enumerate(('file_mid', 'digest_mid', 'pmid')):
                if size == 0:
                    stids.append({'stid': HIDDEN_DATA_CLEANER_WORKER_STIDS_IGNORE_LIST[i % 2], 'type': t})
                else:
                    stids.append({'stid': self._uniq_stid(), 'type': t})

            doc['data'].update({
                'stids': stids,
                'size': size
            })
            doc['zdata'] = compress_data({
                'meta': {
                    'sha256': uniq_str() + uniq_str(),
                    'md5': uniq_str(),
                }
            })
        else:
            pass
        return doc

    def _doc_to_hidden(self, doc):
        self.routed_db['hidden_data'].insert(doc)

    def _file_to_hidden(self):
        doc = self.generate_raw_doc(**self.file_params)
        self._doc_to_hidden(doc)

    def _dir_to_hidden(self):
        doc = self.generate_raw_doc(**self.dir_params)
        self._doc_to_hidden(doc)

    def _get_collections_delta(self, func):
        user_shard = self._get_user_shard()
        with mock.patch('mpfs.core.filesystem.cleaner.controllers.DeletedStidsController.bulk_create') as stub:
            hidden_data_before = self.routed_db['hidden_data'].find_on_shard({}, shard_name=user_shard).count()
            func()
            hidden_data_after = self.routed_db['hidden_data'].find_on_shard({}, shard_name=user_shard).count()
        return sum(len(args[0][0]) for args in stub.call_args_list), hidden_data_after - hidden_data_before

    def _get_user_shard(self):
        user_info = self.json_ok('user_info', {'uid': self.uid})
        shard = user_info['db']['shard']
        if shard == POSTGRES_USER_INFO_ENTRY:
            return PGQueryExecuter().get_shard_id(self.uid)
        return shard

    def test_common(self):
        self._file_to_hidden()
        user_shard = self._get_user_shard()

        def test():
            with self.worker_patch(True, 0, 0):
                HiddenDataCleanerWorker().put(user_shard)

        deleted_stids_delta, hidden_data_delta = self._get_collections_delta(test)
        assert deleted_stids_delta == 3
        assert hidden_data_delta == -1

    def test_null_stid(self):
        file_params = dict(self.file_params, size=0)
        doc = self.generate_raw_doc(**file_params)
        self._doc_to_hidden(doc)
        user_shard = self._get_user_shard()

        def test():
            with self.worker_patch(True, 0, 0):
                HiddenDataCleanerWorker().put(user_shard)

        deleted_stids_delta, hidden_data_delta = self._get_collections_delta(test)
        assert deleted_stids_delta == 0
        assert hidden_data_delta == -1

    def test_size_fill(self):
        self._file_to_hidden()
        user_shard = self._get_user_shard()

        with self.worker_patch(True, 0, 0):
            with MulcaServiceStub() as stub:
                HiddenDataCleanerWorker().put(user_shard)

        stub.get_file_size.assert_called()

    def test_files_limit(self):
        for _ in xrange(10):
            self._file_to_hidden()
        user_shard = self._get_user_shard()

        def test():
            with self.worker_patch(True, 0, 0):
                HiddenDataCleanerWorker().put(user_shard)

        with mock.patch.object(HiddenDataCleanerWorker, 'FETCH_BATCH_SIZE', 3):
            deleted_stids_delta, hidden_data_delta = self._get_collections_delta(test)
        assert deleted_stids_delta == 30
        assert hidden_data_delta == -10

    def test_files_limit_with_prohibited_uids(self):
        for _ in xrange(5):
            self._file_to_hidden()

        another_uid = users.user_3.uid
        self.create_user(another_uid)
        doc = self.generate_raw_doc(uid=another_uid, **self.file_params)
        self._doc_to_hidden(doc)

        for _ in xrange(5):
            self._file_to_hidden()

        user_shard = self._get_user_shard()

        def test():
            with self.worker_patch(True, 0, 0):
                HiddenDataCleanerWorker().put(user_shard)

        from mpfs.core.metastorage.control import support_prohibited_cleaning_users
        support_prohibited_cleaning_users.put(another_uid, 'test', 'mpfs-test')

        with mock.patch.object(HiddenDataCleanerWorker, 'FETCH_BATCH_SIZE', 3):
            deleted_stids_delta, hidden_data_delta = self._get_collections_delta(test)
        assert deleted_stids_delta == 30
        assert hidden_data_delta == -10

    def test_disable(self):
        self._file_to_hidden()
        user_shard = self._get_user_shard()

        def test():
            with self.worker_patch(False, 0, 0):
                HiddenDataCleanerWorker().put(user_shard)

        deleted_stids_delta, hidden_data_delta = self._get_collections_delta(test)
        assert deleted_stids_delta == 0
        assert hidden_data_delta == 0

    def test_too_huge_clean_delay(self):
        self._file_to_hidden()
        user_shard = self._get_user_shard()

        def test():
            # 2 - два дня
            with self.worker_patch(True, 2, 0):
                HiddenDataCleanerWorker().put(user_shard)

        deleted_stids_delta, hidden_data_delta = self._get_collections_delta(test)
        assert deleted_stids_delta == 0
        assert hidden_data_delta == 0

    def test_ignore_dir(self):
        self._dir_to_hidden()
        user_shard = self._get_user_shard()

        def test():
            with self.worker_patch(True, 0, 0):
                HiddenDataCleanerWorker().put(user_shard)

        deleted_stids_delta, hidden_data_delta = self._get_collections_delta(test)
        assert deleted_stids_delta == 0
        assert hidden_data_delta == 0

    def test_prohibited_cleaning_users(self):
        """Протестировать, что файлы пользователя, у которого отключена чистка, не удаляются."""
        file_params = copy.copy(self.file_params)
        file_params['uid'] = self.uid
        doc = self.generate_raw_doc(**file_params)
        self._doc_to_hidden(doc)
        user_shard = self._get_user_shard()

        self.support_ok('add_user_to_prohibited_cleaning', {
            'uid': self.uid,
            'comment': 'your ad here',
            'moderator': 'moderator_nickname'
        })

        def test():
            with self.worker_patch(True, 0, 0):
                HiddenDataCleanerWorker().put(user_shard)

        deleted_stids_delta, hidden_data_delta = self._get_collections_delta(test)
        assert deleted_stids_delta == 0
        assert hidden_data_delta == 0

    def test_copy_file(self):
        filename1 = '/disk/test.txt'
        filename2 = filename1 + '_copy'
        self.upload_file(self.uid, filename1, file_data={'data': 'copy_test'})
        self.json_ok('copy', {'uid': self.uid, 'src': filename1, 'dst': filename2})

        user_shard = self._get_user_shard()

        def test():
            with self.worker_patch(True, 0, 0):
                HiddenDataCleanerWorker().put(user_shard)

        for filename, stids_delta in ((filename1, 0), (filename2, 3)):
            self.json_ok('trash_append', {'uid': self.uid, 'path': filename})
            self.json_ok('trash_drop_all', {'uid': self.uid})

            deleted_stids_delta, hidden_data_delta = self._get_collections_delta(test)
            assert deleted_stids_delta == stids_delta

    def test_cleaning_storage_with_duplicates(self):
        """Протестировать, что дубли найденные не мешают чистке."""
        doc = self.generate_raw_doc(**self.file_params)
        self._doc_to_hidden(doc)
        session = Session.create_from_uid(doc['uid'])
        session.execute(
            'INSERT INTO disk.duplicated_storage_files(storage_id, stid) '
            'VALUES (:storage_id, :first_stid), (:storage_id, :second_stid)',
            {'storage_id': doc['hid'], 'first_stid': self._uniq_stid(), 'second_stid': self._uniq_stid()}
        )
        user_shard = self._get_user_shard()
        def test():
            with self.worker_patch(True, 0, 0):
                HiddenDataCleanerWorker().put(user_shard)
        deleted_stids_delta, hidden_data_delta = self._get_collections_delta(test)
        assert deleted_stids_delta == 3
        assert hidden_data_delta == -1

    def test_send_duplicates_to_cleaning_after_hidden_data_cleaning(self):
        """Протестировать, что дубли найденные при миграции также удаляются отдельной джобой."""
        doc = self.generate_raw_doc(**self.file_params)
        self._doc_to_hidden(doc)
        session = Session.create_from_uid(doc['uid'])
        session.execute(
            'INSERT INTO disk.duplicated_storage_files(storage_id, stid) '
            'VALUES (:storage_id, :first_stid), (:storage_id, :second_stid)',
            {'storage_id': doc['hid'], 'first_stid': self._uniq_stid(), 'second_stid': self._uniq_stid()}
        )
        user_shard = self._get_user_shard()
        # Чистим hidden_data
        with self.worker_patch(True, 0, 0):
            HiddenDataCleanerWorker().put(user_shard)

        def test():
            handle_storage_duplicate_clean(user_shard)

        deleted_stids_delta, _ = self._get_collections_delta(test)
        assert deleted_stids_delta == 2

        count = session.execute('SELECT count(*) FROM disk.duplicated_storage_files').fetchone()[0]
        assert count == 0
