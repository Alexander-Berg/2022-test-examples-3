import pytest
import mail.husky.husky.tasks as tasks
import mock
from mail.husky.husky.sharddb import SharddbAdaptor, ShardIdResolveException
import mail.husky.husky.tasks.tests.helpers as helpers
from mail.husky.husky.types import Errors

from ora2pg.transfer import UserNotInFromDb

parametrize = pytest.mark.parametrize


@mock.patch('ora2pg.tools.find_master_helpers.get_sharddb_master_host')
@mock.patch('mail.husky.husky.tasks.transfer._has_too_many_messages')
@mock.patch('mail.husky.husky.tasks.transfer.get_all_by_uid')
@mock.patch('mail.husky.husky.tasks.transfer.transfer')
@mock.patch.object(tasks.Transfer, 'can_transfer', True)
def test_transfer_parse_args(mocked_transfer, mocked_get_all_by_uid, mocked_has_too_many_messages, mocked_get_sharddb_master_host):
    mocked_get_sharddb_master_host.return_value = 'sharddb dsn'
    mocked_has_too_many_messages.return_value = False
    mocked_get_all_by_uid.return_value = helpers.USER
    retval = tasks.Transfer(*helpers.make_handler_args(task_args=helpers.TASK_ARGS)).run()
    mocked_has_too_many_messages.assert_called_once()
    mocked_transfer.assert_called_once()
    assert retval is None


@mock.patch('ora2pg.tools.find_master_helpers.get_sharddb_master_host')
@mock.patch('mail.husky.husky.tasks.transfer._has_too_many_messages')
@mock.patch('mail.husky.husky.tasks.transfer.get_all_by_uid')
@mock.patch.object(SharddbAdaptor, 'get_shard_id')
@mock.patch('mail.husky.husky.tasks.transfer.transfer')
@mock.patch.object(tasks.Transfer, 'can_transfer', True)
def test_transfer_resolve_shard_name(mocked_transfer, mocked_get_shard_id, mocked_get_all_by_uid, mocked_has_too_many_messages, mocked_get_sharddb_master_host):
    mocked_get_sharddb_master_host.return_value = 'sharddb dsn'
    mocked_has_too_many_messages.return_value = False
    mocked_get_all_by_uid.return_value = helpers.USER
    mocked_get_shard_id.side_effect = [helpers.FROM_DB_SHARD_ID, helpers.TO_DB_SHARD_ID, helpers.FROM_DB_SHARD_ID]
    task_args = dict(**helpers.TASK_ARGS)
    task_args.update({
        'to_db': helpers.TO_DB_NAME,
        'from_db': helpers.FROM_DB_NAME,
    })
    retval = tasks.Transfer(
        *helpers.make_handler_args(task_args=task_args)
    ).run()
    mocked_has_too_many_messages.assert_called_once()
    mocked_transfer.assert_called_once()
    assert retval is None


@mock.patch('ora2pg.tools.find_master_helpers.get_sharddb_master_host')
@mock.patch('mail.husky.husky.tasks.transfer.get_all_by_uid')
@mock.patch.object(SharddbAdaptor, 'get_shard_id')
@mock.patch('mail.husky.husky.tasks.transfer.transfer')
@mock.patch.object(tasks.Transfer, 'can_transfer', True)
def test_transfer_resolve_unexistent_shard_name(_, mocked_get_shard_id, mocked_get_all_by_uid, mocked_get_sharddb_master_host):
    mocked_get_sharddb_master_host.return_value = 'sharddb dsn'
    mocked_get_all_by_uid.return_value = helpers.USER
    mocked_get_shard_id.side_effect = ShardIdResolveException
    task_args = dict(**helpers.TASK_ARGS)
    task_args.update({
        'to_db': 'postgre:unexistent_shard_name',
        'from_db': helpers.FROM_DB_NAME,
    })
    with pytest.raises(tasks.errors.MalformedArgumentError):
        tasks.Transfer(*helpers.make_handler_args(task_args=task_args)).run()


@parametrize(
    ('side_effect', 'error'), [
    (UserNotInFromDb, Errors.InvalidFromDb),
    ]
)
@mock.patch('ora2pg.tools.find_master_helpers.get_sharddb_master_host')
@mock.patch('mail.husky.husky.tasks.transfer._has_too_many_messages')
@mock.patch('mail.husky.husky.tasks.transfer.get_all_by_uid')
@mock.patch('mail.husky.husky.tasks.transfer.transfer')
@mock.patch.object(tasks.Transfer, 'can_transfer', True)
def test_transfer_pass_error(mocked_transfer, mocked_get_all_by_uid, mocked_has_too_many_messages, mocked_get_sharddb_master_host, side_effect, error):
    mocked_get_sharddb_master_host.return_value = 'sharddb dsn'
    mocked_has_too_many_messages.return_value = False
    mocked_get_all_by_uid.return_value = helpers.USER
    mocked_transfer.side_effect = side_effect
    retval = tasks.Transfer(*helpers.make_handler_args(task_args=helpers.TASK_ARGS)).run()
    mocked_has_too_many_messages.assert_called_once()
    assert retval.error == error


@parametrize('check_message_count', [False, True])
@mock.patch('ora2pg.tools.find_master_helpers.get_sharddb_master_host')
@mock.patch('mail.husky.husky.tasks.transfer._has_too_many_messages')
@mock.patch('mail.husky.husky.tasks.transfer.get_all_by_uid')
@mock.patch('mail.husky.husky.tasks.transfer.transfer')
@mock.patch.object(tasks.Transfer, 'can_transfer', True)
def test_transfer_user_with_too_many_messages(mocked_transfer, mocked_get_all_by_uid, mocked_has_too_many_messages, mocked_get_sharddb_master_host, check_message_count):
    mocked_get_sharddb_master_host.return_value = 'sharddb dsn'
    mocked_has_too_many_messages.return_value = True
    mocked_get_all_by_uid.return_value = helpers.USER
    task_args = dict(**helpers.TASK_ARGS)
    task_args.update({
        'check_message_count': check_message_count,
    })
    retval = tasks.Transfer(*helpers.make_handler_args(task_args=task_args)).run()
    if check_message_count:
        mocked_has_too_many_messages.assert_called_once()
        mocked_transfer.assert_not_called()
        assert retval.error == Errors.DeferredDueToMessageCountLimit
    else:
        mocked_has_too_many_messages.assert_not_called()
        mocked_transfer.assert_called_once()
        assert retval is None


@parametrize(
    ('side_effect', 'error'), [
    (AssertionError, Errors.FailedGetData),
    (KeyError, Errors.FailedGetData),
    ]
)
@mock.patch('ora2pg.tools.find_master_helpers.get_sharddb_master_host')
@mock.patch('mail.husky.husky.tasks.transfer._has_too_many_messages')
@mock.patch('mail.husky.husky.tasks.transfer.get_all_by_uid')
@mock.patch('mail.husky.husky.tasks.transfer.transfer')
@mock.patch.object(tasks.Transfer, 'can_transfer', True)
def test_has_too_many_messages_pass_error(mocked_transfer, mocked_get_all_by_uid, mocked_has_too_many_messages, mocked_get_sharddb_master_host, side_effect, error):
    mocked_get_sharddb_master_host.return_value = 'sharddb dsn'
    mocked_has_too_many_messages.side_effect = side_effect
    mocked_get_all_by_uid.return_value = helpers.USER
    retval = tasks.Transfer(*helpers.make_handler_args(task_args=helpers.TASK_ARGS)).run()
    mocked_has_too_many_messages.assert_called_once()
    mocked_transfer.assert_not_called()
    assert retval.error == error
