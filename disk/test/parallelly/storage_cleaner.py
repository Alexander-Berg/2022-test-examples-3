# -*- coding: utf-8 -*-
import contextlib
import itertools

import mock

import mpfs.common.errors as errors
from mpfs.core.filesystem.cleaner.manager import StorageCleanerManager
from mpfs.core.filesystem.cleaner.models import DeletedStid, DeletedStidRetry
from mpfs.core.filesystem.cleaner.worker import CheckingStid, DbChecker, StorageCleanerWorker, CleaningLocksManager
from mpfs.core.filesystem.hardlinks.common import construct_hid
from mpfs.dao.session import Session
from test.base import DiskTestCase
from test.helpers.stubs.services import BazingaInterfaceStub, MulcaServiceStub

STIDS_COUNT = itertools.count()


def get_stid_name():
    return "1.yadisk:1.%0.3i" % STIDS_COUNT.next()


class StorageCleanerManagerTestCase(DiskTestCase):
    @staticmethod
    def patch_settings():
        return contextlib.nested(
            mock.patch.object(StorageCleanerManager, 'ENABLED', True),
            mock.patch.object(StorageCleanerManager, 'BATCH_SIZE', 6),
            mock.patch.object(StorageCleanerManager, 'STIDS_PER_WORKER', 2),
        )

    def load_stids(self, num=10):
        deleted_stids = [DeletedStid(stid=get_stid_name()) for s in range(num)]
        DeletedStid.controller.real_bulk_create(deleted_stids)

    def test_common(self):
        self.load_stids()
        with self.patch_settings():
            with BazingaInterfaceStub() as stub:
                StorageCleanerManager().run()
                stub.get_tasks_count.assert_called_once()
                stub.bulk_create_tasks.assert_called_once()
                args, _ = stub.bulk_create_tasks.call_args
                tasks = args[0]
                assert len(tasks) == 3
                for task in tasks:
                    assert len(task.raw_stids) == 2
                    assert len(task.stids) == 0

    def test_tasks_limit(self):
        self.load_stids(4)
        with self.patch_settings():
            with BazingaInterfaceStub():
                manager = StorageCleanerManager()
                assert manager._get_active_task_limit() == 0
                manager.BATCH_SIZE = 300
                self.load_stids(300)
                assert manager._get_active_task_limit() == 1

    def test_no_spawn_if_worker_runs(self):
        self.load_stids()
        with self.patch_settings():
            with BazingaInterfaceStub(tasks_count=100) as stub:
                StorageCleanerManager().run()
                stub.bulk_create_tasks.assert_not_called()


class DbCheckerTestCase(DiskTestCase):
    checked_mongo_collections = (
        'user_data',
        'attach_data',
        'hidden_data',
        'misc_data',
        'narod_data',
        'photounlim_data',
        'trash',
        'fake_version_data', # фейковая коллекция для проверки stid-ов версий.
        'additional_data',
        'client_data',
        'notes_data',
    )

    def test_no_stids_in_db(self):
        stids = [CheckingStid(get_stid_name()) for _ in range(5)]
        db_checker = DbChecker()
        db_checker.is_stids_in_db(stids)
        for stid in stids:
            assert stid.is_stid_in_db is False

    def test_checked_collections(self):
        db_checker = DbChecker()
        shards_collections = list(db_checker._shard_collection_generator())
        shards_collections.sort()

        expected_shard_collections = list()
        for pg_shard_id in ('1', '2'):
            expected_shard_collections.append((pg_shard_id, 'misc_data'))
            expected_shard_collections.append((pg_shard_id, 'user_data'))
            expected_shard_collections.append((pg_shard_id, 'storage_duplicates'))
        expected_shard_collections.sort()
        assert expected_shard_collections == shards_collections

    def test_stid_in_db(self):
        not_in_db_stids = [CheckingStid(get_stid_name()) for _ in range(5)]
        in_db_stids = []
        for stid_field in ('digest_mid', 'pmid', 'file_mid'):
            stid_value = get_stid_name()
            in_db_stids.append(CheckingStid(stid_value))
            self.upload_file(self.uid, '/disk/test_%s' % stid_field, file_data={stid_field: stid_value})

        db_checker = DbChecker()
        db_checker.is_stids_in_db(not_in_db_stids + in_db_stids)
        for stid in not_in_db_stids:
            assert stid.is_stid_in_db is False
        for stid in in_db_stids:
            assert stid.is_stid_in_db is True

    def test_many_duplicate_stids_in_db(self):
        stid_1 = get_stid_name()
        stid_2 = get_stid_name()
        self.upload_file(self.uid, '/disk/test_1', file_data={'file_mid': stid_1, 'pmid': stid_1, 'digest_mid': stid_1})
        self.upload_file(self.uid, '/disk/test_2', file_data={'file_mid': stid_1, 'pmid': stid_1, 'digest_mid': stid_1})
        self.upload_file(self.uid, '/disk/test_3', file_data={'file_mid': stid_1, 'pmid': stid_1, 'digest_mid': stid_1})
        self.upload_file(self.uid, '/disk/test_4', file_data={'file_mid': stid_1, 'pmid': stid_1, 'digest_mid': stid_1})
        self.upload_file(self.uid, '/disk/test_5', file_data={'file_mid': stid_1, 'pmid': stid_1, 'digest_mid': stid_1})
        self.upload_file(self.uid, '/disk/test_6', file_data={'file_mid': stid_1, 'pmid': stid_1, 'digest_mid': stid_1})
        self.upload_file(self.uid, '/disk/test_7', file_data={'file_mid': stid_2})

        db_checker = DbChecker()
        checking_stid_1 = CheckingStid(stid_1)
        checking_stid_2 = CheckingStid(stid_2)
        db_checker.is_stids_in_db([checking_stid_1, checking_stid_2])
        assert checking_stid_1.is_stid_in_db is True
        assert checking_stid_2.is_stid_in_db is True

    def test_stid_in_trash(self):
        stid = CheckingStid(get_stid_name())
        self.upload_file(self.uid, '/disk/test', file_data={'file_mid': stid.stid})
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/test'})
        db_checker = DbChecker()
        db_checker.is_stids_in_db([stid])
        assert stid.is_stid_in_db is True

    def test_stid_in_attach(self):
        stid = CheckingStid(get_stid_name())
        self.upload_file(self.uid, '/attach/test', file_data={'file_mid': stid.stid})
        db_checker = DbChecker()
        db_checker.is_stids_in_db([stid])
        assert stid.is_stid_in_db is True

    def test_stid_in_hidden(self):
        stid = CheckingStid(get_stid_name())
        self.upload_file(self.uid, '/disk/test', file_data={'file_mid': stid.stid})
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/test'})
        db_checker = DbChecker()
        db_checker.is_stids_in_db([stid])
        assert stid.is_stid_in_db is True

    def test_stid_in_photounlim(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        stid = CheckingStid(get_stid_name())
        self.upload_file(self.uid, '/photounlim/test', file_data={'file_mid': stid.stid})
        db_checker = DbChecker()
        db_checker.is_stids_in_db([stid])
        assert stid.is_stid_in_db is True

    def test_stid_in_version_data(self):
        stid = CheckingStid(get_stid_name())
        self.upload_file(self.uid, '/disk/1.txt', file_data={'file_mid': stid.stid})
        self.upload_file(self.uid, '/disk/1.txt')
        db_checker = DbChecker()
        db_checker.is_stids_in_db([stid])
        assert stid.is_stid_in_db is True


class StorageCleanerWorkerTestCase(DiskTestCase):
    @staticmethod
    def patch_settings():
        return contextlib.nested(
            mock.patch.object(StorageCleanerWorker, 'ENABLED', True),
            mock.patch.object(StorageCleanerWorker, 'DRY_RUN', False),
            mock.patch.object(StorageCleanerWorker, 'SLEEP_MSECS', 0),
            mock.patch.object(CleaningLocksManager, 'ENABLED', True),
        )

    def test_common(self):
        stids = [get_stid_name() for _ in range(2)]
        with self.patch_settings():
            worker = StorageCleanerWorker(stids)
            for check_stid in worker.stids:
                assert isinstance(check_stid, CheckingStid)
                assert check_stid.can_remove_from_deleted_stids is False
                assert check_stid.can_remove_from_storage is False

            with MulcaServiceStub():
                worker.run()

            for check_stid in worker.stids:
                assert check_stid.can_remove_from_storage is True
                assert check_stid.can_remove_from_deleted_stids is True
                assert check_stid.size == MulcaServiceStub._fake_size
                assert check_stid.storage_status == 204

    def test_one_stid_in_db_another_not(self):
        stid_in_db = get_stid_name()
        stid_not_in_db = get_stid_name()
        self.upload_file(self.uid, '/disk/test', file_data={'file_mid': stid_in_db})

        worker = StorageCleanerWorker([stid_in_db, stid_not_in_db])
        with self.patch_settings():
            with MulcaServiceStub():
                worker.run()

        assert worker.stids[0].can_remove_from_storage is False
        assert worker.stids[0].can_remove_from_deleted_stids is True
        assert worker.stids[0].size is MulcaServiceStub._fake_size
        assert worker.stids[0].storage_status is None

        assert worker.stids[1].can_remove_from_storage is True
        assert worker.stids[1].can_remove_from_deleted_stids is True
        assert worker.stids[1].size == MulcaServiceStub._fake_size
        assert worker.stids[1].storage_status == 204

    def test_stid_in_duplicates(self):
        base_stid = get_stid_name()
        stid_duplicate = get_stid_name()
        path = '/disk/test'
        self.upload_file(self.uid, path, file_data={'file_mid': base_stid})
        result_meta = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'md5,sha256,size'})['meta']
        hid = construct_hid(result_meta['md5'], int(result_meta['size']), result_meta['sha256'])
        session = Session.create_from_uid(self.uid)
        session.execute(
            'INSERT INTO disk.duplicated_storage_files(storage_id, stid) VALUES (:storage_id, :stid)',
            {'storage_id': hid, 'stid': stid_duplicate}
        )
        worker = StorageCleanerWorker([base_stid, stid_duplicate])
        with self.patch_settings():
            with MulcaServiceStub():
                worker.run()
        for check_stid in worker.stids:
            assert isinstance(check_stid, CheckingStid)
            assert check_stid.can_remove_from_storage is False
            assert check_stid.can_remove_from_deleted_stids is True
            assert check_stid.storage_status is None

    def test_mulca_fail(self):
        stid = get_stid_name()
        with self.patch_settings():
            worker = StorageCleanerWorker([stid])
            for check_stid in worker.stids:
                assert isinstance(check_stid, CheckingStid)
                assert check_stid.can_remove_from_deleted_stids is False
                assert check_stid.can_remove_from_storage is False
                assert check_stid.storage_status is None

            with MulcaServiceStub():
                with mock.patch('mpfs.core.services.mulca_service.Mulca.remove', side_effect=errors.MulcaNoResponse()):
                    worker.run()

            for check_stid in worker.stids:
                assert check_stid.can_remove_from_storage is True
                assert check_stid.can_remove_from_deleted_stids is True
                assert check_stid.storage_status == 500

            assert DeletedStidRetry.controller.get(stid=stid) is not None

    def test_fotki_stids(self):
        stid = '103.fotki_test'
        with self.patch_settings():
            worker = StorageCleanerWorker([stid])
            worker.run()
            stid_obj = worker.stids[0]
            assert stid_obj.can_remove_from_deleted_stids is True
            assert stid_obj.can_remove_from_storage is False
            assert stid_obj.is_need_retry is False
            assert DeletedStidRetry.controller.get(stid=stid) is None
            assert DeletedStid.controller.get(stid=stid) is None
