from pymdb.operations import UpdateUserState
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from tests_common.pytest_bdd import given, when, then

Q = load_from_my_file(__file__)


def set_user_state(context, new_state):
    qexec(
        context.conn,
        Q.set_user_state,
        uid=context.uid,
        new_state=new_state,
    )
    context.conn.commit()


@given('user has "{state_name}" state')
def step_set_user_state(context, state_name):
    set_user_state(context, state_name)


def get_user_state(context):
    cur = qexec(context.conn, Q.get_user_state, uid=context.uid)
    return cur.fetchone()[0]


@then('user has "{state_name}" state')
@then('user successfully changes state to "{state_name}"')
def step_check_user_state(context, state_name):
    real_state = get_user_state(context)
    assert state_name == real_state


@when('we update user state to "{new_state}"')
def step_update_user_state(context, new_state):
    context.apply_op(
        UpdateUserState,
        new_state=new_state,
    )


@when('we try update user state to "{new_state}" as "{op_id:OpID}"')
def step_try_update_user_state(context, new_state, op_id):
    context.operations[op_id] = context.make_async_operation(UpdateUserState)(
        new_state=new_state,
    )
