import asyncio
import enum
from asyncio import Future
from unittest.mock import Mock

import pytest

from mail.python.theatre.utils.coro_interceptor import CoroInterceptor, InterceptorLogic


async def noop():
    return 42


async def nested_noop():
    await noop()


async def reenters_two_times():
    # just yields without a future
    await asyncio.sleep(0)
    # provides future
    await asyncio.sleep(0.01)


@pytest.mark.parametrize(
    ('coro', 'on_suspend_count', 'on_reenter_count'), [
        (noop, 0, 0),
        (nested_noop, 0, 0),
        (reenters_two_times, 2, 2),
    ]
)
def test_logic_call_counts(coro, on_suspend_count, on_reenter_count):
    loop = asyncio.get_event_loop()
    logic_mock = Mock(wraps=InterceptorLogic())
    interceptor = CoroInterceptor(logic_mock)
    loop.run_until_complete(interceptor(coro()))
    assert logic_mock.on_suspend.call_count == on_suspend_count
    assert logic_mock.on_reenter.call_count == on_reenter_count


def test_return_value():
    loop = asyncio.get_event_loop()
    logic_mock = Mock(wraps=InterceptorLogic())
    interceptor = CoroInterceptor(logic_mock)
    result = loop.run_until_complete(interceptor(noop()))
    assert result == 42


class State(enum.Enum):
    Initial = 'initial'
    Start = 'start'
    Processed = 'processed'
    Finish = 'finish'


class MutState:
    def __init__(self, val=State.Initial):
        self.__state = val

    def get(self):
        return self.__state

    def set(self, value: State):
        self.__state = value


class StateTracker(InterceptorLogic):
    def __init__(self, state: MutState):
        self.state = state
        self.history = []
        super().__init__()

    async def on_suspend(self):
        self.track(self.on_suspend)

    async def on_reenter(self):
        self.track(self.on_reenter)

    def track(self, op):
        self.history.append((op, self.state.get()))


def change_state(future: Future, state: MutState):
    state.set(State.Processed)
    future.set_result(True)


async def async_change_state(state: MutState):
    loop = asyncio.get_event_loop()
    future = loop.create_future()
    loop.call_later(0.01, change_state, future, state)
    state.set(State.Start)
    # on_suspend gets called just before leaving the frame
    await future
    # on_reenter gets called just before returning to the frame
    state.set(State.Finish)


def test_visible_state_in_hooks():
    state = MutState()
    tracker = StateTracker(state)
    loop = asyncio.get_event_loop()

    interceptor = CoroInterceptor(tracker)
    loop.run_until_complete(interceptor(async_change_state(state)))
    assert tracker.history == [
        (tracker.on_suspend, State.Start),
        (tracker.on_reenter, State.Processed),
    ]
    assert state.get() == State.Finish
