from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.data.python.matched_id import TMatchedId


def test_matched_id():
    matched_id = TMatchedId(TId("type", "value"), 1, 2, {"foo": "bar"})

    assert "type" == matched_id.Id.Type
    assert "value" == matched_id.Id.Value
    assert 1 == matched_id.MatchTs
    assert 2 == matched_id.Cas
    assert {"foo": "bar"} == matched_id.Attributes

    assert "TMatchedId(id=TId('type', 'value'), match_ts=1, cas=2, attributes={'foo': 'bar'})" == repr(matched_id)


def test_equality():
    id1 = TMatchedId(TId("type", "value"), 11, 12, {"foo": "bar"})
    id2 = TMatchedId(TId("type-2", "value-2"), 21, 22, {"foo-2": "bar-2"})

    assert id1 == id1
    assert id1 != id2
