from yandex.maps.proto.atom.atom_pb2 import Feed


def test_all_photos(metasearch):
    r = metasearch.get_raw(ms='photos', format='json', business_oid=1018907821)
    message = r.json()

    entries = message.get('entry')
    assert isinstance(entries, list)
    assert len(entries) > 0

    r = metasearch.get_raw(ms='photos', business_oid=1018907821)
    feed = Feed()
    feed.ParseFromString(r.content)
    assert len(feed.entry) > 0

    r = metasearch.get_raw(ms='photos', business_oid=1018907821, uid=123456)
    feed = Feed()
    feed.ParseFromString(r.content)
    assert len(feed.entry) > 0
