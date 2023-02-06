import random
import string

import pytest

from pytest_factor.factortypes.storages import QueryStageStorage
from pytest_factor.factortypes import query
from pytest_factor.factortypes import stage


def get_random_string():
    return ''.join([random.choice(string.ascii_letters) for i in range(60)])


# def test_dataset_performance():
#     queries = [query.Query(cgi={}, text=get_random_string(), lr=random.randint(0, 2500)) for i in range(1000)]
#     betas = [stage.Stage(host=get_random_string()) for i in range(3)]
#     start_time = time.time()
#     for i in range(100):
#         ds = QueryStageStorage()
#         for q in queries:
#             for b in betas:
#                 ds.set_value(query=q, stage=b, value=random.randint(0, 100))
#     time_delta = time.time() - start_time
#     assert time_delta < 3


@pytest.fixture
def queries():
    return [query.Query(cgi={}, text=get_random_string(), lr=random.randint(0, 2500)) for i in range(5)]


@pytest.fixture
def stages():
    return [stage.Stage(host=get_random_string()) for i in range(2)]


@pytest.fixture
def storage(queries, stages):
    storage = QueryStageStorage()
    q1, q2, q3, q4, q5 = queries[0], queries[1], queries[2], queries[3], queries[4],
    s1, s2 = stages[0], stages[1]
    storage.set_value(q1, s1, 11)
    storage.set_value(q2, s1, 21)
    storage.set_value(q3, s1, 31)
    storage.set_value(q4, s1, 41)
    storage.set_value(q5, s2, 52)
    storage.set_value(q1, s2, 12)
    storage.set_value(q2, s2, 22)
    storage.set_value(q3, s2, 32)
    storage.set_value(q4, s2, 42)
    storage.set_value(q5, s2, 52)
    return storage


def test_access(storage, queries, stages):
    assert storage.get_query_row(queries[2])[stages[0]] == 31


def test_get_queries(storage, queries, stages):
    assert storage.get_queries() == queries


def test_add_values():
    storage = QueryStageStorage()
    stage1 = stage.Stage('ya1.ru', {})
    stage2 = stage.Stage('ya2.ru', {})
    storage.set_value(query.Query({}, text='1', lr=1, id=0), stage1, 11)
    storage.set_value(query.Query({}, text='1', lr=1, id=1), stage1, 11)
    storage.set_value(query.Query({}, text='1', lr=1, id=2), stage1, 11)
    storage.set_value(query.Query({}, text='1', lr=1, id=0), stage2, 11)
    storage.set_value(query.Query({}, text='1', lr=1, id=1), stage2, 11)
    storage.set_value(query.Query({}, text='1', lr=1, id=2), stage2, 11)
    assert len(storage.get_stages()) == 2
    assert len(storage.get_queries()) == 3


def test_indexes_access(storage):
    assert storage.queries[0][0] == 11
