"""
    Tests for decorator functions
"""

import pytest

from search.mon.wabbajack.libs.utils import decorators


@decorators.validate_types
def function_for_test_validate_types(a: str, b: int, c: dict) -> bool:
    """
        Function for test validate types decorator
    :param a: str
    :param b: int
    :param c: dict
    """
    if not isinstance(a, str):
        return False
    if not isinstance(b, int):
        return False
    if not isinstance(c, dict):
        return False

    return True


class TestValidateTypes:

    def test_validate_types(self):
        assert function_for_test_validate_types(a='abc', b=123, c={})

    def test_validate_types_error(self):
        with pytest.raises(TypeError):
            function_for_test_validate_types(a=1234, b='abc', c={})
