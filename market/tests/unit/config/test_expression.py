import pytest

from yamarec1.config import ConfigExpression
from yamarec1.config.exceptions import ConfigEvaluationError


def test_expression_provides_access_to_its_code():
    assert ConfigExpression("1 + x * y").code == "1 + x * y"


def test_expression_computes_itself_correctly():
    assert ConfigExpression("1 + x * y").compute("", "test", {"x": 2, "y": 3}) == 7


def test_expression_handles_errors_correctly():
    with pytest.raises(ConfigEvaluationError):
        ConfigExpression("1 + x?").compute("", "test", {"x": 2, "y": 3})


def test_expression_allows_to_use_builtin_functions():
    assert ConfigExpression("str(7) == '7'").compute("", "test", {}) is True
