import json as jsn
from time import sleep

from tests_common.pytest_bdd import given, when, then
from mail.pypg.pypg.common import transaction, qexec
from mail.pypg.pypg.query_conf import load_from_my_file

from .mdb_actions import step_some_partitions, step_drop_partitions, step_check_partitions

QUERIES = load_from_my_file(__file__)


def get_dsn(context, db_user=None):
    dsn = context.config['huskydb']
    if db_user:
        dsn = dsn + ' user=' + db_user
    return dsn


def has_shiva_tasks(context):
    with transaction(get_dsn(context)) as huskydb_conn:
        cur = qexec(
            huskydb_conn,
            QUERIES.get_shiva_tasks
        )
        return cur.fetchone() is not None


@then(u'all shiva tasks finished')
def step_then_shiva_tasks_finished(context):
    has_tasks = True
    for t in (0.1, 0.3, 1, 3, 5, 5, 5, 5, 5, 5):
        sleep(t)
        has_tasks = has_shiva_tasks(context)
        if not has_tasks:
            break
    assert not has_tasks, 'Expected: there are no active shiva tasks, but there are some'


def has_transfer_tasks(context):
    with transaction(get_dsn(context)) as huskydb_conn:
        cur = qexec(
            huskydb_conn,
            QUERIES.get_shiva_tasks
        )
        return cur.fetchone() is not None


@then(u'there are {has_some:NoOrSome} transfer tasks for "{user_name:w}"')
def step_then_has_transfer_tasks(context, has_some, user_name):
    with transaction(get_dsn(context)) as huskydb_conn:
        cur = qexec(
            huskydb_conn,
            QUERIES.get_transfer_tasks,
            uid=context.get_user(user_name).uid
        )
        has_tasks = cur.fetchone() is not None
        if has_some:
            assert has_tasks, (
                'Expect that there are some active transfer tasks for user, but nothing found'
            )
        else:
            assert not has_tasks, (
                'Expect that there are no active transfer tasks for user, but there are some'
            )


@then(u'there are some transfer tasks for "{user_name:w}" with task args containing {task_args}')
def step_then_has_transfer_tasks_(context, user_name, task_args):
    with transaction(get_dsn(context)) as huskydb_conn:
        cur = qexec(
            huskydb_conn,
            QUERIES.get_transfer_tasks,
            uid=context.get_user(user_name).uid
        )
        res = cur.fetchone()
        assert res is not None and set(jsn.loads(task_args).items()).issubset(set(res[0].items())), (
            'Expect that there are some active transfer tasks for user with given task_args, but nothing found'
        )


def clear_shiva_shard_tasks(context):
    with transaction(get_dsn(context)) as huskydb_conn:
        qexec(
            huskydb_conn,
            QUERIES.clear_shiva_shard_tasks,
        )


@given('there are no shiva tasks')
def step_clear_shiva_tasks(context):
    clear_shiva_shard_tasks(context)


def clear_shiva_shards(context):
    with transaction(get_dsn(context)) as huskydb_conn:
        qexec(
            huskydb_conn,
            QUERIES.clear_shiva_shards,
        )


def add_shiva_shard(
        context,
        shard_id,
        cluster_id,
        disk_size,
        used_size=0,
        shard_type='general',
        load_type='general',
        can_transfer_to=False,
        shard_name='shard',
        migration=0,
        priority=0):
    with transaction(get_dsn(context)) as huskydb_conn:
        qexec(
            huskydb_conn,
            QUERIES.add_shiva_shard,
            shard_id=shard_id,
            shard_name=shard_name,
            cluster_id=cluster_id,
            disk_size=disk_size,
            used_size=used_size,
            shard_type=shard_type,
            load_type=load_type,
            can_transfer_to=can_transfer_to,
            migration=migration,
            priority=priority,
        )


@given('all shards have load_type "{load_type}" and are open for registration')
def step_set_load_typefor_all_shards(context, load_type):
    add_shiva_shard(
        context=context,
        shard_id=context.config['shard_id'],
        cluster_id='cluster_id1',
        disk_size=100000000000,
        load_type=load_type,
        can_transfer_to=True,
    )
    add_shiva_shard(
        context=context,
        shard_id=context.config['shard_id2'],
        cluster_id='cluster_id2',
        disk_size=100000000000,
        load_type=load_type,
        can_transfer_to=True,
    )


@when('we make new shiva shards')
def step_make_shiva_shards(context):
    context.shiva_shards = jsn.loads(context.text)
    for shard in context.shiva_shards.values():
        add_shiva_shard(context, **shard)


@given('current shard is shiva shard with disk size "{disk_size:d}"')
def step_set_current_shiva_shard_with_disk_size(context, disk_size):
    add_shiva_shard(
        context=context,
        shard_id=context.config['shard_id'],
        cluster_id='cluster_id',
        disk_size=disk_size,
        can_transfer_to=True,
    )


@given('there are {have_some:NoOrSome} open for transfer shiva shards with "{load_type}" load_type')
def step_make_open_shard(context, have_some, load_type):
    if have_some:
        add_shiva_shard(
            context=context,
            shard_id=666,
            cluster_id='cluster_id_666',
            disk_size=11111111111,
            can_transfer_to=True,
            load_type=load_type,
        )


def read_shards(rows):
    keys = 'shard_id', 'cluster_id', 'disk_size', 'used_size', 'shard_type', 'load_type', 'can_transfer_to', 'shard_name', 'migration', 'priority'
    return {str(row[0]): dict(zip(keys, row)) for row in rows}


def get_shiva_shard(context, shard_id):
    with transaction(get_dsn(context)) as huskydb_conn:
        cur = qexec(
            huskydb_conn,
            QUERIES.get_shiva_shard,
            shard_id=shard_id,
        )
        return read_shards(cur.fetchall())


@then('current shard has updated used_size')
def step_shard_has_updated_used_size(context):
    shard_id = str(context.config['shard_id'])
    shiva_shards = get_shiva_shard(context, shard_id)
    assert shiva_shards[shard_id]['used_size'] > 0, 'Expected: shiva shard has used_size > 0'


@then('transfer for current shard closed')
def step_shard_transfer_closed(context):
    shard_id = str(context.config['shard_id'])
    shiva_shards = get_shiva_shard(context, shard_id)
    assert not shiva_shards[shard_id]['can_transfer_to'], \
        'Expected: transfer for shard is close, but it was opened'


@then('transfer for current shard opened')
def step_shard_transfer_opened(context):
    shard_id = str(context.config['shard_id'])
    shiva_shards = get_shiva_shard(context, shard_id)
    assert shiva_shards[shard_id]['can_transfer_to'], \
        'Expected: transfer for shard is open, but it was closed'


@then('there is "{shard_id:d}" shard in DB')
def step_get_shiva_shard(context, shard_id):
    context.shiva_shards = get_shiva_shard(context, shard_id)
    assert context.shiva_shards, \
        'Missing shard with shard_id="{}" in DB'.format(shard_id)


def get_all_shiva_shards(context):
    with transaction(get_dsn(context)) as huskydb_conn:
        cur = qexec(
            huskydb_conn,
            QUERIES.get_all_shiva_shards,
        )
        return read_shards(cur.fetchall())


@then('there are no shards in DB')
def step_get_all_shiva_shards(context):
    context.shiva_shards = get_all_shiva_shards(context)
    assert not context.shiva_shards, \
        'Expected: there are no shards in DB, but was: "{}"'.format(context.shiva_shards)


@given('there are some partitions for "{table}" in huskydb')
def step_some_partitions_in_huskydb(context, table):
    with transaction(get_dsn(context)) as huskydb_conn:
        step_some_partitions(context, huskydb_conn, table)


@when('we drop "{count:d}" partitions for "{table}" in huskydb')
def step_drop_partitions_in_huskydb(context, table, count):
    with transaction(get_dsn(context)) as huskydb_conn:
        step_drop_partitions(huskydb_conn, table, count)


@then('there are the same partitions for "{table}" in huskydb')
def step_check_partitions_in_huskydb(context, table):
    with transaction(get_dsn(context)) as huskydb_conn:
        step_check_partitions(context, huskydb_conn, table)
