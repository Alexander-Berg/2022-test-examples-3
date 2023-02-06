# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import numpy as np
import time
import typing

from search.martylib.core.date_utils import now

from search.resonance.tester.proto.script_pb2 import TSelectorConfig
from search.resonance.tester.proto.weight_check_pb2 import TWeightCheck

from search.resonance.tester.core import (
    EventLog, ExecuteContext, Selector, RemoteContext, BackendInfo
)
from search.resonance.tester.core.script.backends import BackendsSetupper
from search.resonance.tester.core.script.loadgen import Loadgen


class WeightCheckCase(object):
    class SubsetConfig(object):
        selector: Selector
        setupper: BackendsSetupper

        target_weight: float

    subsets: typing.List[SubsetConfig]
    loadgen: Loadgen
    repeat: int
    timeout: float
    precision: float
    passed_interval: float

    def __init__(self, config: TWeightCheck):
        self.subsets = []
        self.loadgen = Loadgen(config.Loadgen)
        self.repeat = config.Repeat
        if self.repeat < 1:
            self.repeat = 1
        self.timeout = config.Timeout
        self.precision = config.Precision
        self.passed_interval = config.PassedInterval

        total_weights = 0.0
        for backend_set in config.Backends:
            total_weights += backend_set.SubsetWeight
        if total_weights <= 0.0001:
            total_weights = 0
            for backend_set in config.Backends:
                backend_set.SubsetWeight = 1
                total_weights += 1
        for backend_set in config.Backends:
            subset = WeightCheckCase.SubsetConfig()

            subset.target_weight = backend_set.BalancerWeight

            selector_config = TSelectorConfig()
            selector_config.SamplePart = backend_set.SubsetWeight / total_weights
            total_weights -= backend_set.SubsetWeight
            subset.selector = Selector(selector_config)

            subset.setupper = BackendsSetupper((backend_set.Backend,))

            self.subsets.append(subset)

    def process_step(self, remote: RemoteContext, log: EventLog):
        current_weights = []
        target_weights = []
        backends = []

        used_backends = set()
        unused_backends = list(remote.backends)
        for i in range(len(self.subsets)):
            subset = self.subsets[i]

            subset_backends: typing.List[BackendInfo] = subset.selector.select(unused_backends)
            subset.setupper.setup_backends(subset_backends)
            log.backends_group(str(i), subset_backends)

            for backend in subset_backends:
                current_weights.append(0.0)
                target_weights.append(subset.target_weight)
                backends.append(backend)
                used_backends.add(backend.backend_host)
            unused_backends = [
                backend
                for backend in unused_backends
                if backend.backend_host not in used_backends
            ]
        start_time = now().timestamp()
        passed_time = -1
        passed_flag = False
        while not self.timeout or now().timestamp() - start_time < self.timeout:
            error = False
            for i in range(len(backends)):
                if not backends[i].unistat_log:
                    error = True
                    break
                current_weights[i] = backends[i].unistat_log[-1][1].requests
            if not error and np.linalg.norm(current_weights, 1) > 0.1:
                current = np.array(current_weights) / np.linalg.norm(current_weights, 1)
                target = np.array(target_weights) / np.linalg.norm(target_weights, 1)
                mean_delta = np.mean(np.abs(current - target))
                if mean_delta > self.precision:
                    passed_time = -1
                elif passed_time < 0:
                    passed_time = now().timestamp()
                if passed_time > 0 and now().timestamp() - passed_time > self.passed_interval:
                    passed_flag = True
                    break
            time.sleep(0.1)
        log.check('weight_check_passed', passed_flag)
        if passed_flag:
            log.metric('weight_check_pass_duration', passed_time - start_time)

    def process(self, context: ExecuteContext, remote: RemoteContext, log: EventLog):
        loadgen = self.loadgen.start(context, remote, log)
        for backend in remote.backends:
            backend.api.backend_stop(backend.backend_id)
            backend.api.backend_start(backend.backend_id)
        for i in range(self.repeat):
            with log.scope('step {}'.format(i)) as step_log:
                self.process_step(remote, step_log)
        self.loadgen.stop(loadgen, log)
