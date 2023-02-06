# coding: utf-8
from tests_common.pytest_bdd import then

from pymdb import types as TYP
from ora2pg.compare import AreEqual
from ora2pg.pg_get import get_user
from .common import get_connection_to_shard


def compare_metadata(context, user_name, volatile_getter=lambda o: set()):
    uid=context.users[user_name].uid
    with \
        get_connection_to_shard(context, context.shards.first.value) as first_conn, \
        get_connection_to_shard(context, context.shards.second.value) as second_conn \
    :
        return AreEqual(
            sorter=sorted,
            volatile_getter=volatile_getter,
        )(
            l=get_user(uid=uid, conn=first_conn),
            r=get_user(uid=uid, conn=second_conn),
            name='User')


@then(u'"{user_name:w}" metadata is identical')
def step_metadata_identical(context, user_name):
    assert compare_metadata(context, user_name)


def sfs_volatile_misc(obj):
    if isinstance(obj, TYP.SharedFolderSubscription):
        return set(['updated', 'worker_id', 'state'])
    return set()


@then(u'"{user_name:w}" metadata is identical except subscriptions state, worker_id and updated')
def step_metadata_identical_except_subscriptions_misc(context, user_name):
    assert compare_metadata(context, user_name, sfs_volatile_misc)


@then(u'"{user_name:w}" metadata is identical except subscriptions')
def step_metadata_identical_except_subscriptions(context, user_name):
    assert compare_metadata(context, user_name, volatile_getter=lambda o: set(['shared_folder_subscriptions']))
