from yamarec1.beans import ytc
from yamarec1.data.storages import View


def test_view_can_store_schemeful_queryable_data(random_yt_path, queryable_data):
    path = random_yt_path
    storage = View(path=path)
    storage.store(queryable_data)
    assert ytc.get(path) == queryable_data.query.body
    assert storage.data.schema == queryable_data.schema


def test_view_can_store_schemeless_queryable_data(random_yt_path, schemeless_queryable_data):
    path = random_yt_path
    storage = View(path=path)
    storage.store(schemeless_queryable_data)
    assert ytc.get(path) == schemeless_queryable_data.query.body
    assert storage.data.schema is None
    query = storage.data.query
    query.run()
    assert len(list(query.result)) == 2
    assert len(query.result.schema.columns) == 3
