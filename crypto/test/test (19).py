import argparse

import pytest

from crypta.lib.python import argparse_utils


@pytest.mark.parametrize("value,reference", [
    ("true", True),
    ("false", False)
])
def test_valid_boolean_as_string(value, reference):
    assert reference == argparse_utils.boolean_as_string(value)


@pytest.mark.parametrize("value", [
    "",
    "XXX",
    "True",
    "False"
])
def test_invalid_boolean_as_string(value):
    with pytest.raises(argparse.ArgumentTypeError) as excinfo:
        argparse_utils.boolean_as_string(value)
    assert str(excinfo.value) == "'{}' is not 'true' or 'false'".format(value)
