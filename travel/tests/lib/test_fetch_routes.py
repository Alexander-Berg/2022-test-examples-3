# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, contains_inanyorder

from common.models.geo import Station
from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_thread, create_station
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.trains.scripts.lib.fetch_routes import fetch_routes

pytestmark = [pytest.mark.dbuser]


def create_segment_thread(station_from, station_to, t_type=TransportType.TRAIN_ID):
    return create_thread(
        __={'calculate_noderoute': True},
        t_type=t_type,
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ]
    )


def test_route_with_all_transports_but_only_train_url():
    from_settlement = create_settlement(slug='from_city')
    to_settlement = create_settlement(slug='to_city')
    from_station = create_station(title='from_station', slug='from_station', settlement=from_settlement)
    to_station = create_station(title='to_station', slug='to_station', settlement=to_settlement)

    create_segment_thread(from_station, to_station, t_type=TransportType.TRAIN_ID)
    create_segment_thread(from_station, to_station, t_type=TransportType.SUBURBAN_ID)
    create_segment_thread(from_station, to_station, t_type=TransportType.BUS_ID)
    create_segment_thread(from_station, to_station, t_type=TransportType.WATER_ID)
    create_segment_thread(from_station, to_station, t_type=TransportType.PLANE_ID)

    routes = fetch_routes()

    assert routes == {
        ('from_city', 'to_city')
    }


@replace_dynamic_setting('TRAIN_PURCHASE_SITEMAPING_ADLER_STATION', True)
def test_fetch_adler_station():
    from_settlement = create_settlement(slug='from_city')
    to_settlement = create_settlement(slug='to_city')
    from_station = create_station(title='from_station', slug='from_station', settlement=from_settlement)
    to_station = create_station(title='to_station', slug='to_station', settlement=to_settlement)

    sochi = create_settlement(slug='sochi')
    adler_station = create_station(title='adler_station', slug='adler-station', id=Station.ADLER_ID,
                                   settlement=sochi)
    nowaytoadler_settlement = create_settlement(slug='nowaytoadler_city')
    nowaytoadler_station = create_station(title='nowaytoadler_station', slug='nowaytoadler_station',
                                          settlement=nowaytoadler_settlement)

    create_segment_thread(from_station, to_station, t_type=TransportType.TRAIN_ID)
    create_segment_thread(from_station, adler_station, t_type=TransportType.TRAIN_ID)
    create_segment_thread(adler_station, to_station, t_type=TransportType.TRAIN_ID)
    create_segment_thread(nowaytoadler_station, to_station, t_type=TransportType.TRAIN_ID)

    routes = fetch_routes()

    assert_that(routes, contains_inanyorder(
        ('from_city', 'to_city'),
        ('from_city', 'sochi'),
        ('sochi', 'to_city'),
        ('adler-station', 'to_city'),
        ('from_city', 'adler-station'),
        ('nowaytoadler_city', 'to_city'),
    ))
