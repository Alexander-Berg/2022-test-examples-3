# -*- coding: utf-8 -*-
import cjson
import hashlib
import itertools
import time

import mock
import pytest

from test.base import DiskTestCase

from mpfs.core.filesystem.dao import resource
from mpfs.core.filesystem import hardlinks
from mpfs.core.filesystem.hardlinks.controllers import HardlinksController
from mpfs.core.filesystem.hardlinks.common import AbstractLink, construct_hid
from mpfs.metastorage.postgres.exceptions import QueryCanceledError
from mpfs.core.bus import Bus
from mpfs.core.address import Address
from mpfs.core.filesystem import base
from mpfs.common import errors
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.filesystem.dao.resource import ResourceDAO
from test.conftest import INIT_USER_IN_POSTGRES

with open('fixtures/json/file.json') as fix_file:
    fixtures = cjson.decode(fix_file.read())

SIZE      = fixtures["file_value"]["size"]
MD5       = fixtures["file_value"]["meta"]["md5"]
FILE_ID   = fixtures["file_value"]["meta"]["file_mid"]
DIGEST_ID = fixtures["file_value"]["meta"]["digest_mid"]
SHA256    = fixtures["file_value"]["meta"]["sha256"]
MIMETYPE  = fixtures["file_value"]["meta"]["mimetype"]


class HardlinksControllerTestCase(DiskTestCase):
    db = CollectionRoutedDatabase()
    mids_name = ('file_mid', 'digest_mid', 'pmid')

    def extract_mids(self, db_rec):
        mids = {}
        for rec in db_rec['data']['stids']:
            name, value = rec['type'], rec['stid']
            if name in self.mids_name:
                mids[name] = value
        return mids

    def test_update_files_mids(self):
        """
        Тестируется изменение mid-ов по hid-у.
        """
        self.upload_file(self.uid, '/disk/0')
        self.json_ok('copy', {'uid': self.uid, 'src': '/disk/0', 'dst': '/disk/1'})
        self.json_ok('copy', {'uid': self.uid, 'src': '/disk/0', 'dst': '/disk/2'})

        files_mids = []
        hid = None
        for i in range(3):
            db_rec = self.db.user_data.find_one({'uid': self.uid, 'path': '/disk/%i' % i})
            hid = str(db_rec['hid'])
            files_mids.append(self.extract_mids(db_rec))

        for mids_1, mids_2 in itertools.product(files_mids, repeat=2):
            assert mids_1 == mids_2

        new_mids = {name: '123456.yadisk:123456.%d' % i for i, name in enumerate(self.mids_name)}
        HardlinksController().update_files_mids(hid, new_mids)

        for i in range(3):
            db_rec = self.db.user_data.find_one({'uid': self.uid, 'path': '/disk/%i' % i})
            assert self.extract_mids(db_rec) == new_mids


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
class HardlinksTestCase(DiskTestCase):

    def setup_method(self, method):
        super(HardlinksTestCase, self).setup_method(method)
        self.file_path = '/disk/testfile.tst'

        Bus().mkfile(self.uid, Address.Make(self.uid, self.file_path).id, data=fixtures["file_value"])

    def test_get_non_existing(self):
        self.assertRaises(
            errors.HardLinkNotFound,
            lambda: hardlinks.HardLink('ddd', 343, 5435345345)
        )

    def test_file_part(self):
        h = Bus().hardlink(MD5, SIZE, SHA256)
        file_part = h.file_part()
        assert isinstance(file_part, dict)
        self.assertEqual(file_part['mimetype'], MIMETYPE)
        meta = file_part['meta']
        self.assertEqual(meta['md5'], MD5)
        self.assertEqual(meta['sha256'], SHA256)
        self.assertEqual(meta['file_mid'], FILE_ID)
        self.assertEqual(meta['digest_mid'], DIGEST_ID)

    def test_get(self):
        link = hardlinks.HardLink(MD5, SIZE, SHA256)
        self.assertNotEqual(link, None)
        self.assertEqual(link.file_id, FILE_ID)
        self.assertEqual(link.digest_id, DIGEST_ID)

    def test_try_store_photostream_already_exisiting(self):
        opts = {
            'uid': self.uid,
            'path': '/photostream/megafile.jpg',
            'size': SIZE,
            'md5': MD5,
            'sha256': SHA256,
        }
        self.json_error('store', opts, 72)

    def test_move_to_trash_and_still_get(self):
        Bus().trash_append(self.uid, Address.Make(self.uid, '/disk/testfile.tst').id)
        link = hardlinks.HardLink(MD5, SIZE, SHA256)
        self.assertNotEqual(link, None)
        self.assertEqual(link.file_id, FILE_ID)
        self.assertEqual(link.digest_id, DIGEST_ID)

    def test_exception_on_get(self):
        with mock.patch.object(ResourceDAO, 'find_one_file_item_on_shard_by_hid', return_value=None), \
             mock.patch.object(ResourceDAO, 'find_one_file_item_on_all_pg_shards_by_hid', return_value=None):
            try:
                hardlinks.HardLink(MD5, SIZE, SHA256)
                is_found = True
            except errors.HardLinkNotFound:
                is_found = False
            assert is_found is False

    def test_get_on_postgres_ok(self):
        with mock.patch.object(resource, 'POSTGRES_HARDLINKS_QUERY_TIMEOUT', 100500):
            link = hardlinks.HardLink(MD5, SIZE, SHA256)

            self.assertNotEqual(link, None)
            self.assertEqual(link.file_id, FILE_ID)
            self.assertEqual(link.digest_id, DIGEST_ID)

    def test_get_on_postgres_not_found(self):
        with mock.patch('mpfs.core.filesystem.dao.resource.PostgresResourceDAOImplementation._find_one_file_item_on_shard_by_hid',
                        side_effect=QueryCanceledError('abracadabra')):
            try:
                hardlinks.HardLink(MD5, SIZE, SHA256)
            except errors.HardLinkNotFound:
                pass
            else:
                self.fail('expected HardLinkNotFound')

    def test_save_hardlink_on_postgres(self):
        with mock.patch.object(AbstractLink, 'is_file_in_storage', return_value=True):
            rand = str('%f' % time.time()).replace('.', '')[9:]
            file_md5 = hashlib.md5(rand).hexdigest()
            file_sha256 = hashlib.sha256(rand).hexdigest()
            file_size = int(rand)

            try:
                hardlinks.HardLink(file_md5, file_size, file_sha256)
            except errors.HardLinkNotFound:
                pass
            else:
                assert False

            self.upload_file(self.uid, '/disk/file.txt', file_data={
                'md5': file_md5,
                'sha256': file_sha256,
                'size': file_size,
            })
            hid = construct_hid(file_md5, file_size, file_sha256)
            assert hardlinks.sharded.ShardedHardLink._search_by_hid(hid) is not None
