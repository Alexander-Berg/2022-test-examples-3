from tests_common.pytest_bdd import given, when, then
from hamcrest import assert_that, has_entries
from itertools import chain
from time import sleep
import logging
import json

from mail.pypg.pypg.common import qexec, fetch_as_dicts
from mail.pypg.pypg.query_conf import load_from_my_file
from tests_common.fbbdb import user_from_id
from ora2pg.sharpei import init_in_sharpei, add_deleted_user_to_sharpei

QUERIES = load_from_my_file(__file__)
log = logging.getLogger(__name__)


def create_shard(sharddb_conn, huskydb_conn, name, shard_id, load_type='custom', can_transfer_to=True):
    qexec(
        sharddb_conn,
        QUERIES.add_shard,
        name=name,
        shard_id=shard_id,
    )
    qexec(
        sharddb_conn,
        QUERIES.add_shard_instance,
        shard_id=shard_id,
    )
    qexec(
        huskydb_conn,
        QUERIES.add_shard_to_shiva,
        shard_id=shard_id,
        shard_name=name,
        load_type=load_type,
        can_transfer_to=can_transfer_to,
    )
    qexec(
        huskydb_conn,
        QUERIES.add_fake_husky_worker,
        shard_id=shard_id,
    )


@given('we have shard "{name:w}" with shard_id "{shard_id:d}"')
def step_create_shard(context, name, shard_id):
    create_shard(context.sharddb_conn, context.huskydb_conn, name, shard_id)
    sleep(1)  # reset sharpei cache


def make_new_user(context, user_name=None):
    uid = context.get_free_uid()
    user = user_from_id(uid, user_name)
    log.info('ask for new uid=%r, new_user is %r', uid, user)
    return user


@given('in shard "{shard_id:d}" we have users "{user_names}"')
def step_make_users(context, shard_id, user_names):
    names = user_names.split(',')
    context.shard_users.forget()
    for name in names:
        user = make_new_user(context, name)
        init_in_sharpei(
            user.uid,
            context.config.sharddb,
            True,
            shard_id
        )
        context.shard_users.add(user, name)


@given('in shard "{shard_id:d}" we have deleted users "{user_names}"')
def step_make_deleted_users(context, shard_id, user_names):
    names = user_names.split(',')
    context.shard_deleted_users.forget()
    for name in names:
        user = make_new_user(context, name)
        add_deleted_user_to_sharpei(
            user.uid,
            context.config.sharddb,
            shard_id
        )
        context.shard_deleted_users.add(user, name)


@given('husky task queue is clean')
def step_husky_task_queue_is_clean(context):
    qexec(context.huskydb_conn, QUERIES.clean_husky_task_queue)


@when('we get all husky tasks')
def step_get_all_husky_tasks(context):
    context.tasks = {}
    for task in (fetch_as_dicts(qexec(context.huskydb_conn, QUERIES.get_all_tasks))):
        if task['uid'] not in context.tasks:
            context.tasks[task['uid']] = []
        context.tasks[task['uid']].append(task)


def check_tasks_params(context, users):
    params = json.loads(context.text)
    for user in users:
        tasks = context.tasks[user.uid]
        assert len(tasks) == 1
        assert_that(tasks[0], has_entries(**params))


@then('every active shard user has one task with params')
def step_check_shard_users_tasks(context):
    check_tasks_params(context, context.shard_users)


@then('every shard user including deleted has one task with params')
def step_check_shard_deleted_users_tasks(context):
    check_tasks_params(context, chain(context.shard_users, context.shard_deleted_users))


@then('there are no tasks for deleted shard users')
def step_check_shard_deleted_users_no_tasks(context):
    for user in context.shard_deleted_users:
        assert user.uid not in context.tasks
