import logging

from .base_stub import BaseStub

log = logging.getLogger(__name__)


class StubsRunner:
    def __init__(self):
        self._running = []

    async def __aenter__(self):
        for stub in self.__dict__.values():
            if not isinstance(stub, BaseStub):
                continue
            try:
                await stub.start()
                self._running.append(stub)
            except Exception as e:
                log.error("Got exc on stub.start(): %s, %s", stub, e)
                raise
        return self

    async def __aexit__(self, *args, **kwargs):
        for stub in self._running:
            try:
                await stub.stop()
            except Exception as e:
                log.error("Got exc on stub.stop(): %s, %s", stub, e)
        self._running.clear()
