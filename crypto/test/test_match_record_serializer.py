from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.data.python.match import TMatch
from crypta.cm.services.common.data.python.matched_id import TMatchedId
from crypta.cm.services.common.serializers.match.record.python import match_record_serializer


def test_to_serialize_deserialize():
    match = TMatch(TId("type", "value"), {
        "int_type": TMatchedId(TId("int_type", "int_value"), 10, 11, {"foo": "bar"}),
        "int_type_2": TMatchedId(TId("int_type_2", "int_value_2"), 20, 21, {"foo2": "bar2"})
    }, 1500000000, 3600)

    record = match_record_serializer.ToRecord(match)
    deserialized_match = match_record_serializer.FromRecord(record)
    record_2 = match_record_serializer.ToRecord(deserialized_match)

    assert match == deserialized_match
    assert record == record_2
