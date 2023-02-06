# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from threading import Thread

import mock
import pytest
import pytz
from freezegun import freeze_time

from travel.rasp.library.python.common23.tester.utils.request import clean_context  # noqa
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting
from travel.rasp.library.python.common23.date.date_const import MSK_TZ
from travel.rasp.library.python.common23.date.environment import (
    get_time_context, set_time_context, delete_time_context, now, now_aware, now_utc
)


class ContextThread(Thread):
    def __init__(self, *args, **kwargs):
        self.value = kwargs.pop('value')
        self.tmp = kwargs.pop('tmp')
        Thread.__init__(self, *args, **kwargs)

    def run(self):
        set_time_context(self.value)
        self.tmp[self.getName()] = get_time_context()
        delete_time_context()


@pytest.mark.usefixtures('clean_context')
class TestContext(object):
    def test_context(self):
        time_by_thread = {}
        thread_1 = ContextThread(value=mock.sentinel.value_1, tmp=time_by_thread)
        thread_2 = ContextThread(value=mock.sentinel.value_2, tmp=time_by_thread)
        thread_1.start()
        thread_2.start()
        thread_1.join()
        thread_2.join()

        assert time_by_thread[thread_1.getName()] is mock.sentinel.value_1
        assert time_by_thread[thread_2.getName()] is mock.sentinel.value_2

    def test_replace_now(self):
        with mock.patch('{}.set_time_context'.format(TestContext.__module__),
                        side_effect=set_time_context) as m_replace_now:
            set_time_context(mock.sentinel.value_1)
            m_replace_now.assert_called_once_with(mock.sentinel.value_1)
            assert get_time_context() == mock.sentinel.value_1

    def test_delete_now(self):
        set_time_context(mock.sentinel.value_1)
        assert get_time_context() is mock.sentinel.value_1
        delete_time_context()
        assert get_time_context() is None


@pytest.mark.usefixtures('clean_context')
class TestNow(object):
    def test_now(self):
        mock_value = datetime(2001, 1, 1, 10)
        with mock.patch('travel.rasp.library.python.common23.date.environment.ultimate_now', return_value=mock_value) as m_ultimate_now:
            assert now() == mock_value
            m_ultimate_now.assert_called_once_with()

        mock_value = datetime(2010, 2, 3, 15)
        with replace_setting('ENVIRONMENT_NOW', mock_value):
            assert now() == mock_value

        naive_msk_time = datetime(2016, 10, 5, 12)
        set_time_context(naive_msk_time)
        assert now() == naive_msk_time


@pytest.mark.usefixtures('clean_context')
class TestNowAware(object):
    def test_now(self):
        mock_value = datetime(2001, 1, 1, 10)
        with freeze_time(mock_value):
            assert now_aware() == pytz.utc.localize(mock_value).astimezone(MSK_TZ)

        mock_value = datetime(2010, 2, 3, 15)
        with replace_setting('ENVIRONMENT_NOW', mock_value):
            assert now_aware() == MSK_TZ.localize(mock_value)

        naive_msk_time = datetime(2016, 10, 5, 12)
        set_time_context(naive_msk_time)
        assert now_aware() == MSK_TZ.localize(naive_msk_time)


@pytest.mark.usefixtures('clean_context')
def test_now_utc():
    mock_value = datetime(2001, 1, 1, 10)
    with freeze_time(mock_value):
        assert now_utc() == pytz.utc.localize(mock_value).replace(tzinfo=None)
        assert now_utc(aware=True) == pytz.utc.localize(mock_value)

    mock_value = datetime(2010, 2, 3, 15)
    with replace_setting('ENVIRONMENT_NOW', mock_value):
        assert now_utc() == MSK_TZ.localize(mock_value).astimezone(pytz.UTC).replace(tzinfo=None)
        assert now_utc(aware=True) == MSK_TZ.localize(mock_value).astimezone(pytz.UTC)

    naive_msk_time = datetime(2016, 10, 5, 12)
    set_time_context(naive_msk_time)
    assert now_utc() == MSK_TZ.localize(naive_msk_time).astimezone(pytz.UTC).replace(tzinfo=None)
    assert now_utc(aware=True) == MSK_TZ.localize(naive_msk_time).astimezone(pytz.UTC)
