# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals


class TestConfig(object):
    def test_create_tracer(self):
        # should create correct Tracer instance
        # instance should counts as instance of jaeger_client.Tracer
        pass


class TestSpan(object):
    def test_cast(self):
        # no comment
        pass

    def test_set_tag(self):
        pass

    def test_finish_is_sampled(self):
        pass

    def test_finish_is_not_sampled_but_sampling_long_spans(self):
        pass

    def test_finish_is_not_sampled_and_not_sampling_long_spans(self):
        pass
