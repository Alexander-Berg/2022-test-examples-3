# -*- coding: utf-8 -*-
import marshmallow

from travel.avia.ticket_daemon_api.jsonrpc.lib.date import get_msk_now
from travel.avia.ticket_daemon_api.jsonrpc.schemas.fields import DatetimeAware


def test_DatetimeAware():
    class DTSchem(marshmallow.Schema):
        dt_aware = DatetimeAware()
    now = get_msk_now()
    dumped = DTSchem(strict=True).dump({'dt_aware': now}).data
    assert dumped['dt_aware']['tzname'] == 'Europe/Moscow'
    # assert dumped['dt_aware']['offset'] == 180.0
    restored = DTSchem(strict=True).load(dumped).data['dt_aware']
    assert restored.tzinfo == now.tzinfo
    assert restored == now.replace(microsecond=0)
