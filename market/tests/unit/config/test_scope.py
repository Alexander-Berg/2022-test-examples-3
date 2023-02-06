import pytest

from yamarec1.config.scope import ConfigScope


def test_scope_checks_name_membership_correctly():
    scope = ConfigScope([{"a": 1}, {"a": 11, "b": 2}, {"b": 22, "c": 3}])
    assert "a" in scope
    assert "b" in scope
    assert "c" in scope
    assert "d" not in scope


def test_scope_traverses_dictionaries_in_correct_order():
    scope = ConfigScope([{"a": 1}, {"a": 11, "b": 2}, {"b": 22, "c": 3}])
    assert scope["a"] == 1
    assert scope["b"] == 2
    assert scope["c"] == 3
    with pytest.raises(KeyError):
        scope["d"]


def test_scope_iterates_over_keys_in_correct_order():
    scope = ConfigScope([{}, {"a": 1, "b": 2}, {"a": 11, "c": 3}, {"d": 4}])
    keys = list(scope)
    assert set(keys[:2]) == {"a", "b"}
    assert keys[2] == "c"
    assert keys[3] == "d"


def test_scope_get_with_default():
    scope = ConfigScope([{"a": 1}, {"a": 11, "b": 2}, {"b": 22, "c": 3}])
    assert scope.get("a") == 1
    assert scope.get("d") is None
    assert scope.get("d", 1) == 1
    assert scope.get("d", default=1) == 1
    assert "d" not in scope
