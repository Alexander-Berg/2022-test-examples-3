# -*- coding: utf-8 -*-

from datetime import datetime, time

import mock
import pytest
from django.http import HttpResponse

from common.tester.factories import create_station, create_thread
from common.tester.utils.datetime import replace_now
from common.tester.skippers import skip_in_arcadia
from stationschedule.tester.factories import create_ztablo


pytestmark = pytest.mark.dbuser


@skip_in_arcadia
def test_blank_real_ztablo(rasp_client):
    departure_dt = datetime(2000, 1, 1)
    number = 'OC815'
    station = create_station(t_type='plane', type_choices='tablo', tablo_state='real')
    create_ztablo(station=station, number=number, departure=departure_dt, t_type='plane')

    with replace_now(departure_dt):
        response = rasp_client.get('/informers/station/{}'.format(station.id))

    assert response.status_code == 200
    assert number in response.content


@skip_in_arcadia
@replace_now(datetime(2000, 1, 1, 20))
def test_get_nearest_schedule(rasp_client):
    station_from = create_station()
    station_to = create_station()
    create_thread(t_type='train',
                  tz_start_time=time(10),
                  schedule_v1=[[None, 0, station_from],
                               [10, None, station_to]])
    create_thread(t_type='train',
                  tz_start_time=time(15),
                  schedule_v1=[[None, 0, station_from],
                               [10, None, station_to]])
    create_thread(t_type='train',
                  tz_start_time=time(20),
                  schedule_v1=[[None, 0, station_from],
                               [10, None, station_to]])

    # не пятисотит
    response = rasp_client.get('/informers/station/{}/'.format(station_from.id))

    assert response.status_code == 200

    # рейсы на завтра не ограничены по времени (RASPFRONT-1960)
    with mock.patch('travel.rasp.morda.morda.templates.iwidgets.StationInformerWidgetTemplate.render',
                    return_value=HttpResponse()) as m_render:
        rasp_client.get('/informers/station/{}/'.format(station_from.id))

        m_render.assert_called_once()

        (_request, context), _render_kwargs = m_render.call_args

    routes_dts = [route.naive_start_dt for route in context['departure_routes']]
    expected_dts = [
        datetime(2000, 1, 1, 20),
        datetime(2000, 1, 2, 10),
        datetime(2000, 1, 2, 15),
        datetime(2000, 1, 2, 20),
        datetime(2000, 1, 3, 10)
    ]

    assert routes_dts == expected_dts
