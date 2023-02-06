#!/usr/bin/env python
# -*- coding: utf-8 -*-

from contextlib import closing
from datetime import datetime, timedelta
import json
import os
import sys
import unittest

import market.idx.pylibrary.event_log.event_log as event_log


DATE1 = datetime(2017, 3, 8)
DATE2 = datetime(2017, 3, 9)
DATE3 = datetime(2017, 3, 10)

EVENT1 = dict(
    ts=DATE1,
    process='foo',
    trans=event_log.TRANS_START,
    bar=123,
)
EVENT1_JSON = dict(
    ts='2017-03-08 00:00:00',
    process='foo',
    trans=event_log.TRANS_START,
    bar=123,
)

EVENT2 = dict(
    ts=DATE2,
    process='bar',
    trans=event_log.TRANS_WARN,
    bar=123,
)
EVENT2_JSON = dict(
    ts='2017-03-09 00:00:00',
    process='bar',
    trans=event_log.TRANS_WARN,
    bar=123,
)

EVENT3 = dict(
    ts=DATE3,
    process='лалала',
    trans=event_log.TRANS_SUCCESS,
    bar=123,
)
EVENT3_JSON = dict(
    ts='2017-03-10 00:00:00',
    process='лалала',
    trans=event_log.TRANS_SUCCESS,
    bar=123,
)


def tmp_output(name):
    try:
        import yatest
        return yatest.common.output_path(name)
    except:
        try:
            os.mkdir('tmp')
        except OSError:
            pass

        return os.path.join('tmp', name)


class TestException(Exception):
    pass


class BaseTestCase(unittest.TestCase):
    def assertJSONEquals(self, first, second):
        if isinstance(first, str):
            first = event_log.parse_json(first)
        if isinstance(second, str):
            second = event_log.parse_json(second)

        self.assertEquals(first, second)

    def assertJSONListsEqual(self, first, second):
        first = list(first)
        second = list(second)

        self.assertEquals(len(first), len(second))
        for first_line, second_line in zip(first, second):
            self.assertJSONEquals(first_line, second_line)


class TestEvent(BaseTestCase):
    """Tests the event class in isolation.
    """

    def test_parsing_ok(self):
        """Tests that parsing lines works.
        """
        self.assertEquals(
            EVENT1,
            event_log.parse_event(
                '{"ts": "2017-03-08 00:00:00", "process": "foo", "trans": "start", "bar": 123}'
            )
        )

        self.assertEquals(
            EVENT1,
            event_log.parse_event(
                '{"ts": "2017-03-08 00:00:00", "process": "foo", "trans": "start", "bar": 123}\n'
            )
        )

    def test_parsing_date_error(self):
        with self.assertRaises(ValueError):
            self.assertEquals(
                EVENT1,
                event_log.parse_event(
                    '{"process": "foo", "trans": "start", "bar": 123}'
                )
            )

        with self.assertRaises(ValueError):
            self.assertEquals(
                EVENT1,
                event_log.parse_event(
                    '{"ts": "2017-13-08 00:00:00", "process": "foo", "trans": "start", "bar": 123}'
                )
            )

    def test_serialization_ok(self):
        """Tests that valid events are serialized properly.
        """
        self.assertJSONEquals(EVENT1_JSON, event_log.serialize_event(EVENT1))
        self.assertJSONEquals(EVENT2_JSON, event_log.serialize_event(EVENT2))
        self.assertJSONEquals(EVENT3_JSON, event_log.serialize_event(EVENT3))

    def test_serialization_round_trip(self):
        """Tests that serialized events can then be parsed without error.
        """
        self.assertEquals(
            EVENT1,
            event_log.parse_event(event_log.serialize_event(EVENT1))
        )


class TestProcessEventLog(BaseTestCase):
    """Tests event log writer class.
    """
    def test_write(self):
        """Tests that events are written to files correctly.
        """
        log_path = tmp_output('test_write.csv')
        with closing(event_log.EventLogWriter(log_path)) as writer:
            writer.write_event(EVENT1)
            writer.write_event(EVENT2)

        self.assertJSONListsEqual(
            [EVENT1_JSON, EVENT2_JSON],
            open(log_path)
        )

    def test_buffering(self):
        """Tests that concurrent reads and writes happen immediately,
        i.e. the underlying files are line buffered.
        """
        log_path = tmp_output('test_buffering.csv')

        writer1 = event_log.EventLogWriter(log_path)
        writer1.write_event(EVENT1)

        # if EventLogWriter crops the log, it will happen here
        writer2 = event_log.EventLogWriter(log_path)
        writer2.write_event(EVENT2)
        writer1.write_event(EVENT3)

        # if writes are reordered due to buffering, it will happen here
        writer1.close()
        writer2.close()

        self.assertJSONListsEqual(
            [
                EVENT1_JSON,
                EVENT2_JSON,
                EVENT3_JSON,
            ],
            open(log_path)
        )


class TestEventLogReader(BaseTestCase):
    """Tests various event log reading functions.
    """
    def test_read(self):
        """Tests that event log files are read correctly.
        """
        original_stdout = sys.stdout

        log_path = tmp_output('test_read.csv')
        with open(log_path, 'w') as file_object:
            sys.stdout = file_object
            print(json.dumps(EVENT1_JSON))
            print(json.dumps(EVENT2_JSON))
            print(json.dumps(EVENT3_JSON))
            sys.stdout = original_stdout

        with open(log_path) as file_object:
            self.assertEquals(
                [EVENT1, EVENT2, EVENT3],
                list(event_log.read_log_events(file_object))
            )

    def test_read_malformed(self):
        """Tests that malformed lines are skipped without error.
        """
        original_stdout = sys.stdout

        bad_event2_str = 'garbage' + json.dumps(EVENT2_JSON)

        log_path = tmp_output('test_read_malformed.csv')
        with open(log_path, 'w') as file_object:
            sys.stdout = file_object
            print(json.dumps(EVENT1_JSON))
            print(bad_event2_str)
            print(json.dumps(EVENT3_JSON))
            sys.stdout = original_stdout

        with open(log_path) as file_object:
            self.assertEquals(
                [EVENT1, EVENT3],
                list(event_log.read_log_events(file_object))
            )

    def test_round_trip(self):
        """Tests that files written by EventLogWriter are read correctly.
        """
        log_path = tmp_output('test_round_trip.csv')
        writer = event_log.EventLogWriter(log_path)
        writer.write_event(EVENT1)
        writer.write_event(EVENT2)
        writer.write_event(EVENT3)
        writer.close()

        with open(log_path) as file_object:
            self.assertEquals(
                [EVENT1, EVENT2, EVENT3],
                list(event_log.read_log_events(file_object))
            )

    def test_recent_events(self):
        """Tests that load_recent_events correctly filters and sorts events.
        """
        self.maxDiff = None

        log1_path = tmp_output('test_recent_events1.csv')
        writer1 = event_log.EventLogWriter(log1_path)
        writer1.write_event(EVENT1)
        writer1.write_event(EVENT3)
        writer1.close()

        log2_path = tmp_output('test_recent_events2.csv')
        writer2 = event_log.EventLogWriter(log2_path)
        writer2.write_event(EVENT2)
        writer2.close()

        with open(log1_path) as log1_file, open(log2_path) as log2_file:
            self.assertEquals(
                [EVENT2, EVENT3],
                event_log.load_recent_events(
                    [log1_file, log2_file],
                    since_ts=DATE2,
                )
            )


class TestEventLogContexManager(BaseTestCase):
    """Tests that event_log behaves correctly.
    """
    def setUp(self):
        self.log_path = tmp_output('event-log.csv')

    def tearDown(self):
        try:
            os.unlink(self.log_path)
        except OSError:
            pass

    def test_empty(self):
        """Tests that if we do nothing, it's just start and success.
        """
        with event_log.event_log(self.log_path, dict(bar=123)):
            pass

        self.check_log(
            [
                event_log.TRANS_START,
                event_log.TRANS_SUCCESS,
            ],
            dict(bar=123)
        )

    def test_warn(self):
        """Tests that warning don't affect the general behavior.
        """
        with event_log.event_log(self.log_path) as elog:
            elog.warn()

        self.check_log(
            [
                event_log.TRANS_START,
                event_log.TRANS_WARN,
                event_log.TRANS_SUCCESS,
            ],
            {}
        )

    def test_raise(self):
        """Tests that an exception raised causes failure instead of success.
        """
        with self.assertRaises(TestException):
            with event_log.event_log(
                self.log_path,
                args=dict(exception_type='TestException')  # a crutch
            ):
                raise TestException()

        self.check_log(
            [
                event_log.TRANS_START,
                event_log.TRANS_FAIL,
            ],
            dict(exception_type='TestException')
        )

    def test_success_manual(self):
        """Tests that succeeding manually prevents success on exit.
        """
        with event_log.event_log(self.log_path) as elog:
            elog.success()

        self.check_log(
            [
                event_log.TRANS_START,
                event_log.TRANS_SUCCESS,
            ],
            {}
        )

    def test_success_raise(self):
        """Tests that succeeding manually prevents failure on exception.
        """
        with self.assertRaises(TestException):
            with event_log.event_log(self.log_path) as elog:
                elog.success()
                raise TestException()

        self.check_log(
            [
                event_log.TRANS_START,
                event_log.TRANS_SUCCESS,
            ],
            {}
        )

    def test_fail(self):
        """Tests that failing manually prevents success on exit.
        """
        with event_log.event_log(self.log_path) as elog:
            elog.fail()

        self.check_log(
            [
                event_log.TRANS_START,
                event_log.TRANS_FAIL,
            ],
            {}
        )

    def test_fail_raise(self):
        """Tests that failing manually prevents failure on exception.
        """
        with self.assertRaises(TestException):
            with event_log.event_log(self.log_path) as elog:
                elog.fail()
                raise TestException()

        self.check_log(
            [
                event_log.TRANS_START,
                event_log.TRANS_FAIL,
            ],
            {}
        )

    def test_args(self):
        """Tests that arguments are merged in correctly and that
        failures on exceptions tell us about the exception's exact type.
        """
        def strip_ts(event):
            event = dict(event)
            del event['ts']
            return event

        with self.assertRaises(TestException):
            with event_log.event_log(
                self.log_path,
                args=dict(a=1),
                start_args=dict(iam='start')
            ) as elog:
                elog.warn(iam='warn')
                raise TestException()

        with open(self.log_path) as log_file:
            self.assertEquals(
                [
                    dict(a=1, iam='start', trans='start'),
                    dict(a=1, iam='warn', trans='warn'),
                    dict(a=1, exception_type='TestException', trans='fail'),
                ],
                [
                    strip_ts(event)
                    for event in event_log.read_log_events(log_file)
                ]
            )

    def check_log(self, transitions, expected_events):
        now = datetime.now()
        with open(self.log_path) as log_file:
            events = list(event_log.read_log_events(log_file))
            self.assertEquals(len(transitions), len(events))

            for event, transition in zip(events, transitions):
                actual_events = dict(event)
                event_ts = actual_events['ts']
                event_trans = actual_events['trans']
                del actual_events['ts']
                del actual_events['trans']

                self.assertTrue((now - event_ts) < timedelta(hours=1))
                self.assertEquals(transition, event_trans)
                self.assertEquals(expected_events, actual_events)


if __name__ == '__main__':
    unittest.main()
