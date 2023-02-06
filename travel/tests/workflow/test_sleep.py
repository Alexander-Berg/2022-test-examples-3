# coding: utf8
from datetime import datetime

import pytest
from mock import patch, sentinel
from mongoengine import Document, DateTimeField, DynamicField

import common
from common.workflow.sleep import Sleep, SleepEvents, SleepTill
from common.workflow.tests_utils.process import process_state_action


class MyDocument(Document):
    max_pending_till = DateTimeField()
    process = DynamicField()


@pytest.mark.mongouser
def test_sleep():
    doc = MyDocument.objects.create()
    seconds = sentinel.seconds

    with patch.object(common.workflow.sleep, 'time') as m_time:
        event, order = process_state_action({'action': Sleep, 'args': (seconds,)}, (SleepEvents.OK,), doc)

        m_time.sleep.assert_called_once_with(seconds)
        assert event == SleepEvents.OK


@pytest.mark.mongouser
@patch('common.workflow.sleep.now_utc', return_value=datetime(2015, 1, 1))
def test_sleep_till(m_now_aware):
    doc = MyDocument.objects.create(max_pending_till=datetime(2015, 1, 1, 0, 2))
    with patch.object(common.workflow.sleep, 'time') as m_time:
        event, order = process_state_action({'action': SleepTill, 'args': ('max_pending_till',)},
                                            (SleepEvents.OK,), doc)

        m_time.sleep.assert_called_once_with(120)
        assert event == SleepEvents.OK
