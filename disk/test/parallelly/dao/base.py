# -*- coding: utf-8 -*-
from contextlib import nested

from test.base import PostgresUnits
from test.common.sharding import CommonShardingMethods  # must be the first

import pytest
import mock

import mpfs.engine.process

from mpfs.dao.shard_endpoint import ShardEndpoint
from mpfs.dao.session import Session
from mpfs.metastorage.mongo.user import MongoUserController
from mpfs.metastorage.postgres.query_executer import PGQueryExecuter


def iter_existed_table_names(connection):
    schema_name = 'disk'
    cursor = connection.execute("SELECT table_name "
                                "FROM information_schema.tables "
                                "WHERE table_schema='%s';" % schema_name)
    for table_name in cursor:
        yield schema_name + '.' + table_name[0]


class BasePostgresTestCase(CommonShardingMethods):

    @staticmethod
    def truncate_existed_tables(connection):
        tables_str = ''
        for table_name in iter_existed_table_names(connection):
            tables_str += '%s,' % table_name
        connection.execute('TRUNCATE TABLE %s CASCADE;' % tables_str.rstrip(','))

    def setup_method(self, method):
        super(BasePostgresTestCase, self).setup_method(method)
        session = Session.create_from_shard_id(PostgresUnits.UNIT1)
        with session.begin():
            self.truncate_existed_tables(session)


class BaseMigrationTestCase(BasePostgresTestCase):
    mongo_endpoint1 = ShardEndpoint.parse('mongo:%s' % BasePostgresTestCase.mongodb_unit1)
    mongo_endpoint2 = ShardEndpoint.parse('mongo:%s' % BasePostgresTestCase.mongodb_unit2)
    postgres_endpoint1 = ShardEndpoint.parse('postgres:%s' % PostgresUnits.UNIT1)
    postgres_endpoint2 = ShardEndpoint.parse('postgres:%s' % PostgresUnits.UNIT2)

    @staticmethod
    def is_collection_in_postgres(uid, collection_name):
        usrctl = mpfs.engine.process.usrctl()
        return usrctl.is_collection_in_postgres(uid, collection_name)

    def remove_none_fields(self, value):
        if isinstance(value, list):
            for v in value:
                self.remove_none_fields_from_item(v)
            return sorted(value)
        else:
            self.remove_none_fields_from_item(value)
            return value

    def remove_none_fields_from_item(self, value):
        for key in list(value.keys()):
            if value[key] is None:
                value.pop(key)

    def _get_user_shard(self, uid):
        user_info = self.json_ok('user_info', {'uid': uid})
        shard = user_info['db']['shard']
        return shard

    def mock_collection_location_function(self, coll_names, is_in_postgres, mock_for_uid=None):
        if isinstance(coll_names, basestring):
            coll_names = (coll_names,)

        real_is_collection_in_postgres = MongoUserController.is_collection_in_postgres

        def fake_is_collection_in_postgres(self, uid, coll_name):
            if (mock_for_uid is None or mock_for_uid == uid) and coll_name in coll_names:
                return is_in_postgres
            return real_is_collection_in_postgres(self, uid, coll_name)

        return nested(
            mock.patch.object(MongoUserController, 'is_collection_in_postgres', fake_is_collection_in_postgres),
            self.mock_user_location_function(not is_in_postgres, mock_for_uid),
        )

    def mock_user_location_function(self, is_in_postgres, mock_for_uid=None):
        real_is_user_in_postgres = MongoUserController.is_user_in_postgres

        def fake_is_user_in_postgres(self, uid):
            if mock_for_uid is None or mock_for_uid == uid:
                return is_in_postgres
            return real_is_user_in_postgres(self, uid)
        return nested(
            mock.patch.object(MongoUserController, 'is_user_in_postgres', fake_is_user_in_postgres),
            self.mock_pg_user_location_function(is_in_postgres, mock_for_uid),
        )

    def mock_pg_user_location_function(self, is_in_postgres, mock_for_uid=None):
        real_is_user_in_postgres = PGQueryExecuter.is_user_in_postgres

        def fake_is_pg_user_in_postgres(self, uid):
            if mock_for_uid is None or mock_for_uid == uid:
                return is_in_postgres
            return real_is_user_in_postgres(self, uid)
        return mock.patch.object(PGQueryExecuter, 'is_user_in_postgres', fake_is_pg_user_in_postgres)

    def migrate_mongo_postgres_and_check(self, migrator, uid=None):
        if uid is None:
            uid = self.uid
        mongo_endpoint = ShardEndpoint.parse('mongo:%s' % self._get_user_shard(uid))
        assert migrator.run(uid, self.postgres_endpoint1)
        PGQueryExecuter().create_user(uid)
        assert migrator.check_migrated_count(uid, mongo_endpoint, self.postgres_endpoint1)
