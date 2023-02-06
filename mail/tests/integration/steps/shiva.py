from tests_common.pytest_bdd import when, then
from time import sleep
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file

QUERIES = load_from_my_file(__file__)


def has_shiva_tasks(context):
    cur = qexec(
        context.huskydb_conn,
        QUERIES.get_shiva_tasks
    )
    return cur.fetchone() is not None


@then(u'all shiva tasks finished')
def step_then_shiva_tasks_finished(context):
    has_tasks = True
    for t in (0.1, 0.3, 1, 3, 10, 20):
        sleep(t)
        has_tasks = has_shiva_tasks(context)
        if not has_tasks:
            break
    assert not has_tasks, 'Expected: there are no active shiva tasks, but there are some'


@then(u'shiva responds ok')
def step_then_shiva_responds_ok(context):
    assert context.shiva_response.status_code == 200, \
        'Expected: status_code 200, but was: "{}"'.format(context.response.status_code)


@when('we make purge_deleted_user request')
def step_when_purge_deleted_user(context):
    context.shiva_response = context.shiva.client().shard().purge_deleted_user(shard_id=context.config.default_shard_id)


@when('we make purge_storage request')
def step_when_purge_storage(context):
    context.shiva_response = context.shiva.client().shard().purge_storage(shard_id=context.config.default_shard_id, max_delay=0)


@when('shiva {tasks:ShivaTasks} tasks successfully executed')
def step(context, tasks):
    for task in tasks:
        context.execute_steps(u'''
            When we make {task} request
            Then shiva responds ok
        '''.format(task=task))
    context.execute_steps(u'Then all shiva tasks finished')
