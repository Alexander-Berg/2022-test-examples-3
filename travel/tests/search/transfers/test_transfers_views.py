# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import date, datetime

import mock
import pytest
from django.test import Client
from django.utils.http import urlencode
from hamcrest import assert_that, has_entries, contains

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.geo import Country
from common.models.schedule import TrainPurchaseNumber, RThreadType
from common.models.transport import TransportType
from common.tester.factories import create_station, create_thread, create_company, create_transport_model, \
    create_settlement
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from common.utils.date import UTC_TZ


pytestmark = [pytest.mark.dbuser]


def test_empty_transfers():
    point_from = create_station()
    point_to = create_station()
    dt = date(2016, 4, 25)

    with mock.patch(
        'travel.rasp.morda_backend.morda_backend.search.transfers.service.get_transfer_variants', return_value=[]
    ) as m_get_transfer_variants:
        response = Client().get(_build_transfers_url(point_from, point_to, dt, 'uk'))
        m_get_transfer_variants.assert_called_with(point_from, point_to, dt, None)
        assert response.status_code == 200
        data = json.loads(response.content)
        assert len(data['transferVariants']) == 0


@replace_now('2016-04-26 00:00:00')
def test_rasp_db_transfers():
    point_from = create_station(title_uk='first', country_id=Country.RUSSIA_ID)
    point_in_the_middle = create_station(
        title_uk='in_the_middle', time_zone='Asia/Yekaterinburg', country_id=Country.RUSSIA_ID
    )
    point_to = create_station(title_uk='last', country_id=Country.RUSSIA_ID)
    start_date = environment.today()
    company = create_company(title_uk='uk_company', url='some_url')
    first_thread = create_thread(
        company=company, tz_start_time='00:00',
        schedule_v1=[(None, 0, point_from), (60, None, point_in_the_middle)],
        t_type=TransportType.objects.get(id=TransportType.TRAIN_ID)
    )
    first_segment = _create_segment(first_thread, start_date)
    second_thread = create_thread(
        company=company, tz_start_time='02:00',
        schedule_v1=[(None, 0, point_in_the_middle), (60, None, point_to)],
        t_type=TransportType.objects.get(id=TransportType.SUBURBAN_ID)
    )
    second_segment = _create_segment(second_thread, start_date)

    variant = mock.Mock()
    variant.segments = [first_segment, second_segment]
    transfer_variants = [variant]
    train_number = '123Б'
    TrainPurchaseNumber.objects.create(thread=first_thread, number=train_number).save()
    first_segment.train_numbers = [train_number]

    with mock.patch(
        'travel.rasp.morda_backend.morda_backend.search.transfers.service.get_transfer_variants',
        return_value=transfer_variants
    ) as m_get_transfer_variants:
        response = Client().get(_build_transfers_url(point_from, point_to, start_date, 'uk'))
        m_get_transfer_variants.assert_called_with(point_from, point_to, start_date, None)

        assert response.status_code == 200
        data = json.loads(response.content)

        assert_that(data, has_entries(
            'transferVariants', contains(has_entries(
                'segments', contains(
                    _is_segment_condition(first_segment),
                    _is_segment_condition(second_segment)
                )
            ))
        ))


def _is_segment_condition(segment, lang='uk'):
    thread = segment.thread
    station_from = segment.station_from
    station_to = segment.station_to
    company = thread.company
    return has_entries(
        departure=segment.departure.astimezone(UTC_TZ).isoformat(),
        arrival=segment.arrival.astimezone(UTC_TZ).isoformat(),
        company=has_entries(
            id=company.id,
            url=company.url,
            title=company.L_title(lang=lang)
        ),
        stationFrom=has_entries(
            id=station_from.id,
            title=station_from.L_title(lang=lang),
            timezone=station_from.time_zone,
            railwayTimezone='Europe/Moscow'
        ),
        stationTo=has_entries(
            id=station_to.id,
            title=station_to.L_title(lang=lang),
            timezone=station_to.time_zone,
            railwayTimezone='Europe/Moscow'
        ),
        thread=has_entries(
            uid=thread.uid,
            title=thread.L_title(lang=lang),
            number=thread.number
        ),
        trainNumbers=getattr(segment, 'train_numbers')
    )


def _create_segment(thread, start_date):
    naive_start_dt = datetime.combine(start_date, thread.tz_start_time)
    path = list(thread.path)
    return SegmentStub(
        departure=path[0].get_departure_dt(naive_start_dt),
        arrival=path[-1].get_arrival_dt(naive_start_dt),
        thread=thread,
        company=thread.company,
        station_from=path[0].station,
        station_to=path[-1].station
    )


def _build_transfers_url(point_from, point_to, dt, national_version):
    base_url = '/{}/search/transfers/?'.format(national_version)
    request_params = {
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key,
        'when': dt.strftime('%Y-%m-%d')
    }
    return base_url + urlencode(request_params)


class SegmentStub(object):
    def __init__(self, departure, arrival, thread, company, station_from, station_to):
        self.departure = departure
        self.arrival = arrival
        self.thread = thread
        self.company = company
        self.station_to = station_to
        self.station_from = station_from
        self.t_type = thread.t_type


PATHFINDER_RESPONSE_XML = """<?xml version="1.0" encoding="utf-8" ?>
<routes>
    <group>
        <variant tr="4">
            <route
                tr="1" start_date="2020-10-03" thread_id="t_uid"
                departure_datetime="2020-10-03 00:00" departure_station_id="101"
                arrival_datetime="2020-10-03 01:00" arrival_station_id="102"
            />
            <route
                tr="2" start_date="2020-10-03" thread_id="NULL"
                departure_datetime="2020-10-03 01:00" departure_station_id="102"
                arrival_datetime="2020-10-03 02:00" arrival_station_id="103"
            />
            <route
                tr="3" start_date="2020-10-03" thread_id="SU-1_20201212_c1_12"
                departure_datetime="2020-10-03 03:00" departure_station_id="103"
                arrival_datetime="2020-10-03 04:00" arrival_station_id="104"
            />
        </variant>
    </group>
</routes>
"""


ONE_DAY_P2P_BARIS_RESPONSE = {
    'departureStations': [103],
    'arrivalStations': [104],
    'flights': [
        {
            'airlineID': 302,
            'title': 'SU 1',
            'departureDatetime': '2020-10-03 03:00:00+03:00',
            'departureTerminal': 'A',
            'departureStation': 103,
            'arrivalDatetime': '2020-10-03 04:00:00+03:00',
            'arrivalTerminal': '',
            'arrivalStation': 104,
            'transportModelID': 202,
            'route': [103, 104],
            'source': 'flight-board'
        }
    ]
}


def test_transfers():
    settlement1 = create_settlement(id=11, title='c1')
    settlement2 = create_settlement(id=12, title='c2')
    settlement3 = create_settlement(id=13, title='c3')
    settlement4 = create_settlement(id=14, title='c4')
    station1 = create_station(id=101, title='s1', settlement=settlement1)
    station2 = create_station(id=102, title='s2', settlement=settlement2)
    create_station(id=103, title='s3', settlement=settlement3)
    station4 = create_station(id=104, title='s4', settlement=settlement4)

    company1 = create_company(id=301, title='p1')
    create_company(id=302, title='p2')
    create_transport_model(id=202, title='m2')

    create_thread(
        uid='t_uid', number='t_number', title='t_title',
        type=RThreadType.THROUGH_TRAIN_ID,
        t_type=TransportType.TRAIN_ID,
        company=company1,
        schedule_v1=[(None, 0, station1), (60, None, station2)]
    )

    with mock.patch('route_search.transfers.transfers.get_pathfinder_response', return_value=PATHFINDER_RESPONSE_XML):
        with mock_baris_response(ONE_DAY_P2P_BARIS_RESPONSE):
            response = Client().get(_build_transfers_url(station1, station4, date(2020, 10, 3), 'ru'))
            result = json.loads(response.content)

            assert len(result['transferVariants']) == 1
            assert len(result['transferVariants'][0]['segments']) == 2
            assert_that(result, has_entries(
                'transferVariants', contains(has_entries(
                    'segments', contains(
                        has_entries({
                            'departure': '2020-10-02T21:00:00+00:00',
                            'arrival': '2020-10-02T22:00:00+00:00',
                            'isThroughTrain': True,
                            'convenience': 1,
                            'price': None,
                            'trainNumbers': None,
                            'isInterval': False,
                            'thread': has_entries({
                                'number': 't_number',
                                'uid': 't_uid',
                                'title': 't_title',
                                'isBasic': False,
                                'isExpress': False,
                                'isAeroExpress': False,
                                'schedulePlanCode': None,
                                'comment': '',
                                'beginTime': None,
                                'endTime': None,
                                'density': ''
                            }),
                            'transport': has_entries({
                                'code': 'train',
                                'id': 1,
                                'title': 'Поезд',
                            }),
                            'company': has_entries({
                                'id': 301,
                                'title': 'p1'
                            }),
                            'stationFrom': has_entries({
                                'id': 101,
                                'title': 's1',
                                'settlement': has_entries({
                                    'id': 11,
                                    'title': 'c1'
                                })
                            }),
                            'stationTo': has_entries({
                                'id': 102,
                                'title': 's2',
                                'settlement': has_entries({
                                    'id': 12,
                                    'title': 'c2'
                                })
                            }),
                        }),
                        has_entries({
                            'departure': '2020-10-03T00:00:00+00:00',
                            'arrival': '2020-10-03T01:00:00+00:00',
                            'isThroughTrain': False,
                            'convenience': 3,
                            'price': None,
                            'trainNumbers': None,
                            'isInterval': False,
                            'thread': has_entries({
                                'number': 'SU 1',
                                'uid': '',
                                'title': 'c3 \u2013 c4',
                                'isBasic': True,
                                'isExpress': False,
                                'isAeroExpress': False,
                                'schedulePlanCode': None,
                                'comment': '',
                                'beginTime': None,
                                'endTime': None,
                                'density': ''
                            }),
                            'transport': has_entries({
                                'code': 'plane',
                                'id': 2,
                                'title': 'Самолёт',
                                'model': has_entries({'title': 'm2'})
                            }),
                            'company': has_entries({
                                'id': 302,
                                'title': 'p2'
                            }),
                            'stationFrom': has_entries({
                                'id': 103,
                                'title': 's3',
                                'settlement': has_entries({
                                    'id': 13,
                                    'title': 'c3'
                                })
                            }),
                            'stationTo': has_entries({
                                'id': 104,
                                'title': 's4',
                                'settlement': has_entries({
                                    'id': 14,
                                    'title': 'c4'
                                })
                            }),
                        })
                    )
                ))
            ))
