# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import typing

from search.resonance.proto.backend_pb2 import TBackend
from search.resonance.tester.core.context import BackendInfo as BackendContext


class BackendsSetupper(object):
    backends: typing.List[TBackend]

    def __init__(self, backends):
        self.backends = list(backends)

    def setup_backends(self, backends: typing.List[BackendContext]):
        if self.backends:
            for i, backend in enumerate(backends):
                backend.api.backend_set(backend.backend_id, self.backends[i % len(self.backends)])
