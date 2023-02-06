# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.models.geo import Station

from travel.rasp.suggests_tasks.suggests.generate.utils import generate_parallel, retrieve_ids

from common.tester.factories import create_station, create_settlement
from common.tester.testcase import TestCase


class TestGenerateParallel(object):
    data = {1: [2, 3], 4: [5, 6], 7: [8, 9, 10]}

    def test_generate_parallel(self):
        pool_size = 10
        assert [] == list(generate_parallel(parall_test_func, {}, pool_size))
        for pool_size in range(1, 6):
            result = list(generate_parallel(parall_test_func, self.data, pool_size))

            assert sorted(result) == [1, 4, 7]


def parall_test_func((id_, d)):
    assert d == TestGenerateParallel.data[id_]
    return id_


class TestUtils(TestCase):
    def test_retrieve_ids(self):
        create_settlement(id=1)
        create_settlement(id=2)
        stations = [create_station(id=i, settlement=j) for i, j in zip(range(1, 7), [1, 2] * 3)]  # noqa

        assert set(retrieve_ids(Station)) == set(range(1, 7))
        assert len(retrieve_ids(Station, __limit=2)) == 2
        assert set(retrieve_ids(Station, settlement=1)) == {1, 3, 5}
