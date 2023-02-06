import json
import logging
import pytest
from time import sleep
from datetime import datetime
from dateutil.tz import tzlocal
from functools import partial

from tests_common.pytest_bdd import when, then, given
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file

from mail.husky.husky.types import Task
from tests_common.steps.mdb import step_store_messages_impl
from .tasks import get_task_row, get_task_row_expect_one, get_task, check_task
from .common import plan_task, read_app_config, update_app_config, write_app_config
from .instant_delete_user import mdb_connection
from .cut_firstlines import get_border_date
from .shards import create_shard as create_fake_shard


QUERIES = load_from_my_file(__file__)
log = logging.getLogger(__name__)


def set_shard_registration(conn, shard_id, can_transfer_to):
    qexec(
        conn,
        QUERIES.set_can_transfer_to,
        shard_id=shard_id,
        can_transfer_to=can_transfer_to,
    )


@given('"{shard_id:d}" shard registration is closed')
def step_close_registration(context, shard_id):
    set_shard_registration(context.huskydb_conn, shard_id, False)


@given('"{shard_id:d}" shard registration is opened')
def step_open_registration(context, shard_id):
    set_shard_registration(context.huskydb_conn, shard_id, True)


def plan_task_from_context(context, task, **kwargs):
    task_args = json.loads(context.text or '{}')
    context.transfer_id = plan_task(context, task, task_args, **kwargs)
    context.task = task


@pytest.fixture()
def update_app_config_fixture(context, request):
    def finalizer(old_config):
        write_app_config(context, old_config, do_restart=True)

    current_config = read_app_config(context)
    request.addfinalizer(partial(finalizer, current_config))


@when('we set message count limit to "{limit:d}"')
def step_set_message_limit(context, request, limit, update_app_config_fixture):
    update_app_config(context, {'message_count_limit': limit})


@given('user has at least "{limit:d}" messages')
def step_given_store_messages_limit(context, limit):
    return step_store_messages_impl(**locals())


@given('user has "{limit:d}" messages in "{folder_type:w}"')
def step_given_store_messages_in_folder(context, limit, folder_type):
    return step_store_messages_impl(**locals())


@when('we plan "{task:w}"')
def step_plan_task(context, task):
    plan_task_from_context(context, task)


@when('we plan "{task:w}" on sleeping husky "{shard_id:d}"')
def step_plan_delayed_task(context, task, shard_id):
    create_fake_shard(context.sharddb_conn, context.huskydb_conn, str(shard_id), shard_id)
    plan_task_from_context(context, task, shard_id=shard_id)


def step_plan_transfer(context, **transfer_args):
    args = dict(
        from_db='postgre:1',
        to_db='postgre:2',
        **transfer_args
    )
    context.transfer_id = plan_task(context, Task.Transfer, args)
    context.task = Task.Transfer


@when('we plan transfer')
def step_plan_transfer_no_force(context):
    return step_plan_transfer(context)


@when('we plan transfer with force flag')
def step_plan_transfer_force(context):
    step_plan_transfer(context, force=True)


@when('we plan transfer with cutting old firstlines')
def step_plan_transfer_with_cut_old_firstline(context):
    firstline_options = {
        'cut': True,
        'target_len': 3,
        'max_date': get_border_date(context).isoformat(),
    }
    step_plan_transfer(context, force=True, firstline_options=firstline_options)


@when('we plan transfer with cutting all firstlines')
def step_plan_transfer_with_cut_all_firstline(context):
    firstline_options = {
        'cut': True,
        'target_len': 3,
        'max_date': datetime.max.isoformat(),
    }
    step_plan_transfer(context, force=True, firstline_options=firstline_options)


@when('we plan transfer with disabled message count check')
def step_plan_transfer_with_disabled_message_count_check(context):
    step_plan_transfer(context, check_message_count=False)


@when('we plan delete_mail_user')
def step_plan_delete_mail_user(context):
    context.transfer_id = plan_task(
        context,
        Task.DeleteMailUser,
        {
            'shard_id': 1,
            'deleted_date': datetime.now(tz=tzlocal()).isoformat(),
        }
    )
    context.task = Task.Transfer


@when('mark current task as "{task_name}"')
def step_mark_task(context, task_name):
    if not hasattr(context, "tasks_by_name"):
        context.tasks_by_name = {}
    assert task_name not in context.tasks_by_name,\
        'There is already task with name {0}: {1}'.format(
            task_name, context.tasks_by_name[task_name]
        )
    context.tasks_by_name[task_name] = context.transfer_id


@when('wait for husky to poll tasks')
def step_wait():
    sleep(5)


@when('wait "{count:d}" seconds for husky to poll tasks')
def step_wait_count_seconds(count):
    sleep(count)


@when('we move "{task_name}" to running husky')
def step_mark_task_to_be_processed(context, task_name):
    assert task_name in context.tasks_by_name,\
        'There with name {0} is not present'.format(task_name)
    qexec(
        context.huskydb_conn,
        QUERIES.update_shard_id,
        shard_id=context.config.default_shard_id,
        transfer_id=context.tasks_by_name[task_name],
    )


@then('task is successful')
def step_task_is_successfull(context):
    return step_task_is_successfull_impl(**locals())


@then('task "{task_name}" is successful')
def step_task_is_successfull_by_name(context, task_name):
    return step_task_is_successfull_impl(**locals())


def step_task_is_successfull_impl(context, task_name=None):
    if task_name is not None:
        transfer_id = context.tasks_by_name[task_name]
    else:
        transfer_id = context.transfer_id
    check_task(context, transfer_id)


@then('transfer is successful')
def step_transfer_is_success(context):
    assert context.task == Task.Transfer, \
        'Expected transfer task but was %s' % context.task
    check_task(context)


@then('task status is "{status:w}"')
def step_check_status(context, status):
    check_task(context, expected_status=status)


@then('"{task_name}" status is "{status:w}"')
def step_check_status_by_task_name(context, task_name, status):
    check_task(
        context,
        transfer_id=context.tasks_by_name[task_name],
        expected_status=status,
    )


@when('we get task info')
def step_get_task_info(context):
    context.task_info = get_task_row_expect_one(context, context.transfer_id)


def get_task_data(context, key):
    assert hasattr(context.task_info, key), \
        'Task user info doesnt have {key}'.format(key=key)
    return getattr(context.task_info, key)


def check_task_data(context, key, expected):
    actual = get_task_data(context, key)
    assert expected == actual, \
        'Expected {key}: "{expected}" but was: "{actual}"'.format(**locals())


@then('status is "{status:w}"')
def step_check_current_status(context, status):
    check_task_data(context, 'status', status)


@then('task planned in future')
def step_task_planned_in_future(context):
    cur = qexec(
        context.huskydb_conn,
        QUERIES.get_delayed_task,
        transfer_id=context.transfer_id,
    )
    dates = [i[0] for i in cur.fetchall()]
    assert len(dates) > 0


@then('error is "{error:w}"')
def step_check_error(context, error):
    check_task_data(context, 'error_type', error)


@then('there was 1 try')
def step_check_one_try(context):
    return step_check_tries_impl(**locals())


@then('there was {tries:d} tries')
def step_check_tries(context, tries):
    return step_check_tries_impl(**locals())


def step_check_tries_impl(context, tries=1):
    check_task_data(context, 'tries', tries)


@then('try notices contains "{notices_contains}"')
def step_check_try_notices(context, notices_contains):
    notices = get_task_data(context, 'try_notices')
    assert notices, "notices are empty"
    if len(notices) > 1:
        raise NotImplementedError(
            'Write logic here, got too many notices'
        )
    assert notices_contains in notices[0], \
        "Notices doesn't contain %s, notices %r" % (
            notices_contains, notices[0])


@then('"{left}" was processed later than "{right}"')
def step_check_timings(context, left, right):
    left_task = get_task(context, context.tasks_by_name[left])
    right_task = get_task(context, context.tasks_by_name[right])
    assert left_task.last_update > right_task.last_update


@then('there is our "{task_type:w}" task')
def step_check_has_task_with_type(context, task_type):
    task = get_task_row_expect_one(context, context.transfer_id)
    assert task.task == task_type, \
        'Expect %r task got %r' % (task_type, task.task)


@then('{request_name:w} request produce "{task_type:w}" task')
def step_check_request_produce_task(
        context,
        task_type,
        request_name=None):  # pylint: disable=W0613
    for transfer_id in context.transfer_id_list:
        task = get_task_row_expect_one(context, transfer_id)
        if task.task == task_type:
            context.transfer_id = task.transfer_id
            return
    raise AssertionError(
        "Can't find %r task with transfer_ids: %r" % (
            task_type, context.transfer_id_list)
    )


@then('there is no our task')
def step_check_task_does_not_exist(context):
    task = get_task_row(context, context.transfer_id)
    assert task is None, \
        'Found %r task, but expect that there are not such tasks' % task


@when('our "{task_type:w}" task is completed')
def step_check_has_task_and_wait_for_complete(context, task_type):
    context.execute_steps(
        u'''
        Then there is our "{task_type}" task
         And task is successful
        '''.format(task_type=task_type)
    )


@when('DBA cleanup our task')
def step_delete_task(context):
    qexec(
        context.huskydb_conn,
        QUERIES.delete_task,
        transfer_id=context.transfer_id
    )


@when('we make our task "{status:w}" with addition task args')
def step_retriger_task(context, status):
    task = get_task_row(context, context.transfer_id)
    new_task_args = task.task_args
    new_task_args.update(json.loads(context.text))

    qexec(
        context.huskydb_conn,
        QUERIES.set_task_status_and_args,
        transfer_id=context.transfer_id,
        status=status,
        task_args=json.dumps(new_task_args)
    )


@then('this task has task args with "{value:SpecifiedValue}" in "{field:w}"')
def step_check_task_args(context, field, value):
    task = get_task_row_expect_one(context, context.transfer_id)
    assert task.task_args[field] == value, \
        'Expected task args containing {"%s": %s}, but got: %s' % (
            field, value, task.task_args)


@then('all tasks are successful')
def step_check_all_user_tasks(context):
    for transfer_id in context.transfer_id_list:
        context.transfer_id = transfer_id
        context.execute_steps(u'Then task is successful')


@then('received date in deleted box has been transferred')
def step_received_date_is_not_null(context):
    with mdb_connection(context, shard_id=2) as conn:
        cur = qexec(
            conn,
            QUERIES.get_dates_from_deleted_box,
            uid=context.user.uid
        )
        dates = [i[0] for i in cur.fetchall()]
    assert len(dates) > 0
    for date in dates:
        assert type(date) == datetime


@given('"{message_count}" messages have received_date "{date}"')
def step_set_date_is_not_null(context, message_count, date):
    with mdb_connection(context, shard_id=1) as conn:
        qexec(
            conn,
            QUERIES.set_received_date_in_box,
            uid=context.user.uid,
            message_count=message_count,
            received_date=date,
        )


def get_task_in_users_in_dogsleds(context):
    cur = qexec(
        context.huskydb_conn,
        QUERIES.get_task_in_users_in_dogsleds,
        transfer_id=context.transfer_id,
    )
    return cur.fetchone()


@then('there is our task in users_in_dogsleds')
def step_task_in_users_in_dogsleds(context):
    assert get_task_in_users_in_dogsleds(context) is not None, \
        'Missing task with id=%s in users_in_dogsleds' % context.transfer_id


@then('there is no our task in users_in_dogsleds')
def step_no_task_in_users_in_dogsleds(context):
    assert get_task_in_users_in_dogsleds(context) is None, \
        'Found task with id=%s in users_in_dogsleds, but expect that there are not such tasks' % context.transfer_id


def get_task_in_processed_tasks(context):
    cur = qexec(
        context.huskydb_conn,
        QUERIES.get_task_in_processed_tasks,
        transfer_id=context.transfer_id,
    )
    return cur.fetchone()


@then('there is our task in processed_tasks')
def step_task_in_processed_tasks(context):
    assert get_task_in_processed_tasks(context) is not None, \
        'Missing task with id=%s in processed_tasks' % context.transfer_id


@then('there is no our task in processed_tasks')
def step_no_task_in_processed_tasks(context):
    assert get_task_in_processed_tasks(context) is None, \
        'Found task with id=%s in processed_tasks, but expect that there are not such tasks' % context.transfer_id
