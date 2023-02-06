# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import logging
import time
from datetime import datetime, timedelta

import mock
import pytest
from django.db import connections

from common.models.timestamp import Timestamp
from travel.rasp.wizards.wizard_lib.utils.timestamp_checker import TimestampChecker

pytestmark = pytest.mark.dbignore  # не используем транзакции потому что работаем с базой в двух потоках


class DummyChecker(object):
    def __init__(self, timestamp):
        self.callback = mock.Mock()
        self.timestamp = timestamp
        self.checker = TimestampChecker(code=timestamp.code, callback=self.callback, interval=1e-3)

    def wait(self):
        time.sleep(1e-1)

    def start(self):
        self.checker.start()
        self.wait()  # чтобы подхватилось начальное значение

    def update_timestamp(self):
        timestamp = self.timestamp
        previous_value = timestamp.value
        timestamp.value += timedelta(1)
        timestamp.save()
        return previous_value, timestamp.value

    def stop(self):
        self.checker.stop()
        self.checker.join()


@pytest.fixture
def dummy_timestamp():
    timestamp = Timestamp.objects.create(code='dummy', value=datetime(2000, 1, 1))
    try:
        yield timestamp
    finally:
        timestamp.delete()


@pytest.fixture
def dummy_checker(dummy_timestamp):
    checker = DummyChecker(dummy_timestamp)
    checker.start()
    try:
        yield checker
    finally:
        checker.stop()


class TestTimestampChecker(object):
    def test_update_timestamp(self, dummy_checker):
        previous_value, current_value = dummy_checker.update_timestamp()
        dummy_checker.wait()

        dummy_checker.callback.assert_called_once_with(previous_value, current_value)

    def test_on_db_switched(self, dummy_checker):
        with mock.patch.object(connections, 'close_all', autospec=True) as m_close_all:
            dummy_checker.checker.on_db_switched()
            dummy_checker.wait()

            m_close_all.assert_called_once_with()

            m_close_all.reset_mock()
            dummy_checker.wait()

            assert not m_close_all.called

    def test_exception_handling(self, dummy_checker, caplog):
        with caplog.at_level(logging.INFO, logger='wizard_lib.utils.timestamp_checker'):
            dummy_checker.callback.side_effect = RuntimeError()
            dummy_checker.update_timestamp()
            dummy_checker.wait()

        checker_log = tuple(record for record in caplog.records if record.name == 'wizard_lib.utils.timestamp_checker')
        assert checker_log
        assert all(
            record.levelname == 'ERROR' and record.message == 'Exception while checking timestamp'
            for record in checker_log
        )
