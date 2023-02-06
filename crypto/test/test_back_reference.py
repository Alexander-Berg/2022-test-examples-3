from crypta.cm.services.common.data.python.back_reference import TBackReference
from crypta.cm.services.common.data.python.id import TId


YANDEXUID_TYPE = "yandexuid"
EXT_NS = "ext_ns"
EXT_NS_2 = "ext_ns_2"


def test_empty():
    back_ref = TBackReference(TId("", ""), [])

    ref_id = TId("", "")
    ref_refs = set()

    assert ref_id == back_ref.GetId()
    assert ref_refs == back_ref.GetRefs()


def test_full():
    id = TId(YANDEXUID_TYPE, "100500")
    ext_ids = {TId(EXT_NS, "value-1"), TId(EXT_NS_2, "value-2")}
    back_ref = TBackReference(id, ext_ids)

    assert id == back_ref.GetId()
    assert ext_ids == back_ref.GetRefs()
    assert id != TId("foo", "bar")


def test_equality():
    id_1 = TId(YANDEXUID_TYPE, "100500")
    ext_ids_1 = [TId(EXT_NS, "1500000000"), TId(EXT_NS_2, "1600000000")]
    back_ref_1 = TBackReference(id_1, ext_ids_1)
    back_ref_1_dup = TBackReference(id_1, ext_ids_1)

    id_2 = TId(YANDEXUID_TYPE, "200500")
    ext_ids_2 = [TId(EXT_NS, "1500"), TId(EXT_NS, "1600")]
    back_ref_2 = TBackReference(id_2, ext_ids_2)

    assert back_ref_1 == back_ref_1
    assert back_ref_1_dup == back_ref_1
    assert back_ref_2 != back_ref_1
