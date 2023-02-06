from parallel_offline.signals.tests.common import get_basket, get_signal_calculator
from parallel_offline.signals import QueryTypeSignal, RelevanceSignal, MarketabilitySignal


def test():

    STATUS_PROBABILITIES = {
        'FAILED': 0.02,
        'SUCCEEDED': 0.9,
        'RUNNING': 0.08
    }

    basket = get_basket()
    signal_calculator = get_signal_calculator(batch_status_dict = STATUS_PROBABILITIES)

    for row in basket:
        signal_calculator.add_signal(QueryTypeSignal(query=row['query']))
        signal_calculator.add_signal(MarketabilitySignal(query=row['query']))
        signal_calculator.add_signal(RelevanceSignal(query=row['query'], url=row['url']))

    signal_calculator.complete_signals(pause_seconds=1)

    assert all([signal.is_completed for signal in signal_calculator.all_signals.values()])
