from __future__ import absolute_import

import random
import threading

import pytest

from .. import statistics
from ..types import statistics as ctst


class CustomHandler(statistics.SignalHandler):
    types = ("anything_goes", "test_signal")

    def handle(self, signal_type, signals):
        pass


class GeneralHandler(statistics.SignalHandler):
    types = (ctst.ALL_SIGNALS,)

    def handle(self, signal_type, signals):
        pass


class TestSignaler(object):
    def test__signaler(self, tests_dir):
        values = []

        def handle(_, __, ss):
            with threading.RLock():
                values.extend(ss)

        for class_ in (CustomHandler, GeneralHandler):
            class_.handle = handle

        with pytest.raises(AssertionError):
            statistics.Signaler().push(dict(
                utter=1,
                nonsense=2
            ))

        statistics.Signaler(
            CustomHandler(),
            GeneralHandler(),
        )

        sigcount = 100
        stypes = [
            random.choice(CustomHandler.types + ("abcdef", "nonsense"))
            for _ in xrange(sigcount)
        ]
        custom_count = len(filter(lambda s: s in CustomHandler.types, stypes))

        for st in stypes:
            statistics.Signaler().push(dict(
                type=st,
                hitcount=1234,
                omg="wtf",
            ))
        statistics.Signaler().wait()
        assert len(values) == sigcount + custom_count
