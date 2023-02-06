# coding: utf8

import pytest
from hamcrest import assert_that, contains_inanyorder, has_entries

from common.models.factories import create_tariff_group, create_aeroex_tariff, create_tariff_type
from common.models.tariffs import TariffType
from common.models.transport import TransportType
from common.tester.factories import create_rthread_segment

from travel.rasp.export.export.v3.core.tariffs.tariffs import (
    get_suburban_tariffs, get_segment_tariff, SegmentTariff, get_thread_suburban_tariffs
)
from travel.rasp.export.tests.v3.factories import create_thread, create_station


pytestmark = pytest.mark.dbuser


class TestSuburbanTariffs(object):
    def test_get_data(self):
        station_from = create_station()
        station_to = create_station()

        tariff_type = create_tariff_type(code='type_1', category=TariffType.USUAL_CATEGORY, is_main=False,
                                         title='тариф1', description_ru='описание1', order=1, link='url1')
        tariff_1 = create_aeroex_tariff(id=101, tariff=11, type=tariff_type,
                                        station_from=station_from, station_to=station_to)
        segment_tariff_1 = SegmentTariff(tariff_1, tariff_type)

        assert segment_tariff_1.type == tariff_type
        assert_that(segment_tariff_1.get_data(), has_entries({
            'price': has_entries({
                'currency': 'RUR',
                'value': 11.0,
            }),
            'title': 'тариф1',
            'code': 'type_1',
            'description': 'описание1',
            'order': 1,
            'url': 'url1',
            'category': 'usual',
            'is_main': False,
            'id': 101,
        }))

        tariff_type_2 = create_tariff_type(order=2)
        replace_type = create_tariff_type(code='type_2', category=TariffType.USUAL_CATEGORY, is_main=True,
                                          title='тариф2', description_ru='описание2', order=3, link='url2')
        tariff_2 = create_aeroex_tariff(id=102, tariff=22, type=tariff_type_2, replace_tariff_type=replace_type,
                                        currency='USD', station_from=station_from, station_to=station_to)

        segment_tariff_2 = SegmentTariff(tariff_2, replace_type)

        assert segment_tariff_2.type == replace_type
        assert_that(segment_tariff_2.get_data(), has_entries({
            'price': has_entries({
                'currency': 'USD',
                'value': 22,
            }),
            'title': 'тариф2',
            'code': 'type_2',
            'description': 'описание2',
            'order': 3,
            'url': 'url2',
            'category': 'usual',
            'is_main': True,
            'id': 102,
        }))

    def test_get_segment_tariff(self):
        station_from = create_station()
        station_to = create_station()
        tariff_type = create_tariff_type(code='type_1', category=TariffType.USUAL_CATEGORY)
        thread = create_thread()
        segment = create_rthread_segment(thread=thread, station_from=station_from, station_to=station_to)

        tariff = create_aeroex_tariff(station_from=station_from, station_to=station_to, type=tariff_type,
                                      tariff=11.5, currency='BYN')
        segment.base_tariff = SegmentTariff(tariff, tariff_type)

        assert_that(get_segment_tariff(segment), has_entries({
            'currency': 'BYN',
            'value': 11.5,
        }))

    def test_thread_tariffs_groups(self):
        station_from = create_station(id=100)
        station_to = create_station(id=200)
        create_tariff = create_aeroex_tariff.mutate(station_from=station_from, station_to=station_to)

        group_a = create_tariff_group()
        group_b = create_tariff_group()
        group_c = create_tariff_group()

        season_1 = create_tariff_type(
            code='season_1', title='s1', order=1,
            category=TariffType.SEASON_TICKET_CATEGORY, __={'tariff_groups': [group_a]}
        )
        create_tariff(id=201, tariff=10, type=season_1)

        season_2 = create_tariff_type(
            code='season_2', title='s2', order=2,
            category=TariffType.SEASON_TICKET_CATEGORY, __={'tariff_groups': [group_a, group_b]}
        )
        create_tariff(id=202, tariff=20, type=season_2)

        season_3 = create_tariff_type(
            code='season_3', title='s3', order=3,
            category=TariffType.SEASON_TICKET_CATEGORY, __={'tariff_groups': [group_c]}
        )
        create_tariff(id=203, tariff=30, type=season_3)

        usual_1 = create_tariff_type(
            code='usual_1', title='u1', order=4,
            category=TariffType.USUAL_CATEGORY, __={'tariff_groups': [group_a, group_b]}
        )
        create_tariff(id=204, tariff=1, type=usual_1)

        usual_2 = create_tariff_type(
            code='usual_2', title='u2', order=5,
            category=TariffType.USUAL_CATEGORY, __={'tariff_groups': [group_c]}
        )
        create_tariff(id=205, tariff=2, type=usual_2)

        thread = create_thread(t_type=TransportType.SUBURBAN_ID, tariff_type=usual_1)

        tariffs, tariff = get_thread_suburban_tariffs(thread, 100, 200)

        assert len(tariffs) == 3

        assert_that(tariffs[0], has_entries({
            'category': 'season_ticket',
            'title': 's1',
            'code': 'season_1',
            'order': 1,
            'price': has_entries({'currency': 'RUR', 'value': 10}),
            'id': 201,
        }))
        assert_that(tariffs[1], has_entries({
            'category': 'season_ticket',
            'title': 's2',
            'code': 'season_2',
            'order': 2,
            'price': has_entries({'currency': 'RUR', 'value': 20}),
            'id': 202,
        }))
        assert_that(tariffs[2], has_entries({
            'category': 'usual',
            'title': 'u1',
            'code': 'usual_1',
            'order': 4,
            'price': has_entries({'currency': 'RUR', 'value': 1}),
            'id': 204,
        }))

        assert_that(tariff, has_entries({'currency': 'RUR', 'value': 1}))

    def test_thread_tariffs_replace(self):
        station_from = create_station(id=100)
        station_to = create_station(id=200)
        group_a = create_tariff_group()

        create_tariff = create_aeroex_tariff.mutate(station_from=station_from, station_to=station_to)
        create_type = create_tariff_type.mutate(category=TariffType.USUAL_CATEGORY, __={'tariff_groups': [group_a]})

        usual_1 = create_type(code='usual_1', title='u1', order=1, is_main=True)
        create_tariff(id=301, tariff=1, type=usual_1)

        usual_2 = create_type(code='usual_2', title='u2', order=2)
        usual_3 = create_type(code='usual_3', title='u3', order=3, is_main=True)
        create_tariff(id=302, tariff=2, type=usual_2, replace_tariff_type=usual_3, currency='USD')

        usual_4 = create_type(code='usual_4', title='u4', order=4)
        create_tariff(id=304, tariff=4, type=usual_4, currency='USD')

        create_type(code='usual_5', title='u5', order=5)

        thread = create_thread(t_type=TransportType.SUBURBAN_ID, tariff_type=usual_2)

        tariffs, tariff = get_thread_suburban_tariffs(thread, 100, 200)

        assert_that(tariff, has_entries({'currency': 'USD', 'value': 2}))

        assert len(tariffs) == 3
        assert_that(tariffs, contains_inanyorder(
            has_entries({
                'category': 'usual',
                'title': 'u1',
                'code': 'usual_1',
                'order': 1,
                'is_main': True,
                'price': has_entries({'currency': 'RUR', 'value': 1}),
                'id': 301,
            }),
            has_entries({
                'category': 'usual',
                'title': 'u3',
                'code': 'usual_3',
                'order': 3,
                'is_main': True,
                'price': has_entries({'currency': 'USD', 'value': 2}),
                'id': 302,
            }),
            has_entries({
                'category': 'usual',
                'title': 'u4',
                'code': 'usual_4',
                'order': 4,
                'is_main': False,
                'price': has_entries({'currency': 'USD', 'value': 4}),
                'id': 304,
            })
        ))

    def test_search_tariffs(self):
        station_from = create_station()
        station_to_a = create_station()
        station_to_b = create_station()

        suburban = create_tariff_group()
        express = create_tariff_group()
        mega = create_tariff_group()

        season_s1 = create_tariff_type(
            code='season_s1', title='ss1', order=1,
            category=TariffType.SEASON_TICKET_CATEGORY, __={'tariff_groups': [suburban]}
        )
        season_s2 = create_tariff_type(
            code='season_s2', title='ss2', order=2,
            category=TariffType.SEASON_TICKET_CATEGORY, __={'tariff_groups': [suburban]}
        )
        season_e1 = create_tariff_type(
            code='season_e1', title='se1', order=3,
            category=TariffType.SEASON_TICKET_CATEGORY, __={'tariff_groups': [express]}
        )
        create_tariff_type(
            code='season_m1', title='sm1', order=4,
            category=TariffType.SEASON_TICKET_CATEGORY, __={'tariff_groups': [mega]}
        )

        usual_s1 = create_tariff_type(
            code='usual_s1', title='us1', order=5,
            category=TariffType.USUAL_CATEGORY, __={'tariff_groups': [suburban]}
        )
        usual_s2 = create_tariff_type(
            code='usual_s2', title='us2', order=6, is_main=True,
            category=TariffType.USUAL_CATEGORY, __={'tariff_groups': [suburban]}
        )
        usual_s3 = create_tariff_type(
            code='usual_s3', title='us3', order=7, is_main=True,
            category=TariffType.USUAL_CATEGORY, __={'tariff_groups': [suburban]}
        )
        usual_e1 = create_tariff_type(
            code='usual_e1', title='ue1', order=8, is_main=True,
            category=TariffType.USUAL_CATEGORY, __={'tariff_groups': [express]}
        )
        usual_e2 = create_tariff_type(
            code='usual_e2', title='ue2', order=9,
            category=TariffType.USUAL_CATEGORY, __={'tariff_groups': [suburban, express]}
        )
        create_tariff_type(
            code='usual_m1', title='um1', order=10,
            category=TariffType.USUAL_CATEGORY, __={'tariff_groups': [mega]}
        )

        create_tariff = create_aeroex_tariff.mutate(station_from=station_from, station_to=station_to_a)

        create_tariff(id=401, tariff=10, type=season_s1)
        create_tariff(id=402, tariff=20, type=season_s2)
        create_tariff(id=403, tariff=40, type=season_e1)

        create_tariff(id=405, tariff=2, type=usual_s1, replace_tariff_type=usual_s2)
        create_tariff(id=407, tariff=3, type=usual_s3)
        create_tariff(id=409, tariff=6, type=usual_e2, replace_tariff_type=usual_e1)

        create_tariff = create_aeroex_tariff.mutate(station_from=station_from, station_to=station_to_b)

        create_tariff(id=411, tariff=21, type=season_s1, replace_tariff_type=season_s2)
        create_tariff(id=415, tariff=2.2, type=usual_s1)
        create_tariff(id=417, tariff=3.3, type=usual_s3)

        thread_s = create_thread(t_type=TransportType.SUBURBAN_ID, tariff_type=usual_s1)
        thread_e = create_thread(t_type=TransportType.SUBURBAN_ID, tariff_type=usual_e2)
        thread_ss = create_thread(t_type=TransportType.SUBURBAN_ID, tariff_type=usual_s1)  # Нитка с тем же тарифом

        segment_sa = create_rthread_segment(thread=thread_s, station_from=station_from, station_to=station_to_a)
        segment_sb = create_rthread_segment(thread=thread_s, station_from=station_from, station_to=station_to_b)
        segment_ea = create_rthread_segment(thread=thread_e, station_from=station_from, station_to=station_to_a)
        segment_ssa = create_rthread_segment(thread=thread_ss, station_from=station_from, station_to=station_to_a)

        tariffs = get_suburban_tariffs([segment_sa, segment_sb, segment_ea, segment_ssa])

        assert len(tariffs) == 9

        assert_that(tariffs, contains_inanyorder(
            has_entries({
                'category': 'season_ticket',
                'title': 'ss1',
                'code': 'season_s1',
                'order': 1,
                'is_main': False,
                'price': has_entries({'currency': 'RUR', 'value': 10}),
                'id': 401,
            }),
            has_entries({
                'category': 'season_ticket',
                'title': 'ss2',
                'code': 'season_s2',
                'order': 2,
                'is_main': False,
                'price': has_entries({'currency': 'RUR', 'value': 20}),
                'id': 402,
            }),
            has_entries({
                'category': 'season_ticket',
                'title': 'se1',
                'code': 'season_e1',
                'order': 3,
                'is_main': False,
                'price': has_entries({'currency': 'RUR', 'value': 40}),
                'id': 403,
            }),
            has_entries({
                'category': 'usual',
                'title': 'us2',
                'code': 'usual_s2',
                'order': 6,
                'is_main': True,
                'price': has_entries({'currency': 'RUR', 'value': 2}),
                'id': 405,
            }),
            has_entries({
                'category': 'usual',
                'title': 'us3',
                'code': 'usual_s3',
                'order': 7,
                'is_main': True,
                'price': has_entries({'currency': 'RUR', 'value': 3}),
                'id': 407,
            }),
            has_entries({
                'category': 'usual',
                'title': 'ue1',
                'code': 'usual_e1',
                'order': 8,
                'is_main': True,
                'price': has_entries({'currency': 'RUR', 'value': 6}),
                'id': 409,
            }),
            has_entries({
                'category': 'season_ticket',
                'title': 'ss2',
                'code': 'season_s2',
                'order': 2,
                'is_main': False,
                'price': has_entries({'currency': 'RUR', 'value': 21}),
                'id': 411,
            }),
            has_entries({
                'category': 'usual',
                'title': 'us1',
                'code': 'usual_s1',
                'order': 5,
                'is_main': False,
                'price': has_entries({'currency': 'RUR', 'value': 2.2}),
                'id': 415,
            }),
            has_entries({
                'category': 'usual',
                'title': 'us3',
                'code': 'usual_s3',
                'order': 7,
                'is_main': True,
                'price': has_entries({'currency': 'RUR', 'value': 3.3}),
                'id': 417,
            })
        ))

        assert segment_sa.tariffs_ids == [401, 402, 405, 407]
        assert segment_sb.tariffs_ids == [411, 415, 417]
        assert segment_ea.tariffs_ids == [401, 402, 403, 405, 407, 409]
        assert segment_ssa.tariffs_ids == [401, 402, 405, 407]

        assert_that(segment_sa.base_tariff.get_price_data(), has_entries({'currency': 'RUR', 'value': 2}))
        assert_that(segment_sb.base_tariff.get_price_data(), has_entries({'currency': 'RUR', 'value': 2.2}))
        assert_that(segment_ea.base_tariff.get_price_data(), has_entries({'currency': 'RUR', 'value': 6}))
        assert_that(segment_ssa.base_tariff.get_price_data(), has_entries({'currency': 'RUR', 'value': 2}))

    def test_search_stations_tariffs(self):
        station_from_1 = create_station(title='from1')
        station_from_2 = create_station(title='from2')
        station_to_1 = create_station(title='to1')
        station_to_2 = create_station(title='to2')
        group = create_tariff_group()

        tariff_type1 = create_tariff_type(code='test1', category='season_ticket', order=1,
                                          title='tariff1', __={'tariff_groups': [group]})
        tariff_type2 = create_tariff_type(code='test2', category='season_ticket', order=2,
                                          title='tariff2', __={'tariff_groups': [group]})

        create_aeroex_tariff(station_from=station_from_1, station_to=station_to_1,
                             id=511, tariff=1.1, type=tariff_type1),
        create_aeroex_tariff(station_from=station_from_1, station_to=station_to_1,
                             id=512, tariff=1.2, type=tariff_type2),
        create_aeroex_tariff(station_from=station_from_1, station_to=station_to_2,
                             id=502, tariff=2, type=tariff_type1),
        create_aeroex_tariff(station_from=station_from_2, station_to=station_to_1,
                             id=503, tariff=3, type=tariff_type1),
        create_aeroex_tariff(station_from=station_from_2, station_to=station_to_2,
                             id=504, tariff=4, type=tariff_type1),

        station_pairs = [
            (station_from_1, station_to_1),
            (station_from_1, station_to_2),
            (station_from_2, station_to_1),
            (station_from_2, station_to_2),
        ]

        segments = []
        segments_by_stations = {}
        for station_from, station_to in station_pairs:
            thread = create_thread(tariff_type=tariff_type1)
            segment = create_rthread_segment(
                thread=thread,
                station_from=station_from,
                station_to=station_to,
            )
            segments.append(segment)
            segments_by_stations[(station_from, station_to)] = segment

        tariffs = get_suburban_tariffs(segments)

        def _check_station_pair(station_pair, tariff_value, tariffs_ids):
            seg = segments_by_stations[station_pair]
            price = seg.base_tariff.get_price_data()
            assert price['value'] == tariff_value
            assert price['currency'] == 'RUR'
            assert seg.tariffs_ids == tariffs_ids

        _check_station_pair((station_from_1, station_to_1), 1.1, [511, 512])
        _check_station_pair((station_from_1, station_to_2), 2, [502])
        _check_station_pair((station_from_2, station_to_1), 3, [503])
        _check_station_pair((station_from_2, station_to_2), 4, [504])

        assert len(tariffs) == 5

        assert_that(tariffs, contains_inanyorder(
            has_entries({
                'category': 'season_ticket',
                'title': 'tariff1',
                'code': 'test1',
                'order': 1,
                'is_main': False,
                'price': has_entries({'currency': 'RUR', 'value': 1.1}),
                'id': 511,
            }),
            has_entries({
                'category': 'season_ticket',
                'title': 'tariff2',
                'code': 'test2',
                'order': 2,
                'is_main': False,
                'price': has_entries({'currency': 'RUR', 'value': 1.2}),
                'id': 512,
            }),
            has_entries({
                'category': 'season_ticket',
                'title': 'tariff1',
                'code': 'test1',
                'order': 1,
                'is_main': False,
                'price': has_entries({'currency': 'RUR', 'value': 2}),
                'id': 502,
            }),
            has_entries({
                'category': 'season_ticket',
                'title': 'tariff1',
                'code': 'test1',
                'order': 1,
                'is_main': False,
                'price': has_entries({'currency': 'RUR', 'value': 3}),
                'id': 503,
            }),
            has_entries({
                'category': 'season_ticket',
                'title': 'tariff1',
                'code': 'test1',
                'order': 1,
                'is_main': False,
                'price': has_entries({'currency': 'RUR', 'value': 4}),
                'id': 504,
            }),
        ))
