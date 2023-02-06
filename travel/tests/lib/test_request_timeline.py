# -*- coding: utf-8 -*-
from datetime import timedelta
from unittest import TestCase

from freezegun import freeze_time

from travel.avia.ticket_daemon_api.jsonrpc.lib.request_timer import RequestTimer


class TestRequestTimeline(TestCase):
    def setUp(self):
        self.freezer = freeze_time("2012-01-14 12:00:01")
        self.freeze_manager = self.freezer.start()
        self.timer = RequestTimer(10.0)

    def tearDown(self):
        self.freezer.stop()

    def test_available_timeout(self):
        assert 10.0 == self.timer.get_available_timeout()

        self.freeze_manager.tick(delta=timedelta(seconds=6))

        assert 4.0 == self.timer.get_available_timeout()

    def test_more_then_timeout(self):
        self.freeze_manager.tick(delta=timedelta(seconds=6))

        assert 2.0 == self.timer.get_available_timeout(2.0)

    def test_less_then_timeout(self):
        self.freeze_manager.tick(delta=timedelta(seconds=6))

        assert 4.0 == self.timer.get_available_timeout(5.0)

    def test_not_less_then_zero(self):
        self.freeze_manager.tick(delta=timedelta(seconds=11))

        assert 0.0 == self.timer.get_available_timeout(5.0)
