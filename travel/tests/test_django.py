# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.library.python.tracing.django import Tracing


def test_init():
    tracer = object()
    t = Tracing(tracer)
    assert t.tracer is tracer


def test_traced_view():
    # should check span name
    pass


def test_traced_view_no_tracing():
    # should ensure that we will not fail if no tracer provided
    pass


class TestTracingMiddleware(object):
    def test_request_id_extracted(self):
        pass

    def test_global_context_added(self):
        pass
