import pytest

from crypta.siberia.bin.common.data.proto import user_pb2
from crypta.siberia.bin.users_uploader.lib import upload_users_mapper


@pytest.mark.parametrize("value,reference", [
    ("@yandexuid", False),
    ("yandexuid", True),
])
def test_is_attr_key_correct(value, reference):
    assert reference == upload_users_mapper.is_attr_key_correct(value)


@pytest.mark.parametrize("row,fields_id_types,reference", [
    (
        {"user_yandexuid": "100000001000000001"},
        {"user_yandexuid": "yandexuid"},
        {"@yandexuid": user_pb2.TUser.TInfo.TAttributeValues(Values=["100000001000000001"])},
    ),
    (
        {"user_yandexuid": "1000000"},
        {"user_yandexuid": "yandexuid"},
        {"@yandexuid": user_pb2.TUser.TInfo.TAttributeValues(Values=["1000000"])},
    ),
    (
        {}, {}, {},
    ),
    (
        {"user_yandexuid": "100000001000000001"},
        {},
        {"user_yandexuid": user_pb2.TUser.TInfo.TAttributeValues(Values=["100000001000000001"])},
    ),
    (
        {"user_yandexuid": "100000001000000001", "user_login": "nagibator", "param": "my_param", "user_email": "invalid_email"},
        {"user_yandexuid": "yandexuid", "user_login": "login", "user_email": "email"},
        {
            "@yandexuid": user_pb2.TUser.TInfo.TAttributeValues(Values=["100000001000000001"]),
            "@login": user_pb2.TUser.TInfo.TAttributeValues(Values=["nagibator"]),
            "@email": user_pb2.TUser.TInfo.TAttributeValues(Values=["invalid_email"]),
            "param": user_pb2.TUser.TInfo.TAttributeValues(Values=["my_param"]),
        },
    ),
    (
        {"list_field": [1, 2], "invalid_field": "\xFF"},
        {},
        {
            "list_field": user_pb2.TUser.TInfo.TAttributeValues(Values=["[1, 2]"]),
            "invalid_field": user_pb2.TUser.TInfo.TAttributeValues(Values=[""]),
        },
    ),
])
def test_serialize_attributes(row, fields_id_types, reference):
    assert reference == upload_users_mapper.serialize_attributes(row, fields_id_types)
