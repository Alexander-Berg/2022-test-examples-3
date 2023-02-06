# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime
from StringIO import StringIO

import mock
import pytest
from hamcrest import anything, assert_that, has_entries, has_entry

from common.apps.suburban_events.factories import ThreadStationStateFactory, EventStateFactory, ThreadStationKeyFactory
from common.apps.suburban_events.models import SuburbanKey
from common.apps.suburban_events.utils import get_rtstation_key
from common.models.schedule import RTStation
from common.models_abstract.schedule import ExpressType
from common.tester.utils.datetime import replace_now

from travel.rasp.export.tests.v3.factories import create_station, create_thread
from travel.rasp.export.tests.v3.helpers import api_get_json


pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]

# Тесты реализуют следующую карту:
#
# 4201 --- 4202 --- 4203 --- 4204
#                центр зоны

NEAREST_SUBURBANS_MDS_DATA = {
    '4201': {
        'fc': [],
        'tc': [
            {
                'from_arrival': None,
                'from_departure': 0,
                'from_id': 4201,
                'name': {
                    'ru': 'ru_title',
                    'tr': 'tr_title_1 — tr_title_3',
                    'uk': 'uk_title_1 — uk_title_3'
                },
                'time': '2017-07-13T12:10:00+03:00',
                'touch_url': ('https://t.rasp.yandex.ru/thread/uid_1/?'
                              'departure_from=2017-07-13&tt=suburban&station_from=4201&station_to=4203'),
                'travel_time': '00:50',
                'tz_thread_start_date': '2017-07-13T12:10:00',
                'uid': 'uid_1'
            }
        ]
    },
    '4202': {
        'fc': [
            {
                'express': 1,
                'from_arrival': 20,
                'from_departure': 30,
                'from_id': 4203,
                'uid': 'uid_3',
                'travel_time': '00:50',
                'time': '2017-07-13T13:50:00+03:00',
                'touch_url': ('https://t.rasp.yandex.ru/thread/uid_3/?'
                              'departure_from=2017-07-13&tt=suburban&station_from=4203&station_to=4202'),
                'tz_thread_start_date': '2017-07-13T13:20:00',
                'name': {
                    'ru': 'ru_title',
                    'tr': 'tr_title_4 — tr_title_2',
                    'uk': 'uk_title_4 — uk_title_2'
                }
            },
            {
                'from_arrival': None,
                'from_departure': 0,
                'from_id': 4203,
                'uid': 'uid_2',
                'travel_time': '00:50',
                'time': '2017-07-13T14:00:00+03:00',
                'touch_url': ('https://t.rasp.yandex.ru/thread/uid_2/?'
                              'departure_from=2017-07-13&tt=suburban&station_from=4203&station_to=4202'),
                'tz_thread_start_date': '2017-07-13T14:00:00',
                'name': {
                    'ru': 'ru_title',
                    'tr': 'tr_title_3 — tr_title_2',
                    'uk': 'uk_title_3 — uk_title_2'
                }
            }
        ],
        'tc': [
            {
                'from_arrival': 10,
                'from_departure': 20,
                'from_id': 4202,
                'name': {
                    'ru': 'ru_title',
                    'tr': 'tr_title_1 — tr_title_3',
                    'uk': 'uk_title_1 — uk_title_3'
                },
                'time': '2017-07-13T12:30:00+03:00',
                'touch_url': ('https://t.rasp.yandex.ru/thread/uid_1/?'
                              'departure_from=2017-07-13&tt=suburban&station_from=4202&station_to=4203'),
                'travel_time': '00:30',
                'tz_thread_start_date': '2017-07-13T12:10:00',
                'uid': 'uid_1'
            }
        ]
    }
}


MAIN_SUBURBANS_MDS_DATA = {
    '4203': [
        {
            'express': 1,
            'from_arrival': 20,
            'from_departure': 30,
            'from_id': 4203,
            'uid': 'uid_3',
            'travel_time': '00:50',
            'time': '2017-07-13T13:50:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_3/?'
                          'departure_from=2017-07-13&tt=suburban&station_from=4203&station_to=4202'),
            'tz_thread_start_date': '2017-07-13T13:20:00',
            'name': {
                'ru': 'ru_title',
                'tr': 'tr_title_4 — tr_title_2',
                'uk': 'uk_title_4 — uk_title_2'
            }
        },
        {
            'from_arrival': None,
            'from_departure': 0,
            'from_id': 4203,
            'uid': 'uid_2',
            'travel_time': '00:50',
            'time': '2017-07-13T14:00:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_2/?'
                          'departure_from=2017-07-13&tt=suburban&station_from=4203&station_to=4202'),
            'tz_thread_start_date': '2017-07-13T14:00:00',
            'name': {
                'ru': 'ru_title',
                'tr': 'tr_title_3 — tr_title_2',
                'uk': 'uk_title_3 — uk_title_2'
            }
        }
    ]
}


def _make_schedule():
    st_1 = create_station(id=4201)
    st_2 = create_station(id=4202)
    st_3 = create_station(id=4203)
    st_4 = create_station(id=4204)

    thread_1_start_dt = datetime(2017, 7, 13, 12, 10)
    thread_1 = create_thread(
        uid='uid_1',
        tz_start_time=thread_1_start_dt.time(),
        schedule_v1=[
            [None, 0, st_1],
            [10, 20, st_2],
            [50, None, st_3],
        ],
    )
    sub_key_1 = SuburbanKey.objects.create(thread=thread_1, key='thread_1_key')

    thread_2_start_dt = datetime(2017, 7, 13, 14)
    thread_2 = create_thread(
        uid='uid_2',
        tz_start_time=thread_2_start_dt.time(),
        schedule_v1=[
            [None, 0, st_3],
            [50, None, st_2],
        ],
    )
    sub_key_2 = SuburbanKey.objects.create(thread=thread_2, key='thread_2_key')

    thread_3_start_dt = datetime(2017, 7, 13, 13, 20)
    thread_3 = create_thread(
        uid='uid_3',
        tz_start_time=thread_3_start_dt.time(),
        schedule_v1=[
            [None, 0, st_4],
            [20, 30, st_3],
            [80, None, st_2],
        ],
        express_type=ExpressType.EXPRESS,
    )
    sub_key_3 = SuburbanKey.objects.create(thread=thread_3, key='thread_3_key')

    rts_th_1_st_1 = RTStation.objects.get(thread=thread_1, station=st_1)
    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key_1.key,
            'thread_start_date': thread_1_start_dt,
            'station_key': get_rtstation_key(rts_th_1_st_1),
            'arrival': rts_th_1_st_1.tz_arrival,
            'departure': rts_th_1_st_1.tz_departure
        }),
        'departure_state': EventStateFactory(**{
            'dt': datetime(2017, 7, 13, 12, 10),
            'type': 'possible_delay',
            'thread_uid': 'uid_1',
            'minutes_from': 0,
            'minutes_to': 0
        }),
        'passed_several_times': False,
        'tz': rts_th_1_st_1.station.time_zone
    })

    rts_th_1_st_2 = RTStation.objects.get(thread=thread_1, station=st_2)
    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key_1.key,
            'thread_start_date': thread_1_start_dt,
            'station_key': get_rtstation_key(rts_th_1_st_2),
            'arrival': rts_th_1_st_2.tz_arrival,
            'departure': rts_th_1_st_2.tz_departure
        }),
        'arrival_state': EventStateFactory(**{
            'dt': datetime(2017, 7, 13, 12, 20),
            'type': 'possible_delay',
            'thread_uid': 'uid_1',
            'minutes_from': 0,
            'minutes_to': 0
        }),
        'departure_state': EventStateFactory(**{
            'dt': datetime(2017, 7, 13, 12, 30),
            'type': 'possible_delay',
            'thread_uid': 'uid_1',
            'minutes_from': 0,
            'minutes_to': 0
        }),
        'passed_several_times': False,
        'tz': rts_th_1_st_2.station.time_zone
    })

    rts_th_2_st_3 = RTStation.objects.get(thread=thread_2, station=st_3)
    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key_2.key,
            'thread_start_date': thread_2_start_dt,
            'station_key': get_rtstation_key(rts_th_2_st_3),
            'arrival': rts_th_2_st_3.tz_arrival,
            'departure': rts_th_2_st_3.tz_departure
        }),
        'departure_state': EventStateFactory(**{
            'dt': datetime(2017, 7, 13, 14, 00),
            'type': 'fact',
            'thread_uid': 'uid_2',
            'minutes_from': 0,
            'minutes_to': 0
        }),
        'passed_several_times': False,
        'tz': rts_th_2_st_3.station.time_zone
    })

    rts_th_3_st_3 = RTStation.objects.get(thread=thread_3, station=st_3)
    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key_3.key,
            'thread_start_date': thread_3_start_dt,
            'station_key': get_rtstation_key(rts_th_3_st_3),
            'arrival': rts_th_3_st_3.tz_arrival,
            'departure': rts_th_3_st_3.tz_departure
        }),
        'arrival_state': EventStateFactory(**{
            'dt': datetime(2017, 7, 13, 13, 40),
            'type': 'possible_delay',
            'thread_uid': 'uid_3',
            'minutes_from': 0,
            'minutes_to': 0
        }),
        'departure_state': EventStateFactory(**{
            'dt': datetime(2017, 7, 13, 13, 50),
            'type': 'possible_delay',
            'thread_uid': 'uid_3',
            'minutes_from': 0,
            'minutes_to': 0
        }),
        'passed_several_times': False,
        'tz': rts_th_3_st_3.station.time_zone
    })


@replace_now(datetime(2017, 7, 13, 12))
def test_nearest_suburbans():
    with mock.patch('common.db.mds.clients.mds_s3_common_client.get_data') as m_data:
        m_data.return_value = StringIO(json.dumps(NEAREST_SUBURBANS_MDS_DATA, ensure_ascii=False, encoding='utf-8'))

        _make_schedule()

        response = api_get_json('/v3/partners/nearest_suburban_to_from_center')

        assert 'fc' not in response['4201']
        assert len(response['4201']['tc']) == 1
        assert len(response['4202']['fc']) == 2
        assert len(response['4202']['tc']) == 1

        assert response['4201']['tc'][0] == {
            'express': None,
            'name': {
                'ru': 'ru_title',
                'tr': 'tr_title_1 — tr_title_3',
                'uk': 'uk_title_1 — uk_title_3'
            },
            'time': '2017-07-13T12:10:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_1/?'
                          'departure_from=2017-07-13&tt=suburban&station_from=4201&station_to=4203'),
            'transport_subtype': None,
            'travel_time': '00:50',
            'uid': 'uid_1',
            'states': {
                'departure': {
                    'title': {
                        'ru': 'возможно опоздание',
                        'uk': 'можливе запiзення'
                    },
                    'type': 'possible_delay'
                }
            }
        }

        assert response['4202']['fc'][0] == {
            'express': 1,
            'name': {
                'ru': 'ru_title',
                'tr': 'tr_title_4 — tr_title_2',
                'uk': 'uk_title_4 — uk_title_2'},
            'time': '2017-07-13T13:50:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_3/?'
                          'departure_from=2017-07-13&tt=suburban&station_from=4203&station_to=4202'),
            'transport_subtype': None,
            'travel_time': '00:50',
            'uid': 'uid_3',
            'states': {
                'arrival': {
                    'title': {
                        'ru': 'возможно опоздание',
                        'uk': 'можливе запiзення'
                    },
                    'type': 'possible_delay'
                },
                'departure': {
                    'title': {
                        'ru': 'возможно опоздание',
                        'uk': 'можливе запiзення'
                    },
                    'type': 'possible_delay'
                }
            }
        }

        assert response['4202']['fc'][1] == {
            'express': None,
            'name': {
                'ru': 'ru_title',
                'tr': 'tr_title_3 — tr_title_2',
                'uk': 'uk_title_3 — uk_title_2'},
            'time': '2017-07-13T14:00:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_2/?'
                          'departure_from=2017-07-13&tt=suburban&station_from=4203&station_to=4202'),
            'transport_subtype': None,
            'travel_time': '00:50',
            'uid': 'uid_2',
        }

        assert response['4202']['tc'][0] == {
            'express': None,
            'name': {
                'ru': 'ru_title',
                'tr': 'tr_title_1 — tr_title_3',
                'uk': 'uk_title_1 — uk_title_3'
            },
            'time': '2017-07-13T12:30:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_1/?'
                          'departure_from=2017-07-13&tt=suburban&station_from=4202&station_to=4203'),
            'transport_subtype': None,
            'travel_time': '00:30',
            'uid': 'uid_1',
            'states': {
                'arrival': {
                    'title': {
                        'ru': 'возможно опоздание',
                        'uk': 'можливе запiзення'
                    },
                    'type': 'possible_delay'
                },
                'departure': {
                    'title': {
                        'ru': 'возможно опоздание',
                        'uk': 'можливе запiзення'
                    },
                    'type': 'possible_delay'
                }
            }
        }


@replace_now(datetime(2017, 7, 13, 12))
def test_main_stations_suburbans():
    with mock.patch('common.db.mds.clients.mds_s3_common_client.get_data') as m_data:
        m_data.return_value = StringIO(json.dumps(MAIN_SUBURBANS_MDS_DATA, ensure_ascii=False, encoding='utf-8'))

        _make_schedule()

        response = api_get_json('/v3/partners/nearest_suburban_main_stations_departures')

        assert len(response['4203']) == 2

        assert response['4203'][0] == {
            'express': 1,
            'name': {
                'ru': 'ru_title',
                'tr': 'tr_title_4 — tr_title_2',
                'uk': 'uk_title_4 — uk_title_2'
            },
            'time': '2017-07-13T13:50:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_3/?'
                          'departure_from=2017-07-13&tt=suburban&station_from=4203&station_to=4202'),
            'transport_subtype': None,
            'travel_time': '00:50',
            'uid': 'uid_3',
            'states': {
                'arrival': {
                    'title': {
                        'ru': 'возможно опоздание',
                        'uk': 'можливе запiзення'
                    },
                    'type': 'possible_delay'
                },
                'departure': {
                    'title': {
                        'ru': 'возможно опоздание',
                        'uk': 'можливе запiзення'
                    },
                    'type': 'possible_delay'
                }
            }
        }

        assert response['4203'][1] == {
            'express': None,
            'name': {
                'ru': 'ru_title',
                'tr': 'tr_title_3 — tr_title_2',
                'uk': 'uk_title_3 — uk_title_2'},
            'time': '2017-07-13T14:00:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_2/?'
                          'departure_from=2017-07-13&tt=suburban&station_from=4203&station_to=4202'),
            'transport_subtype': None,
            'travel_time': '00:50',
            'uid': 'uid_2',
        }


@replace_now(datetime(2017, 7, 13, 12))
def test_not_fail_on_get_states_error():
    def raise_ex():
        raise ValueError

    with mock.patch('common.db.mds.clients.mds_s3_common_client.get_data') as m_data:
        with mock.patch('travel.rasp.export.export.v3.partners.suburban_states._get_states_for_obj',
                        side_effect=raise_ex):

            m_data.return_value = StringIO(json.dumps(MAIN_SUBURBANS_MDS_DATA, ensure_ascii=False, encoding='utf-8'))

            _make_schedule()

            response = api_get_json('/v3/partners/nearest_suburban_main_stations_departures')

            assert len(response['4203']) == 2
            assert_that(response['4203'][0], not has_entry('states', anything()))
            assert_that(response['4203'][0], has_entries({
                'express': 1,
                'name': {
                    'ru': 'ru_title',
                    'tr': 'tr_title_4 — tr_title_2',
                    'uk': 'uk_title_4 — uk_title_2'
                },
                'time': '2017-07-13T13:50:00+03:00',
                'touch_url': ('https://t.rasp.yandex.ru/thread/uid_3/?'
                              'departure_from=2017-07-13&tt=suburban&station_from=4203&station_to=4202'),
                'transport_subtype': None,
                'travel_time': '00:50',
                'uid': 'uid_3'
            }))
