import pytest

from crypta.lib.python import casts


@pytest.mark.parametrize("boolean_val, res", [
    (True, "true"),
    (False, "false")
])
def test_to_string_bool(boolean_val, res):
    assert casts.to_string_bool(boolean_val) == res


def test_to_string_bool_not_boolean():
    with pytest.raises(Exception):
        casts.to_string_bool("string")
