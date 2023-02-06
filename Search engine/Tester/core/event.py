# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import typing

from contextlib import contextmanager
from threading import RLock

from search.martylib.core.date_utils import now
from search.resonance.pylib.loadgen import LoadgenClientBase

from search.resonance.tester.core.context import BackendInfo

from search.resonance.tester.proto.result_pb2 import (
    TEvent, TEventPayload,
)


class EventLog:
    root_event: TEvent
    lock: RLock

    def __init__(self, root_event: TEvent, lock: RLock):
        self.root_event = root_event
        self.lock = lock
        self.begin()

    def log(self, payload: TEventPayload):
        with self.lock:
            event = self.root_event.Scope.add()
            event.Begin = event.End = now().timestamp()
            event.Payload.CopyFrom(payload)
            return event

    def begin(self):
        with self.lock:
            self.root_event.Begin = self.root_event.End = now().timestamp()

    def end(self):
        with self.lock:
            self.root_event.End = now().timestamp()

    @contextmanager
    def scope_event(self, event: TEvent):
        event_log: EventLog = None
        try:
            event_log = EventLog(event, self.lock)
            yield event_log
        finally:
            event_log.end()

    @contextmanager
    def scope_payload(self, payload=None):
        with self.scope_event(self.log(payload)) as event_log:
            yield event_log

    @contextmanager
    def scope(self, scope_type: str):
        payload = TEventPayload()
        payload.Type = scope_type
        with self.scope_payload(payload) as event_log:
            yield event_log

    def loadgen_start(self, id: str, config: LoadgenClientBase.Config):
        payload = TEventPayload()
        payload.LoadgenStart.Id = id
        payload.LoadgenStart.Threads = config.threads
        payload.LoadgenStart.Connections = config.connections
        payload.LoadgenStart.Rps = config.rps
        payload.LoadgenStart.Host = config.host
        payload.LoadgenStart.Path.extend(config.paths)
        payload.LoadgenStart.Header.extend(config.headers)
        return self.log(payload)

    def loadgen_stop(self, id: str):
        payload = TEventPayload()
        payload.LoadgenStop.Id = id
        return self.log(payload)

    def check(self, name: str, passed: bool):
        payload = TEventPayload()
        payload.Check.Name = name
        payload.Check.Passed = passed
        return self.log(payload)

    def metric(self, name: str, value: float):
        payload = TEventPayload()
        payload.Metric.Name = name
        payload.Metric.Value = value
        return self.log(payload)

    def info(self, message: str):
        payload = TEventPayload()
        payload.Info = message
        return self.log(payload)

    def error(self, message: str):
        payload = TEventPayload()
        payload.Error = message
        return self.log(payload)

    def backends_group(self, group: str, backends: typing.List[BackendInfo]):
        payload = TEventPayload()
        payload.BackendsGroup.Group = group
        payload.BackendsGroup.Backends.extend((backend.backend_host for backend in backends))
        return self.log(payload)
