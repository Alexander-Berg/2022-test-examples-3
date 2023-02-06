# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import math
import os
import typing

from search.martylib.core.date_utils import CompatibleDateTime

from search.resonance.pylib.backend_utils.client import ResonanceClient
from search.resonance.pylib.backend_utils.unistat import BackendUnistat
from search.resonance.pylib.loadgen.client import LoadgenClientBase, LoadgenClientLocal


class InitContext(object):
    def __init__(self, root_path: str, args):
        self.root_path = root_path
        self.args = args

    def get_path(self, path: str):
        return os.path.join(self.root_path, path)

    def has_root(self):
        return bool(self.root_path)


class BackendInfo(object):
    admin_host: str
    backend_id: str
    backend_host: str
    api: ResonanceClient

    unistat_raw_log: typing.List[typing.Tuple[CompatibleDateTime, BackendUnistat]]
    unistat_log: typing.List[typing.Tuple[CompatibleDateTime, BackendUnistat]]

    def __init__(self, admin_host: str, backend_id: str, backend_host: str):
        self.admin_host = admin_host
        self.backend_id = backend_id
        self.backend_host = backend_host
        self.api = ResonanceClient(base_url=admin_host)
        self.unistat_raw_log = []
        self.unistat_log = []

    def push_unistat(self, time: CompatibleDateTime, unistat: BackendUnistat):
        if self.unistat_raw_log and self.unistat_raw_log[0][0] > time:
            raise ValueError('time')

        self.unistat_raw_log.append((time, unistat))
        nearest_time, nearest_unistat = self.unistat_raw_log[-1]
        for i in range(len(self.unistat_raw_log)):
            next_time, next_unistat = self.unistat_raw_log[-1 - i]
            prev_delta = math.fabs(time.timestamp() - nearest_time.timestamp() - 1)
            next_delta = math.fabs(time.timestamp() - next_time.timestamp() - 1)
            if next_delta <= prev_delta:
                nearest_time, nearest_unistat = next_time, next_unistat
            elif next_delta > 1.1:
                break

        result_unistat = unistat - nearest_unistat
        self.unistat_log.append((time, result_unistat))
        return result_unistat


class RemoteContext(object):
    def __init__(self, upstreams: typing.Tuple[str], backends: typing.Tuple[BackendInfo]):
        self.upstreams = upstreams
        self.backends = backends

    def sub_context(self, upstreams: typing.Tuple[str] = None, backends: typing.Tuple[BackendInfo] = None):
        if upstreams is None:
            upstreams = self.upstreams
        if backends is None:
            backends = self.backends

        return RemoteContext(upstreams, backends)


class ExecuteContext(object):
    def __init__(self, loadgen_path: str):
        self.loadgen_path = loadgen_path
        self.active_loadgens = []

    def create_loadgen(self) -> LoadgenClientBase:
        loadgen = LoadgenClientLocal(self.loadgen_path)
        self.active_loadgens.append(loadgen)
        return loadgen

    def dispose(self):
        for loadgen in self.active_loadgens:
            loadgen.stop()
