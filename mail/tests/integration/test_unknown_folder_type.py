from pytest_bdd import scenarios
from .conftest import get_path
from .test_hidden_trash import step_check_folder_in_response, step_check_no_folder_in_response
from tests_common.pytest_bdd import then

scenarios(
    "unknown_folder_type.feature",
    features_base_dir=get_path("mail/hound/tests/integration/features/unknown_folder_type.feature"),
    strict_gherkin=False
)


@then(u'there is folder with empty type in response')
def step_check_folder_empty_type_in_response(context):
    step_check_folder_in_response(context, '')


@then('there is no folder with empty type in response')
def step_check_no_folder_empty_type_in_response(context):
    step_check_no_folder_in_response(context, '')
