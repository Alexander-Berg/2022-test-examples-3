from tests_common.pytest_bdd import then, given

from mail.pypg.pypg.common import transaction, qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from pymdb.queries import fetch_one_row_with_one_element

QUERIES = load_from_my_file(__file__)


def move_user_to_deleted(context, user_name):
    user = context.get_user(user_name)
    with transaction(context.config['sharddb']) as conn:
        qexec(
            conn,
            QUERIES.move_user_to_deleted,
            uid=user.uid,
        )


def move_deleted_user_to_users(context, user_name):
    user = context.get_user(user_name)
    with transaction(context.config['sharddb']) as conn:
        qexec(
            conn,
            QUERIES.move_deleted_user_to_users,
            uid=user.uid,
        )


def drop_user_from_deleted_users(context, user_name):
    user = context.get_user(user_name)
    with transaction(context.config['sharddb']) as conn:
        qexec(
            conn,
            QUERIES.drop_user_from_deleted_users,
            uid=user.uid,
        )


def drop_user_from_users(context, user_name):
    user = context.get_user(user_name)
    with transaction(context.config['sharddb']) as conn:
        qexec(
            conn,
            QUERIES.drop_user_from_users,
            uid=user.uid,
        )


def get_user_shard(context, user_name):
    user = context.get_user(user_name)
    with transaction(context.config['sharddb']) as conn:
        cur = qexec(
            conn,
            QUERIES.get_user_shard,
            uid=user.uid,
        )
        return fetch_one_row_with_one_element(cur)


def change_deleted_user_shard(context, user_name, shard_id):
    user = context.get_user(user_name)
    with transaction(context.config['sharddb']) as conn:
        qexec(
            conn,
            QUERIES.change_deleted_user_shard,
            uid=user.uid,
            shard_id=shard_id,
        )


def change_user_shard(context, user_name, shard_id):
    user = context.get_user(user_name)
    with transaction(context.config['sharddb']) as conn:
        qexec(
            conn,
            QUERIES.change_user_shard,
            uid=user.uid,
            shard_id=shard_id,
        )


def open_shard_registartion(context, shard_id):
    with transaction(context.config['sharddb']) as conn:
        qexec(
            conn,
            QUERIES.open_shard_registartion,
            shard_id=shard_id,
        )


def is_registration_opened(context, shard_id):
    with transaction(context.config['sharddb']) as conn:
        cur = qexec(
            conn,
            QUERIES.get_shard_registartion,
            shard_id=shard_id,
        )
        return cur.fetchone() is not None


def is_user_in_sharddb_users(context, user_name):
    user = context.get_user(user_name)
    with transaction(context.config['sharddb']) as conn:
        cur = qexec(
            conn,
            QUERIES.get_user,
            uid=user.uid,
        )
        return cur.fetchone() is not None


def is_user_in_sharddb_deleted_users(context, user_name):
    user = context.get_user(user_name)
    with transaction(context.config['sharddb']) as conn:
        cur = qexec(
            conn,
            QUERIES.get_deleted_user,
            uid=user.uid,
        )
        return cur.fetchone() is not None


@then('registration for current shard closed')
def step_shard_registration_closed(context):
    assert not is_registration_opened(context, context.config['shard_id']), (
        'Expected: shard is close, but it was opened'
    )


@then('registration for current shard opened')
def step_shard_registration_opened(context):
    assert is_registration_opened(context, context.config['shard_id']), (
        'Expected: shard is open, but it was closed'
    )


@given('"{user_name:w}" is absent in sharddb')
def step_drop_user_from_sharddb(context, user_name):
    drop_user_from_deleted_users(context, user_name)
    drop_user_from_users(context, user_name)


@then('"{user_name:w}" is absent in sharddb users')
def step_check_user_is_absent_in_sharddb_users(context, user_name):
    assert not is_user_in_sharddb_users(context, user_name), (
        'Expected: the user is absent in sharddb users, but he is present'
    )


@then('"{user_name:w}" is present in sharddb users')
def step_check_user_is_present_in_sharddb_users(context, user_name):
    assert is_user_in_sharddb_users(context, user_name), (
        'Expected: the user is present in sharddb users, but he is absent'
    )


@then('"{user_name:w}" is absent in sharddb deleted users')
def step_check_user_is_absent_in_sharddb_deleted_users(context, user_name):
    assert not is_user_in_sharddb_deleted_users(context, user_name), (
        'Expected: the user is absent in sharddb deleted users, but he is present'
    )


@then('"{user_name:w}" is present in sharddb deleted users')
def step_check_user_is_present_in_sharddb_deleted_users(context, user_name):
    assert is_user_in_sharddb_deleted_users(context, user_name), (
        'Expected: the user is present in sharddb deleted users, but he is absent'
    )


@given('"{user_name:w}" is not deleted in sharddb')
def step_move_deleted_user_to_users(context, user_name):
    move_deleted_user_to_users(context, user_name)


@given('"{user_name:w}" is deleted in sharddb')
def step_move_user_to_deleted(context, user_name):
    move_user_to_deleted(context, user_name)


@given('"{user_name:w}" is in different shard in sharddb')
def step_change_deleted_user_shard(context, user_name):
    shard_id = context.config['shard_id2']
    change_deleted_user_shard(context, user_name, shard_id)


@given('"{user_name:w}" is in the original shard in sharddb')
def step_change_user_shard_to_original(context, user_name):
    shard_id = context.config['shard_id']
    change_user_shard(context, user_name, shard_id)


@given('"{user_name:w}" is accidentally in different shard in sharddb')
def step_change_user_shard(context, user_name):
    shard_id = context.config['shard_id2']
    change_user_shard(context, user_name, shard_id)


def get_shard_id(context, is_main_shard):
    return int(context.config['shard_id'] if is_main_shard else context.config['shard_id2'])


@given('"{user_name:w}" is in {is_main:IsMain} shard in sharddb')
def step_given_user_in_given_shard(context, user_name, is_main):
    shard_id = get_shard_id(context, is_main)
    change_user_shard(context, user_name, shard_id)


@then('"{user_name:w}" is in {is_main:IsMain} shard in sharddb')
def step_then_user_in_given_shard(context, user_name, is_main):
    assert get_user_shard(context, user_name) == get_shard_id(context, is_main)
