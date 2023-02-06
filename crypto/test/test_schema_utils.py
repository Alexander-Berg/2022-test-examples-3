from __future__ import absolute_import

import pytest
from yt import yson

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.schema_utils.test.user_data_pb2 import TUserData


SORT_ORDER_FIELD_1 = {"name": "sort_order_field_1", "type": "uint64", "required": True, "sort_order": "ascending"}
SORT_ORDER_FIELD_2 = {"name": "sort_order_field_2", "type": "uint64", "required": True, "sort_order": "ascending"}
REQUIRED_TRUE_FIELD = {"name": "required_true_field", "type": "uint64", "required": True}
REQUIRED_FALSE_FIELD = {"name": "required_false_field", "type": "uint64", "required": False}
NO_REQUIRED_FIELD = {"name": "no_required_field", "type": "uint64"}
FULL_NO_REQUIRED_FIELD = {"name": "no_required_field", "type": "uint64", "required": False}


def get_schema_with_attribute(columns, attr, value):
    schema = yson.YsonList(columns)
    schema.attributes[attr] = value
    return schema


@pytest.mark.parametrize("schema_1,schema_2", [
    (yson.YsonList(),
     yson.YsonList()),
    (yson.YsonList([NO_REQUIRED_FIELD]),
     yson.YsonList([FULL_NO_REQUIRED_FIELD])),
    (yson.YsonList([SORT_ORDER_FIELD_1, SORT_ORDER_FIELD_2, REQUIRED_TRUE_FIELD, REQUIRED_FALSE_FIELD, NO_REQUIRED_FIELD]),
     yson.YsonList([SORT_ORDER_FIELD_1, SORT_ORDER_FIELD_2, REQUIRED_FALSE_FIELD, NO_REQUIRED_FIELD, REQUIRED_TRUE_FIELD])),
    (get_schema_with_attribute(yson.YsonList([SORT_ORDER_FIELD_1]), "attr", "value"),
     get_schema_with_attribute(yson.YsonList([SORT_ORDER_FIELD_1]), "attr", "value"))
])
def test_are_schemas_equal_true(schema_1, schema_2):
    assert schema_utils.are_schemas_equal(schema_1, schema_2)


@pytest.mark.parametrize("schema_1,schema_2", [
    [yson.YsonList(),
     yson.YsonList([NO_REQUIRED_FIELD])],
    [yson.YsonList([SORT_ORDER_FIELD_1, SORT_ORDER_FIELD_2]),
     yson.YsonList([SORT_ORDER_FIELD_2, SORT_ORDER_FIELD_1])],
    [yson.YsonList([SORT_ORDER_FIELD_1, SORT_ORDER_FIELD_2, REQUIRED_FALSE_FIELD, NO_REQUIRED_FIELD]),
     yson.YsonList([SORT_ORDER_FIELD_1, SORT_ORDER_FIELD_2, REQUIRED_FALSE_FIELD, NO_REQUIRED_FIELD, REQUIRED_TRUE_FIELD])],
    [yson.YsonList([SORT_ORDER_FIELD_1, REQUIRED_TRUE_FIELD, REQUIRED_FALSE_FIELD, NO_REQUIRED_FIELD]),
     yson.YsonList([SORT_ORDER_FIELD_1, SORT_ORDER_FIELD_2, REQUIRED_FALSE_FIELD, NO_REQUIRED_FIELD, REQUIRED_TRUE_FIELD])],
    [get_schema_with_attribute(yson.YsonList([SORT_ORDER_FIELD_1]), "attr", "value_1"),
     get_schema_with_attribute(yson.YsonList([SORT_ORDER_FIELD_1]), "attr", "value_2")]
])
def test_are_schemas_equal_false(schema_1, schema_2):
    assert not schema_utils.are_schemas_equal(schema_1, schema_2)


def test_get_schema_from_proto():
    return yson.yson_to_json(schema_utils.get_schema_from_proto(TUserData, ["yuid", "timestamp"]))
