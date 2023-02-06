from pytest import approx

from search.metrics.ccc.ccc_lib.aggregations.descriptive_statistics import DescriptiveAggregator


def test_descriptive_aggregator():
    da = DescriptiveAggregator()
    descriptive_result = da([1, 2, 3, 4, 5])
    assert descriptive_result.nobs == 5
    assert descriptive_result.mean == approx(3)
    assert descriptive_result.variance == approx(2.5)
