import yandex.maps.proto.search.business_pb2 as pb_business
import yandex.maps.proto.sprav.tycoon_pb2 as pb_tycoon


YANDEX = 1124715036


def test_langs(metasearch):
    metasearch.set_query(business_oid=YANDEX)

    r = metasearch.get_pb()
    assert r.first_doc.name == 'Yandex'

    r = metasearch.get_pb(lang='ru_RU')
    assert r.first_doc.name == 'Яндекс'

    rw = metasearch.get_raw(lang='fake')
    assert rw.status_code == 400


def test_response_formats(metasearch):
    metasearch.set_query(business_oid=YANDEX)

    r = metasearch.get_pb()
    assert r.first_doc.has_metadata(pb_business.GEO_OBJECT_METADATA)
    assert not r.first_doc.has_metadata(pb_tycoon.TYCOON_METADATA)

    r = metasearch.get_pb(type='sprav')
    assert not r.first_doc.has_metadata(pb_business.GEO_OBJECT_METADATA)
    assert r.first_doc.has_metadata(pb_tycoon.TYCOON_METADATA)


def test_ask_same_org_twice(metasearch):
    metasearch.set_query(business_oid=[YANDEX, YANDEX], gta='cluster_permalinks')
    r = metasearch.get_pb()
    assert r.doc_count == 1

    cluster_permalinks = r.first_doc.permalinks()
    assert len(cluster_permalinks) >= 1
    assert str(YANDEX) in cluster_permalinks

    metasearch.set_query(business_oid=cluster_permalinks)
    r = metasearch.get_pb()
    assert r.doc_count == 1
