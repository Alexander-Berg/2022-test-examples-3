# -*- coding: utf8 -*-
import pytest
from datetime import date, datetime

from mock import patch

from travel.avia.library.python.tester.factories import create_partner

from travel.avia.admin.lib.partner_mail_sender import send_mail


@pytest.mark.dbuser
def test_send_unknown_mail_name_to_partner():
    partner = create_partner()

    assert send_mail(partner, 'unknown_name') is False


@pytest.mark.dbuser
@patch('travel.avia.library.python.common.utils.environment.today')
def test_send_mail_to_partner_but_mail_checks_is_fail(mock_today):
    mock_today.return_value = date(2015, 10, 1)
    partner = create_partner(current_balance=100,
                             null_balance_notification_sent=datetime(2015, 9, 1))

    assert send_mail(partner, 'null_balance_notification') is False


@pytest.mark.dbuser
@patch('travel.avia.library.python.common.utils.environment.today')
def test_send_mail_to_partner_without_emails(mock_today):
    mock_today.return_value = date(2015, 10, 1)
    partner = create_partner(current_balance=-50,
                             null_balance_notification_sent=datetime(2015, 9, 1))

    assert send_mail(partner, 'null_balance_notification') is False
