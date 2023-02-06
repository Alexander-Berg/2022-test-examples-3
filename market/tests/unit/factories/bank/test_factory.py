from yamarec1.data.storages import Table


def test_bank_factory_configures_banks_correctly(bank_factory):
    assert bank_factory["my.items"]["model"].key == ("object_type", "object_id")
    assert bank_factory["my.items"]["model"].mode == "inner"
    storage = bank_factory["my.items"]["model", "category", "2017-08-20"]
    assert storage == Table(path="items/2017-08-20/model/category")
