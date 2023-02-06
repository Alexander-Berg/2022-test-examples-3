import pytest

from crypta.lib.python.purgatory.convertor import (
    convert_and_store,
    Convertor
)


@pytest.mark.parametrize("src,optional,dst,ref", [
    ({"field": "125"}, False, {"preexistent": "value"}, {"target": 125, "preexistent": "value"}),
    ({"field": "125"}, True, {"preexistent": "value"}, {"target": 125, "preexistent": "value"}, ),
    ({"another field": "125"}, True, {"preexistent": "value"}, {"target": None, "preexistent": "value"})
])
def test_convert_and_store(src, optional, dst, ref):
    convert_and_store(src, dst, "field", "target", int, optional)
    assert dst == ref


@pytest.mark.parametrize("src,dst,ref", [
    (
        {"first_src": "1", "second_src": "2", "optional_src": "3"},
        None,
        {"first_dst": 1, "second_dst": 4, "optional_dst": "3"}
    ),
    (
        {"first_src": "4", "second_src": "5"},
        {"preexistent": "6"},
        {"first_dst": 4, "second_dst": 25, "preexistent": "6", "optional_dst": None}
    )
])
def test_dict_convertor(src, dst, ref):
    convertor = Convertor()
    convertor.add("first_src", "first_dst", int)
    convertor.add([], "second_dst", lambda src: int(src["second_src"])**2)
    convertor.add_optional("optional_src", "optional_dst")

    assert convertor.convert(src, dst) == ref
