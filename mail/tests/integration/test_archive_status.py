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
    "archive_status.feature",
    features_base_dir=get_path("mail/hound/tests/integration/features/archive_status.feature"),
    strict_gherkin=False
)


@given(u'message count is "{message_count:d}", restored message count is "{restored_message_count:d}", archive state is "{archive_state:w}"')
def step_completely_update_archive_state(context, archive_state, message_count, restored_message_count):
    context.coordinator.components[Mdb].execute('''
        INSERT INTO
            mail.archives (uid, state, message_count, restored_message_count)
        VALUES
            ({uid}, '{state}', {message_count}, {restored_message_count})
        ON CONFLICT (uid) DO UPDATE SET
            state = excluded.state,
            message_count = excluded.message_count,
            restored_message_count = excluded.restored_message_count
    '''.format(uid=context.params['uid'], state=archive_state,
                message_count=message_count, restored_message_count=restored_message_count))


@given(u'archive state is "{archive_state}"')
def step_update_archive_state_without_counts(context, archive_state):
    step_completely_update_archive_state(context, archive_state, 0, 0)


@then(u'archive status info contains only user state, which is "{user_state}"')
def step_check_nonarchived_user_archive_status(context, user_state):
    expected = {
        u'user_state': user_state
    }
    response = context.response.json()
    assert_that(response, equal_to(expected))


@then(u'archive status info is full: user state: "{user_state:w}", archive state: "{archive_state:w}", message count: "{message_count:d}", restored message count: "{restored_message_count:d}"')
def step_check_archived_user_archive_status(context, user_state, archive_state, message_count, restored_message_count):
    expected = {
        u'user_state': user_state,
        u'archive_state': archive_state,
        u'message_count': message_count,
        u'restored_message_count': restored_message_count
    }
    response = context.response.json()
    assert_that(response, equal_to(expected))
