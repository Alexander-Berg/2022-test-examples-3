from datetime import timedelta

from hamcrest import (
    assert_that,
    has_length,
    only_contains,
    is_not,
    empty,
)

from .lists import AttributeCaster, TableCompartor
from pymdb.operations import (
    AddUnsubscribeTask,
    GetUnsubscribeTasks,
    DeleteSubscriptionsAndUnsubscribeTask,
)
from pymdb.queries import Queries
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from tests_common.pytest_bdd import given, when, then

Q = load_from_my_file(__file__)


@given('there are no tasks left')
def step_clear_all_tasks(context):
    qexec(context.conn, Q.clear_unsubscribe_tasks)
    context.conn.commit()


@given('new initialized owner')
def step_new_owner(context):
    context.execute_steps(
        u'Given new initialized user "owner"'
    )
    context.owner_uid = context.users['owner']


@given('new initialized owner with tasks')
def step_added_unsubscribe_tasks(context):
    context.base_table = context.table
    context.execute_steps(u'''
        Given new initialized owner
         When we add unsubscribe tasks for this owner with params
    ''')


def get_our_subscription(context):
    subs = context.qs.shared_folder_subscriptions()
    assert_that(subs, has_length(1))
    return subs[0]


@given('unsubscribe task for this subscription')
def step_add_task_by_subscription(context):
    sub = get_our_subscription(context)
    context.our_subscription = sub
    context.returned_tasks = []
    op = context.make_operation(
        AddUnsubscribeTask
    )(
        task_request_id='some_request_id',
        owner_uid=context.uid,
        owner_fids=[sub.fid],
        subscriber_uid=sub.subscriber_uid,
        root_subscriber_fid=10,
    )
    op.commit()
    context.returned_tasks.extend(op.result)


def update_task_assigned(conn, task, passed):
    qexec(
        conn,
        Q.assign_unsubscribe_task,
        task_id=task.task_id,
        passed=passed,
    )
    conn.commit()


@when('we add unsubscribe tasks for this owner with params')
def step_add_unsubscribe_tasks(context):
    table = context.table if context.table else context.base_table
    context.returned_tasks = []
    owner_uid = context.owner_uid
    for row in table.rows:
        owner_fids = [int(f.strip()) for f in row['owner_fids'].split(',')]
        op = context.make_operation(
            AddUnsubscribeTask
        )(
            task_request_id=row['task_request_id'],
            owner_uid=owner_uid,
            owner_fids=owner_fids,
            subscriber_uid=row['subscriber_uid'],
            root_subscriber_fid=row['root_subscriber_fid'],
        )
        op.commit()
        context.returned_tasks.extend(op.result)
        if 'assigned' in row.headings:
            if row['assigned'] == 'now':
                update_task_assigned(context.conn, op.result[0], 1)
            elif row['assigned'] == 'before':
                update_task_assigned(context.conn, op.result[0], -1)


def async_add_unsubscribe_tasks(context, op_id, owner_uid):
    table = context.table if context.table else context.base_table
    for row in table.rows:
        owner_fids = [int(f.strip()) for f in row['owner_fids'].split(',')]
        context.operations[op_id] = context.make_async_operation(
            AddUnsubscribeTask
        )(
            task_request_id=row['task_request_id'],
            owner_uid=owner_uid,
            owner_fids=owner_fids,
            subscriber_uid=row['subscriber_uid'],
            root_subscriber_fid=row['root_subscriber_fid'],
        )


@when('we try add unsubscribe tasks for this owner as "{op_id:OpID}" with params')
def step_try_add_unsubscribe_tasks(context, op_id):
    async_add_unsubscribe_tasks(context, op_id, context.owner_uid)


def invalid_uid(context):
    result = 666
    while Queries(context.conn, result).user_exists():
        result *= 10
        if result > 2 ** 63:
            result /= 2 ** 60
    return result


@when('we try add unsubscribe tasks for different owner as "{op_id:OpID}" with params')
def step_try_add_unsubscribe_tasks_different_owner(context, op_id):
    async_add_unsubscribe_tasks(context, op_id, invalid_uid(context))


def delete_unsubscribe_task(context, pos):
    task_id = context.returned_tasks[pos - 1].task_id
    op = context.make_operation(
        DeleteSubscriptionsAndUnsubscribeTask
    )(
        task_id=task_id,
    )
    op.commit()
    context.deleted_subscriptions = op.result


@when('we delete task at position {pos:d}')
def step_delete_unsubscribe_task(context, pos):
    delete_unsubscribe_task(context, pos)


@when('we delete this task')
def step_delete_this_unsubscribe_task(context):
    delete_unsubscribe_task(context, 1)


@when('we get {limit:d} tasks with ttl {timeout:d} sec')
def step_get_unsubscribe_tasks(context, limit, timeout):
    op = context.make_operation(
        GetUnsubscribeTasks
    )(
        task_limit=limit,
        task_ttl=timedelta(seconds=timeout),
    )
    op.commit()
    context.returned_tasks = op.result


@when('we try get {limit:d} tasks with ttl {timeout:d} sec as "{op_id:OpID}"')
def step_try_get_unsubscribe_tasks(context, limit, timeout, op_id):
    context.operations[op_id] = context.make_async_operation(
        GetUnsubscribeTasks
    )(
        task_limit=limit,
        task_ttl=timedelta(seconds=timeout),
    )


class UnsubscribeTaskCaster(AttributeCaster):
    def _cast_owner_fids(self, real, expected):
        return real, [int(f.strip()) for f in expected.split(',')]


def compare_tasks(context, tasks):
    TableCompartor(
        context=context,
        pk='task_request_id',
        obj_name='unsubscribe_tasks row',
        caster=UnsubscribeTaskCaster(context),
    ).compare(
        seq=[t.as_dict() for t in tasks],
        count=len(context.table.rows),
    )


@then('there are tasks for owner with params')
def step_check_tasks(context):
    tasks = context.qs.unsubscribe_tasks()
    compare_tasks(context, tasks)


def check_task_count(context, count):
    tasks = context.qs.unsubscribe_tasks()
    assert_that(tasks, has_length(count))


@then('there are {count:d} tasks for owner')
@then('there is {count:d} task for owner')
def step_check_task_count(context, count):
    check_task_count(context, count)


@then('there are no tasks for owner')
def step_check_no_tasks(context):
    check_task_count(context, 0)


@then('returned tasks are')
def step_check_returned_tasks(context):
    tasks = context.returned_tasks
    compare_tasks(context, tasks)


@then('there are {count:d} returned tasks')
@then('there is {count:d} returned task')
def step_check_returned_tasks_count(context, count):
    assert_that(context.returned_tasks, has_length(count))


@then('task assigned was updated')
def step_check_task_assigned(context):
    tasks = context.qs.unsubscribe_tasks()
    assert_that([t.assigned for t in tasks], only_contains(is_not(empty)))


@then('returned tasks in "{op_id:OpID}" are')
def step_check_returned_tasks_in_operation(context, op_id):
    tasks = context.operations[op_id].result
    compare_tasks(context, tasks)


@then('there are {count:d} returned tasks in "{first_op:OpID}" and "{second_op:OpID}" combined')
def step_check_returned_tasks_count_combined(context, count, first_op, second_op):
    combined = context.operations[first_op].result + context.operations[second_op].result
    assert_that(combined, has_length(count))


@then('tasks in "{first_op:OpID}" and "{second_op:OpID}" are different')
def step_tasks_in_ops_are_different(context, first_op, second_op):
    task_id_first = [t.task_id for t in context.operations[first_op].result]
    task_id_second = [t.task_id for t in context.operations[second_op].result]

    common = set(task_id_first).intersection(task_id_second)
    assert_that(common, empty())


@then('owner has no subscriptions')
def step_check_subscriptions_empty(context):
    subs = context.qs.shared_folder_subscriptions()
    assert_that(subs, empty())


@then('deleted subscriptions returned')
def step_check_deleted_subscriptions(context):
    assert_that(context.deleted_subscriptions,
                only_contains(context.our_subscription))
