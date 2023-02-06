from judged import JudgedMetric
from test_utils import create_serp, create_component, SCALE


def test_empty():
    metric = JudgedMetric(SCALE)
    assert metric(create_serp()) is None


def test_half_judged():
    metric = JudgedMetric(SCALE)
    serp = create_serp([create_component("TEST"), {}])
    assert metric(serp) == 0.5


def test_depth():
    metric = JudgedMetric(SCALE, depth=1)
    serp = create_serp([create_component("TEST"), {}])
    assert metric(serp) == 1
