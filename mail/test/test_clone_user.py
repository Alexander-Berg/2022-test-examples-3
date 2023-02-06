# coding: utf-8

from helpers import (
    mk_source_user,
    mk_dest_user,
    auto_patch,
    test_config,
)
from ora2pg.clone_user import clone_user


clone_patch = auto_patch('ora2pg.clone_user')


def test_clone_user_calls():
    source_user = mk_source_user()
    dest_user = mk_dest_user()

    with \
        clone_patch('assert_can_clone_into_dest_user') as can_clone_into_dest_user_mock, \
        clone_patch('assert_can_clone_source_user') as can_clone_source_user_mock, \
        clone_patch('register_dest_user_in_pg') as register_dest_user_in_pg_mock, \
        clone_patch('purge_dest_user') as purge_dest_user_mock, \
        clone_patch('clone_user_data_') as clone_user_data_mock \
    :
        clone_user(source_user, dest_user, test_config)
        can_clone_into_dest_user_mock.assert_called_once_with(
            dest_user, test_config
        )
        can_clone_source_user_mock.assert_called_once_with(
            source_user, test_config
        )
        register_dest_user_in_pg_mock.assert_called_once_with(
            dest_user, test_config
        )
        purge_dest_user_mock.assert_called_once_with(
            dest_user, test_config
        )
        clone_user_data_mock.assert_called_once_with(
            source_user, dest_user, test_config, None, None
        )
