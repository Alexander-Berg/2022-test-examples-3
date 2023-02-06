import pytest

from crypta.lib.python.native_yt import proto
from crypta.lib.python.native_yt.test import test_pb2

STRING_FIELD = {'name': 'string_value', 'type': 'string'}
UINT64_FIELD = {'name': 'uint64_value', 'type': 'uint64'}
INT64_FIELD = {'name': 'int64_value', 'type': 'int64'}
ENUM_FIELD = {'name': 'enum_value', 'type': 'string'}
MESSAGE_FIELD = {'name': 'message_value', 'type': 'string'}

SORT_ORDER = {'sort_order': 'ascending'}
REQUIRED = {'required': 'true'}
EXTRA_PROPERTIES = {'message_value': {'some_extra_property': 'extra_property_value'}}


@pytest.mark.parametrize("dynamic,strong,kwargs,result", [
    pytest.param(
        False,
        False,
        {},
        [
            STRING_FIELD,
            UINT64_FIELD,
            INT64_FIELD,
            ENUM_FIELD,
            MESSAGE_FIELD,
        ],
        id='not-dynamic-not-strong',
    ),
    pytest.param(
        True,
        False,
        {},
        [
            STRING_FIELD,
            UINT64_FIELD,
            INT64_FIELD,
            ENUM_FIELD,
            MESSAGE_FIELD,
        ],
        id='dynamic-not-strong',
    ),
    pytest.param(
        False,
        True,
        {},
        [
            dict(list(UINT64_FIELD.items()) + list(REQUIRED.items()) + list(SORT_ORDER.items())),
            STRING_FIELD,
            INT64_FIELD,
            ENUM_FIELD,
            MESSAGE_FIELD,
        ],
        id='not-dynamic-strong',
    ),
    pytest.param(
        True,
        True,
        {},
        [
            dict(list(UINT64_FIELD.items()) + list(SORT_ORDER.items())),
            STRING_FIELD,
            INT64_FIELD,
            ENUM_FIELD,
            MESSAGE_FIELD,
        ],
        id='dynamic-strong',
    ),
    pytest.param(
        False,
        False,
        EXTRA_PROPERTIES,
        [
            STRING_FIELD,
            UINT64_FIELD,
            INT64_FIELD,
            ENUM_FIELD,
            dict(list(MESSAGE_FIELD.items()) + list(EXTRA_PROPERTIES[MESSAGE_FIELD['name']].items())),
        ],
        id='not-dynamic-not-strong-kwargs',
    ),
])
def test_create_schema(dynamic, strong, kwargs, result):
    assert result == proto.create_schema(test_pb2.Record, dynamic=dynamic, strong=strong, **kwargs)
