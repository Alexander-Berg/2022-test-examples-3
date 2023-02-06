import pytest

from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.serializers.id.string.python import id_string_serializer


@pytest.mark.parametrize("id,ref", [
    [TId("", ""), ":"],
    [TId("type", ""), "type:"],
    [TId("", "value"), ":value"],
    [TId("type", "value"), "type:value"],
])
def test_id_string_serializer(id, ref):
    serialized_id = id_string_serializer.ToString(id)
    assert ref == serialized_id
    assert id == id_string_serializer.FromString(serialized_id)
