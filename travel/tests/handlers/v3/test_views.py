# -*- coding: utf-8 -*-
from travel.avia.library.python.tester.factories import create_partner
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.views import get_partner_redirect_timeout
import pytest


@pytest.mark.dbuser
def test_partner_redirect_timeout():
    reset_all_caches()
    create_partner(code='aeroflot')
    create_partner(code='pilotua')
    assert get_partner_redirect_timeout('aeroflot') is not None
    assert get_partner_redirect_timeout('aeroflot') == 5
    assert get_partner_redirect_timeout('pilotua') == 10.0
