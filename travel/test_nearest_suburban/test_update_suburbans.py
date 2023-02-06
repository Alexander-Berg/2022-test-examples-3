# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import logging
from datetime import datetime, time, date

from travel.rasp.library.python.common23.tester.factories import create_external_direction, create_external_direction_marker
from common.models.geo import StationMajority
from common.models.transport import TransportType
from common.models_abstract.schedule import ExpressType
from common.tester.factories import (create_thread, create_transport_subtype,
                                     create_station, create_settlement, create_suburban_zone)
from common.tester.testcase import TestCase
from common.tester.utils.datetime import replace_now
from travel.rasp.tasks.nearest_suburbans.main_stations_generator import MainStationsSuburbansGenerator
from travel.rasp.tasks.nearest_suburbans.nearest_generator import NearestSuburbansGenerator

create_thread = create_thread.mutate(t_type='suburban', __={'calculate_noderoute': True})

log = logging.getLogger('test_update_suburbans')


class TestNearestAndMainSuburbans(TestCase):
    @replace_now(datetime(2019, 2, 10, 12))
    def test_main_stations(self):
        self._make_schedule()
        generator = MainStationsSuburbansGenerator(log)
        generator.generate_stations_data()
        station_data = generator.prepare_stations_data()

        assert station_data == {
            '1000': {
                'main_stations': [
                    {
                        'id': 1001,
                        'lat': 10.0,
                        'lon': 10.0,
                        'name': {
                            'ru': 'ru_main_1_1',
                            'tr': 'tr_main_1_1',
                            'uk': 'uk_main_1_1'
                        }
                    },
                    {
                        'id': 1002,
                        'lat': 20.0,
                        'lon': 10.0,
                        'name': {
                            'ru': 'ru_main_1_2',
                            'tr': 'tr_main_1_2',
                            'uk': 'uk_main_1_2'
                        }
                    }
                ]
            },
            '2000': {
                'main_stations': [
                    {
                        'id': 2001,
                        'lat': 10.0,
                        'lon': 50.0,
                        'name': {
                            'ru': 'ru_main_2_1',
                            'tr': 'tr_main_2_1',
                            'uk': 'uk_main_2_1'
                        }
                    }
                ]
            }
        }

    @replace_now(datetime(2019, 2, 10, 12))
    def test_nearest_stations(self):
        self._make_schedule()
        generator = NearestSuburbansGenerator(True, log)
        generator.generate_stations_data()
        station_data = generator.prepare_stations_data()

        assert station_data == {
            '1100': {
                'stations': [
                    {
                        'default': True,
                        'cst': {
                            'lat': 10.0,
                            'lon': 10.0,
                            'id': 1001,
                            'name': {
                                'ru': 'ru_main_1_1',
                                'tr': 'tr_main_1_1',
                                'uk': 'uk_main_1_1'
                            }
                        },
                        'st': {
                            'lat': 10.0,
                            'lon': 30.0,
                            'id': 1101,
                            'name': {
                                'ru': 'ru_small_1_1',
                                'tr': 'tr_small_1_1',
                                'uk': 'uk_small_1_1'
                            }
                        }
                    }
                ]
            },
            '2100': {
                'stations': [
                    {
                        'default': True,
                        'cst': {
                            'lat': 20.0,
                            'lon': 10.0,
                            'id': 1002,
                            'name': {
                                'ru': 'ru_main_1_2',
                                'tr': 'tr_main_1_2',
                                'uk': 'uk_main_1_2'
                            }
                        },
                        'st': {
                            'lat': 50.0,
                            'lon': 10.0,
                            'id': 2101,
                            'name': {
                                'ru': 'ru_small_2_1',
                                'tr': 'tr_small_2_1',
                                'uk': 'uk_small_2_1'
                            }
                        }

                    }
                ]
            },
            '3100': {
                'stations': [
                    {
                        'default': True,
                        'cst': {
                            'lat': 10.0,
                            'lon': 50.0,
                            'id': 2001,
                            'name': {
                                'ru': 'ru_main_2_1',
                                'tr': 'tr_main_2_1',
                                'uk': 'uk_main_2_1'
                            }
                        },
                        'st': {
                            'lat': 20.0,
                            'lon': 50.0,
                            'id': 3101,
                            'name': {
                                'ru': 'ru_small_3_1',
                                'tr': 'tr_small_3_1',
                                'uk': 'uk_small_3_1'
                            }
                        }

                    }
                ]
            },
            '4100': {
                'stations': [
                    {
                        'default': True,
                        'cst': {
                            'lat': 10.0,
                            'lon': 50.0,
                            'id': 2001,
                            'name': {
                                'ru': 'ru_main_2_1',
                                'tr': 'tr_main_2_1',
                                'uk': 'uk_main_2_1'
                            }
                        },
                        'st': {
                            'lat': 0.0,
                            'lon': 50.0,
                            'id': 4101,
                            'name': {
                                'ru': 'ru_small_4_1',
                                'tr': 'tr_small_4_1',
                                'uk': 'uk_small_4_1'
                            }
                        }

                    }
                ]
            }
        }

    @replace_now(datetime(2019, 2, 10, 12))
    def test_all_nearest_stations(self):
        self._make_schedule()
        generator = NearestSuburbansGenerator(False, log)
        generator.generate_stations_data()
        station_data = generator.prepare_stations_data()

        assert station_data == {
            '1100': {
                'stations': [
                    {
                        'default': True,
                        'cst': {
                            'lat': 10.0,
                            'lon': 10.0,
                            'id': 1001,
                            'name': {
                                'ru': 'ru_main_1_1',
                                'tr': 'tr_main_1_1',
                                'uk': 'uk_main_1_1'
                            }
                        },
                        'st': {
                            'lat': 10.0,
                            'lon': 30.0,
                            'id': 1101,
                            'name': {
                                'ru': 'ru_small_1_1',
                                'tr': 'tr_small_1_1',
                                'uk': 'uk_small_1_1'
                            }
                        }
                    },
                    {
                        'default': False,
                        'cst': {
                            'lat': 10.0,
                            'lon': 10.0,
                            'id': 1001,
                            'name': {
                                'ru': 'ru_main_1_1',
                                'tr': 'tr_main_1_1',
                                'uk': 'uk_main_1_1'
                            }
                        },
                        'st': {
                            'lat': 10.0,
                            'lon': 40.0,
                            'id': 1102,
                            'name': {
                                'ru': 'ru_small_1_2',
                                'tr': 'tr_small_1_2',
                                'uk': 'uk_small_1_2'
                            }
                        }
                    }
                ]
            },
            '2100': {
                'stations': [
                    {
                        'default': True,
                        'cst': {
                            'lat': 20.0,
                            'lon': 10.0,
                            'id': 1002,
                            'name': {
                                'ru': 'ru_main_1_2',
                                'tr': 'tr_main_1_2',
                                'uk': 'uk_main_1_2'
                            }
                        },
                        'st': {
                            'lat': 50.0,
                            'lon': 10.0,
                            'id': 2101,
                            'name': {
                                'ru': 'ru_small_2_1',
                                'tr': 'tr_small_2_1',
                                'uk': 'uk_small_2_1'
                            }
                        }
                    },
                    {
                        'default': False,
                        'cst': {
                            'lat': 20.0,
                            'lon': 10.0,
                            'id': 1002,
                            'name': {
                                'ru': 'ru_main_1_2',
                                'tr': 'tr_main_1_2',
                                'uk': 'uk_main_1_2'
                            }
                        },
                        'st': {
                            'lat': 40.0,
                            'lon': 10.0,
                            'id': 2102,
                            'name': {
                                'ru': 'ru_small_2_2',
                                'tr': 'tr_small_2_2',
                                'uk': 'uk_small_2_2'
                            }
                        }
                    }
                ]
            },
            '3100': {
                'stations': [
                    {
                        'default': True,
                        'cst': {
                            'lat': 10.0,
                            'lon': 50.0,
                            'id': 2001,
                            'name': {
                                'ru': 'ru_main_2_1',
                                'tr': 'tr_main_2_1',
                                'uk': 'uk_main_2_1'
                            }
                        },
                        'st': {
                            'lat': 20.0,
                            'lon': 50.0,
                            'id': 3101,
                            'name': {
                                'ru': 'ru_small_3_1',
                                'tr': 'tr_small_3_1',
                                'uk': 'uk_small_3_1'
                            }
                        }
                    }
                ]
            },
            '4100': {
                'stations': [
                    {
                        'default': True,
                        'cst': {
                            'lat': 10.0,
                            'lon': 50.0,
                            'id': 2001,
                            'name': {
                                'ru': 'ru_main_2_1',
                                'tr': 'tr_main_2_1',
                                'uk': 'uk_main_2_1'
                            }
                        },
                        'st': {
                            'lat': 0.0,
                            'lon': 50.0,
                            'id': 4101,
                            'name': {
                                'ru': 'ru_small_4_1',
                                'tr': 'tr_small_4_1',
                                'uk': 'uk_small_4_1'
                            }
                        }

                    },
                    {
                        'default': False,
                        'cst': {
                            'lat': 10.0,
                            'lon': 50.0,
                            'id': 2001,
                            'name': {
                                'ru': 'ru_main_2_1',
                                'tr': 'tr_main_2_1',
                                'uk': 'uk_main_2_1'
                            }
                        },
                        'st': {
                            'lat': 0.0,
                            'lon': 60.0,
                            'id': 4102,
                            'name': {
                                'ru': 'ru_small_4_2',
                                'tr': 'tr_small_4_2',
                                'uk': 'uk_small_4_2'
                            }
                        }

                    }
                ]
            }
        }

    @replace_now(datetime(2019, 2, 10, 12))
    def test_main_suburbans(self):
        self._make_schedule()
        generator = MainStationsSuburbansGenerator(log)
        generator.generate_stations_data()
        suburbans_data = generator.generate_suburban_threads_data()

        assert len(suburbans_data) == 3
        assert set(suburbans_data.keys()) == {'1001', '1002', '2001'}

        assert len(suburbans_data[u'1001']) == 4
        assert [s['uid'] for s in suburbans_data[u'1001']] == [
            u'uid_m_1_s_1',
            u'uid_m_1_m_2',
            u'uid_m_1_s_3',
            u'uid_m_2_m_2'
        ]
        assert [s['time'] for s in suburbans_data[u'1001']] == [
            '2019-02-10T12:10:00+03:00',
            '2019-02-10T12:20:00+03:00',
            '2019-02-10T12:30:00+03:00',
            '2019-02-10T15:41:00+03:00'
        ]

        assert len(suburbans_data[u'1002']) == 10
        assert [s['uid'] for s in suburbans_data[u'1002']] == [
            u'uid_m_1_s_2_1',
            u'uid_m_1_s_2_2',
            u'uid_m_1_s_2_3',
            u'uid_m_1_s_2_4',
            u'uid_m_1_s_2_5',
            u'uid_m_1_s_2_6',
            u'uid_m_1_s_2_7',
            u'uid_m_1_s_2_8',
            u'uid_m_1_s_2_9',
            u'uid_m_1_s_2_10',
        ]
        assert [s['time'] for s in suburbans_data[u'1002']] == [
            '2019-02-10T14:10:00+03:00',
            '2019-02-10T14:20:00+03:00',
            '2019-02-10T14:30:00+03:00',
            '2019-02-10T14:40:00+03:00',
            '2019-02-10T14:50:00+03:00',
            '2019-02-10T15:00:00+03:00',
            '2019-02-10T15:10:00+03:00',
            '2019-02-10T15:20:00+03:00',
            '2019-02-10T15:30:00+03:00',
            '2019-02-10T15:40:00+03:00',
        ]

        assert len(suburbans_data[u'2001']) == 5
        assert [s['uid'] for s in suburbans_data[u'2001']] == [
            u'uid_m_1_s_3',
            u'uid_s_3_m_1',
            u'uid_s_3_s_1',
            u'uid_m_2_m_2',
            u'uid_m_2_s_4',
        ]
        assert [s['time'] for s in suburbans_data[u'2001']] == [
            '2019-02-10T13:11:00+03:00',
            '2019-02-10T14:21:00+03:00',
            '2019-02-10T14:31:00+03:00',
            '2019-02-10T15:00:00+03:00',
            '2019-02-10T17:20:00+03:00',
        ]

        assert suburbans_data['1001'][0] == {
            'uid': u'uid_m_1_s_1',
            'name': {
                'ru': 'ru_m_1_s_1',
                'tr': 'tr_m_1_s_1',
                'uk': 'uk_m_1_s_1'
            },
            'tz_thread_start_date': '2019-02-10T12:10:00',
            'travel_time': u'00:20',
            'from_id': 1001,
            'from_arrival': None,
            'from_departure': 0,
            'time': '2019-02-10T12:10:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_m_1_s_1/?'
                          'departure=2019-02-10&tt=suburban&station_from=1001&station_to=1101'),
            'transport_subtype': {
                'ru': u'подтип_электрички_ru',
                'tr': 'suburban_subtype_tr',
                'uk': 'suburban_subtype_uk'
            }
        }

        assert suburbans_data['1001'][3] == {
            'uid': u'uid_m_2_m_2',
            'name': {
                'ru': 'ru_m_2_m_2',
                'tr': 'tr_m_2_m_2',
                'uk': 'uk_m_2_m_2'
            },
            'tz_thread_start_date': '2019-02-10T15:00:00',
            'travel_time': u'00:39',
            'from_id': 1001,
            'from_arrival': 40,
            'from_departure': 41,
            'time': '2019-02-10T15:41:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_m_2_m_2/?'
                          'departure=2019-02-10&tt=suburban&station_from=1001&station_to=2001'),
            'transport_subtype': {
                'ru': u'подтип_электрички_ru',
                'tr': 'suburban_subtype_tr',
                'uk': 'suburban_subtype_uk'
            }
        }

        assert suburbans_data['1002'][0] == {
            'uid': u'uid_m_1_s_2_1',
            'name': {
                'ru': 'ru_m_1_s_2_1',
                'tr': 'tr_m_1_s_2_1',
                'uk': 'uk_m_1_s_2_1'
            },
            'tz_thread_start_date': '2019-02-10T14:10:00',
            'travel_time': u'00:30',
            'from_id': 1002,
            'from_arrival': None,
            'from_departure': 0,
            'time': '2019-02-10T14:10:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_m_1_s_2_1/?'
                          'departure=2019-02-10&tt=suburban&station_from=1002&station_to=2101'),
            'transport_subtype': {
                'ru': u'подтип_электрички_ru',
                'tr': 'suburban_subtype_tr',
                'uk': 'suburban_subtype_uk'
            },
            'express': 1
        }

    @replace_now(datetime(2019, 2, 10, 15))
    def test_main_suburbans_late(self):
        self._make_schedule()
        generator = MainStationsSuburbansGenerator(log)
        generator.generate_stations_data()
        suburbans_data = generator.generate_suburban_threads_data()

        assert len(suburbans_data) == 3
        assert set(suburbans_data.keys()) == {'1001', '1002', '2001'}

        assert len(suburbans_data[u'1001']) == 4
        assert [s['uid'] for s in suburbans_data[u'1001']] == [
            u'uid_m_2_m_2',
            u'uid_m_1_s_1',
            u'uid_m_1_m_2',
            u'uid_m_1_s_3',
        ]
        assert [s['time'] for s in suburbans_data[u'1001']] == [
            '2019-02-10T15:41:00+03:00',
            '2019-02-11T12:10:00+03:00',
            '2019-02-11T12:20:00+03:00',
            '2019-02-11T12:30:00+03:00',
        ]

        assert len(suburbans_data[u'1002']) == 10
        assert [s['uid'] for s in suburbans_data[u'1002']] == [
            u'uid_m_1_s_2_7',
            u'uid_m_1_s_2_8',
            u'uid_m_1_s_2_9',
            u'uid_m_1_s_2_10',
            u'uid_m_1_s_2_11',
            u'uid_m_1_s_2_2',
            u'uid_m_1_s_2_3',
            u'uid_m_1_s_2_4',
            u'uid_m_1_s_2_5',
            u'uid_m_1_s_2_6',
        ]
        assert [s['time'] for s in suburbans_data[u'1002']] == [
            '2019-02-10T15:10:00+03:00',
            '2019-02-10T15:20:00+03:00',
            '2019-02-10T15:30:00+03:00',
            '2019-02-10T15:40:00+03:00',
            '2019-02-10T15:50:00+03:00',
            '2019-02-11T14:20:00+03:00',
            '2019-02-11T14:30:00+03:00',
            '2019-02-11T14:40:00+03:00',
            '2019-02-11T14:50:00+03:00',
            '2019-02-11T15:00:00+03:00',
        ]

        assert len(suburbans_data[u'2001']) == 5
        assert [s['uid'] for s in suburbans_data[u'2001']] == [
            u'uid_m_2_s_4',
            u'uid_m_1_s_3',
            u'uid_s_3_m_1',
            u'uid_s_3_s_1',
            u'uid_m_2_m_2',
        ]
        assert [s['time'] for s in suburbans_data[u'2001']] == [
            '2019-02-10T17:20:00+03:00',
            '2019-02-11T13:11:00+03:00',
            '2019-02-11T14:21:00+03:00',
            '2019-02-11T14:31:00+03:00',
            '2019-02-11T15:00:00+03:00',
        ]

    @staticmethod
    def _check_result_default_suburbans(suburbans_data):
        assert set(suburbans_data[u'1101']) == {'fc', 'tc'}
        assert set(suburbans_data[u'2101']) == {'fc', 'tc'}
        assert set(suburbans_data[u'3101']) == {'fc', 'tc'}
        assert set(suburbans_data[u'4101']) == {'fc', 'tc'}

        suburbans = suburbans_data[u'1101']['fc']
        assert len(suburbans) == 4
        assert [s['uid'] for s in suburbans] == [
            u'uid_m_1_s_1',
            u'uid_m_1_m_2',
            u'uid_m_1_s_3',
            u'uid_m_2_m_2',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T12:10:00+03:00',
            '2019-02-10T12:20:00+03:00',
            '2019-02-10T12:30:00+03:00',
            '2019-02-10T15:41:00+03:00',
        ]

        suburbans = suburbans_data[u'1101']['tc']
        assert len(suburbans) == 2
        assert [s['uid'] for s in suburbans] == [
            u'uid_s_3_m_1',
            u'uid_m_2_m_2',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T14:41:00+03:00',
            '2019-02-10T15:21:00+03:00',
        ]

        suburbans = suburbans_data[u'2101']['fc']
        assert len(suburbans) == 10
        assert [s['uid'] for s in suburbans] == [
            u'uid_m_1_s_2_1',
            u'uid_m_1_s_2_2',
            u'uid_m_1_s_2_3',
            u'uid_m_1_s_2_4',
            u'uid_m_1_s_2_5',
            u'uid_m_1_s_2_6',
            u'uid_m_1_s_2_7',
            u'uid_m_1_s_2_8',
            u'uid_m_1_s_2_9',
            u'uid_m_1_s_2_10',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T14:10:00+03:00',
            '2019-02-10T14:20:00+03:00',
            '2019-02-10T14:30:00+03:00',
            '2019-02-10T14:40:00+03:00',
            '2019-02-10T14:50:00+03:00',
            '2019-02-10T15:00:00+03:00',
            '2019-02-10T15:10:00+03:00',
            '2019-02-10T15:20:00+03:00',
            '2019-02-10T15:30:00+03:00',
            '2019-02-10T15:40:00+03:00',
        ]

        suburbans = suburbans_data[u'2101']['tc']
        assert len(suburbans) == 1
        assert [s['uid'] for s in suburbans] == [
            u'uid_s_2_m_1',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T13:00:00+03:00',
        ]

        suburbans = suburbans_data[u'3101']['fc']
        assert len(suburbans) == 1
        assert [s['uid'] for s in suburbans] == [
            u'uid_m_1_s_3',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T13:11:00+03:00',
        ]

        suburbans = suburbans_data[u'3101']['tc']
        assert len(suburbans) == 4
        assert [s['uid'] for s in suburbans] == [
            u'uid_s_3_m_2',
            u'uid_s_3_m_1',
            u'uid_s_3_s_1',
            u'uid_a_m_2',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T16:00:00+05:00',
            '2019-02-10T16:10:00+05:00',
            '2019-02-10T16:20:00+05:00',
            '2019-02-10T17:11:00+05:00',
        ]

        suburbans = suburbans_data[u'4101']['fc']
        assert len(suburbans) == 0

        suburbans = suburbans_data[u'4101']['tc']
        assert len(suburbans) == 1
        assert [s['uid'] for s in suburbans] == [
            u'uid_s_4_m_2',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T17:00:00+03:00',
        ]

        assert suburbans_data['1101']['fc'][0] == {
            'uid': u'uid_m_1_s_1',
            'name': {
                'ru': u'ru_m_1_s_1',
                'tr': u'tr_m_1_s_1',
                'uk': u'uk_m_1_s_1'
            },
            'tz_thread_start_date': '2019-02-10T12:10:00',
            'travel_time': u'00:20',
            'from_id': 1001,
            'from_arrival': None,
            'from_departure': 0,
            'time': '2019-02-10T12:10:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_m_1_s_1/?'
                          'departure=2019-02-10&tt=suburban&station_from=1001&station_to=1101'),
            'transport_subtype': {
                'ru': u'подтип_электрички_ru',
                'tr': 'suburban_subtype_tr',
                'uk': 'suburban_subtype_uk'
            }
        }

        assert suburbans_data['1101']['tc'][0] == {
            'uid': u'uid_s_3_m_1',
            'name': {
                'ru': u'ru_s_3_m_1',
                'tr': u'tr_s_3_m_1',
                'uk': u'uk_s_3_m_1'
            },
            'tz_thread_start_date': '2019-02-10T14:10:00',
            'travel_time': u'00:19',
            'from_id': 1101,
            'from_arrival': 30,
            'from_departure': 31,
            'time': '2019-02-10T14:41:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_s_3_m_1/?'
                          'departure=2019-02-10&tt=suburban&station_from=1101&station_to=1001'),
            'transport_subtype': {
                'ru': u'подтип_электрички_ru',
                'tr': 'suburban_subtype_tr',
                'uk': 'suburban_subtype_uk'
            }
        }

        assert suburbans_data['2101']['fc'][0] == {
            'uid': u'uid_m_1_s_2_1',
            'name': {
                'ru': u'ru_m_1_s_2_1',
                'tr': u'tr_m_1_s_2_1',
                'uk': u'uk_m_1_s_2_1'
            },
            'tz_thread_start_date': '2019-02-10T14:10:00',
            'travel_time': u'00:30',
            'from_id': 1002,
            'from_arrival': None,
            'from_departure': 0,
            'time': '2019-02-10T14:10:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_m_1_s_2_1/?'
                          'departure=2019-02-10&tt=suburban&station_from=1002&station_to=2101'),
            'transport_subtype': {
                'ru': u'подтип_электрички_ru',
                'tr': 'suburban_subtype_tr',
                'uk': 'suburban_subtype_uk'
            },
            'express': 1
        }

        assert suburbans_data['3101']['tc'][0] == {
            'uid': u'uid_s_3_m_2',
            'name': {
                'ru': u'ru_s_3_m_2',
                'tr': u'tr_s_3_m_2',
                'uk': u'uk_s_3_m_2'
            },
            'tz_thread_start_date': '2019-02-10T14:00:00',
            'travel_time': u'00:10',
            'from_id': 3101,
            'from_arrival': None,
            'from_departure': 0,
            'time': '2019-02-10T16:00:00+05:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_s_3_m_2/?'
                          'departure=2019-02-10&tt=suburban&station_from=3101&station_to=2001'),
            'transport_subtype': {
                'ru': u'подтип_электрички_ru',
                'tr': 'suburban_subtype_tr',
                'uk': 'suburban_subtype_uk'
            }
        }

    @replace_now(datetime(2019, 2, 10, 12))
    def test_nearest_suburbans(self):
        self._make_schedule()
        generator = NearestSuburbansGenerator(True, log)
        generator.generate_stations_data()
        suburbans_data = generator.generate_suburban_threads_data()
        assert len(suburbans_data) == 4
        assert set(suburbans_data.keys()) == {'1101', '2101', '3101', '4101'}

        self._check_result_default_suburbans(suburbans_data)

    @replace_now(datetime(2019, 2, 10, 12))
    def test_all_nearest_suburbans(self):
        self._make_schedule()
        generator = NearestSuburbansGenerator(False, log)
        generator.generate_stations_data()
        suburbans_data = generator.generate_suburban_threads_data()

        assert len(suburbans_data) == 7
        assert set(suburbans_data.keys()) == {'1101', '1102', '2101', '2102', '3101', '4101', '4102'}

        self._check_result_default_suburbans(suburbans_data)

        assert set(suburbans_data[u'1102']) == {'fc', 'tc'}
        assert set(suburbans_data[u'2102']) == {'fc', 'tc'}
        assert set(suburbans_data[u'4102']) == {'fc', 'tc'}

        suburbans = suburbans_data[u'1102']['fc']
        assert len(suburbans) == 2
        assert [s['uid'] for s in suburbans] == [
            u'uid_m_1_s_3',
            u'uid_m_2_m_2',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T12:30:00+03:00',
            '2019-02-10T15:41:00+03:00',
        ]

        suburbans = suburbans_data[u'1102']['tc']
        assert len(suburbans) == 2
        assert [s['uid'] for s in suburbans] == [
            u'uid_s_3_m_1',
            u'uid_m_2_m_2',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T14:31:00+03:00',
            '2019-02-10T15:11:00+03:00',
        ]

        suburbans = suburbans_data[u'2102']['fc']
        assert len(suburbans) == 10
        assert [s['uid'] for s in suburbans] == [
            u'uid_m_1_s_2_2',
            u'uid_m_1_s_2_3',
            u'uid_m_1_s_2_4',
            u'uid_m_1_s_2_5',
            u'uid_m_1_s_2_6',
            u'uid_m_1_s_2_7',
            u'uid_m_1_s_2_8',
            u'uid_m_1_s_2_9',
            u'uid_m_1_s_2_10',
            u'uid_m_1_s_2_11',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T14:20:00+03:00',
            '2019-02-10T14:30:00+03:00',
            '2019-02-10T14:40:00+03:00',
            '2019-02-10T14:50:00+03:00',
            '2019-02-10T15:00:00+03:00',
            '2019-02-10T15:10:00+03:00',
            '2019-02-10T15:20:00+03:00',
            '2019-02-10T15:30:00+03:00',
            '2019-02-10T15:40:00+03:00',
            '2019-02-10T15:50:00+03:00',
        ]

        suburbans = suburbans_data[u'2102']['tc']
        assert len(suburbans) == 1
        assert [s['uid'] for s in suburbans] == [
            u'uid_s_2_m_1',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T13:11:00+03:00',
        ]

        suburbans = suburbans_data[u'4102']['fc']
        assert len(suburbans) == 1
        assert [s['uid'] for s in suburbans] == [
            u'uid_m_2_s_4',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T17:20:00+03:00',
        ]

        suburbans = suburbans_data[u'4102']['tc']
        assert len(suburbans) == 0

        assert suburbans_data['1102']['fc'][0] == {
            'uid': u'uid_m_1_s_3',
            'name': {
                'ru': u'ru_m_1_s_3',
                'tr': u'tr_m_1_s_3',
                'uk': u'uk_m_1_s_3'
            },
            'tz_thread_start_date': '2019-02-10T12:30:00',
            'travel_time': u'00:30',
            'from_id': 1001,
            'from_arrival': None,
            'from_departure': 0,
            'time': '2019-02-10T12:30:00+03:00',
            'touch_url': ('https://t.rasp.yandex.ru/thread/uid_m_1_s_3/?'
                          'departure=2019-02-10&tt=suburban&station_from=1001&station_to=1102'),
            'transport_subtype': {
                'ru': u'подтип_электрички_ru',
                'tr': 'suburban_subtype_tr',
                'uk': 'suburban_subtype_uk'
            }
        }

    @replace_now(datetime(2019, 2, 10, 15))
    def test_nearest_suburbans_late(self):
        self._make_schedule()
        generator = NearestSuburbansGenerator(True, log)
        generator.generate_stations_data()
        suburbans_data = generator.generate_suburban_threads_data()
        assert len(suburbans_data) == 4
        assert set(suburbans_data.keys()) == {'1101', '2101', '3101', '4101'}

        assert set(suburbans_data[u'1101']) == {'fc', 'tc'}
        assert set(suburbans_data[u'2101']) == {'fc', 'tc'}
        assert set(suburbans_data[u'3101']) == {'fc', 'tc'}
        assert set(suburbans_data[u'4101']) == {'fc', 'tc'}

        suburbans = suburbans_data[u'1101']['fc']
        assert len(suburbans) == 4
        assert [s['uid'] for s in suburbans] == [
            u'uid_m_2_m_2',
            u'uid_m_1_s_1',
            u'uid_m_1_m_2',
            u'uid_m_1_s_3',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T15:41:00+03:00',
            '2019-02-11T12:10:00+03:00',
            '2019-02-11T12:20:00+03:00',
            '2019-02-11T12:30:00+03:00',
        ]

        suburbans = suburbans_data[u'1101']['tc']
        assert len(suburbans) == 2
        assert [s['uid'] for s in suburbans] == [
            u'uid_m_2_m_2',
            u'uid_s_3_m_1',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T15:21:00+03:00',
            '2019-02-11T14:41:00+03:00',
        ]

        suburbans = suburbans_data[u'2101']['fc']
        assert len(suburbans) == 10
        assert [s['uid'] for s in suburbans] == [
            u'uid_m_1_s_2_7',
            u'uid_m_1_s_2_8',
            u'uid_m_1_s_2_9',
            u'uid_m_1_s_2_10',
            u'uid_m_1_s_2_11',
            u'uid_m_1_s_2_2',
            u'uid_m_1_s_2_3',
            u'uid_m_1_s_2_4',
            u'uid_m_1_s_2_5',
            u'uid_m_1_s_2_6',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T15:10:00+03:00',
            '2019-02-10T15:20:00+03:00',
            '2019-02-10T15:30:00+03:00',
            '2019-02-10T15:40:00+03:00',
            '2019-02-10T15:50:00+03:00',
            '2019-02-11T14:20:00+03:00',
            '2019-02-11T14:30:00+03:00',
            '2019-02-11T14:40:00+03:00',
            '2019-02-11T14:50:00+03:00',
            '2019-02-11T15:00:00+03:00',
        ]

        suburbans = suburbans_data[u'2101']['tc']
        assert len(suburbans) == 1
        assert [s['uid'] for s in suburbans] == [
            u'uid_s_2_m_1',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-11T13:00:00+03:00',
        ]

        suburbans = suburbans_data[u'3101']['fc']
        assert len(suburbans) == 1
        assert [s['uid'] for s in suburbans] == [
            u'uid_m_1_s_3',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-11T13:11:00+03:00',
        ]

        suburbans = suburbans_data[u'3101']['tc']
        assert len(suburbans) == 4
        assert [s['uid'] for s in suburbans] == [
            u'uid_a_m_2',
            u'uid_s_3_m_2',
            u'uid_s_3_m_1',
            u'uid_s_3_s_1',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T17:11:00+05:00',
            '2019-02-11T16:00:00+05:00',
            '2019-02-11T16:10:00+05:00',
            '2019-02-11T16:20:00+05:00',
        ]

        suburbans = suburbans_data[u'4101']['fc']
        assert len(suburbans) == 0

        suburbans = suburbans_data[u'4101']['tc']
        assert len(suburbans) == 1
        assert [s['uid'] for s in suburbans] == [
            u'uid_s_4_m_2',
        ]
        assert [s['time'] for s in suburbans] == [
            '2019-02-10T17:00:00+03:00',
        ]

    def _make_schedule(self):
        """
        Тест реализует следующуюя карту. *-главная станция города
                                                                 small_4_1  small_4_2
                                                                    |       /
        *main_1_1 --- main_1_3 --- *small_1_1 --- small_1_2 --- *main_2_1
                                                                    |
        *main_1_2                                               *small_3_1 (TZ=+5)
           |                                                        |
        main_1_4                                                 airport
           |
        small_2_2
           |
        *small_2_1
        """
        sett_main_1 = create_settlement(id=1000, _geo_id=1000)
        sett_main_2 = create_settlement(id=2000, _geo_id=2000)
        sett_small_1 = create_settlement(id=1100, _geo_id=1100)
        sett_small_2 = create_settlement(id=2100, _geo_id=2100)
        sett_small_3 = create_settlement(id=3100, _geo_id=3100, time_zone='Asia/Yekaterinburg')
        sett_small_4 = create_settlement(id=4100, _geo_id=4100)

        zone_1 = create_suburban_zone(id=1, settlement=sett_main_1)
        zone_2 = create_suburban_zone(id=2, settlement=sett_main_2)

        main_1_1 = create_station(id=1001, settlement=sett_main_1, suburban_zone=zone_1,
                                  majority=StationMajority.MAIN_IN_CITY_ID,
                                  latitude=10.0, longitude=10.0, t_type=TransportType.TRAIN_ID,
                                  title_ru='ru_main_1_1', title_uk='uk_main_1_1', title_tr='tr_main_1_1')
        main_1_2 = create_station(id=1002, settlement=sett_main_1, suburban_zone=zone_1,
                                  majority=StationMajority.MAIN_IN_CITY_ID,
                                  latitude=20.0, longitude=10.0, t_type=TransportType.TRAIN_ID,
                                  title_ru='ru_main_1_2', title_uk='uk_main_1_2', title_tr='tr_main_1_2')
        main_1_3 = create_station(id=1003, settlement=sett_main_1, suburban_zone=zone_1,
                                  majority=StationMajority.IN_TABLO,
                                  latitude=10.0, longitude=20.0, t_type=TransportType.TRAIN_ID,
                                  title_ru='ru_main_1_3', title_uk='uk_main_1_3', title_tr='tr_main_1_3')
        main_1_4 = create_station(id=1004, settlement=sett_main_1, suburban_zone=zone_1,
                                  majority=StationMajority.NOT_IN_TABLO_ID,
                                  latitude=30.0, longitude=10.0, t_type=TransportType.TRAIN_ID,
                                  title_ru='ru_main_1_4', title_uk='uk_main_1_4', title_tr='tr_main_1_4')
        main_2_1 = create_station(id=2001, settlement=sett_main_2, suburban_zone=zone_2,
                                  majority=StationMajority.IN_TABLO,
                                  latitude=10.0, longitude=50.0, t_type=TransportType.TRAIN_ID,
                                  title_ru='ru_main_2_1', title_uk='uk_main_2_1', title_tr='tr_main_2_1')
        small_1_1 = create_station(id=1101, settlement=sett_small_1, suburban_zone=zone_1,
                                   majority=StationMajority.MAIN_IN_CITY_ID,
                                   latitude=10.0, longitude=30.0, t_type=TransportType.TRAIN_ID,
                                   title_ru='ru_small_1_1', title_uk='uk_small_1_1', title_tr='tr_small_1_1')
        small_1_2 = create_station(id=1102, settlement=sett_small_1, suburban_zone=zone_1,
                                   majority=StationMajority.IN_TABLO,
                                   latitude=10.0, longitude=40.0, t_type=TransportType.TRAIN_ID,
                                   title_ru='ru_small_1_2', title_uk='uk_small_1_2', title_tr='tr_small_1_2')
        small_2_1 = create_station(id=2101, settlement=sett_small_2, suburban_zone=zone_1,
                                   majority=StationMajority.IN_TABLO,
                                   latitude=50.0, longitude=10.0, t_type=TransportType.TRAIN_ID,
                                   title_ru='ru_small_2_1', title_uk='uk_small_2_1', title_tr='tr_small_2_1')
        small_2_2 = create_station(id=2102, settlement=sett_small_2, suburban_zone=zone_1,
                                   majority=StationMajority.NOT_IN_TABLO_ID,
                                   latitude=40.0, longitude=10.0, t_type=TransportType.TRAIN_ID,
                                   title_ru='ru_small_2_2', title_uk='uk_small_2_2', title_tr='tr_small_2_2')
        small_3_1 = create_station(id=3101, settlement=sett_small_3, suburban_zone=zone_2,
                                   majority=StationMajority.MAIN_IN_CITY_ID, time_zone='Asia/Yekaterinburg',
                                   latitude=20.0, longitude=50.0, t_type=TransportType.TRAIN_ID,
                                   title_ru='ru_small_3_1', title_uk='uk_small_3_1', title_tr='tr_small_3_1')
        airport = create_station(id=2011, settlement=sett_main_2, suburban_zone=zone_2,
                                 majority=StationMajority.IN_TABLO_ID,
                                 latitude=20.0, longitude=50.0, t_type=TransportType.PLANE_ID,
                                 title_ru='ru_airport', title_uk='uk_airport', title_tr='tr_airport')
        small_4_1 = create_station(id=4101, settlement=sett_small_4, suburban_zone=zone_2,
                                   majority=StationMajority.MAIN_IN_CITY_ID,
                                   latitude=0.0, longitude=50.0, t_type=TransportType.TRAIN_ID,
                                   title_ru='ru_small_4_1', title_uk='uk_small_4_1', title_tr='tr_small_4_1')
        small_4_2 = create_station(id=4102, settlement=sett_small_4, suburban_zone=zone_2,
                                   majority=StationMajority.IN_TABLO,
                                   latitude=0.0, longitude=60.0, t_type=TransportType.TRAIN_ID,
                                   title_ru='ru_small_4_2', title_uk='uk_small_4_2', title_tr='tr_small_4_2')

        self._create_external_direction(zone_1, main_1_1,
                                        [main_1_1, main_1_3, small_1_1, small_1_2, main_2_1])

        self._create_external_direction(zone_1, main_1_2,
                                        [main_1_2, main_1_4, small_2_2, small_2_1])

        self._create_external_direction(zone_2, main_2_1,
                                        [main_2_1, small_1_2, small_1_1, main_1_3, main_1_1])

        self._create_external_direction(zone_2, main_2_1,
                                        [main_2_1, small_3_1, airport])

        self._create_external_direction(zone_2, main_2_1,
                                        [main_2_1, small_4_1, small_4_2])

        self._subtype = create_transport_subtype(
            t_type=TransportType.objects.get(id=TransportType.SUBURBAN_ID),
            title_short_ru=u'подтип_электрички_ru',
            title_short_uk='suburban_subtype_uk',
            title_short_tr='suburban_subtype_tr',
        )

        self._create_thread('m_1_s_1', time(12, 10), [
            [None, 0, main_1_1],
            [10, 11, main_1_3],
            [20, None, small_1_1]
        ])

        self._create_thread('m_1_m_2', time(12, 20), [
            [None, 0, main_1_1],
            [10, 11, main_1_3],
            [20, 21, small_1_1],
            [40, None, main_2_1],
        ])

        self._create_thread('m_1_s_3', time(12, 30), [
            [None, 0, main_1_1],
            [10, 11, main_1_3],
            [20, 21, small_1_1],
            [30, 31, small_1_2],
            [40, 41, main_2_1],
            [170, None, small_3_1],
        ])

        self._create_thread('s_3_m_2', time(14, 00), [
            [None, 0, small_3_1],
            [10, None, main_2_1]
        ])

        self._create_thread('s_3_m_1', time(14, 10), [
            [None, 0, small_3_1],
            [10, 11, main_2_1],
            [20, 21, small_1_2],
            [30, 31, small_1_1],
            [40, 41, main_1_3],
            [50, None, main_1_1],
        ])

        self._create_thread('s_3_s_1', time(14, 20), [
            [None, 0, small_3_1],
            [10, 11, main_2_1],
            [20, 21, small_1_2],
            [30, None, small_1_1]
        ])

        self._create_thread('a_m_2', time(15, 00), [
            [None, 0, airport],
            [10, 11, small_3_1],
            [20, None, main_2_1]
        ])

        self._create_thread('m_2_m_2', time(15, 00), [
            [None, 0, main_2_1],
            [10, 11, small_1_2],
            [20, 21, small_1_1],
            [30, 31, main_1_3],
            [40, 41, main_1_1],
            [50, 51, main_1_3],
            [60, 61, small_1_1],
            [70, 71, small_1_2],
            [80, None, main_2_1]
        ])

        self._create_thread('s_4_m_2', time(17, 00), [
            [None, 0, small_4_1],
            [10, None, main_2_1]
        ])

        self._create_thread('m_2_s_4', time(17, 20), [
            [None, 0, main_2_1],
            [10, None, small_4_2]
        ])

        self._create_thread('s_2_m_1', time(13, 00), [
            [None, 0, small_2_1],
            [10, 11, small_2_2],
            [20, 21, main_1_4],
            [30, None, main_1_2]
        ])

        create_thread(
            uid=u'uid_m_1_s_2_1',
            tz_start_time=time(14, 10),
            schedule_v1=[
                [None, 0, main_1_2],
                [30, None, small_2_1]
            ],
            t_subtype=self._subtype,
            title='ru_m_1_s_2_1',
            title_tr='tr_m_1_s_2_1',
            title_uk='uk_m_1_s_2_1',
            express_type=ExpressType.EXPRESS,
            year_days=[date(2019, 2, 10)]
        )

        self._create_thread('m_1_s_2_2', time(14, 20), [
            [None, 0, main_1_2],
            [10, 11, main_1_4],
            [20, 21, small_2_2],
            [30, None, small_2_1]
        ])

        self._create_thread('m_1_s_2_3', time(14, 30), [
            [None, 0, main_1_2],
            [10, 11, main_1_4],
            [20, 21, small_2_2],
            [30, None, small_2_1]
        ])

        self._create_thread('m_1_s_2_4', time(14, 40), [
            [None, 0, main_1_2],
            [10, 11, main_1_4],
            [20, 21, small_2_2],
            [30, None, small_2_1]
        ])

        self._create_thread('m_1_s_2_5', time(14, 50), [
            [None, 0, main_1_2],
            [10, 11, main_1_4],
            [20, 21, small_2_2],
            [30, None, small_2_1]
        ])

        self._create_thread('m_1_s_2_6', time(15, 00), [
            [None, 0, main_1_2],
            [10, 11, main_1_4],
            [20, 21, small_2_2],
            [30, None, small_2_1]
        ])

        self._create_thread('m_1_s_2_7', time(15, 10), [
            [None, 0, main_1_2],
            [10, 11, main_1_4],
            [20, 21, small_2_2],
            [30, None, small_2_1]
        ])

        self._create_thread('m_1_s_2_8', time(15, 20), [
            [None, 0, main_1_2],
            [10, 11, main_1_4],
            [20, 21, small_2_2],
            [30, None, small_2_1]
        ])

        self._create_thread('m_1_s_2_9', time(15, 30), [
            [None, 0, main_1_2],
            [10, 11, main_1_4],
            [20, 21, small_2_2],
            [30, None, small_2_1]
        ])

        self._create_thread('m_1_s_2_10', time(15, 40), [
            [None, 0, main_1_2],
            [10, 11, main_1_4],
            [20, 21, small_2_2],
            [30, None, small_2_1]
        ])

        self._create_thread('m_1_s_2_11', time(15, 50), [
            [None, 0, main_1_2],
            [10, 11, main_1_4],
            [20, 21, small_2_2],
            [30, None, small_2_1]
        ])

    @staticmethod
    def _create_external_direction(suburban_zone, base_station, stations):
        ext_dir = create_external_direction(suburban_zone=suburban_zone, base_station=base_station)

        order = 0
        for station in stations:
            create_external_direction_marker(external_direction=ext_dir, station=station, order=order)
            order += 1

    def _create_thread(self, code, tz_start_time, schedule):
        day10 = date(2019, 2, 10)
        day11 = date(2019, 2, 11)
        day12 = date(2019, 2, 12)
        create_thread(
            uid=u'uid_{}'.format(code),
            tz_start_time=tz_start_time,
            schedule_v1=schedule,
            t_subtype=self._subtype,
            title='ru_{}'.format(code),
            title_tr='tr_{}'.format(code),
            title_uk='uk_{}'.format(code),
            year_days=[day10, day11, day12],
        )
