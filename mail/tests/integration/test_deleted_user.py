from pytest_bdd import scenarios
from .conftest import get_path

from hamcrest import (
    assert_that,
    has_entry,
    has_length,
)
from tests_common.pytest_bdd import then


scenarios(
    "deleted_user.feature",
    features_base_dir=get_path("mail/hound/tests/integration/features/deleted_user.feature"),
    strict_gherkin=False
)


@then(u'there are "{count:d}" deleted messages in response')
def step_check_response_length(context, count):
    assert_that(context.response.json(), has_entry(
        'deleted_messages', has_length(count)))
