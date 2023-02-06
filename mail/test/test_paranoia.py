# coding: utf-8

from ora2pg.clone_user import paranoia
from ora2pg.sharpei import ShardInfo

import pytest

from helpers import (
    mk_dest_user,
    mk_source_user,
    test_config,
    auto_patch,
)

paranoia_patch = auto_patch('ora2pg.clone_user.paranoia')


def test_register_dest_user_in_pg_for_pg_user_call_init_in_sharpei_only():
    dest_user = mk_dest_user('pg')

    with \
        paranoia_patch('init_in_sharpei') as init_in_sharpei_mock \
    :
        paranoia.register_dest_user_in_pg(
            dest_user, test_config
        )
        init_in_sharpei_mock.assert_called_once_with(
            dsn=test_config.sharddb,
            shard_id=test_config.dest_shard_id,
            uid=dest_user.uid,
            allow_inited=True,
        )


# pylint: disable=R0201


class Test_assert_can_clone_source_user(object):
    def call_it(self, user, shard_id=None):
        with paranoia_patch('get_shard_info') as get_shard_info_mock:
            get_shard_info_mock.return_value = ShardInfo(shard_id, False)

            paranoia.assert_can_clone_source_user(
                user,
                test_config,
            )

    def test_ok_pg_user_registered_in_sharpei(self):
        self.call_it(
            mk_source_user('pg'),
            shard_id=13
        )


class Test_assert_can_clone_into_dest_user(object):
    def call_it(self, user, shard_id=None, initialized_in_mdb=False):
        with \
            paranoia_patch('get_shard_id') as get_shard_id_mock, \
            paranoia_patch('get_shard_info') as get_shard_info_mock, \
            paranoia_patch('is_initialized_in_mdb_for_more_than_1min') as is_initialized_in_mdb_mock \
        :
            get_shard_info_mock.return_value = ShardInfo(shard_id, False)
            get_shard_id_mock.return_value = shard_id
            is_initialized_in_mdb_mock.return_value = initialized_in_mdb
            paranoia.assert_can_clone_into_dest_user(
                user,
                test_config
            )

    def test_ok_pg_user_not_registred_in_sharpei(self):
        self.call_it(
            mk_dest_user('pg'),
            shard_id=None,
        )

    def test_ok_pg_user_registred_in_sharpei_but_without_mdb_meta(self):
        self.call_it(
            mk_dest_user('pg'),
            shard_id=None,
            initialized_in_mdb=False
        )

    def test_raise_for_pg_user_registred_in_sharpei_and_initialized_in_mdb(self):
        with pytest.raises(paranoia.DestUserShouldNotBeRegistered):
            self.call_it(
                mk_dest_user('pg'),
                shard_id=13,
                initialized_in_mdb=True
            )
