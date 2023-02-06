# coding=utf-8
from __future__ import unicode_literals, absolute_import, print_function

import pytest

from travel.avia.admin.avia_scripts.conversion.update_conversions import get_billing_order_ids
from travel.avia.library.python.tester.factories import create_partner


@pytest.mark.parametrize('partner, result', (
    (dict(code='usual', billing_order_id=100, use_in_update_conversions=False, disabled=False, can_fetch_by_daemon=True), set()),
    (dict(code='conversion', billing_order_id=200, use_in_update_conversions=True, disabled=False, can_fetch_by_daemon=True), {200}),
    (dict(code='conversion-null', billing_order_id=None, use_in_update_conversions=True, disabled=False, can_fetch_by_daemon=True), set()),
    (dict(code='disabled-by-daemon', billing_order_id=300, use_in_update_conversions=True, disabled=False, can_fetch_by_daemon=False), set()),
    (dict(code='disabled-by-partnerka', billing_order_id=400, use_in_update_conversions=True, disabled=True, can_fetch_by_daemon=True), set()),
))
@pytest.mark.dbuser
def test_get_billing_order_ids(partner, result):
    create_partner(**partner)
    assert get_billing_order_ids() == result
