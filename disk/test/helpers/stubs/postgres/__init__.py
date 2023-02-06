# -*- coding: utf-8 -*-
import copy
import random

import os
import mock

from mpfs.config import settings
from mpfs.dao.session import POSTGRES_SPECIAL_UID_FOR_COMMON_SHARD
from mpfs.metastorage.postgres.services import SharpeiUserNotFoundError

from ..base import ChainedPatchBaseStub

TESTS_POSTGRES_LOCAL = settings.tests['postgres']['local']


def get_postgres_connection_stub_impl(common_shard_id):
    return LocalPostgresConnectionsStub(common_shard_id)


class SharpeiShardsFabric(object):
    _cur_id = 1

    @classmethod
    def build_sharpei_stat(cls, shards):
        assert len(shards) > 0
        return {s['id']: s for s in shards}

    @classmethod
    def build_shard(cls, name=None, id=None, databases=None):
        assert len(databases) > 0
        id = cls._cur_id if id is None else id
        cls._cur_id += 1
        name = name or "test_shard:%s" % id
        return {
            "id": id,
            "name": name,
            "databases": databases,
        }

    @staticmethod
    def build_database(port, dbname=None, host='localhost', data_center='PGAAS', role='master', status='alive', lag=0):
        assert isinstance(port, (int, long))
        assert isinstance(lag, (int, long))
        assert role in ('master', 'replica')
        dbname = dbname or '%s:%s' % (host, port)
        return {
            "address": {
                "dataCenter": data_center,
                "dbname": dbname,
                "host": host,
                "port": port,
                "dataCenter": data_center
            },
            "role": role,
            "state": {
                "lag": lag
            },
            "status": status
        }

class LocalPostgresConnectionsStub(ChainedPatchBaseStub):
    _db_user = 'disk_mpfs'
    _db_password = 'diskpasswd'

    def __init__(self, common_shard_id):
        super(LocalPostgresConnectionsStub, self).__init__()

        self.uid_shard_id_map = {}
        self.shard_map = {}
        self.registration_shard_ids = []

        shards_config = TESTS_POSTGRES_LOCAL.get(self.get_worker_id(), 'default')
        for shard_config in shards_config:
            shard_id = shard_config['id']
            db_conf = []
            for database_config in shard_config['databases']:
                db_conf.append(SharpeiShardsFabric.build_database(**database_config))
            self.shard_map[shard_id] = SharpeiShardsFabric.build_shard(name=shard_config['name'], id=shard_id, databases=db_conf)
            if shard_config['registration_enabled']:
                self.registration_shard_ids.append(shard_id)
        self.stat_resp = SharpeiShardsFabric.build_sharpei_stat(self.shard_map.values())

        self.stat_patch = mock.patch(
            'mpfs.metastorage.postgres.services.Sharpei._get_stat_data',
            new=self.get_stat_response)
        self.connection_info_patch = mock.patch(
            'mpfs.metastorage.postgres.services.Sharpei._get_data_by_uid',
            side_effect=self.get_data_by_uid)
        self.path_pg_user = mock.patch(
            'mpfs.metastorage.postgres.services.POSTGRES_USER',
            new=self._db_user)
        self.path_pg_password = mock.patch(
            'mpfs.metastorage.postgres.services.POSTGRES_PASSWORD',
            new=self._db_password)
        self.create_user_mock = mock.patch(
            'mpfs.metastorage.postgres.services.Sharpei._create_user',
            new=self.create_user)
        self.update_user_mock = mock.patch(
            'mpfs.metastorage.postgres.services.Sharpei._update_user',
            new=self.update_user)
        self.create_user(POSTGRES_SPECIAL_UID_FOR_COMMON_SHARD, shard_id=common_shard_id)

    def get_stat_response(self):
        return copy.deepcopy(self.stat_resp)

    def get_data_by_uid(self, uid):
        uid = str(uid)
        if uid not in self.uid_shard_id_map:
            raise SharpeiUserNotFoundError()
        shard_info = self.shard_map[self.uid_shard_id_map[uid]]
        return {'shard': copy.deepcopy(shard_info)}

    def create_user(self, uid, shard_id=None):
        uid = str(uid)
        if uid not in self.uid_shard_id_map:
            if shard_id is None:
                shard_id = random.choice(self.registration_shard_ids)
            shard_id = int(shard_id)
            assert shard_id in self.shard_map
            self.uid_shard_id_map[uid] = shard_id
        shard_info = self.shard_map[self.uid_shard_id_map[uid]]
        return copy.deepcopy(shard_info)

    def update_user(self, uid, curent_shard_id, new_shard_id):
        uid = str(uid)
        curent_shard_id = int(curent_shard_id)
        new_shard_id = int(new_shard_id)
        if uid not in self.uid_shard_id_map:
            raise SharpeiUserNotFoundError()
        assert self.uid_shard_id_map[uid] == curent_shard_id
        assert new_shard_id in self.shard_map
        self.uid_shard_id_map[uid] = new_shard_id

    @staticmethod
    def get_worker_id():
        default_worker_id = 'default'
        return os.environ.get('PYTEST_XDIST_WORKER', default_worker_id)
