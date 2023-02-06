import pytest
from pymongo import WriteConcern

from common.db.mongo.counter import MongoCounter
from common.tester.utils.mongo import tmp_collection


@pytest.mark.parametrize('write_concern', [None, WriteConcern(w='majority')])
def test_counter(write_concern):
    with tmp_collection('counters') as collection:
        counter = MongoCounter('test_counter', collection=collection, write_concern=write_concern)
        assert counter.next_value() == 1
        assert counter.next_value() == 2
        assert counter.next_value() == 3

        counter.reset()
        assert counter.next_value() == 1
