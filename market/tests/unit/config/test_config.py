import pytest

from yamarec1.config import Config
from yamarec1.config import ConfigExpression


def test_config_computes_values_within_correct_scope():
    x = ConfigExpression("a")
    y = ConfigExpression("x + 1")
    config = Config("test", {"x": x, "y": y}, {"a": 1})
    assert config.x == 1
    assert config.y == 2
    with pytest.raises(AttributeError):
        config.z
