from pytest_bdd import scenarios
from .conftest import get_path
from tests_common.pytest_bdd import given, when, then


def test_outlined():
    pass


scenarios(
    "common.feature",
    features_base_dir=get_path('mail/docker_tests/tests_common/tests/features/common.feature')
)


@given(u'var is qwerty')
def given_qwerty(context):
    context.var = 'qwerty'


@then(u'var is qwerty')
def then_qwerty(context):
    assert context.var == 'qwerty'


@then(u'stripped var is qwerty')
def then_stripped_qwerty(context):
    assert context.var.strip() == 'qwerty'


@when(u'we string set var to qwerty')
def when_string(context):
    context.var = 'qwerty'


@when(u'we parametrized set var to "{var}"')
def when_param(context, var):
    context.var = var


@when(u'we outlined set var to {param}')
def when_outline(context, param):
    context.var = param


@when(u'we set var from text')
def when_text(context):
    assert context.text
    context.var = context.text


@when(u'we set var from table')
def when_table(context):
    assert context.table
    assert len(context.table) == 1
    context.var = context.table[0]['var']


@when(u'we set var from outlined table')
def when_outline_table(context):
    assert context.table
    assert len(context.table) == 1
    context.var = context.table[0]['var']


@then(u'context text is not set in this step')
@then(u'context text is not set')
def then_context_text_is_not_set(context):
    assert context.text is None
