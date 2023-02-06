import pytest
from contextlib import contextmanager
from collections import namedtuple

import mock

import mail.husky.husky.tasks as tasks
from ora2pg.clone_user import CloneConfig, NotSupportedUserError
import mail.husky.husky.tasks.tests.helpers as helpers

CloneEntryPoints = namedtuple(
    'CloneEntryPoints',
    ['clone_user', 'get_all_by_uid', 'get_sharddb_master_host']
)


@contextmanager
def mocked_clone():
    with \
        mock.patch('mail.husky.husky.tasks.clone_user.clone_user', autospec=True) as mock_clone, \
        mock.patch('mail.husky.husky.tasks.clone_user.get_all_by_uid', autospec=True) as mock_get_all, \
        mock.patch('ora2pg.tools.find_master_helpers.get_sharddb_master_host', autospec=True) as mock_get_sharddb_master_host \
    :
        yield CloneEntryPoints(mock_clone, mock_get_all, mock_get_sharddb_master_host)


def test_clone_user_calls():
    with \
        mocked_clone() as clone_mocks, \
        mock.patch.object(tasks.CloneUser, 'mark_clone_audit_as_completed') as mark_clone_audit_as_completed_mock \
    :
        task_args = {
            'dest_shard': 4200,
            'source_user_uid': helpers.SRC_USER.uid,
        }
        clone_mocks.get_all_by_uid.side_effect = [helpers.SRC_USER, helpers.USER]
        clone_mocks.get_sharddb_master_host.side_effect = ['sharddb dsn', 'sharddb dsn']

        tasks.CloneUser(
            *helpers.make_handler_args(task_args=task_args)
        ).run()

        test_config = helpers.APP.args
        clone_mocks.clone_user.assert_called_once_with(
            source_user=helpers.SRC_USER,
            dest_user=helpers.USER,
            config=CloneConfig(
                dest_shard_id=4200,
                sharddb='sharddb dsn',
                sharpei=test_config.sharpei,
                mailhost=test_config.mailhost,
                blackbox=test_config.blackbox,
                mulcagate=test_config.mulcagate,
                maildb_dsn_suffix=test_config.maildb_dsn_suffix,
                huskydb='huskydb dsn',
            ),
            force=False,
            min_received_date=None,
            max_received_date=None,
        )
        mark_clone_audit_as_completed_mock.assert_called_once_with()


def test_clone_user_raise_not_supported_user_error_on_unexpected_user():
    with mocked_clone() as clone_mocks:
        clone_mocks.clone_user.side_effect = [NotSupportedUserError('oh')]
        with pytest.raises(tasks.errors.NotSupportedError):
            tasks.CloneUser(
                *helpers.make_handler_args(task_args=dict(source_user_uid=helpers.SRC_USER.uid, dest_shard=100500))
            ).run()
