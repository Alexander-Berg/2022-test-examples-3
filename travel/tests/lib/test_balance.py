# -*- coding: utf8 -*-
import pytest
from mock import MagicMock
from travel.avia.library.python.tester.factories import create_partner
from travel.avia.admin.lib.balance import _update_balance
from travel.avia.admin.lib.partner_mail_sender import (
    LOW_BALANCE_NOTIFICATION_MAIL_TYPE,
    NONPOSTIVE_BALANCE_NOTIFICATION_MAIL_TYPE,
    POSTIVE_BALANCE_NOTIFICATION_MAIL_TYPE
)

from travel.avia.library.python.common.models.partner import UpdateHistoryRecord


@pytest.mark.dbuser
def test_update_balance_and_disable_partner():
    partner = create_partner(current_balance=1000, disabled=False)
    partner.disable = MagicMock('disabled_method')

    assert _update_balance(partner, -100) == NONPOSTIVE_BALANCE_NOTIFICATION_MAIL_TYPE
    partner.disable.assert_called_with(yandex_login='balance', role='admin')


@pytest.mark.dbuser
def test_update_balance_and_not_disable_partner():
    partner = create_partner(
        current_balance=1000, disabled=False, enabled_with_negative_balance=True
    )
    partner.disable = MagicMock('disable_method')
    partner.enable = MagicMock('enable_method')

    assert _update_balance(partner, -100) is None
    assert partner.disable.call_count == 0
    assert partner.enable.call_count == 0


@pytest.mark.dbuser
def test_update_balance_and_enable_partner():
    partner = create_partner(current_balance=-100, disabled=True)
    partner.enable = MagicMock('enabled_method')

    UpdateHistoryRecord(action=UpdateHistoryRecord.CHANGE_ACTIVE_STATUS_ACTION,
                        partner=partner,
                        updater_yandex_login='balance').save()

    assert _update_balance(partner, 1000) == POSTIVE_BALANCE_NOTIFICATION_MAIL_TYPE
    partner.enable.assert_called_with(yandex_login='balance', role='admin')


@pytest.mark.dbuser
def test_update_balance_and_cant_enable_partner_because_zzz_disable_the_partner():
    partner = create_partner(current_balance=-100, disabled=True)

    UpdateHistoryRecord(action=UpdateHistoryRecord.CHANGE_ACTIVE_STATUS_ACTION,
                        partner=partner,
                        updater_yandex_login='zzz').save()

    assert _update_balance(partner, 1000) is None


@pytest.mark.dbuser
def test_update_balance_and_cant_enable_partner_because_an_unknown_user_disable_the_partner():
    partner = create_partner(current_balance=-100, disabled=True)

    assert _update_balance(partner, 1000) is None


@pytest.mark.dbuser
def test_update_balance_and_send_notification_about_low_balance():
    partner = create_partner(current_balance=300, disabled=True)

    assert _update_balance(partner, 299) is LOW_BALANCE_NOTIFICATION_MAIL_TYPE
