# -*- coding: utf-8 -*-

from datetime import datetime

import pytest

from common.utils.date import UTC_TZ
from stationschedule.models import ZTablo2
from stationschedule.tester.factories import create_ztablo


@pytest.mark.dbuser
def test_get_event_naive_dt():
    z_tablo = create_ztablo({
        'original_departure': datetime(2000, 1, 1, 1),
        'departure': datetime(2000, 1, 1, 2),
        'real_departure': datetime(2000, 1, 1, 3),
    })

    with pytest.raises(ValueError):
        z_tablo.get_event_naive_dt('invalid event', ZTablo2.PLANNED)

    with pytest.raises(ValueError):
        z_tablo.get_event_naive_dt('departure', 'invalid variant')

    assert z_tablo.get_event_naive_dt('departure', ZTablo2.ORIGINAL) == datetime(2000, 1, 1, 1)
    assert z_tablo.get_event_naive_dt('departure', ZTablo2.PLANNED) == datetime(2000, 1, 1, 2)
    assert z_tablo.get_event_naive_dt('departure', ZTablo2.REAL) == datetime(2000, 1, 1, 3)
    assert z_tablo.get_event_naive_dt('departure', ZTablo2.ACTUAL) == datetime(2000, 1, 1, 3)

    assert create_ztablo().get_event_naive_dt('departure', ZTablo2.ACTUAL) is None
    assert create_ztablo({
        'departure': datetime(2000, 1, 1)
    }).get_event_naive_dt('departure', ZTablo2.ACTUAL) == datetime(2000, 1, 1)
    assert create_ztablo({
        'real_departure': datetime(2000, 1, 1)
    }).get_event_naive_dt('departure', ZTablo2.ACTUAL) == datetime(2000, 1, 1)


@pytest.mark.dbuser
def test_get_event_dt():
    z_tablo = create_ztablo()
    assert z_tablo.get_event_dt('departure') is z_tablo.get_event_dt('arrival') is None

    z_tablo = create_ztablo({'station': {'time_zone': 'UTC'}, 'real_departure': datetime(2000, 1, 1)})
    assert (z_tablo.get_event_dt('departure') ==
            z_tablo.get_event_dt('departure', ZTablo2.REAL) == datetime(2000, 1, 1, tzinfo=UTC_TZ))

    assert z_tablo.get_departure_dt() == z_tablo.get_departure_dt(ZTablo2.REAL) == datetime(2000, 1, 1, tzinfo=UTC_TZ)

    z_tablo = create_ztablo({'station': {'time_zone': 'UTC'}, 'real_arrival': datetime(2000, 1, 1)})
    assert z_tablo.get_arrival_dt() == z_tablo.get_arrival_dt(ZTablo2.REAL) == datetime(2000, 1, 1, tzinfo=UTC_TZ)
