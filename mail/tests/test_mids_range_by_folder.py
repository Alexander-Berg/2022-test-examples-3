from pytest_bdd import scenarios
from tests_common.pytest_bdd import given, when, then
from pymdb.queries import Queries
from tests_common.mdb import user_connection
from .conftest import get_path, get_inbox_messages, make_new_user_with_messages, wait_for_completion_all_async_tasks


scenarios(
    "mids_range_by_folder.feature",
    features_base_dir=get_path("mail/mops/tests/features/mids_range_by_folder.feature"),
    strict_gherkin=False
)


@given(u'test user with "{msg_count:d}" messages')
def step_new_user_with_messages(context, msg_count):
    make_new_user_with_messages(context, msg_count)


@when(u'we request purge for fid "{fid}"')
def step_request_purge(context, fid):
    mops_api = context.mops.api(uid=context.user.uid)
    context.response = mops_api.purge(fid=fid, **context.params)


@when(u'we create label with name "{label_name}"')
def step_create_label(context, label_name):
    mops_api = context.mops.api(uid=context.user.uid)
    context.response = mops_api.create_label(name=label_name, **context.params)
    context.lid = context.response.json()["lid"]


@when(u'we request label fid "{fid}" by lid from context')  # noqa: F811
def step_request_label_fid(context, fid):
    mops_api = context.mops.api(uid=context.user.uid)
    context.response = mops_api.label(lids=context.lid, fid=fid, **context.params)


@then(u'response is OK')
def step_check_ok(context):
    assert context.response.status_code == 200


@then(u'wait {sec:d} seconds for completion all async tasks')
def step_wait_for_completion_all_async_tasks(context, sec):
    wait_for_completion_all_async_tasks(context, sec=sec)


@then(u'folder with fid "{fid}" is empty')
def step_folder_is_empty(context, fid):
    uid = context.user.uid
    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        messages = qs.messages(fid=fid)
        assert len(messages) == 0


@then(u'all messages in folder with fid "{fid}" has label')
def step_all_messages_in_folder_has_label(context, fid):
    uid = context.user.uid
    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        messages = qs.messages(fid=fid)
        lids = "{{{0}}}".format(context.lid)
        messages = qs.mids_with_lids(lids=lids, limit=len(messages))
        assert len(messages) == 11


@given(u'messages have received dates')
def step_set_received_dates(context):
    mids_count = len(context.table)
    found_mids = [m['mid'] for m in get_inbox_messages(context, mids_count)]
    for i in range(mids_count):
        row = context.table.rows[i]
        context.maildb.execute('''
            UPDATE mail.box
               SET received_date = '{new_date}'
             WHERE uid = {uid}
               AND mid = {mid}
        '''.format(
            new_date=row['received_date'],
            uid=context.user.uid,
            mid=found_mids[i]
        ))
