import pytest

from yamarec1.kit.helpers import Registry


def test_registry_is_empty_by_default():
    registry = Registry()
    assert registry.items == {}


def test_registry_can_be_initialized_with_custom_dictionary():
    registry = Registry({"x": 1})
    assert registry.items == {"x": 1}


def test_registry_allows_to_get_items_directly():
    registry = Registry({"x": 1})
    assert registry["x"] == 1
    with pytest.raises(KeyError):
        registry["y"]


def test_registry_allows_to_set_items_directly():
    registry = Registry()
    registry["x"] = 1
    assert registry.items == {"x": 1}


def test_registry_allows_to_add_items_via_decorator():
    registry = Registry()

    @registry.item("a")
    def a():
        pass

    @registry.item("B")
    class B(object):
        pass

    assert registry.items == {"a": a, "B": B}
