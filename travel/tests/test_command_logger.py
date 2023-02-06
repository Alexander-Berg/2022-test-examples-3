# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock

from travel.rasp.library.python.common23.db.mongo.command_logger import CommandLogger
from travel.rasp.library.python.common23.tester.helpers.caplog import filter_out_runtest_call_records


class TestCommandLogger(object):
    def test_succeeded(self, caplog):
        caplog.set_level('DEBUG')
        CommandLogger.get_cumulative_time_and_reset()
        listener = CommandLogger()
        listener.succeeded(mock.Mock(command_name='mongoCommand', duration_micros=100500))

        records = filter_out_runtest_call_records(caplog.records)
        assert len(records) == 1
        record = records[0]
        assert record.message.startswith('mongoCommand succeeded using ')
        assert record.command_name == 'mongoCommand'
        assert record.duration == 0.1005
        assert CommandLogger.get_cumulative_time_and_reset() == .1005

    def test_failed(self, caplog):
        caplog.set_level('DEBUG')
        CommandLogger.get_cumulative_time_and_reset()
        listener = CommandLogger()
        listener.failed(mock.Mock(command_name='mongoCommand', duration_micros=100500))

        records = filter_out_runtest_call_records(caplog.records)
        assert len(records) == 1
        record = records[0]
        assert record.message.startswith('mongoCommand failed using ')
        assert record.command_name == 'mongoCommand'
        assert record.duration == 0.1005
        assert CommandLogger.get_cumulative_time_and_reset() == .1005
