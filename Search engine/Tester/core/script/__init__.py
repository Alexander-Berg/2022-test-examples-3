# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import time
import typing

from google.protobuf import text_format

from search.resonance.tester.core.context import ExecuteContext, RemoteContext, InitContext
from search.resonance.tester.core.event import EventLog
from search.resonance.tester.core.selector import Selector
from search.resonance.proto.backend_pb2 import TBackend
from search.resonance.tester.proto.script_pb2 import TScript

from search.resonance.tester.core.script.backends import BackendsSetupper
from search.resonance.tester.core.script.loadgen import Loadgen


class ScriptExecutor(object):
    config: TScript

    sleep_time: float
    repeat: int

    backends: BackendsSetupper
    loadgen: Loadgen
    reset_states: bool
    stop_backends: bool
    start_backends: bool
    backends_selector: typing.List[Selector]
    subscripts: tuple

    def __init__(self, context: InitContext, config: TScript):
        self.config = config

        self.sleep_time = self.config.SleepTime
        self.repeat = max(self.config.Repeat, 1)

        backends = list(self.config.Backend)
        for file_name in self.config.BackendFile:
            with open(context.get_path(file_name)) as f:
                backends.append(text_format.Parse(f.read(), TBackend()))
        if backends:
            self.backends = BackendsSetupper(backends)
        else:
            self.backends = None

        self.backends_selector = tuple(
            Selector(selector)
            for selector in self.config.BackendsSelector
        )

        if self.config.HasField('Loadgen'):
            self.loadgen = Loadgen(self.config.Loadgen)
        else:
            self.loadgen = None

        self.reset_states = self.config.ResetStates
        self.stop_backends = self.config.StopBackends
        self.start_backends = self.config.StartBackends

        included_subscripts = []
        for include_path in self.config.Include:
            with open(context.get_path(include_path)) as f:
                script_config = text_format.Parse(f.read(), TScript())
                included_subscripts.append(ScriptExecutor(context, script_config))

        self.subscripts = tuple(included_subscripts) + tuple(
            ScriptExecutor(context, subscript)
            for subscript in self.config.Script
        )

    @staticmethod
    def do_reset_states(remote: RemoteContext):
        for backend in remote.backends:
            backend.api.state_reset(backend.backend_id)

    @staticmethod
    def do_stop_backends(remote: RemoteContext):
        for backend in remote.backends:
            backend.api.backend_stop(backend.backend_id)

    @staticmethod
    def do_start_backends(remote: RemoteContext):
        for backend in remote.backends:
            backend.api.backend_start(backend.backend_id)

    def execute(self, context: ExecuteContext, remote: RemoteContext, log: EventLog):
        if self.backends_selector:
            backends = remote.backends
            for selector in self.backends_selector:
                backends = selector.select(backends)
            remote = remote.sub_context(backends=tuple(backends))

        for i in range(self.repeat):
            if self.backends:
                self.backends.setup_backends(remote.backends)

            if self.stop_backends:
                self.do_stop_backends(remote)
            if self.start_backends:
                self.do_start_backends(remote)
            if self.reset_states:
                self.do_reset_states(remote)

            loadgen_state = self.loadgen.start(context, remote, log) if self.loadgen else None

            for subscript in self.subscripts:
                with log.scope('subscript') as subscript_log:
                    subscript.execute(context, remote, subscript_log)

            if self.sleep_time:
                time.sleep(self.sleep_time)

            if loadgen_state:
                self.loadgen.stop(loadgen_state, log)
