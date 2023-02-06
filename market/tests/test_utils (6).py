# -*- coding: utf-8 -*-
from __future__ import absolute_import

from utils import get_iport


class TestUtils(object):
    def test_get_iport_with_env(self, monkeypatch):
        monkeypatch.setenv('BSCONFIG_IPORT', '8080')
        assert get_iport() == 8080

    def test_get_iport(self):
        assert get_iport() == 0
