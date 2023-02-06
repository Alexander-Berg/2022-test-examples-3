from pymdb.operations import ExchangeArchiveState
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from tests_common.pytest_bdd import given, when, then

Q = load_from_my_file(__file__)


def set_archive_state(context, state):
    qexec(
        context.conn,
        Q.set_archive_state,
        uid=context.uid,
        state=state,
    )
    context.conn.commit()


def get_archive_state(context):
    cur = qexec(context.conn, Q.get_archive_state, uid=context.uid)
    return cur.fetchone()[0]


def set_archive_notice(context, notice):
    qexec(
        context.conn,
        Q.set_archive_notice,
        uid=context.uid,
        notice=notice,
    )
    context.conn.commit()


def get_archive_notice(context):
    cur = qexec(context.conn, Q.get_archive_notice, uid=context.uid)
    return cur.fetchone()[0]


@given('archive is in "{state}" state')
def step_set_archive_state(context, state):
    set_archive_state(context, state)


@then('archive is in "{state}" state')
def step_check_archive_state(context, state):
    real_state = get_archive_state(context)
    assert state == real_state


@given('archive has "{notice}" notice')
def step_set_archive_notice(context, notice):
    set_archive_notice(context, notice)


@then('archive has "{notice}" notice')
def step_check_archive_notice(context, notice):
    real = get_archive_notice(context)
    assert notice == real


@then('archive update result is "{result}"')
def step_check_archive_update_result(context, result):
    assert result == context.res[ExchangeArchiveState]


@then('archive update result is ok')
def step_check_archive_update_result_is_ok(context):
    assert context.res[ExchangeArchiveState] is None


@when('we update archive state with "{notice}" from "{from_state}" to "{to_state}" assuming user in "{user_state}" state')
def step_exchange_archive_state_full(context, from_state, to_state, user_state, notice):
    context.res[ExchangeArchiveState] = context.apply_op(
        ExchangeArchiveState,
        from_state=from_state, to_state=to_state,
        user_state=user_state, notice=notice
    ).simple_result


@when('we update archive state from "{from_state}" to "{to_state}" assuming user in "{user_state}" state')
def step_exchange_archive_state_without_notice(context, from_state, to_state, user_state):
    return step_exchange_archive_state_full(context, from_state, to_state, user_state, None)
