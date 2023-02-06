# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, has_properties, has_entries, contains_string

from common.email_sender import EmailIntent
from common.email_sender.factories import EmailIntentFactory
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.train_api.train_purchase.utils.email_sender_callback import report_fail_email

pytestmark = pytest.mark.mongouser


@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
def test_report_fail_email(m_campaign):
    failed_email_intent = EmailIntentFactory(data={'order_uid': 'some-order-uid'}, log_context={'foo': 'bar'})

    with replace_dynamic_setting('TRAIN_PURCHASE_ERRORS_EMAIL', 'a@example.org'):
        report_fail_email(failed_email_intent)

    created_email_intent = EmailIntent.objects.get(key='failed_email_intent_{}'.format(failed_email_intent.key))
    assert_that(created_email_intent, has_properties(
        email='a@example.org',
        data=failed_email_intent.data,
        log_context=failed_email_intent.log_context,
        args=has_entries(error_message=contains_string('some-order-uid'))
    ))
    assert m_campaign.mock_calls == [
        mock.call.create_rasp_campaign(created_email_intent.campaign_code),
        mock.call.create_rasp_campaign().send(
            to_email=created_email_intent.email,
            args=created_email_intent.args,
            attachments=created_email_intent.attachments
        )
    ]
