import pytest

from crypta.lib.python.purgatory.validation import (
    make_type_validator,

    validate_dict,
    validate_float,
    validate_float_range,
    validate_lat,
    validate_list,
    validate_lon,
    validate_or,
    validate_regex,
    validate_uint,
    validate_uint_range,
    validate_unixtime
)


@pytest.mark.parametrize("value,regex,valid", [
    ("good_regex", "\\w+", True),
    ("bad_regex", "bad\\dregex", False)
])
def test_validate_regex(value, regex, valid):
    assert validate_regex(value, regex) == valid


@pytest.mark.parametrize("value,valid", [
    (123123, True),
    (0, True),

    (-20, False),
    (30.5, False),
    ("123", False),
    ("seven", False),
    ("", False)
])
def test_validate_uint(value, valid):
    assert validate_uint(value) == valid


@pytest.mark.parametrize("value,min_,max_,valid", [
    (100, 23, 1245, True),
    (0, 0, 1, True),
    (10, 10, 10, True),
    (10, 0, 10, True),

    (229, 230, 1245, False),
    (1246, 230, 1245, False),
    (-5, -10, 10, False),
    ("ten", 0, 100, False)
])
def test_validate_uint_range(value, min_, max_, valid):
    assert validate_uint_range(value, min_, max_) == valid


@pytest.mark.parametrize("value,valid", [
    (1234567890, True),

    ("", False),
    ("unixtime", False),
    (123456789, False),
    (12345678909, False),
    (-1234567890, False)

])
def test_validate_unixtime(value, valid):
    assert validate_unixtime(value) == valid


@pytest.mark.parametrize("value,valid", [
    (123, True),
    (123.123, True),
    (-123, True),
    (-123.123, True),

    ("not float", False),
    ("", False)
])
def test_validate_float(value, valid):
    assert validate_float(value) == valid


@pytest.mark.parametrize("value,min_,max_,valid", [
    (123, -300, 300, True),
    (12.57, 12.57, 14.214, True),
    (14.214, 12.57, 14.214, True),
    (-213.123, -300.1253, -200.45, True),

    (-500, -300, 300, False),
    (100.214, 12.57, 14.214, False),
    ("thirteen", 12.57, 14.214, False)
])
def test_validate_float_range(value, min_, max_, valid):
    assert validate_float_range(value, min_, max_) == valid


@pytest.mark.parametrize("value,valid", [
    (90., True),
    (0., True),
    (75.213, True),
    (-90, True),

    (100.4, False),
    (-2213, False),
    ("0.", False)
])
def test_validate_lat(value, valid):
    assert validate_lat(value) == valid


@pytest.mark.parametrize("value,valid", [
    (180., True),
    (0., True),
    (125.213, True),
    (-180, True),

    (180.4, False),
    (-2213, False),
    ("0.", False)
])
def test_validate_lon(value, valid):
    assert validate_lon(value) == valid


@pytest.mark.parametrize("value,valid", [
    ({"key": "value"}, True),

    ("not a dict", False),
    (["also not a dict"], False),
    (0, False)
])
def test_validate_dict(value, valid):
    assert validate_dict(value) == valid


@pytest.mark.parametrize("value,allow_empty,validate,valid", [
    ([1, 2, 3], True, lambda x: True, True),
    ([], True, lambda x: True, True),
    (["strings", "are", "everywhere"], True, lambda x: isinstance(x, str), True),

    ("list", True, lambda x: True, False),
    (None, True, lambda x: True, False),
    (2, True, lambda x: True, False),
    ([], False, lambda x: True, False),
    (["list", "of", "strings", 0], True, lambda x: isinstance(x, str), False)
])
def test_validate_list(value, allow_empty, validate, valid):
    assert validate_list(value, validate=validate, allow_empty=allow_empty) == valid


@pytest.mark.parametrize("value,valid", [
    ("string", True),
    (1, True),
    (1.5, True),

    ({}, False)
])
def test_validate_or(value, valid):
    assert validate_or(value, validators=[lambda x: isinstance(x, str), validate_uint, validate_float]) == valid


@pytest.mark.parametrize("value,type_,valid", [
    ("string", str, True),
    (1, int, True),
    (1.2, float, True),
    ([], list, True),

    (1, str, False),
    ("10", int, False),
    (1, float, False),
    (1.2, int, False),
    ({}, list, False)
])
def test_make_type_validator(value, type_, valid):
    assert make_type_validator(type_)(value) == valid
