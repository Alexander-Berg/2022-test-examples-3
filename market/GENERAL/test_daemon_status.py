#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase


class T(TestCase):
    def test_ping(self):
        response = self.zeus.request('ping')
        self.assertEqual('ok', response)

    def test_unknown_command(self):
        response = self.zeus.request('shakalaka')
        self.assertEqual('unknown command', response)
