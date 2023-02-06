def test_all_photos(upper):
    r = upper.get_raw(ms='photos', format='json', business_oid=1018907821)
    message = r.json()

    entries = message.get('entry')
    assert isinstance(entries, list)
    assert len(entries) > 0
