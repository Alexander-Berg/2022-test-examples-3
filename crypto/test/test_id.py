from crypta.cm.services.common.data.python.id import TId


def test_id():
    id = TId("type", "value")
    assert "type" == id.Type
    assert "value" == id.Value
    assert "TId('type', 'value')" == repr(id)


def test_equality():
    id1 = TId("type", "value")
    id1_dup = TId("type", "value")
    id2 = TId("type-2", "value-2")

    assert id1 == id1
    assert id1 == id1_dup
    assert id1 != id2


def test_hash():
    d = dict()

    type_1, type_2 = "type-1", "type-2"
    value_1, value_2 = "value-1", "value-2"

    d[TId(type_1, value_1)] = "first"
    d[TId(type_2, value_2)] = "second"

    assert "first" == d[TId(type_1, value_1)]
    assert "second" == d[TId(type_2, value_2)]

    d[TId(type_1, value_1)] = "first v2"
    assert "first v2" == d[TId(type_1, value_1)]

    assert set([TId("foo", "bar")]) == set([TId("foo", "bar"), TId("foo", "bar")])
