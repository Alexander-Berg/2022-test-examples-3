from pytest_bdd import scenarios
from .conftest import get_path

from .steps.request import fill_params
from tests_common.pytest_bdd import given, when


scenarios(
    "archive_change.feature",
    features_base_dir=get_path("mail/hound/tests/integration/features/archive_change.feature"),
    strict_gherkin=False
)


@given(u'endpoint is "{endpoint}"')
def step_set_archive_change_ep(context, endpoint):
    context.archive_change_ep = endpoint


@given(u'"{unused_parameter}" is unused')
def step_example_unused(context, unused_parameter):
    pass


@when(u'we request with "action={action}"')
def step_request_hound_with_args(context, action):
    context.params['action'] = action
    fill_params(context)
    context.response = context.hound.request_get(context.archive_change_ep, **context.params)


@when(u'we request without args')
def step_request_hound_without_args(context):
    context.response = context.hound.request_get(context.archive_change_ep)
