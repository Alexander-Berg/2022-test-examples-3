import pytest
from parallel_offline.signals.tests.common import get_basket, get_signal_calculator
from parallel_offline.signals import QueryTypeSignal, RelevanceSignal, MarketabilitySignal

# Проверка группировки батчей с разными процессами хитмана
def test_batch_grouping_different_processes():
    basket = get_basket()
    signal_calculator = get_signal_calculator()

    #  Add two type signals
    for row in basket:
        signal_calculator.add_signal(QueryTypeSignal(query=row['query']))
        signal_calculator.add_signal(MarketabilitySignal(query=row['query']))
        signal_calculator.add_signal(RelevanceSignal(query=row['query'], url=row['url']))

    signal_calculator._make_loop()

    assert len(signal_calculator.running_batches) == 3


# Проверка группировки батчей с одинаковым процессом хитмана с разными параметрами
def test_batch_grouping_same_processes():
    basket = get_basket()

    # Иначе не наберем нужное кол-во разных батчей
    assert len(basket) > 1

    signal_calculator = get_signal_calculator()

    #  Add two type signals
    for i, row in enumerate(basket):
        priority = 2 + (i % 2)
        signal_calculator.add_signal(QueryTypeSignal(query=row['query'], toloka_priority=priority))
        signal_calculator.add_signal(RelevanceSignal(query=row['query'], url=row['url'], toloka_priority=priority))

    signal_calculator._make_loop()

    assert len(signal_calculator.running_batches) == 4


# Проверка возврата ошибки в случае, когда в hitman-процессе отсутствует группировка
def test_batch_grouping_absence():

    COMPLETED_ONLY = {
        'SUCCEEDED': 1
    }

    basket = get_basket()
    signal_calculator = get_signal_calculator(
        input_is_grouped=False,
        batch_status_dict=COMPLETED_ONLY
    )

    for row in basket:
        signal_calculator.add_signal(QueryTypeSignal(query=row['query']))

    signal_calculator._make_loop()

    with pytest.raises(RuntimeError):
        signal_calculator._make_loop()


