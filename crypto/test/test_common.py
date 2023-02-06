from crypta.cm.services.common.data.python import common


DICT_BINARY = {
    b"key": b"value",
    b"key-2": b"value-2",
}

DICT_STR = {
    "key": "value",
    "key-2": "value-2",
}


def test_ensure_dict_str():
    assert DICT_STR == common.ensure_dict_str(DICT_BINARY)
    assert DICT_STR == common.ensure_dict_str(DICT_STR)


def test_ensure_dict_binary():
    assert DICT_BINARY == common.ensure_dict_binary(DICT_STR)
    assert DICT_BINARY == common.ensure_dict_binary(DICT_BINARY)
