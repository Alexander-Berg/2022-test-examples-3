from serp_failed import SerpFailedMetric
from test_utils import create_failed_serp, create_serp


metric = SerpFailedMetric()


def test_failed():
    assert metric(create_failed_serp()) == 1


def test_passed():
    assert metric(create_serp()) == 0
