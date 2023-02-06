# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, time

import json
import mock
import pytest
from google.protobuf import json_format
from hamcrest import assert_that, has_entries, contains_inanyorder

from common.apps.archival_data import generation
from common.apps.archival_data.factories import ArchivalSettlementsDataFactory
from common.apps.archival_data.generation import SearchDataGenerator, SettlementsDataGenerator
from common.apps.archival_data.models import ArchivalSearchData, ArchivalSettlementsData
from common.data_api.baris.service import BarisService
from common.models.transport import TransportType
from common.tester.factories import create_station, create_settlement, create_thread, create_transport_subtype
from common.tests.utils import has_route_search
from common.tester.utils.datetime import replace_now
from travel.proto.dicts.avia.schedule_dump_pb2 import TFlight

create_thread = create_thread.mutate(__={'calculate_noderoute': True})

pytestmark = [pytest.mark.mongouser, pytest.mark.dbripper, has_route_search]


def test_generate_data():
    generator = SearchDataGenerator()
    settlement_from = create_settlement()
    settlement_to = create_settlement()
    station_from = create_station(settlement=settlement_from)
    station_to = create_station(settlement=settlement_to)

    station_from_bus = create_station()
    station_to_bus = create_station()

    create_thread(
        t_type=TransportType.BUS_ID,
        schedule_v1=[
            [None, 0, station_from_bus],
            [30, None, station_to_bus],
        ]
    )
    generator.generate()
    data = list(ArchivalSearchData.objects.all().aggregate())
    bus_dict = {
        'transport_type': TransportType.BUS_ID,
        'point_from': station_from_bus.point_key,
        'point_to': station_to_bus.point_key
    }

    assert_that(data, contains_inanyorder(
        has_entries(
            bus_dict
        )
    ))

    create_thread(
        t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[
            [None, 0, station_from],
            [20, None, station_to],
        ]
    )
    generator.generate()
    data = list(ArchivalSearchData.objects.all().aggregate())

    assert_that(data, contains_inanyorder(
        has_entries(
            bus_dict
        ),
        has_entries({
            'transport_type': TransportType.SUBURBAN_ID,
            'point_from': station_from.point_key,
            'point_to': station_to.point_key
        }),
        has_entries({
            'transport_type': TransportType.SUBURBAN_ID,
            'point_from': station_from.point_key,
            'point_to': settlement_to.point_key
        }),
        has_entries({
            'transport_type': TransportType.SUBURBAN_ID,
            'point_from': settlement_from.point_key,
            'point_to': station_to.point_key
        }),
        has_entries({
            'transport_type': TransportType.SUBURBAN_ID,
            'point_from': settlement_from.point_key,
            'point_to': settlement_to.point_key
        })
    ))


def test_generate_data_different_t_types():
    generator = SearchDataGenerator()
    station_from = create_station()
    station_to = create_station()

    create_thread(
        t_type=TransportType.BUS_ID,
        schedule_v1=[
            [None, 0, station_from],
            [30, None, station_to],
        ]
    )
    create_thread(
        t_type=TransportType.HELICOPTER_ID,
        schedule_v1=[
            [None, 0, station_from],
            [30, None, station_to],
        ]

    )

    generator.generate()
    data = list(ArchivalSearchData.objects.all().aggregate())

    assert_that(data, contains_inanyorder(
        has_entries({
            'transport_type': TransportType.BUS_ID,
            'point_from': station_from.point_key,
            'point_to': station_to.point_key
        }),
        has_entries({
            'transport_type': TransportType.HELICOPTER_ID,
            'point_from': station_from.point_key,
            'point_to': station_to.point_key
        })
    ))


def test_generate_data_use_baris():
    generator = SearchDataGenerator()
    station_from = create_station(id=222)
    station_to = create_station(id=223)

    create_thread(
        t_type=TransportType.BUS_ID,
        schedule_v1=[
            [None, 0, station_from],
            [30, None, station_to],
        ]
    )

    with mock.patch.object(BarisService, 'get_p2p_summary', return_value=((222, 223),)):
        generator.generate(use_baris=True)
    data = list(ArchivalSearchData.objects.all().aggregate())

    assert_that(data, contains_inanyorder(
        has_entries({
            'transport_type': TransportType.BUS_ID,
            'point_from': station_from.point_key,
            'point_to': station_to.point_key
        }),
        has_entries({
            'transport_type': TransportType.PLANE_ID,
            'point_from': station_from.point_key,
            'point_to': station_to.point_key
        })
    ))


def test_generate_data_use_baris_only():
    generator = SearchDataGenerator()
    settlement_from = create_settlement()
    settlement_to = create_settlement()
    station_from_1 = create_station(id=222, settlement=settlement_from, t_type=TransportType.PLANE_ID)
    station_to_1 = create_station(id=223, settlement=settlement_to, t_type=TransportType.PLANE_ID)
    station_from_2 = create_station(id=333)
    station_to_2 = create_station(id=334, settlement=settlement_to, t_type=TransportType.PLANE_ID)

    with mock.patch.object(BarisService, 'get_p2p_summary', return_value=((222, 223), (333, 334))):
        generator.generate(use_baris=True)
    data = list(ArchivalSearchData.objects.all().aggregate())

    assert_that(data, contains_inanyorder(
        has_entries({
            'transport_type': TransportType.PLANE_ID,
            'point_from': station_from_1.point_key,
            'point_to': station_to_1.point_key
        }),
        has_entries({
            'transport_type': TransportType.PLANE_ID,
            'point_from': settlement_from.point_key,
            'point_to': station_to_1.point_key
        }),
        has_entries({
            'transport_type': TransportType.PLANE_ID,
            'point_from': station_from_1.point_key,
            'point_to': settlement_to.point_key
        }),
        has_entries({
            'transport_type': TransportType.PLANE_ID,
            'point_from': settlement_from.point_key,
            'point_to': settlement_to.point_key
        }),
        has_entries({
            'transport_type': TransportType.PLANE_ID,
            'point_from': station_from_2.point_key,
            'point_to': station_to_2.point_key
        }),
        has_entries({
            'transport_type': TransportType.PLANE_ID,
            'point_from': station_from_2.point_key,
            'point_to': settlement_to.point_key
        })
    ))


@replace_now('2020-05-20 10:00:00')
def test_generate_cities_search_data():
    settlement_from = create_settlement()
    settlement_to_1 = create_settlement()
    settlement_to_2 = create_settlement()
    settlement_to_3 = create_settlement()
    station_from = create_station(settlement=settlement_from)
    station_to_1 = create_station(settlement=settlement_to_1)
    station_to_2 = create_station(settlement=settlement_to_2)
    station_to_3 = create_station(settlement=settlement_to_3)
    helicopter = TransportType.objects.get(id=TransportType.HELICOPTER_ID)
    bus = TransportType.objects.get(id=TransportType.BUS_ID)
    helicopter_subtype = create_transport_subtype(t_type=TransportType.HELICOPTER_ID, code='from',  color={'color': '#f'})

    ArchivalSettlementsDataFactory(
        point_from=settlement_from.point_key,
        point_to=settlement_to_1.point_key,
        update_dt=datetime(2020, 5, 19),
        transport_type=TransportType.TRAIN_ID,
        segments=[
            {
                'title': 'old_data',
                'station_from': {'id': station_from.id},
                'station_to': {'id': station_to_1.id},
                'transport_type': {'id': TransportType.TRAIN_ID}
            }
        ]
    )

    ArchivalSettlementsDataFactory(
        point_from=settlement_from.point_key,
        point_to=settlement_to_2.point_key,
        update_dt=datetime(2020, 5, 5),
        transport_type=TransportType.HELICOPTER_ID,
        segments=[
            {
                'title': 'outdated',
                'station_from': {'id': station_from.id},
                'station_to': {'id': station_to_2.id},
                'transport_type': {'id': TransportType.HELICOPTER_ID}
            }
        ]
    )

    create_thread(
        t_type=TransportType.TRAIN_ID,
        schedule_v1=[
            [None, 0, station_from],
            [30, None, station_to_1],
        ]
    )
    create_thread(
        t_subtype=helicopter_subtype,
        number='basic_helicopter',
        t_type=TransportType.HELICOPTER_ID,
        schedule_v1=[
            [None, 0, station_from],
            [30, None, station_to_2],
        ],
        tz_start_time=time(15),
        year_days=[datetime(2020, 5, 25)],
        title='replace_data'
    )
    create_thread(
        number='another_basic_helicopter',
        t_type=TransportType.HELICOPTER_ID,
        schedule_v1=[
            [None, 0, station_from],
            [30, None, station_to_2],
        ]
    )
    create_thread(
        number='interval_bus',
        type='interval',
        t_type=TransportType.BUS_ID,
        schedule_v1=[
            [None, 0, station_from],
            [30, None, station_to_3],
        ],
        year_days=[datetime(2020, 5, 21), datetime(2020, 6, 10)],
        begin_time='10:00',
        end_time='22:00',
        title='insert_data'
    )

    SettlementsDataGenerator().generate()
    data = list(ArchivalSettlementsData.objects.all().aggregate())

    assert len(data) == 3
    assert_that(data, contains_inanyorder(
        has_entries({
            'update_dt': datetime(2020, 5, 19),
            'point_from': settlement_from.point_key,
            'point_to': settlement_to_1.point_key,
            'transport_type': TransportType.TRAIN_ID,
            'segments': [
                {
                    'title': 'old_data',
                    'station_from': {'id': station_from.id},
                    'station_to': {'id': station_to_1.id},
                    'transport_type': {'id': TransportType.TRAIN_ID},
                    'run_days_by_tz': {},
                    'thread': {}
                }
            ]
        }),
        has_entries({
            'update_dt': datetime(2020, 05, 20, 10),
            'point_from': settlement_from.point_key,
            'point_to': settlement_to_2.point_key,
            'transport_type': TransportType.HELICOPTER_ID,
            'segments': contains_inanyorder(
                has_entries({
                    'arrival': datetime(2020, 05, 25, 12, 30),
                    'departure': datetime(2020, 05, 25, 12),
                    'title': 'replace_data',
                    'station_from': {
                        'id': station_from.id,
                        'title': station_from.L_title()
                    },
                    'station_to': {
                        'id': station_to_2.id,
                        'title': station_to_2.L_title()
                    },
                    'transport_type': {
                        'id': TransportType.HELICOPTER_ID,
                        'code': helicopter.code,
                        'title': helicopter.L_title(),
                        'subtype': {
                            'id': helicopter_subtype.id,
                            'code': helicopter_subtype.code,
                            'title': helicopter_subtype.L_title(),
                            'color': helicopter_subtype.color.color
                        }
                    },
                    'thread': {
                        'number': 'basic_helicopter',
                        'begin_time': None,
                        'end_time': None,
                        'density': ''
                    },
                    'run_days_by_tz': {
                        'Europe/Moscow': {
                            'days_text': 'только 25\xa0мая'
                        }
                    }
                }),
                has_entries({
                    'thread': has_entries({
                        'number': 'another_basic_helicopter',
                    })
                })
            )
        }),
        has_entries({
            'update_dt': datetime(2020, 05, 20, 10),
            'point_from': settlement_from.point_key,
            'point_to': settlement_to_3.point_key,
            'transport_type': TransportType.BUS_ID,
            'segments': contains_inanyorder(
                has_entries({
                    'arrival': None,
                    'departure': None,
                    'title': 'insert_data',
                    'station_from': {
                        'id': station_from.id,
                        'title': station_from.L_title()
                    },
                    'station_to': {
                        'id': station_to_3.id,
                        'title': station_to_3.L_title()
                    },
                    'transport_type': {
                        'id': TransportType.BUS_ID,
                        'code': bus.code,
                        'title': bus.L_title()
                    },
                    'thread': {
                        'number': 'interval_bus',
                        'begin_time': '10:00:00',
                        'end_time': '22:00:00',
                        'density': ''
                    },
                    'run_days_by_tz': {
                        'Europe/Moscow': {
                            'days_text': 'только 21\xa0мая, 10\xa0июня'
                        }
                    }
                })
            )
        })
    ))


FLIGHT_1 = {
    'Title': 'SU 1',
    'AirlineID': 301,
    'Schedules': [{
        'Route': [
            {
                'AirportID': 222,
                'ArrivalTime': '',
                'ArrivalDayShift': 0,
                'DepartureTime': '01:00:00',
                'DepartureDayShift': 0
            },
            {
                'AirportID': 223,
                'ArrivalTime': '02:00:00',
                'ArrivalDayShift': 0,
                'DepartureTime': '',
                'DepartureDayShift': 0
            }
        ],
        'Masks': [
            {
                'From': '2020-10-01',
                'Until': '2020-10-11',
                'On': 12
            },
            {
                'From': '2020-10-21',
                'Until': '2020-10-31',
                'On': 3
            }
        ]
    }]
}

FLIGHT_2 = {
    'Title': 'SU 1',
    'AirlineID': 301,
    'Schedules': [
        {
            'Route': [
                {
                    'AirportID': 222,
                    'ArrivalTime': '',
                    'ArrivalDayShift': 0,
                    'DepartureTime': '10:00:00',
                    'DepartureDayShift': 0
                },
                {
                    'AirportID': 223,
                    'ArrivalTime': '12:00:00',
                    'ArrivalDayShift': 0,
                    'DepartureTime': '',
                    'DepartureDayShift': 0
                }
            ],
            'Masks': [
                {
                    'From': '2020-10-01',
                    'Until': '2020-10-11',
                    'On': 13
                }
            ]
        },
        {
            'Route': [
                {
                    'AirportID': 222,
                    'ArrivalTime': '',
                    'ArrivalDayShift': 0,
                    'DepartureTime': '10:30:00',
                    'DepartureDayShift': 0
                },
                {
                    'AirportID': 223,
                    'ArrivalTime': '13:00:00',
                    'ArrivalDayShift': 0,
                    'DepartureTime': '',
                    'DepartureDayShift': 0
                }
            ],
            'Masks': [
                {
                    'From': '2020-10-01',
                    'Until': '2020-10-11',
                    'On': 45
                }
            ]
        },
        {
            'Route': [
                {
                    'AirportID': 222,
                    'ArrivalTime': '',
                    'ArrivalDayShift': 0,
                    'DepartureTime': '09:00:00',
                    'DepartureDayShift': 0
                },
                {
                    'AirportID': 223,
                    'ArrivalTime': '11:00:00',
                    'ArrivalDayShift': 0,
                    'DepartureTime': '12:00:00',
                    'DepartureDayShift': 0
                },
                {
                    'AirportID': 224,
                    'ArrivalTime': '16:00:00',
                    'ArrivalDayShift': 0,
                    'DepartureTime': '',
                    'DepartureDayShift': 0
                }
            ],
            'Masks': [
                {
                    'From': '2020-10-01',
                    'Until': '2020-10-07',
                    'On': 1
                }
            ]
        }
    ]
}


@replace_now('2020-05-20 10:00:00')
def test_generate_settlements_use_baris():
    settlement_from = create_settlement(title='c_from')
    settlement_to = create_settlement(title='c_to')
    station_from = create_station(id=222, settlement=settlement_from, t_type=TransportType.PLANE_ID)
    station_to = create_station(id=223, settlement=settlement_to, t_type=TransportType.PLANE_ID)

    create_thread(
        t_type=TransportType.TRAIN_ID,
        schedule_v1=[
            [None, 0, station_from],
            [30, None, station_to],
        ]
    )

    flight = json_format.Parse(json.dumps(FLIGHT_1), TFlight())
    with mock.patch.object(generation, 'all_flights_iterate', return_value=[flight]):
        SettlementsDataGenerator().generate(use_baris=True)

    data = list(ArchivalSettlementsData.objects.all().aggregate())

    assert len(data) == 2
    assert_that(data, contains_inanyorder(
        has_entries({
            'update_dt': datetime(2020, 5, 20, 10),
            'point_from': settlement_from.point_key,
            'point_to': settlement_to.point_key,
            'transport_type': TransportType.TRAIN_ID,
            'segments': contains_inanyorder(
                has_entries({
                    'station_from': has_entries({'id': station_from.id}),
                    'station_to': has_entries({'id': station_to.id}),
                    'transport_type': has_entries({'id': TransportType.TRAIN_ID}),
                })
            )
        }),
        has_entries({
            'update_dt': datetime(2020, 5, 20, 10),
            'point_from': settlement_from.point_key,
            'point_to': settlement_to.point_key,
            'transport_type': TransportType.PLANE_ID,
            'segments': contains_inanyorder(
                has_entries({
                    'arrival': datetime(2020, 10, 5, 2),
                    'departure': datetime(2020, 10, 5, 1),
                    'title': 'c_from \u2013 c_to',
                    'station_from': has_entries({'id': station_from.id}),
                    'station_to': has_entries({'id': station_to.id}),
                    'transport_type': {'code': 'plane', 'id': TransportType.PLANE_ID, 'title': 'Самолёт'},
                    'run_days_by_tz': {
                        'Europe/Moscow': {
                            'days_text': '5, 6, 21, 28\xa0октября'
                        }
                    },
                    'thread': {'number': 'SU 1'}
                })
            )
        })
    ))


@replace_now('2020-05-20 10:00:00')
def test_generate_settlements_use_baris_only():
    settlement_from = create_settlement(title='c_from')
    settlement_to = create_settlement(title='c_to')
    another_settlement_to = create_settlement(title='a_c_to')
    station_from = create_station(id=222, settlement=settlement_from, t_type=TransportType.PLANE_ID)
    station_to = create_station(id=223, settlement=settlement_to, t_type=TransportType.PLANE_ID)
    another_station_to = create_station(id=224, settlement=another_settlement_to, t_type=TransportType.PLANE_ID)

    flight_1 = json_format.Parse(json.dumps(FLIGHT_1), TFlight())
    flight_2 = json_format.Parse(json.dumps(FLIGHT_2), TFlight())
    with mock.patch.object(generation, 'all_flights_iterate', return_value=[flight_1, flight_2]):
        SettlementsDataGenerator().generate(use_baris=True)

    data = list(ArchivalSettlementsData.objects.all().aggregate())

    assert len(data) == 3
    assert_that(data, contains_inanyorder(
        has_entries({
            'update_dt': datetime(2020, 5, 20, 10),
            'point_from': settlement_from.point_key,
            'point_to': settlement_to.point_key,
            'segments': contains_inanyorder(
                has_entries({
                    'arrival': datetime(2020, 10, 5, 2),
                    'departure': datetime(2020, 10, 5, 1),
                    'title': 'c_from \u2013 c_to',
                    'station_from': has_entries({'id': station_from.id}),
                    'station_to': has_entries({'id': station_to.id}),
                    'transport_type': {'code': 'plane', 'id': TransportType.PLANE_ID, 'title': 'Самолёт'},
                    'run_days_by_tz': {
                        'Europe/Moscow': {
                            'days_text': '5, 6, 21, 28\xa0октября'
                        }
                    },
                    'thread': {'number': 'SU 1'}
                }),
                has_entries({
                    'arrival': datetime(2020, 10, 5, 12),
                    'departure': datetime(2020, 10, 5, 10),
                    'title': 'c_from \u2013 c_to',
                    'station_from': has_entries({'id': station_from.id}),
                    'station_to': has_entries({'id': station_to.id}),
                    'transport_type': {'code': 'plane', 'id': TransportType.PLANE_ID, 'title': 'Самолёт'},
                    'run_days_by_tz': {
                        'Europe/Moscow': {
                            'days_text': '5, 7\xa0октября'
                        }
                    },
                    'thread': {'number': 'SU 1'}
                }),
                has_entries({
                    'arrival': datetime(2020, 10, 1, 13),
                    'departure': datetime(2020, 10, 1, 10, 30),
                    'title': 'c_from \u2013 c_to',
                    'station_from': has_entries({'id': station_from.id}),
                    'station_to': has_entries({'id': station_to.id}),
                    'transport_type': {'code': 'plane', 'id': TransportType.PLANE_ID, 'title': 'Самолёт'},
                    'run_days_by_tz': {
                        'Europe/Moscow': {
                            'days_text': '1, 2, 8, 9\xa0октября'
                        }
                    },
                    'thread': {'number': 'SU 1'}
                }),
                has_entries({
                    'arrival': datetime(2020, 10, 5, 11),
                    'departure': datetime(2020, 10, 5, 9),
                    'title': 'c_from \u2013 a_c_to',
                    'station_from': has_entries({'id': station_from.id}),
                    'station_to': has_entries({'id': station_to.id}),
                    'transport_type': {'code': 'plane', 'id': TransportType.PLANE_ID, 'title': 'Самолёт'},
                    'run_days_by_tz': {
                        'Europe/Moscow': {
                            'days_text': '5\xa0октября'
                        }
                    },
                    'thread': {'number': 'SU 1'}
                })
            )
        }),
        has_entries({
            'update_dt': datetime(2020, 5, 20, 10),
            'point_from': settlement_from.point_key,
            'point_to': another_settlement_to.point_key,
            'segments': contains_inanyorder(
                has_entries({
                    'arrival': datetime(2020, 10, 5, 16),
                    'departure': datetime(2020, 10, 5, 9),
                    'title': 'c_from \u2013 a_c_to',
                    'station_from': has_entries({'id': station_from.id}),
                    'station_to': has_entries({'id': another_station_to.id}),
                    'transport_type': {'code': 'plane', 'id': TransportType.PLANE_ID, 'title': 'Самолёт'},
                    'run_days_by_tz': {
                        'Europe/Moscow': {
                            'days_text': '5\xa0октября'
                        }
                    },
                    'thread': {'number': 'SU 1'}
                })
            )
        }),
        has_entries({
            'update_dt': datetime(2020, 5, 20, 10),
            'point_from': settlement_to.point_key,
            'point_to': another_settlement_to.point_key,
            'segments': contains_inanyorder(
                has_entries({
                    'arrival': datetime(2020, 10, 5, 16),
                    'departure': datetime(2020, 10, 5, 12),
                    'title': 'c_from \u2013 a_c_to',
                    'station_from': has_entries({'id': station_to.id}),
                    'station_to': has_entries({'id': another_station_to.id}),
                    'transport_type': {'code': 'plane', 'id': TransportType.PLANE_ID, 'title': 'Самолёт'},
                    'run_days_by_tz': {
                        'Europe/Moscow': {
                            'days_text': '5\xa0октября'
                        }
                    },
                    'thread': {'number': 'SU 1'}
                })
            )
        })
    ))
