from datetime import datetime

from tests_common.pytest_bdd import when, then
import mail.pypg.pypg.common as pg
from mail.pypg.pypg.query_conf import load_from_my_file
from mail.husky.husky.types import Task
from .common import plan_task
from .instant_delete_user import mdb_connection

TARGET_LEN = 2
Q = load_from_my_file(__file__)


def get_border_date(context):
    with mdb_connection(context) as conn:
        cur = pg.qexec(conn, Q.get_all_messages, uid=context.user.uid)
        res = sorted([i[1] for i in cur])
        return res[len(res) // 2]


def we_cut_firstlines_impl(context, all_msgs):
    max_date = datetime.max.isoformat() if all_msgs else get_border_date(context).isoformat()
    context.transfer_id = plan_task(
        context,
        Task.CutFirstlines,
        {
            'target_length': TARGET_LEN,
            'max_received_date': max_date
        }
    )
    context.task = Task.CutFirstlines


@when('we cut firstlines for old messages')
def step_we_cut_firstlines_for_old(context):
    return we_cut_firstlines_impl(context, all_msgs=False)


def line_is_cut(line):
    return len(line.split()) == 1


@when('we cut firstlines for all messages')
def step_we_cut_firstlines_for_all(context):
    return we_cut_firstlines_impl(context, all_msgs=True)


def get_firstlines(context, msg_type, shard_id):
    with mdb_connection(context, shard_id) as conn:
        query = Q.get_old_messages if msg_type == 'old' else Q.get_new_messages
        res = [i for i in
                pg.qexec(conn, query, border_date=get_border_date(context), uid=context.user.uid)]
        return [i[0] for i in res]


def check_messages_firstlines(context, msg_type, should_be_cut, shard_id=None):
    lines = get_firstlines(context, msg_type, shard_id)
    assert len(lines) > 0
    for firstline in lines:
        assert line_is_cut(firstline) == should_be_cut, firstline


@then('old messages are cut')
def step_old_messages_are_cut(context):
    return check_messages_firstlines(context, msg_type="old", should_be_cut=True)


@then('new messages are cut')
def step_new_messages_are_cut(context):
    return check_messages_firstlines(context, msg_type="new", should_be_cut=True)


@then('new messages are unchanged')
def step_new_messages_are_not_cut(context):
    return check_messages_firstlines(context, msg_type="new", should_be_cut=False)


@then('old messages in the new shard are cut')
def step_old_messages_in_new_shard_are_cut(context):
    return check_messages_firstlines(context, msg_type="old", should_be_cut=True, shard_id=2)


@then('new messages in the new shard are unchanged')
def step_new_messages_in_new_shard_are_not_cut(context):
    return check_messages_firstlines(context, msg_type="new", should_be_cut=False, shard_id=2)


@then('new messages in the new shard are cut')
def step_new_messages_in_new_shard_are_cut(context):
    return check_messages_firstlines(context, msg_type="new", should_be_cut=True, shard_id=2)
