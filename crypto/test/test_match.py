from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.data.python.match import TMatch
from crypta.cm.services.common.data.python.matched_id import TMatchedId


EXT_ID = TId("type", "value")
EXT_ID_2 = TId("type-2", "value-2")
INT_ID_1_1 = TMatchedId(TId("int_type_1", "int_value_1"), 10, 11, {"foo_1": "bar_1"})
INT_ID_1_2 = TMatchedId(TId("int_type_2", "int_value_2"), 20, 21, {"foo_2": "bar_2"})
INT_ID_2_1 = TMatchedId(TId("int_type_1-2", "int_value_1-2"), 30, 31, {"foo_1-2": "bar_1-2"})
INT_ID_2_2 = TMatchedId(TId("int_type_2-2", "int_value_2-2"), 40, 41, {"foo_2-2": "bar_2-2"})


def test_match():
    int_ids = {
        INT_ID_1_1.Id.Type: INT_ID_1_1,
        INT_ID_1_2.Id.Type: INT_ID_1_2,
    }
    match = TMatch(EXT_ID, int_ids, 100, 50)

    assert EXT_ID == match.GetExtId()
    assert int_ids == match.GetInternalIds()

    ref_repr = (
        "TMatch(ext_id=TId('type', 'value'), matched_ids={"
        "'int_type_1': TMatchedId(id=TId('int_type_1', 'int_value_1'), match_ts=10, cas=11, attributes={'foo_1': 'bar_1'}), "
        "'int_type_2': TMatchedId(id=TId('int_type_2', 'int_value_2'), match_ts=20, cas=21, attributes={'foo_2': 'bar_2'})"
        "}, touch=100, ttl=50)"
    )
    assert ref_repr == repr(match)


def test_equality():
    match_1 = TMatch(
        EXT_ID,
        {
            INT_ID_1_1.Id.Type: INT_ID_1_1,
            INT_ID_1_2.Id.Type: INT_ID_1_2,
        },
        100,
        50
    )
    match_2 = TMatch(
        EXT_ID_2,
        {
            INT_ID_2_1.Id.Type: INT_ID_2_1,
            INT_ID_2_2.Id.Type: INT_ID_2_2,
        },
        100,
        50
    )

    assert match_1 == match_1
    assert match_1 != match_2
