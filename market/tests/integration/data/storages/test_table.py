from yamarec1.data.storages import Table


def test_table_can_store_and_provide_iterable_data(random_yt_path, iterable_data):
    path = random_yt_path
    storage = Table(path=path)
    storage.store(iterable_data)
    assert len(storage.data.schema.columns) == 4
    assert len(list(storage.data)) == 2


def test_table_can_store_and_provide_queryable_data(random_yt_path, queryable_data):
    path = random_yt_path
    storage = Table(path=path)
    storage.store(queryable_data)
    assert len(storage.data.schema.columns) == 3
    query = storage.data.query
    query.run()
    assert len(list(query.result)) == 2
