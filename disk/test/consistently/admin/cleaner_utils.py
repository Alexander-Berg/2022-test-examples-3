# -*- coding: utf-8 -*-
import time
import hashlib
import mock
import pytest
from pymongo import ReadPreference
from pymongo.collection import Collection

from mpfs.common.util import trace_calls
from test.base import DiskTestCase
from test.conftest import REAL_MONGO
from mpfs.core.filesystem.cleaner.models import StorageCleanCheckStid
from mpfs.metastorage.mongo.collections import clean


class CleanerUtilsTestCase(DiskTestCase):
    DISK_FILE_PATH = '/disk/file1.txt'
    DISK_FILE_PATH_2 = '/disk/file2.txt'
    ATTACH_FILE_PATH = '/attach/file.txt'
    FOLDER_PATH = '/disk/folder'

    def test_adding_to_accessed_stids_on_rm(self):
        self.upload_file(self.uid, self.DISK_FILE_PATH)
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.DISK_FILE_PATH, 'meta': 'file_mid,digest_mid,pmid'})
        stid = resp['meta']['file_mid']
        digest_stid = resp['meta']['digest_mid']
        pmid = resp['meta']['pmid']

        StorageCleanCheckStid(stid=stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=digest_stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=pmid, counter=0).save(upsert=True)

        self.json_ok('rm', {'uid': self.uid, 'path': self.DISK_FILE_PATH})

        accessed_stid = StorageCleanCheckStid.controller.get(stid=stid)
        accessed_digest_stid = StorageCleanCheckStid.controller.get(stid=digest_stid)
        accessed_pmid = StorageCleanCheckStid.controller.get(stid=pmid)

        assert accessed_stid.counter > 0
        assert accessed_digest_stid.counter > 0
        assert accessed_pmid.counter > 0

        accessed_stid.delete()
        accessed_digest_stid.delete()
        accessed_pmid.delete()

    def test_adding_to_accessed_stids_on_move(self):
        self.upload_file(self.uid, self.ATTACH_FILE_PATH)
        resp = self.json_ok('list', {'uid': self.uid, 'path': '/attach'})

        attach_file_path = None
        for item in resp:
            if item['path'].startswith(self.ATTACH_FILE_PATH):
                attach_file_path = item['path']

        assert attach_file_path is not None

        resp = self.json_ok('info', {'uid': self.uid, 'path': attach_file_path, 'meta': 'file_mid,digest_mid,pmid'})
        stid = resp['meta']['file_mid']
        digest_stid = resp['meta']['digest_mid']
        pmid = resp['meta']['pmid']

        StorageCleanCheckStid(stid=stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=digest_stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=pmid, counter=0).save(upsert=True)

        opts = {
            'uid': self.uid,
            'src': attach_file_path,
            'dst': self.DISK_FILE_PATH,
        }
        self.json_ok('move', opts)

        accessed_stid = StorageCleanCheckStid.controller.get(stid=stid)
        accessed_digest_stid = StorageCleanCheckStid.controller.get(stid=digest_stid)
        accessed_pmid = StorageCleanCheckStid.controller.get(stid=pmid)

        assert accessed_stid.counter > 0
        assert accessed_digest_stid.counter > 0
        assert accessed_pmid.counter > 0

        accessed_stid.delete()
        accessed_digest_stid.delete()
        accessed_pmid.delete()

    def test_adding_to_accessed_stids_on_store(self):
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()
        file_size = int(rand)

        self.upload_file(self.uid, self.DISK_FILE_PATH, file_data={'md5': file_md5, 'sha256': file_sha256,
                                                                   'size': file_size})

        resp = self.json_ok('info', {'uid': self.uid, 'path': self.DISK_FILE_PATH, 'meta': 'file_mid,digest_mid,pmid'})
        stid = resp['meta']['file_mid']
        digest_stid = resp['meta']['digest_mid']
        pmid = resp['meta']['pmid']

        StorageCleanCheckStid(stid=stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=digest_stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=pmid, counter=0).save(upsert=True)

        self.upload_file(self.uid, self.DISK_FILE_PATH_2, hardlink=True, file_data={'md5': file_md5,
                                                                                    'sha256': file_sha256,
                                                                                    'size': file_size})

        accessed_stid = StorageCleanCheckStid.controller.get(stid=stid)
        accessed_digest_stid = StorageCleanCheckStid.controller.get(stid=digest_stid)
        accessed_pmid = StorageCleanCheckStid.controller.get(stid=pmid)

        assert accessed_stid.counter > 0
        assert accessed_digest_stid.counter > 0
        assert accessed_pmid.counter > 0

        accessed_stid.delete()
        accessed_digest_stid.delete()
        accessed_pmid.delete()

    def test_adding_to_accessed_stids_on_trash_append(self):
        self.upload_file(self.uid, self.DISK_FILE_PATH)
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.DISK_FILE_PATH, 'meta': 'file_mid,digest_mid,pmid'})
        stid = resp['meta']['file_mid']
        digest_stid = resp['meta']['digest_mid']
        pmid = resp['meta']['pmid']

        StorageCleanCheckStid(stid=stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=digest_stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=pmid, counter=0).save(upsert=True)

        self.json_ok('trash_append', {'uid': self.uid, 'path': self.DISK_FILE_PATH})

        accessed_stid = StorageCleanCheckStid.controller.get(stid=stid)
        accessed_digest_stid = StorageCleanCheckStid.controller.get(stid=digest_stid)
        accessed_pmid = StorageCleanCheckStid.controller.get(stid=pmid)

        assert accessed_stid.counter > 0
        assert accessed_digest_stid.counter > 0
        assert accessed_pmid.counter > 0

        accessed_stid.delete()
        accessed_digest_stid.delete()
        accessed_pmid.delete()

    def test_adding_to_accessed_stids_on_trash_restore(self):
        self.upload_file(self.uid, self.DISK_FILE_PATH)
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.DISK_FILE_PATH, 'meta': 'file_mid,digest_mid,pmid'})
        stid = resp['meta']['file_mid']
        digest_stid = resp['meta']['digest_mid']
        pmid = resp['meta']['pmid']

        resp = self.json_ok('trash_append', {'uid': self.uid, 'path': self.DISK_FILE_PATH})
        trash_file_path = resp['this']['id']

        StorageCleanCheckStid(stid=stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=digest_stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=pmid, counter=0).save(upsert=True)

        self.json_ok('trash_restore', {'uid': self.uid, 'path': trash_file_path})

        accessed_stid = StorageCleanCheckStid.controller.get(stid=stid)
        accessed_digest_stid = StorageCleanCheckStid.controller.get(stid=digest_stid)
        accessed_pmid = StorageCleanCheckStid.controller.get(stid=pmid)

        assert accessed_stid.counter > 0
        assert accessed_digest_stid.counter > 0
        assert accessed_pmid.counter > 0

        accessed_stid.delete()
        accessed_digest_stid.delete()
        accessed_pmid.delete()

    def test_adding_to_accessed_stids_on_trash_restore_all(self):
        self.upload_file(self.uid, self.DISK_FILE_PATH)
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.DISK_FILE_PATH, 'meta': 'file_mid,digest_mid,pmid'})
        stid = resp['meta']['file_mid']
        digest_stid = resp['meta']['digest_mid']
        pmid = resp['meta']['pmid']

        self.json_ok('trash_append', {'uid': self.uid, 'path': self.DISK_FILE_PATH})

        StorageCleanCheckStid(stid=stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=digest_stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=pmid, counter=0).save(upsert=True)

        self.support_ok('trash_restore_all', {'uid': self.uid})

        accessed_stid = StorageCleanCheckStid.controller.get(stid=stid)
        accessed_digest_stid = StorageCleanCheckStid.controller.get(stid=digest_stid)
        accessed_pmid = StorageCleanCheckStid.controller.get(stid=pmid)

        assert accessed_stid.counter > 0
        assert accessed_digest_stid.counter > 0
        assert accessed_pmid.counter > 0

        accessed_stid.delete()
        accessed_digest_stid.delete()
        accessed_pmid.delete()

    def test_adding_to_accessed_stids_on_trash_drop(self):
        self.upload_file(self.uid, self.DISK_FILE_PATH)
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.DISK_FILE_PATH, 'meta': 'file_mid,digest_mid,pmid'})
        stid = resp['meta']['file_mid']
        digest_stid = resp['meta']['digest_mid']
        pmid = resp['meta']['pmid']

        resp = self.json_ok('trash_append', {'uid': self.uid, 'path': self.DISK_FILE_PATH})
        trash_file_path = resp['this']['id']

        StorageCleanCheckStid(stid=stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=digest_stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=pmid, counter=0).save(upsert=True)

        self.json_ok('trash_drop', {'uid': self.uid, 'path': trash_file_path})

        accessed_stid = StorageCleanCheckStid.controller.get(stid=stid)
        accessed_digest_stid = StorageCleanCheckStid.controller.get(stid=digest_stid)
        accessed_pmid = StorageCleanCheckStid.controller.get(stid=pmid)

        assert accessed_stid.counter > 0
        assert accessed_digest_stid.counter > 0
        assert accessed_pmid.counter > 0

        accessed_stid.delete()
        accessed_digest_stid.delete()
        accessed_pmid.delete()

    def test_adding_to_accessed_stids_on_trash_drop_all(self):
        self.upload_file(self.uid, self.DISK_FILE_PATH)
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.DISK_FILE_PATH, 'meta': 'file_mid,digest_mid,pmid'})
        stid = resp['meta']['file_mid']
        digest_stid = resp['meta']['digest_mid']
        pmid = resp['meta']['pmid']

        self.json_ok('trash_append', {'uid': self.uid, 'path': self.DISK_FILE_PATH})

        StorageCleanCheckStid(stid=stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=digest_stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=pmid, counter=0).save(upsert=True)

        self.json_ok('trash_drop_all', {'uid': self.uid})

        accessed_stid = StorageCleanCheckStid.controller.get(stid=stid)
        accessed_digest_stid = StorageCleanCheckStid.controller.get(stid=digest_stid)
        accessed_pmid = StorageCleanCheckStid.controller.get(stid=pmid)

        assert accessed_stid.counter > 0
        assert accessed_digest_stid.counter > 0
        assert accessed_pmid.counter > 0

        accessed_stid.delete()
        accessed_digest_stid.delete()
        accessed_pmid.delete()

    def test_adding_to_accessed_stids_on_restore_deleted(self):
        self.upload_file(self.uid, self.DISK_FILE_PATH)
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.DISK_FILE_PATH, 'meta': 'file_mid,digest_mid,pmid'})
        stid = resp['meta']['file_mid']
        digest_stid = resp['meta']['digest_mid']
        pmid = resp['meta']['pmid']

        self.json_ok('trash_append', {'uid': self.uid, 'path': self.DISK_FILE_PATH})
        self.json_ok('trash_drop_all', {'uid': self.uid})
        opts = {'uid': self.uid, 'path': '/hidden'}
        result = self.support_ok('list', opts)
        hidden_file_path = result[1]['path']

        StorageCleanCheckStid(stid=stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=digest_stid, counter=0).save(upsert=True)
        StorageCleanCheckStid(stid=pmid, counter=0).save(upsert=True)

        self.service_ok('restore_deleted', {'uid': self.uid, 'path': '%s:%s' % (self.uid, hidden_file_path),
                                            'dest': '%s:%s' % (self.uid, self.DISK_FILE_PATH), 'force': 0})

        accessed_stid = StorageCleanCheckStid.controller.get(stid=stid)
        accessed_digest_stid = StorageCleanCheckStid.controller.get(stid=digest_stid)
        accessed_pmid = StorageCleanCheckStid.controller.get(stid=pmid)

        assert accessed_stid.counter > 0
        assert accessed_digest_stid.counter > 0
        assert accessed_pmid.counter > 0

        accessed_stid.delete()
        accessed_digest_stid.delete()
        accessed_pmid.delete()

    def test_trash_drop_on_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.FOLDER_PATH})
        self.json_ok('trash_append', {'uid': self.uid, 'path': self.FOLDER_PATH})

    @pytest.mark.skipif(not REAL_MONGO,
                        reason="Не проверяем заглушку MongoDB")
    def test_read_preference_and_write_concern(self):
        stid1 = 'super_fake_stid1'
        stid2 = 'super_fake_stid2'

        def fake_base_update(self, *args, **kwargs):
            return {
                'n': 1,
                'w': kwargs.get('w'),
                'wtimeout': kwargs.get('wtimeout'),
            }

        def fake_base_find(self, *args, **kwargs):
            class FakeResult(dict):
                def count(self):
                    return 1

            return FakeResult([
                ('n', 1),
                ('read_preference', kwargs.get('read_preference')),
            ])

        StorageCleanCheckStid(stid=stid2, counter=0).save(upsert=True)

        with mock.patch.object(clean, 'MONGO_OPTIONS_COUNTERS_COLLECTION_OPTIONS_ENABLE_PRIMARY_READ_PREFERENCE', True):
            with mock.patch.object(clean, 'MONGO_OPTIONS_COUNTERS_COLLECTION_OPTIONS_ENABLE_WRITE_CONCERN', True):
                with mock.patch.object(Collection, 'update', fake_base_update):
                    with trace_calls(Collection, 'update') as tracer:
                        StorageCleanCheckStid(stid=stid1, counter=0).save(upsert=True)
                        return_value = tracer['return_value']

                        assert return_value['w'] is not None
                        assert return_value['wtimeout'] is not None

                        assert return_value['wtimeout'] == \
                               clean.MONGO_OPTIONS_COUNTERS_COLLECTION_OPTIONS_WRITE_TIMEOUT
                        assert return_value['w'] == 2

                with mock.patch.object(Collection, 'find', fake_base_find):
                    with trace_calls(Collection, 'find') as tracer:
                        list(StorageCleanCheckStid.controller.filter(stid=stid2))
                        return_value = tracer['return_value']

                        assert return_value['read_preference'] is not None
                        assert return_value['read_preference'] == ReadPreference.PRIMARY
