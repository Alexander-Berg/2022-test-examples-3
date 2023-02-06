# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest
from django.conf import settings
from mock import call
from mongoengine import DoesNotExist
from ylog.context import get_log_context

from common.email_sender import run_queue, EmailIntent
from common.email_sender.factories import EmailIntentFactory
from common.email_sender.tasks import send_email, log as send_email_log
from common.tester.utils.replace_setting import replace_setting

pytestmark = pytest.mark.mongouser('module')

CREATED_2001 = datetime(2001, 1, 1)


class _TestException(Exception):
    pass


@replace_setting('EMAIL_SENDER_CONCURRENT_PROCESSING', False)
@mock.patch('common.email_sender.tasks.send_email', autospec=True)
def test_run_queue_only_not_sent(m_send_email):
    EmailIntentFactory.create_batch(3, created_at=CREATED_2001)
    EmailIntentFactory(is_sent=True)
    run_queue()
    assert m_send_email.mock_calls == [
        call(e.id, True) for e in EmailIntent.objects(is_sent=False).order_by('-created_at')
    ]


@replace_setting('EMAIL_SENDER_CONCURRENT_PROCESSING', False)
@mock.patch('common.email_sender.tasks.send_email', autospec=True)
def test_dont_sent_with_max_time_in_queue_exceeded(m_send_email):
    EmailIntentFactory(max_time_in_queue_exceeded=True)
    run_queue()
    assert not m_send_email.called


@replace_setting('EMAIL_SENDER_CONCURRENT_PROCESSING', True)
@replace_setting('EMAIL_SENDER_NUMBER_TO_PROCESS_PER_LAUNCH', 10)
@mock.patch('common.email_sender.tasks.send_email', autospec=True)
def test_run_queue_no_more_then_limit(m_send_email):
    EmailIntentFactory.create_batch(
        settings.EMAIL_SENDER_NUMBER_TO_PROCESS_PER_LAUNCH + 1,
        created_at=CREATED_2001
    )
    run_queue()
    assert len(m_send_email.mock_calls) == settings.EMAIL_SENDER_NUMBER_TO_PROCESS_PER_LAUNCH


@replace_setting('EMAIL_SENDER_CONCURRENT_PROCESSING', True)
@mock.patch('common.email_sender.tasks.send_email', autospec=True)
def test_run_queue_async(m_send_email):
    EmailIntentFactory.create_batch(3, created_at=CREATED_2001)
    run_queue()
    assert m_send_email.mock_calls == [
        call.apply_async([str(e.id), True]) for e in EmailIntent.objects.all().order_by('-created_at')
    ]


@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
def test_send_email(m_campaign):
    email_intent = EmailIntentFactory.create()
    send_email(email_intent.id)
    assert m_campaign.mock_calls == [
        call.create_rasp_campaign(email_intent.campaign_code),
        call.create_rasp_campaign().send(
            to_email=email_intent.email,
            args=email_intent.args,
            attachments=email_intent.attachments
        )
    ]


@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
def test_dont_send_already_sent_email(m_campaign):
    email_intent = EmailIntentFactory.create(is_sent=True)
    send_email(email_intent.id)
    assert not m_campaign.called


@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
def test_dont_send_email_with_max_time_in_queue_exceeded(m_campaign):
    email_intent = EmailIntentFactory.create(max_time_in_queue_exceeded=True)
    send_email(email_intent.id)
    assert not m_campaign.called


@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
def test_send_email_failed(m_campaign):
    m_campaign.create_rasp_campaign.side_effect = _TestException('bah!')
    email_intent = EmailIntentFactory.create(log_context={'key': '12345'})

    def check_context(*_args, **_kwargs):
        assert get_log_context() == {'key': '12345'}
    with mock.patch.object(send_email_log, 'exception', side_effect=check_context) as m_log_exception:
        with pytest.raises(_TestException):
            send_email(email_intent.id)
    assert m_log_exception.call_count == 1


def test_send_email_not_found():
    def check_context(*_args, **_kwargs):
        assert get_log_context() == {}
    with mock.patch.object(send_email_log, 'exception', side_effect=check_context) as m_log_exception:
        with pytest.raises(DoesNotExist):
            send_email('111111111111111111111111')
    assert m_log_exception.call_count == 1


@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
def test_old_email_has_chance_to_be_sent(m_campaign):
    email_intent = EmailIntentFactory.create(created_at=CREATED_2001)

    send_email(email_intent.id)

    assert m_campaign.mock_calls == [
        call.create_rasp_campaign(email_intent.campaign_code),
        call.create_rasp_campaign().send(
            to_email=email_intent.email,
            args=email_intent.args,
            attachments=email_intent.attachments
        )
    ]


@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
def test_set_max_time_in_queue_exceeded_if_cant_send_old_email(m_campaign):
    m_campaign.create_rasp_campaign.side_effect = Exception('bah!')
    email_intent = EmailIntentFactory.create(created_at=CREATED_2001)

    with pytest.raises(Exception):
        send_email(email_intent.id)

    email_intent.reload()
    assert email_intent.max_time_in_queue_exceeded


@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
def test_good_callback_when_cant_send_old_email(m_campaign):
    m_campaign.create_rasp_campaign.side_effect = Exception('bah!')
    email_intent = EmailIntentFactory.create(created_at=CREATED_2001)

    m_callback = mock.Mock()
    with replace_setting('EMAIL_SENDER_CALLBACK_ON_MAX_ALLOWED_TIME_IN_QUEUE_EXCEEDED', m_callback):
        with pytest.raises(Exception):
            send_email(email_intent.id)

    m_callback.assert_called_once_with(email_intent)
    email_intent.reload()
    assert email_intent.max_time_in_queue_exceeded


@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
def test_bad_callback_when_cant_send_old_email(m_campaign):
    m_campaign.create_rasp_campaign.side_effect = Exception('bah!')
    email_intent = EmailIntentFactory.create(created_at=CREATED_2001)

    m_callback = mock.Mock(side_effect=Exception('Boom!'))
    with replace_setting('EMAIL_SENDER_CALLBACK_ON_MAX_ALLOWED_TIME_IN_QUEUE_EXCEEDED', m_callback):
        with pytest.raises(Exception):
            send_email(email_intent.id)

    m_callback.assert_called_once_with(email_intent)
    email_intent.reload()
    assert email_intent.max_time_in_queue_exceeded
