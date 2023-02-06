from datetime import datetime
import time

from pytest_bdd import scenarios
from .conftest import get_path

from mail.devpack.lib.components.mdb import Mdb

from hamcrest import (
    assert_that,
    equal_to,
)

from tests_common.pytest_bdd import (
    given,
    then,
)


scenarios(
    "userinfo.feature",
    features_base_dir=get_path("mail/hound/tests/integration/features/userinfo.feature"),
    strict_gherkin=False
)


def epoch_time(date):
    local_dt = datetime.fromtimestamp(int(time.mktime(time.strptime(date, "%Y-%m-%d"))))
    epoch = datetime(1970, 1, 1, 0, 0, 0)
    offset = 3 * 60 * 60
    return int((local_dt - epoch).total_seconds() - offset)


@given(u'user state is <state>, notifications count is <ncount>, last updated time is <lutime>')
def step_update_user_freezing_info(context, state, ncount, lutime):
    context.coordinator.components[Mdb].execute('''
        UPDATE mail.users
        SET state = '{state}',
            notifies_count = {ncount},
            last_state_update = '{lutime}'::timestamptz
        WHERE uid = {uid}
    '''.format(uid=context.params['uid'], state=state, ncount=ncount, lutime=lutime))


@then(u'state is <state>, notifications count is <ncount>, last update time is <lutime>')
def step_check_user_freezing_info(context, state, ncount, lutime):
    expected = {
        'state': state,
        'lastStateUpdateEpoch': epoch_time(lutime),
        'notifiesCount': int(ncount),
    }
    response = context.response.json()
    assert_that(response, equal_to(expected))
