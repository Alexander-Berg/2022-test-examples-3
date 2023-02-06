import logging

from tests_common.pytest_bdd import given, then
from tests_common.mdb import user_connection, Queries
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from pymdb import operations as OPS

from .instant_delete_user import mdb_connection

Q = load_from_my_file(__file__)
log = logging.getLogger(__name__)


@given('user has label "{label_name:w}"')
def step_create_label(context, label_name):
    with mdb_connection(context) as conn:
        op = OPS.CreateLabel(conn, context.user.uid)(
            name=label_name,
            type='user',
            color='green'
        ).commit()
        context.lids[label_name] = op.result[0].lid


@given('his messages have label "{label_name:w}"')
def step_set_label(context, label_name):
    with mdb_connection(context) as conn:
        OPS.UpdateMessages(conn, context.user.uid)(
            mids=context.mids,
            seen=None,
            recent=None,
            deleted=None,
            lids_add=[context.lids[label_name]],
        ).commit()


@given('user has folder "{folder_name:w}" with symbol "{symbol:w}"')
def step_create_folder(context, folder_name, symbol):
    with mdb_connection(context) as conn:
        OPS.CreateFolder(conn, context.user.uid)(
            name=folder_name,
            type=symbol,
        ).commit()


@then('"{user_name:w}" has "{message_count:d}" mails in folder "{folder_name:w}"')
def step_user_has_messages(context, user_name, message_count, folder_name):
    uid = context.users.get(user_name).uid
    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        folder = qs.folder_by_name(folder_name)
        messages = qs.messages(fid=folder.fid)
        assert len(messages) == message_count


@given('"{user_name:w}" is in "{state_name}" archivation state')
def step_given_user_is_in_archivation_state(context, user_name, state_name):
    uid = context.users.get(user_name).uid
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.set_user_archivation_state,
            uid=uid,
            state=state_name,
        )


@given('"{user_name:w}" is in "{state_name}" state')
def step_given_user_is_in_state(context, user_name, state_name):
    uid = context.users.get(user_name).uid
    with mdb_connection(context) as conn:
        qexec(
            conn,
            Q.set_user_state,
            uid=uid,
            state=state_name,
        )


@then('"{user_name:w}" is in "{state_name}" state')
def step_then_user_is_in_state(context, user_name, state_name):
    uid = context.users.get(user_name).uid
    with mdb_connection(context) as conn:
        cur = qexec(
            conn,
            Q.get_user_state,
            uid=uid)
        user_state = cur.fetchone()[0]

        assert user_state == state_name
