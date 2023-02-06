from .timers import TimerAction, MockBeholderTimers
from .incidents import MockBeholderIncidents


class MockBeholder(MockBeholderTimers, MockBeholderIncidents):
    def __init__(self, *args, **kwargs):
        MockBeholderTimers.__init__(self, *args, **kwargs)
        MockBeholderIncidents.__init__(self, *args, **kwargs)

    async def start_protocol(self, context, proto):
        pass
