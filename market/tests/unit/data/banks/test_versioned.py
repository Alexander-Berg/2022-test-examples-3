from yamarec1.data import QueryableData


def test_bank_handles_slices(versioned_data_bank):
    storage = versioned_data_bank["2017-01-01":"2017-01-07"]
    assert isinstance(storage.data, QueryableData)
    assert storage.data.query.body == (
        "SELECT * FROM RANGE (`//home/me`, `2017-01-01`, `2017-01-07`)"
    )


def test_bank_handles_suffixed_slices(versioned_and_suffixed_data_bank):
    storage = versioned_and_suffixed_data_bank["2017-01-01":"2017-01-07"]
    assert isinstance(storage.data, QueryableData)
    assert storage.data.query.body == (
        "SELECT * FROM RANGE (`//home/me`, `2017-01-01`, `2017-01-07`, `suffix`)"
    )
