from pytest_bdd import scenarios
from .conftest import get_path

from hamcrest import (
    assert_that,
    has_length,
    equal_to,
)

from pymdb.queries import Queries
from tests_common.mdb import user_connection
from tests_common.pytest_bdd import (
    given,
    when,
    then,
)


scenarios(
    "same_received_date.feature",
    features_base_dir=get_path("mail/hound/tests/integration/features/same_received_date.feature"),
    strict_gherkin=False
)


def get_inbox_messages(context, count):
    uid = context.user.uid
    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        folder = qs.folder_by_type('inbox')
        messages = qs.messages(fid=folder.fid)
        assert_that(messages, has_length(count))
        return messages


@given(u'messages have received dates')
def step_set_received_dates(context):
    dates = {
        'old': '2020-02-02 19:20:20',
        'mid': '2020-02-02 20:20:20',
        'new': '2020-02-02 21:20:20',
    }

    mids_count = len(context.table)
    context.mids = {}
    found_mids = [m['mid'] for m in get_inbox_messages(context, mids_count)]
    found_mids.sort(reverse=True)
    for i in range(mids_count):
        row = context.table.rows[i]
        context.mids[row['mid']] = found_mids[i]
        context.maildb.execute('''
            UPDATE mail.box
               SET received_date = '{new_date}'
             WHERE uid = {uid}
               AND mid = {mid}
        '''.format(
            new_date=dates[row['received_date']],
            uid=context.user.uid,
            mid=found_mids[i]
        ))


@when(u'we request nearest messages for mid "{from_mid}"')
def step_request_nearest_messages(context, from_mid):
    context.params['mid'] = context.mids[from_mid]
    context.params['deviation'] = '1'
    context.execute_steps('When we request "nearest_messages"')


@then(u'response has "{mids_list}" mids in order')
def step_check_mids_in_response(context, mids_list):
    expected_mids = [str(context.mids[m.strip()]) for m in mids_list.split(',')]

    resp = context.response.json()
    resp_mids = [e['mid'] for e in resp['envelopes']]

    assert_that(resp_mids, equal_to(expected_mids))
