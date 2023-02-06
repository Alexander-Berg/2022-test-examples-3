# -*- coding: utf-8 -*-

from datetime import datetime
import unittest

from market.idx.pylibrary.mindexer_core.qbid.yt_monitoring import (
    do_check_status,
    do_check_stalled,
)
import market.idx.pylibrary.event_log.event_log as event_log


DATE1 = datetime(2017, 3, 8)
DATE2 = datetime(2017, 3, 9)
DATE3 = datetime(2017, 3, 10)


class TestQBidsYtMonitoringStatus(unittest.TestCase):
    def test_ok(self):
        events = [
            dict(ts=DATE1, trans=event_log.TRANS_FAIL),
            dict(ts=DATE2, trans=event_log.TRANS_START),
            dict(ts=DATE3, trans=event_log.TRANS_SUCCESS),
        ]

        self.assertEqual('0;OK', do_check_status(events, DATE2))

    def test_warn(self):
        events = [
            dict(ts=DATE1, trans=event_log.TRANS_WARN),
            dict(ts=DATE2, trans=event_log.TRANS_START),
        ]

        self.assertEqual('1;', do_check_status(events, DATE1)[:2])
        self.assertNotEqual('1;', do_check_status(events, DATE2)[:2])

    def test_fail(self):
        events = [
            dict(ts=DATE1, trans=event_log.TRANS_FAIL),
            dict(ts=DATE2, trans=event_log.TRANS_START),
        ]

        self.assertEqual('2;', do_check_status(events, DATE1)[:2])
        self.assertNotEqual('2;', do_check_status(events, DATE2)[:2])

    def test_horror(self):
        msg = do_check_status(1, 2)
        self.assertEqual('2;', msg[:2])
        self.assertTrue('TypeError' in msg)

    def test_last_ok(self):
        # За последние 8 часов последняя запись - удчная. Мониторинг должен быть ok
        events = [
            dict(ts=datetime(1984, 10, 17, 10, 0, 0), trans=event_log.TRANS_FAIL),
            dict(ts=datetime(1984, 10, 17, 11, 20, 0), trans=event_log.TRANS_SUCCESS),
            dict(ts=datetime(1984, 10, 17, 11, 10, 0), trans=event_log.TRANS_START)
        ]

        self.assertEqual('0;OK', do_check_status(events, datetime(1984, 10, 17, 0, 0, 0)))

    def test_master_no_events_fail(self):
        events = []
        self.assertEqual('2;No recent log records since 1984-10-17 00:00:00', do_check_status(events, datetime(1984, 10, 17, 0, 0, 0)))

    def test_not_a_master_ok(self):
        events = []
        self.assertEqual('0;OK', do_check_status(events, datetime(1984, 10, 17, 0, 0, 0), master=False))


class TestQBidsYtMonitoringStalled(unittest.TestCase):
    def test_stalled(self):
        events = [
            dict(
                ts=DATE1,
                trans=event_log.TRANS_SUCCESS,
                delta_generation=None,
            ),
            dict(
                ts=DATE2,
                trans=event_log.TRANS_SUCCESS,
                delta_generation='foo',
            ),
            dict(
                ts=DATE3,
                trans=event_log.TRANS_WARN,
                delta_generation=None,
            ),
        ]

        self.assertEqual('0;OK', do_check_stalled(events, DATE1))

        msg = do_check_stalled(events, DATE3)
        self.assertEqual('2;', msg[:2])
        self.assertTrue('Main' in msg)

        self.assertEqual(
            '0;OK',
            do_check_stalled(events, DATE1),
        )

        msg = do_check_stalled(events, DATE3)
        self.assertEqual('2;', msg[:2])
        self.assertTrue('Main' in msg)

    def test_horror(self):
        msg = do_check_stalled(1, 2)
        self.assertEqual('2;', msg[:2])
        self.assertTrue('TypeError' in msg)


if __name__ == '__main__':
    unittest.main()
