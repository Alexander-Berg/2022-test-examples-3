from datetime import datetime
from hamcrest import (
    assert_that,
    has_length,
)
from pymdb.queries import Queries
from pymdb.operations import DeleteMessages, DeleteUser, MoveMessages, CreateFolder
from pymdb.vegetarian import fill_messages_in_folder, SAMPLE_STIDS
from tests_common.mdb import user_connection
from tests_common.fbbdb import is_user_exists
from tests_common.pytest_bdd import given, then
from tests_common.user import make_user_oneline
from pytest_bdd import parsers
from mail.devpack.lib.components.mdb import Mdb


def apply_op(OpClass, conn, user, **op_args):
    o = OpClass(conn, user.uid)
    o(**op_args)
    o.commit()
    return o


def make_new_user(context):
    make_user_oneline(context, empty=True)
    uid = context.user.uid
    context.params['uid'] = uid
    return uid


def make_new_user_with_messages(context, msg_count):
    uid = make_new_user(context)

    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        folder = qs.folder_by_type('inbox')
        context.params['fid'] = folder.fid

        fill_messages_in_folder(conn, uid, folder, msg_count, SAMPLE_STIDS)

    return uid, folder.fid


@given(u'test user')
def step_new_user(context):
    make_new_user(context)


@given(u'test user with "{msg_count:d}" messages')
def step_new_user_with_messages(context, msg_count):
    make_new_user_with_messages(context, msg_count)


@given(u'test user with "{msg_count:d}" deleted messages')
def step_new_user_with_deleted(context, msg_count):
    uid, fid = make_new_user_with_messages(context, msg_count)

    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        all_messages = qs.messages(fid=fid)
        apply_op(DeleteMessages, conn, context.user, mids=[m['mid'] for m in all_messages])

        assert_that(qs.deleted_messages(), has_length(msg_count))


@given(u'test user with "{msg_count:d}" messages in tab "{tab_type:w}"')
def step_new_user_with_in_tab(context, msg_count, tab_type):
    make_new_user_with_messages(context, msg_count)
    uid = context.params['uid']
    fid = context.params['fid']

    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        all_messages = qs.messages(fid=fid)
        apply_op(MoveMessages, conn, context.user, mids=[m['mid'] for m in all_messages], new_fid=fid, new_tab=tab_type)


@given(u'nonexistent uid')
def step_nonexistent_uid(context):
    invalid_uid = 666
    # Hopefully this will converge
    while is_user_exists(context.fbbdb_conn, invalid_uid):
        invalid_uid *= 10
        if invalid_uid > 2 ** 63:
            invalid_uid /= 2 ** 60

    context.params['uid'] = invalid_uid


@given(u'user was deleted')
def step_delete_user(context):
    uid = context.user.uid
    with user_connection(context, uid) as conn:
        apply_op(DeleteUser, conn, context.user, deleted_date=datetime.now())

    context.fbbdb.execute('''
        DELETE FROM fbb.users WHERE uid = {uid}
    '''.format(uid=uid))

    context.sharddb.execute('''
        SELECT code.move_user_to_deleted({uid})
    '''.format(uid=uid))


def parse_is_able(is_able):
    if is_able == 'can':
        return True
    elif is_able == 'cannot':
        return False
    else:
        return None


@given(ur'user (?P<is_able>(can|cannot)) read tabs', parse_builder=parsers.re)
def step_user_can_read_tabs(context, is_able):
    uid = context.user.uid

    context.maildb.execute('''
        UPDATE mail.users
           SET can_read_tabs = {can}
         WHERE uid = {uid}
    '''.format(can=parse_is_able(is_able), uid=uid))


@given(u'user has folder "{folder_name}" with symbol "{folder_type:w}"')
def step_user_has_folder(context, folder_name, folder_type):
    uid = context.user.uid
    with user_connection(context, uid) as conn:
        apply_op(CreateFolder, conn, context.user, name=folder_name, type=folder_type)


@given(u'user has folder "{folder_name}" with symbol "{folder_type:w}" and fid "{fid}"')
def step_user_has_folder_with_fid(context, folder_name, folder_type, fid):
    uid = context.user.uid
    with user_connection(context, uid) as conn:
        create_op = apply_op(CreateFolder, conn, context.user, name=folder_name, type=folder_type)
        context.folders[fid] = create_op.fid


@given(u'there is new symbol "{folder_type:w}"')
def step_create_folder_type(context, folder_type):
    context.maildb.execute('''
        ALTER TYPE mail.folder_types
            ADD VALUE '{folder_type}';
    '''.format(folder_type=folder_type))


@given(u'in "{folder_type:w}" there are "{msg_count:d}" messages')
def folder_has_messages(context, folder_type, msg_count):
    uid = context.user.uid
    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        folder = qs.folder_by_type(folder_type)
        fill_messages_in_folder(conn, uid, folder, msg_count, SAMPLE_STIDS)


@given(u'user state is "{state}"')
def step_update_user_state(context, state):
    context.coordinator.components[Mdb].execute('''
        UPDATE mail.users
        SET state = '{state}'
        WHERE uid = {uid}
    '''.format(uid=context.params['uid'], state=state))


@then(u'user state is "{state}"')
def step_check_user_state(context, state):
    rows = context.coordinator.components[Mdb].query(
        '''
            SELECT state
              FROM mail.users
             WHERE uid = %(uid)s
        ''',
        uid=context.params['uid'],
    )
    assert rows[0][0] == state
