from empty_serp import EmptySerpMetric
from test_utils import create_serp


metric = EmptySerpMetric()


def test_empty():
    assert metric(create_serp()) == 1


def test_not_empty():
    assert metric(create_serp([{}])) == 0
