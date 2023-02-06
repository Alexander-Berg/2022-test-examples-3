from tests_common.pytest_bdd import then


@then('We fail')
def step_we_fail(context):
    raise AssertionError(
        'You ask - we fail'
    )
