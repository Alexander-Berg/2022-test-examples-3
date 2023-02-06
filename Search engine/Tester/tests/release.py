# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import random
import time

from search.resonance.tester.core import (
    EventLog, ExecuteContext, RemoteContext, ScriptExecutor, TestBase, InitContext
)
from search.resonance.tester.core.script.loadgen import Loadgen


class ReleaseTest(TestBase):
    def prepare(self, context: InitContext):
        self.config = context.args
        self.repeat = self.config.Repeat
        self.instances_down = self.config.InstancesDown
        self.startup_time = self.config.StartupTime

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

    def process(self, context: ExecuteContext, remote: RemoteContext, log: EventLog):
        loadgen = Loadgen(self.config.Loadgen)

        for _ in range(self.repeat):
            self.do_stop_backends(remote)
            self.do_start_backends(remote)
            self.do_reset_states(remote)

            loadgen_state = loadgen.start(context, remote, log)

            backends = list(remote.backends)
            random.shuffle(backends)

            used = 0
            while used < len(backends):
                activating = backends[used:used + self.instances_down]

                for backend in activating:
                    backend.api.backend_stop(backend.backend_id)

                time.sleep(self.startup_time)

                for backend in activating:
                    backend.api.backend_start(backend.backend_id)

                used += self.instances_down

            loadgen.stop(loadgen_state, log)

