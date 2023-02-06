import pytest

from mail.nwsmtp.tests.lib.confdict import ConfDict


def test_access_by_getattr():
    assert ConfDict({"a": 1}).a == 1
    assert ConfDict({"a": {"b": 2}}).a.b == 2


def test_access_by_getitem():
    assert ConfDict({"a": 1})["a"] == 1
    assert ConfDict({"a": {"b": 2}})["a"]["b"] == 2


# AttrDict is not supported key names with underscore,
#  but our configs contain such keys
def test_access_with_underscore():
    assert ConfDict({"_a": 1})._a == 1


def test_in_operator():
    assert "a" in ConfDict({"a": 1})


def test_bool():
    assert not ConfDict({})
    assert ConfDict({"a": 1})


def test_raise_key_error_when_no_such_keys_found():
    with pytest.raises(KeyError):
        ConfDict({"a": 1}).b
    with pytest.raises(KeyError):
        ConfDict({"a": 1})["b"]


def test_iter():
    assert next(iter(ConfDict({"a": 1}))) == "a"
    with pytest.raises(StopIteration):
        next(iter(ConfDict({})))


def test_items():
    assert list(ConfDict({"a": {"b": 2, "c": 3}}).a.items()) == [("b", 2), ("c", 3)]
