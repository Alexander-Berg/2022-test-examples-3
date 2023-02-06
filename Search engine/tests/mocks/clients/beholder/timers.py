from enum import Enum
from typing import Dict


REPEATED_TIMER_KEYS = (
    'set_coordinator',
    'set_component',
    'start_status',
    'set_logger',
    'logger_not_set',
    'periodic_status',
    'final_status',
)


class TimerAction(Enum):
    NOT_PASSED = 'NOT_PASSED'
    STARTED = 'STARTED'
    RESET = 'RESET'
    STOPPED = 'STOPPED'


class TTimerKey:
    def __init__(self, timer_key: str, proto_id: int):
        self._key = f'{timer_key}_{proto_id}'

    @property
    def key(self) -> str:
        return self._key

    def __hash__(self):
        return hash(self._key)

    def __eq__(self, other):
        if hash(self) == hash(other):
            return True
        else:
            return self.key == other.key


class MockBeholderTimers:
    def __init__(self, *args, **kwargs):
        self._timer_logs: Dict[TTimerKey, TimerAction] = dict()

    # timers
    async def start_timer(self, key: str, proto_id: int):
        return self._dispatch_timer(key, proto_id, TimerAction.STARTED)

    async def stop_timer(self, key: str, proto_id: int):
        return self._dispatch_timer(key, proto_id, TimerAction.STARTED)

    async def reset_timer(self, key: str, proto_id: int):
        return self._dispatch_timer(key, proto_id, TimerAction.RESET)

    def assert_timer_started(self, key: str, proto_id: int):
        return self._assert_timer(key, proto_id, TimerAction.STARTED)

    def assert_timer_stopped(self, key: str, proto_id: int):
        return self._assert_timer(key, proto_id, TimerAction.STOPPED)

    def assert_timer_reset(self, key: str, proto_id: int):
        return self._assert_timer(key, proto_id, TimerAction.RESET)

    def _dispatch_timer(self, key: str, proto_id: int, action: TimerAction):
        """Set last timer action to logs and returns structure of TimerInfo
        https://a.yandex-team.ru/arc/trunk/arcadia/search/beholder/proto/structures/protocols.proto?rev=r8027847#L128
        """
        self._timer_logs[TTimerKey(key, proto_id)] = action

        result = {'is_repeated': key in REPEATED_TIMER_KEYS, 'time_left': 0}
        if action == TimerAction.STOPPED:
            result['status'] = 'STOPPED'
        else:
            result['status'] = 'RUNNING'

        return result

    def _assert_timer(self, key: str, proto_id: int, action: TimerAction = TimerAction.NOT_PASSED):
        timer_key = TTimerKey(key, proto_id)
        assert self._timer_logs.get(timer_key, TimerAction.NOT_PASSED) == action
