from pymdb.operations import AddToStorageDeleteQueue
from pymdb.tools import generate_st_id
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from tests_common.pytest_bdd import when, given, then

Q = load_from_my_file(__file__)


def st_id_exists(conn, st_id):
    cur = qexec(conn, Q.st_id_exists, st_id=st_id)
    conn.wait()
    return cur.fetchone()[0]


@given('new st_id that does not exist in messages and storage delete queue')
def step_new_st_id_that_does_not_exist(context):
    for _ in range(10):
        st_id = generate_st_id('msg')
        if not st_id_exists(context.conn, st_id):
            context.our_st_id = st_id
            return
    raise RuntimeError('Something strange happens - can\'t generate new st_id')


@when('we add our new st_id to storage delete queue')
def step_add_to_storage_delete_queue(context):
    AddToStorageDeleteQueue(context.conn, context.uid)(context.our_st_id).commit()


@when('we store "{mid:Mid}" into "{folder_type:w}" with our new st_id')
def step_store_our_st_id(context, mid, folder_type):
    context.execute_steps(u'''
     When we store "{mid}" into "{folder_type}"
       | st_id   |
       | {st_id} |
    '''.format(mid=mid, folder_type=folder_type, st_id=context.our_st_id))


@when('we store "{mid:Mid}" into "{folder_type:w}" with our new st_id and attributes "{attrs}"')
def step_store_our_st_id_with_attrs(context, mid, folder_type, attrs):
    context.execute_steps(u'''
     When we store "{mid}" into "{folder_type}"
       | st_id   | attributes |
       | {st_id} | {attrs}    |
    '''.format(mid=mid, folder_type=folder_type, st_id=context.our_st_id, attrs=attrs))


@then('our new st_id exist in storage delete queue')
def step_check_st_id_in_storage_delete_queue(context):
    context.execute_steps(u'''
     Then in storage delete queue there is
       | st_id   |
       | {st_id} |
    '''.format(st_id=context.our_st_id))
