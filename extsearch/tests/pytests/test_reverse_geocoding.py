RUBIN_PLAZA_LL = '27.526267,53.891011'


def test_type_required(metasearch):
    metasearch.set_query(mode='reverse', ll=RUBIN_PLAZA_LL)
    r = metasearch.get_raw()
    assert r.status_code == 400
    r = metasearch.get_raw(type='biz,geo')
    assert r.status_code == 400
    r = metasearch.get_raw(type='geo')
    assert r.status_code == 200


def test_vital_objects(metasearch):
    metasearch.set_query(mode='reverse', type='geo', results=1, lang='ru_RU')

    r = metasearch.get_pb(ll=RUBIN_PLAZA_LL)
    assert r.doc_count == 1
    assert r.first_doc.is_toponym()
    r.first_doc.check_name_contains('проспект Дзержинского, 5')
