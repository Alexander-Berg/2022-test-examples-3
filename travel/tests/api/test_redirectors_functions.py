# -*- coding: utf-8 -*-
import pytest

from travel.avia.library.python.common.models.partner import DohopVendor, Partner

from travel.avia.ticket_daemon.ticket_daemon.api.redirect import get_partner_by_code, get_redirect_partner
from travel.avia.ticket_daemon.ticket_daemon.api.query import get_query_module


partners = [
    p for p in Partner.objects.all()
    if p.can_fetch_by_daemon and p.enabled
]
vendors = [
    p for p in DohopVendor.objects.all()
    if p.enabled
]


@pytest.mark.parametrize('partner', vendors + partners)
def test_get_query_module(partner):
    assert get_query_module(partner)


@pytest.mark.parametrize('partner_code', [p.code for p in partners + vendors])
def test_get_partner_by_code(partner_code):
    assert get_partner_by_code(partner_code).code == partner_code


@pytest.mark.parametrize('partner', vendors + partners)
def test_get_redirect_partner(partner):
    assert partner == get_redirect_partner({'partner': partner.code})
