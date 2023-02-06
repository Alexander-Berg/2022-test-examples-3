import pytest
from parallel_offline.signals import QueryTypeSignal, MarketabilitySignal
from parallel_offline.signal_calculator import SignalBatch


# Проверяем, что в случае, если в батч засунуть сигналы с неуникальными batch_id,
# то вернется ошибка
def test_batch_with_different_signals():
    signal_1 = QueryTypeSignal(query="test")
    signal_2 = MarketabilitySignal(query="test")

    with pytest.raises(RuntimeError):
        batch = SignalBatch(signals=[signal_1, signal_2])


# Проверяем, что в случае, если в батч засунуть сигналы с неуникальными параметрами,
# то вернется ошибка
def test_batch_with_different_params():
    signal_1 = QueryTypeSignal(query="test", toloka_priority=2)
    signal_2 = QueryTypeSignal(query="test", toloka_priority=3)

    with pytest.raises(RuntimeError):
        batch = SignalBatch(signals=[signal_1, signal_2])

