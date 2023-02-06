import pytest
from parallel_offline.signals.tests.common import get_basket, get_signal_calculator
from parallel_offline.signals import QueryTypeSignal, RelevanceSignal, MarketabilitySignal


def test_start_failed_status_exception():
    basket = get_basket()

    START_FAILED_ONLY = {
        'START_FAILED': 1
    }

    signal_calculator = get_signal_calculator(batch_status_dict=START_FAILED_ONLY)

    for row in basket:
        signal_calculator.add_signal(QueryTypeSignal(query=row['query']))

    # Первый make_loop - раскидали сигналы по батчам и запустили
    signal_calculator._make_loop()

    # Тут вызывается первая проверка статуса и обнаруживается, что процесс не удалось запустить
    with pytest.raises(RuntimeError):
        signal_calculator._make_loop()


@pytest.mark.parametrize('max_retries', [3, 4, 5, 6])
def test_failed_status_exception(max_retries):
    basket = get_basket()

    FAILED_ONLY = {
        'FAILED': 1
    }

    signal_calculator = get_signal_calculator(batch_status_dict=FAILED_ONLY, max_retries=max_retries)

    for row in basket:
        signal_calculator.add_signal(QueryTypeSignal(query=row['query']))

    # Первый make_loop - раскидали сигналы по батчам и запустили
    signal_calculator._make_loop()

    # Тут вызывается первая проверка статуса и обнаруживается, что процесс не удалось запустить
    for c in range(max_retries):
        if c < max_retries-1:
            signal_calculator._make_loop()
        else:
            with pytest.raises(RuntimeError):
                signal_calculator._make_loop()



