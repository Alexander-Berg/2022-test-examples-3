from aggregators import MultiAggregator, AggregatorResult
from metrics import MetricsCalculator
from filters import only_search_result
from empty_serp import EmptySerpMetric
from test_utils import create_serp


def test_simple_aggregation():
    calculators = _get_calculators()
    calculator, multi_aggregator = calculators
    _calculate_serp(calculators, create_serp())

    assert multi_aggregator.get_results() == [AggregatorResult('empty', 1, 1, 1, 'default', 'onlySearchResult')]


def test_aggregation():
    calculators = _get_calculators()
    calculator, multi_aggregator = calculators

    serp = create_serp()
    _calculate_serp(calculators, serp)
    _calculate_serp(calculators, serp)

    assert multi_aggregator.get_results() == [AggregatorResult('empty', 2, 1, 2, 'default', 'onlySearchResult')]


def test_simple_aggregation_diff_with_config_id():
    calculators = _get_calculators()
    calculator, multi_aggregator = calculators

    _calculate_serp(calculators, create_serp(config_id='1'))
    _calculate_serp(calculators, create_serp(config_id='2'))
    assert multi_aggregator.get_results() == [AggregatorResult('empty', 1, 1, 1, 'default', 'onlySearchResult', '1'),
                                              AggregatorResult('empty', 1, 1, 1, 'default', 'onlySearchResult', '2')]


def test_aggregation_diff_with_config_id():
    calculators = _get_calculators()
    calculator, multi_aggregator = calculators

    serp1 = create_serp(config_id='1')
    serp2 = create_serp(config_id='2')
    _calculate_serp(calculators, serp1)
    _calculate_serp(calculators, serp2)
    _calculate_serp(calculators, serp1)
    _calculate_serp(calculators, serp2)
    _calculate_serp(calculators, serp2)
    assert multi_aggregator.get_results() == [AggregatorResult('empty', 2, 1, 2, 'default', 'onlySearchResult', '1'),
                                              AggregatorResult('empty', 3, 1, 3, 'default', 'onlySearchResult', '2')]


def test_simple_aggregation_diff_with_table_index():
    calculators = _get_calculators()
    calculator, multi_aggregator = calculators

    _calculate_serp(calculators, create_serp(table_index=0))
    _calculate_serp(calculators, create_serp(table_index=1))
    assert multi_aggregator.get_results() == [AggregatorResult('empty', 1, 1, 1, 'default', 'onlySearchResult', table_index=0),
                                              AggregatorResult('empty', 1, 1, 1, 'default', 'onlySearchResult', table_index=1)]


def test_aggregation_diff_with_table_index():
    calculators = _get_calculators()
    calculator, multi_aggregator = calculators

    serp1 = create_serp(table_index=0)
    serp2 = create_serp(table_index=2)
    _calculate_serp(calculators, serp1)
    _calculate_serp(calculators, serp2)
    _calculate_serp(calculators, serp1)
    _calculate_serp(calculators, serp2)
    _calculate_serp(calculators, serp2)
    assert multi_aggregator.get_results() == [AggregatorResult('empty', 2, 1, 2, 'default', 'onlySearchResult', table_index=0),
                                              AggregatorResult('empty', 3, 1, 3, 'default', 'onlySearchResult', table_index=2)]


def test_aggregation_diff_with_config_id_and_table_index():
    calculators = _get_calculators()
    calculator, multi_aggregator = calculators

    serp1 = create_serp(config_id='0', table_index=0)
    serp2 = create_serp(config_id='0', table_index=1)
    serp3 = create_serp(config_id='1', table_index=0)
    _calculate_serp(calculators, serp1)
    _calculate_serp(calculators, serp2)
    _calculate_serp(calculators, serp3)
    _calculate_serp(calculators, serp3)
    _calculate_serp(calculators, serp2)
    assert multi_aggregator.get_results() == [AggregatorResult('empty', 1, 1, 1, 'default', 'onlySearchResult', '0', 0),
                                              AggregatorResult('empty', 2, 1, 2, 'default', 'onlySearchResult', '0', 1),
                                              AggregatorResult('empty', 2, 1, 2, 'default', 'onlySearchResult', '1', 0)]


def _calculate_serp(calculators, serp):
    calculator, multi_aggregator = calculators
    calculator.calculate(serp)
    multi_aggregator.append(serp)


def _get_calculators():
    calculator = MetricsCalculator(
        {only_search_result: {
            'empty': EmptySerpMetric()
        }},
        []
    )
    return calculator, MultiAggregator(calculator.metrics_by_filter)
