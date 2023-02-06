import pytest
from pytest_bdd import types
from tests_common.pytest_bdd import (
    StepsExecuter,
    StepError,
)


@pytest.mark.parametrize('steps, expected', [
    ('Given some', [[types.GIVEN, 'some']]),
    ('When some', [[types.WHEN, 'some']]),
    ('Then some', [[types.THEN, 'some']]),
    ('''
        When some
        other
    ''', [[types.WHEN, 'some\n        other']]),
    ('''
        When some
        And other
    ''', [[types.WHEN, 'some'], [types.WHEN, 'other']]),
    ('''
        When some
          """
            qwerty
          """
    ''', [[types.WHEN, 'some\n          """\n            qwerty\n          """']]),
    ('''
        When some
          | col1 | col2 |
          | 42   | asdf |
          | 13   | qqq  |
    ''', [[types.WHEN, 'some\n          | col1 | col2 |\n          | 42   | asdf |\n          | 13   | qqq  |']]),
])
def test_split_steps(steps, expected):
    assert StepsExecuter.split_steps(steps) == expected


@pytest.mark.parametrize('steps, error', [
    ('some', 'got step without type'),
    ('And some', 'using continuation prefix for unknown step type'),
])
def test_split_steps_error(steps, error):
    with pytest.raises(StepError) as exc:
        StepsExecuter.split_steps(steps)
    assert error in str(exc.value)
