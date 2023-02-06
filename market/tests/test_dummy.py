# -*- coding: utf-8 -*-
from __future__ import absolute_import


class TestDummy(object):
    def test_dummy(self, manifest):
        assert manifest.execute('dummy')

    def test_dummy_with_args(self, manifest):
        assert manifest.execute('dummy', ('foo', 'bar'))
