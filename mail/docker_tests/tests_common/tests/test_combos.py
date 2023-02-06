from pytest_bdd import scenarios
from .conftest import get_path
from tests_common.pytest_bdd import given, when, then


def test_outlined():
    pass


scenarios(
    "combos.feature",
    features_base_dir=get_path('mail/docker_tests/tests_common/tests/features/combos.feature')
)


@given(u'var1 is qwerty')
def given_var1_qwerty(context):
    context.var1 = 'qwerty'


@given(u'var2 is asdf')
def given_var2_asdf(context):
    context.var2 = 'asdf'


@given(u'var1 is qwerty and var2 is asdf')
def given_var1_qwerty_var2_asdf(context):
    context.execute_steps(u'''
        Given var1 is qwerty
        And var2 is asdf
    ''')


@given(u'var1 is param "{value}"')
def given_var1_param(context, value):
    context.var1 = value


@given(u'var2 is param "{value}"')
def given_var2_param(context, value):
    context.var2 = value


@given(u'var1 is "{value1}" and var2 is "{value2}"')
def given_var1_param_var2_param(context, value1, value2):
    context.execute_steps(u'''
        Given var1 is param "{v1}"
        And var2 is param "{v2}"
    '''.format(v1=value1, v2=value2))


@given(u'var1 from text')
def given_var1_text(context):
    context.var1 = context.text


@given(u'var2 from text')
def given_var2_text(context):
    context.var2 = context.text


@given(u'var1 is qwerty and var2 is asdf from text')
def given_var1_text_var2_text(context):
    context.execute_steps(u'''
        Given var1 from text
          """
qwerty
          """
        And var2 from text
          """
asdf
          """
    ''')


@when(u'we set var1 to qwerty')
def when_var1_qwerty(context):
    context.var1 = 'qwerty'


@when(u'we set var2 to asdf')
def when_var2_asdf(context):
    context.var2 = 'asdf'


@when(u'we set var1 to qwerty and var2 to asdf')
def when_var1_qwerty_var2_asdf(context):
    context.execute_steps(u'''
        When we set var1 to qwerty
        And we set var2 to asdf
    ''')


@when(u'we set var1 to param "{value}"')
def when_var1_param(context, value):
    context.var1 = value


@when(u'we set var2 to param "{value}"')
def when_var2_param(context, value):
    context.var2 = value


@when(u'we set var1 to "{value1}" and var2 to "{value2}"')
def when_var1_param_var2_param(context, value1, value2):
    context.execute_steps(u'''
        When we set var1 to param "{v1}"
        And we set var2 to param "{v2}"
    '''.format(v1=value1, v2=value2))


@when(u'we set var1 from text')
def when_var1_text(context):
    context.var1 = context.text


@when(u'we set var2 from text')
def when_var2_text(context):
    context.var2 = context.text


@when(u'we set var1 to qwerty and var2 to asdf from text')
def when_var1_text_var2_text(context):
    context.execute_steps(u'''
        When we set var1 from text
          """
qwerty
          """
        And we set var2 from text
          """
asdf
          """
    ''')


@then(u'var1 is qwerty')
def then_var1_qwerty(context):
    assert context.var1 == 'qwerty'


@then(u'var2 is asdf')
def then_var2_asdf(context):
    assert context.var2 == 'asdf'


@then(u'var1 is qwerty and var2 is asdf')
def then_var1_qwerty_var2_asdf(context):
    context.execute_steps(u'''
        Then var1 is qwerty
        And var2 is asdf
    ''')


@then(u'var1 is param "{value}"')
def then_var1_param(context, value):
    assert context.var1 == value


@then(u'var2 is param "{value}"')
def then_var2_param(context, value):
    assert context.var2 == value


@then(u'var1 is "{value1}" and var2 is "{value2}"')
def then_var1_param_var2_param(context, value1, value2):
    context.execute_steps(u'''
        Then var1 is param "{v1}"
        And var2 is param "{v2}"
    '''.format(v1=value1, v2=value2))


@then(u'var1 from text')
def then_var1_text(context):
    assert context.var1 == context.text


@then(u'var2 from text')
def then_var2_text(context):
    assert context.var2 == context.text


@then(u'var1 is qwerty and var2 is asdf from text')
def then_var1_text_var2_text(context):
    context.execute_steps(u'''
        Then var1 from text
          """
qwerty
          """
        And var2 from text
          """
asdf
          """
    ''')
