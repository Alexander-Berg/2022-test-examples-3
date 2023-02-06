# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import typing
import json

from collections import defaultdict
from threading import RLock

from search.martylib.core.date_utils import CompatibleDateTime

from search.resonance.pylib.backend_utils import BackendUnistat
from search.resonance.pylib.fetcher import FetcherClient

from search.resonance.tester.core.context import BackendInfo

from search.resonance.tester.proto.result_pb2 import TResult


class UnistatWatcher(object):
    fetcher: FetcherClient

    url_to_backends: typing.DefaultDict[str, typing.List[BackendInfo]]
    result: TResult
    result_lock: RLock

    def __init__(self, fetcher_path, backends: typing.List[BackendInfo], result: TResult, result_lock: RLock):
        self.url_to_backends = defaultdict(list)
        for backend in backends:
            self.url_to_backends['{}/unistat'.format(backend.admin_host)].append(backend)

        self.fetcher = FetcherClient(fetcher_path, tuple(self.url_to_backends.keys()), 100, self._on_unistat)
        self.result = result
        self.result_lock = result_lock

    def start(self):
        self.fetcher.start()

    def stop(self):
        self.fetcher.stop()

    def _on_unistat(self, time: CompatibleDateTime, url: str, payload: bytes):
        unistat_data = {
            k: v
            for k, v in json.loads(payload.decode())
        }
        for backend in self.url_to_backends[url]:
            unistat: BackendUnistat = backend.push_unistat(time, BackendUnistat.from_unistat(unistat_data, backend.backend_id))
            with self.result_lock:
                unistat_item = self.result.BackendUnistat[backend.backend_host].Items.add()
                unistat_item.Time = time.timestamp()
                unistat_item.Requests = unistat.requests
                unistat_item.Responses = unistat.responses
                unistat_item.ConnectionReset = unistat.connection_reset
                unistat_item.Failures = unistat.failures
                unistat_item.Timeouts = unistat.timeouts
                unistat_item.TooManyRequests = unistat.too_many_requests
                unistat_item.ResponseTimeMean = unistat.response_time.mean()
