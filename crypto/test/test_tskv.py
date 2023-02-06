from crypta.lib.python.tskv import (
    tskv_to_dict,
    dict_to_tskv
)

import pytest


@pytest.mark.parametrize("tskv_line, res_dict", [
    ("", {}),
    ("a", {}),
    ("a=", {"a": ""}),
    ("a=b\tc", {"a": "b"}),
    ("a=b\tc=", {"a": "b", "c": ""}),
    ("a=b\tc=\te=f", {"a": "b", "c": "", "e": "f"}),
    ("a=b\tc\te=f", {"a": "b", "e": "f"}),
    ("a=b\tc=d\te=f", {"a": "b", "c": "d", "e": "f"})
])
def test_tskv_to_dict(tskv_line, res_dict):
    assert tskv_to_dict(tskv_line) == res_dict


@pytest.mark.parametrize("src_dict, tskv_line", [
    ({}, ""),
    ({"a": ""}, "a="),
    ({"a": "b"}, "a=b"),
    ({1: "b"}, "1=b"),
    ({"a": 123}, "a=123"),
    ({"a": [1, 2]}, "a=[1, 2]"),
    ({"a": ["\t"]}, "a=['\\t']"),
    ({"a": {1: "2"}}, "a={1: '2'}"),
    ({"a": {1: "\t2"}}, "a={1: '\\t2'}"),
    ({"a": "b", "c": ""}, "a=b\tc="),
    ({"a": "b", "c": "", "e": "f"}, "a=b\tc=\te=f"),
    ({"a": "b", "c": "d", "e": "f"}, "a=b\tc=d\te=f")
])
def test_dict_to_tskv(tskv_line, src_dict):
    assert dict_to_tskv(src_dict) == tskv_line


@pytest.mark.parametrize("src_dict", [
    ({"a\ta": "b"}),
    ({"a": "b\tb"}),
    ({"a\ta": "b\tb"}),
    ({"a": "b", "c\t": "d", "e": "f"})
])
def test_dict_to_tskv_tabs(src_dict):
    with pytest.raises(Exception):
        dict_to_tskv(src_dict)
