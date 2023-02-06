from json import loads
from parallel_offline.mock_signal_calculator import SignalCalculatorMock


def get_basket():
    with open('parallel_offline/signals/tests/basket.json', 'r') as f:
        basket = loads(f.read())
        return basket


def get_signal_calculator(**kwargs):
    signal_calculator = SignalCalculatorMock(
        hitman_token="some_token",
        hitman_requester="test_user",
        **kwargs
    )
    return signal_calculator
