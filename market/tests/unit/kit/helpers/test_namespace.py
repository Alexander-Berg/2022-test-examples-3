import pytest

from yamarec1.kit.helpers import Namespace


def test_namespace_provides_access_via_indexing():
    namespace = Namespace({"?": 1, "!": 2})
    assert namespace["?"] == 1
    assert namespace["!"] == 2
    with pytest.raises(KeyError):
        namespace["."]


def test_namespace_provides_access_via_attributes():
    namespace = Namespace({"a": 1, "b": 2})
    assert namespace.a == 1
    assert namespace.b == 2
    with pytest.raises(AttributeError):
        namespace.c


def test_namespace_allows_to_check_name_membership():
    namespace = Namespace({"?": 1, "!": 2})
    assert "?" in namespace
    assert "." not in namespace


def test_namespace_provides_access_to_underlying_dictionary():
    namespace = Namespace({"a": 1, "b": 2})
    assert namespace.__dictionary__ == {"a": 1, "b": 2}


def test_namespace_provides_access_to_its_class():
    namespace = Namespace({"a": 1, "b": 2})
    assert namespace.__class__.__name__ == "Namespace"


def test_namespace_supports_iteration_over_names():
    namespace = Namespace({"a": 1, "b": 2})
    assert set(namespace) == {"a", "b"}


def test_namespace_is_immutable():
    namespace = Namespace({"a": 1, "b": 2})
    with pytest.raises(AttributeError):
        del namespace.a
    with pytest.raises(AttributeError):
        namespace.a = 3
