import pytest

from crypta.lib.native.database.python.record import TRecord
from crypta.styx.services.common.data.proto.puid_state_pb2 import (
    TOblivionEvent,
    TPuidState,
)
from crypta.styx.services.common.serializers.python import puid_state_record_serializer


REF_KEY = "puid:100500"
REF_VALUE = b"\x12\t\x08\x94\x91\x06\x12\x03foo\x12\t\x08\xa4\xdf\x06\x12\x03bar"


@pytest.fixture(scope="function")
def ref_puid_state():
    state = TPuidState()

    state.Puid = 100500
    state.OblivionEvents.append(TOblivionEvent(Unixtime=100500, Obfuscated="foo"))
    state.OblivionEvents.append(TOblivionEvent(Unixtime=110500, Obfuscated="bar"))

    return state


def test_to_record(ref_puid_state):
    record = puid_state_record_serializer.ToRecord(ref_puid_state)
    assert REF_KEY == record.Key
    assert REF_VALUE == record.Value


def test_from_record(ref_puid_state):
    record = TRecord.Create(REF_KEY, REF_VALUE)
    assert ref_puid_state == puid_state_record_serializer.FromRecord(record)
