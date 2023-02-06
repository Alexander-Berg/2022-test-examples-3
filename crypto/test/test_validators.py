import re

import pytest

from crypta.lib.python.purgatory.validation import (
    PathValidator,
    Validator,
    ValidationError
)


@pytest.mark.parametrize("row,path,optional,valid,error_dict", [
    ({"field": True}, "field", False, True, None),
    ({"field": False}, "field", False, False,
     {"path": ["field"], "validator_name": "<lambda>"}),
    ({"field": False}, "optional", True, True, None),
    ({"path": {"value": True}}, ["path", "value"], False, True, None),
    ({"not path": {"value": True}}, ["path", "value"], False, False,
     {"path": ["path", "value"], "validator_name": "'path' is not present or is not a dict"})
])
def test_path_validator(row, path, optional, valid, error_dict):
    validator = PathValidator(path, lambda x: x, optional)

    test_valid, test_errors = validator(row)
    assert test_valid == valid

    if error_dict:
        assert len(test_errors) == 1
        assert test_errors[0].path == error_dict["path"]
        assert test_errors[0].validator_name == error_dict["validator_name"]


@pytest.mark.parametrize("row,valid,exception_match", [
    ({"required": 1, "optional": "3"}, True, None),
    ({"required": 1}, True, None),
    ({"required": "1", "optional": "3"}, False, "Validator: <<lambda>>"),
    ({"optional": "3"}, False, "'required' is not present")
])
def test_validator(row, valid, exception_match):
    validator = Validator()
    validator.add("required", lambda x: isinstance(x, int))
    validator.add_optional("optional")

    assert validator.validate(row) == valid

    if exception_match:
        with pytest.raises(ValidationError) as excinfo:
            validator.raise_if_invalid(row)
        excinfo.match(re.escape(exception_match))
