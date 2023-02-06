# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.models.geo import Station, StationMajority, Station2Settlement, StationType
from common.models.schedule import RThreadType, RTStation, RThread
from common.models.transport import TransportType
from common.tester.testcase import TestCase
from common.tester.factories import create_thread, create_station, create_settlement
from common.utils.date import MSK_TIMEZONE

from route_search.helpers import remove_through_trains
from route_search.base import PreSegment, PlainSegmentSearch, NODEROUTE_THREAD_EXTRA
from route_search.helpers import remove_duplicates, fetch_unrelated, _t_type2t_type_list, LimitConditions


class TestHelpers(TestCase):
    def test_remove_duplicates(self):
        settlement_to = create_settlement()
        settlement_from = create_settlement()
        station_from_1 = create_station(settlement=settlement_from, majority=StationMajority.IN_TABLO_ID)
        station_from_2 = create_station(settlement=settlement_from, majority=StationMajority.MAIN_IN_CITY_ID)
        station_to_1 = create_station(settlement=settlement_to, majority=StationMajority.MAIN_IN_CITY_ID)
        station_to_2 = create_station(settlement=settlement_to, majority=StationMajority.IN_TABLO_ID)

        create_thread(
            uid=1, supplier={'id': 1},
            schedule_v1=[
                [None, 0, station_from_1],
                [10, 20, station_from_2],
                [30, 40, station_to_1],
                [50, None, station_to_2],
            ],
            __={'calculate_noderoute': True}
        )

        threads = RThread.objects.all().order_by()

        threads = list(threads.filter(**{
            'znoderoute2__%s_from' % settlement_from.type: settlement_from,
            'znoderoute2__%s_to' % settlement_to.type: settlement_to,
        }).extra(select=NODEROUTE_THREAD_EXTRA))

        # добавляем к ниткам отсутствующие в них поля из других моделей
        fetch_unrelated(threads, Station, 'station_from', 'station_to')
        fetch_unrelated(threads, RTStation, 'rtstation_from', 'rtstation_to')

        threads = list(remove_duplicates(threads))

        assert len(threads) == 1
        assert threads[0].station_from == station_from_2
        assert threads[0].station_to == station_to_1

    def test_exclude_suburban_airport(self):
        settlement_moscow = create_settlement()
        station_moscow = create_station(settlement=settlement_moscow, majority=StationMajority.MAIN_IN_CITY_ID)
        station_mid = create_station(majority=StationMajority.NOT_IN_TABLO_ID)
        station_airport = create_station(settlement=settlement_moscow, station_type_id=StationType.AIRPORT_ID, majority=StationMajority.NOT_IN_TABLO_ID)

        settlement_ekb = create_settlement()
        station_ekb = create_station(settlement=settlement_ekb, station_type_id=StationType.AIRPORT_ID)

        create_thread(
            title='Москва - а/п Домодедово',
            t_type=TransportType.SUBURBAN_ID,
            schedule_v1=[
                [None, 0, station_moscow],
                [30, 31, station_mid],
                [40, None, station_airport],
            ],
            __={'calculate_noderoute': True}
        )

        create_thread(
            title='Москва - Ленинская',
            t_type=TransportType.SUBURBAN_ID,
            schedule_v1=[
                [None, 0, station_moscow],
                [20, None, station_mid],
            ],
            __={'calculate_noderoute': True}
        )

        create_thread(
            title='а/п Домодедово - Москва',
            t_type=TransportType.SUBURBAN_ID,
            schedule_v1=[
                [None, 0, station_airport],
                [10, 11, station_mid],
                [40, None, station_moscow],
            ],
            __={'calculate_noderoute': True}
        )

        create_thread(
            title='Ленинская - Москва',
            t_type=TransportType.SUBURBAN_ID,
            schedule_v1=[
                [None, 0, station_mid],
                [20, None, station_moscow],
            ],
            __={'calculate_noderoute': True}
            )

        create_thread(
            title='Екатеринбург - Москва',
            t_type=TransportType.PLANE_ID,
            schedule_v1=[
                [None, 0, station_ekb],
                [60, None, station_moscow],
            ],
            __={'calculate_noderoute': True}
        )

        create_thread(
            title='Москва - Екатеринбург',
            t_type=TransportType.PLANE_ID,
            schedule_v1=[
                [None, 0, station_moscow],
                [60, None, station_ekb],
            ],
            __={'calculate_noderoute': True}
        )

        search = PlainSegmentSearch(settlement_moscow, station_mid, t_type=None)
        threads_by_zone = search._get_threads()

        assert len(threads_by_zone) == 1
        threads = threads_by_zone.values()[0]
        assert len(threads) == 2
        threads_set = set(thread.title for thread in threads)
        assert {'Москва - а/п Домодедово', 'Москва - Ленинская'} == threads_set

        search = PlainSegmentSearch(station_mid, station_moscow, t_type=None)
        threads_by_zone = search._get_threads()

        assert len(threads_by_zone) == 1
        threads = threads_by_zone.values()[0]
        assert len(threads) == 2
        threads_set = set(thread.title for thread in threads)
        assert {'а/п Домодедово - Москва', 'Ленинская - Москва'} == threads_set

        search = PlainSegmentSearch(settlement_moscow, station_ekb, t_type=None)
        threads_by_zone = search._get_threads()
        assert len(threads_by_zone) == 1
        threads = threads_by_zone.values()[0]
        assert len(threads) == 1
        threads_set = set(thread.title for thread in threads)
        assert {'Москва - Екатеринбург'} == threads_set

        search = PlainSegmentSearch(settlement_ekb, station_moscow, t_type=None)
        threads_by_zone = search._get_threads()
        assert len(threads_by_zone) == 1
        threads = threads_by_zone.values()[0]
        assert len(threads) == 1
        threads_set = set(thread.title for thread in threads)
        assert {'Екатеринбург - Москва'} == threads_set

    def test_through_trains(self):
        station_from = create_station()
        station_to = create_station()

        create_thread_args = [('basic_train',  RThreadType.BASIC_ID, TransportType.TRAIN_ID, 0),
                              ('basic_bus', RThreadType.BASIC_ID, TransportType.BUS_ID, 0),
                              ('pseudo_train_5', RThreadType.PSEUDO_BACK_ID, TransportType.TRAIN_ID, 5),
                              ('pseudo_train', RThreadType.PSEUDO_BACK_ID, TransportType.TRAIN_ID, 0)]

        for uid, thread_type, t_type, departure_time in create_thread_args:
            create_thread(
                uid=uid,
                type=thread_type,
                t_type=t_type,
                schedule_v1=[
                    [None, departure_time, station_from],
                    [10, None, station_to],
                ],
                __={'calculate_noderoute': True}
            )

        search = PlainSegmentSearch(station_from, station_to, t_type=None)
        threads = search._get_threads()
        presegms = list(map(PreSegment, threads[MSK_TIMEZONE]))
        presegms = list(remove_through_trains(presegms))

        assert len(presegms) == 3
        assert presegms[0].thread.uid == 'basic_train'
        assert presegms[1].thread.uid == 'pseudo_train_5'
        assert presegms[2].thread.uid == 'basic_bus'

    def test_t_type2t_type_list(self):
        transport_type_ids = [TransportType.TRAIN_ID, TransportType.PLANE_ID]
        transport_types = [TransportType.objects.get(pk=i) for i in transport_type_ids]
        transport_type_codes = [TransportType.objects.get(pk=i).code for i in transport_type_ids]
        transport_type = TransportType.objects.get(pk=transport_type_ids[0])
        transport_type_code = TransportType.objects.get(pk=transport_type_ids[0]).code

        assert set(_t_type2t_type_list(None)) == set(TransportType.objects.all())
        assert set(_t_type2t_type_list(transport_types)) == set(transport_types)
        assert set(_t_type2t_type_list(transport_type_codes)) == set(transport_types)
        assert _t_type2t_type_list(transport_type) == [transport_type]
        assert _t_type2t_type_list(transport_type_code) == [transport_type]

    def test_fetch_unrelated(self):
        """Проверяем добавление полей, которых в изначальной модели нет."""
        create_thread(__={'calculate_noderoute': True},)

        threads = RThread.objects.filter().extra(select={'rtstation_from_id': 'www_znoderoute2.rtstation_from_id',
                                                         'rtstation_to_id': 'www_znoderoute2.rtstation_to_id'},
                                                 tables=['www_znoderoute2'],
                                                 where=['www_rthread.id=www_znoderoute2.thread_id'])

        assert len(threads) == 1
        assert getattr(threads[0], 'rtstation_from', None) is None
        assert getattr(threads[0], 'rtstation_to', None) is None

        fetch_unrelated(threads, RTStation, 'rtstation_from', 'rtstation_to')

        assert threads[0].rtstation_from == RTStation.objects.get(pk=threads[0].rtstation_from_id)
        assert threads[0].rtstation_to == RTStation.objects.get(pk=threads[0].rtstation_to_id)


class TestLimitConditions(TestCase):
    def test_init(self):
        settlement_to = create_settlement()
        settlement_from = create_settlement()
        station_from = create_station(settlement=settlement_from)
        station_to = create_station(settlement=settlement_to)
        station_to_same = create_station(settlement=settlement_from)
        t_types = TransportType.objects.filter(id__in=[TransportType.PLANE_ID, TransportType.TRAIN_ID])

        with pytest.raises(AssertionError):
            limit_conditions = LimitConditions(station_from, station_from)

        limit_conditions = LimitConditions(settlement_from, settlement_to)
        assert set(limit_conditions.t_types) == set(TransportType.objects.all())
        assert (limit_conditions.from_max_majority_id == limit_conditions.to_max_majority_id ==
                StationMajority.objects.get(code='not_in_tablo').id)

        limit_conditions = LimitConditions(settlement_from, settlement_to, t_types=t_types)
        assert limit_conditions.t_types == t_types

        limit_conditions = LimitConditions(station_from, station_to, t_types=t_types)
        assert limit_conditions.t_types == t_types
        assert (limit_conditions.from_max_majority_id == limit_conditions.to_max_majority_id ==
                StationMajority.objects.get(code='station').id)

        limit_conditions = LimitConditions(station_from, station_to_same, t_types=t_types)
        assert limit_conditions.t_types == t_types
        assert (limit_conditions.from_max_majority_id == limit_conditions.to_max_majority_id ==
                StationMajority.objects.get(code='station').id)

        limit_conditions = LimitConditions(station_from, settlement_from, t_types=t_types)
        assert limit_conditions.t_types == t_types
        assert (limit_conditions.from_max_majority_id == limit_conditions.to_max_majority_id ==
                StationMajority.objects.get(code='station').id)

        limit_conditions = LimitConditions(settlement_to, station_to, t_types=t_types)
        assert limit_conditions.t_types == t_types
        assert (limit_conditions.from_max_majority_id == limit_conditions.to_max_majority_id ==
                StationMajority.objects.get(code='station').id)

        limit_conditions = LimitConditions(station_from, settlement_to, t_types=t_types)
        assert limit_conditions.t_types == t_types
        assert limit_conditions.from_max_majority_id == StationMajority.objects.get(code='station').id
        assert limit_conditions.to_max_majority_id == StationMajority.objects.get(code='not_in_tablo').id

        limit_conditions = LimitConditions(settlement_from, station_to, t_types=t_types)
        assert limit_conditions.t_types == t_types
        assert limit_conditions.from_max_majority_id == StationMajority.objects.get(code='not_in_tablo').id
        assert limit_conditions.to_max_majority_id == StationMajority.objects.get(code='station').id

        t_types = TransportType.objects.filter(id__in=[TransportType.PLANE_ID, TransportType.TRAIN_ID,
                                                       TransportType.BUS_ID])

        limit_conditions = LimitConditions(station_from, station_to_same, t_types=t_types, )
        assert limit_conditions.t_types == [TransportType.objects.get(pk=TransportType.BUS_ID)]
        assert (limit_conditions.from_max_majority_id == limit_conditions.to_max_majority_id ==
                StationMajority.objects.get(code='station').id)

        limit_conditions = LimitConditions(station_from, settlement_from, t_types=t_types)
        assert limit_conditions.t_types == [TransportType.objects.get(pk=TransportType.BUS_ID)]
        assert (limit_conditions.from_max_majority_id == limit_conditions.to_max_majority_id ==
                StationMajority.objects.get(code='station').id)

        limit_conditions = LimitConditions(station_from, settlement_from, t_types=t_types, extended=True)
        assert limit_conditions.t_types == t_types
        assert (limit_conditions.from_max_majority_id == limit_conditions.to_max_majority_id ==
                StationMajority.objects.get(code='station').id)

    def test_allow(self):
        station_from = create_station()
        station_to = create_station()
        t_types = TransportType.objects.filter(id__in=[TransportType.BUS_ID, TransportType.TRAIN_ID])
        limit_conditions = LimitConditions(station_from, station_to, t_types=t_types)

        assert not limit_conditions.allow(StationMajority.MAIN_IN_CITY_ID, StationMajority.MAIN_IN_CITY_ID,
                                          TransportType.objects.get(pk=TransportType.PLANE_ID))

        assert limit_conditions.allow(StationMajority.NOT_IN_SEARCH_ID, StationMajority.NOT_IN_SEARCH_ID,
                                      TransportType.objects.get(pk=TransportType.BUS_ID))

        assert not limit_conditions.allow(StationMajority.NOT_IN_SEARCH_ID, StationMajority.MAIN_IN_CITY_ID,
                                          TransportType.objects.get(pk=TransportType.TRAIN_ID))

        assert not limit_conditions.allow(StationMajority.MAIN_IN_CITY_ID, StationMajority.NOT_IN_SEARCH_ID,
                                          TransportType.objects.get(pk=TransportType.TRAIN_ID))

        assert limit_conditions.allow(StationMajority.IN_TABLO_ID, StationMajority.IN_TABLO_ID,
                                      TransportType.objects.get(pk=TransportType.TRAIN_ID))


@pytest.mark.dbuser
@pytest.mark.parametrize("from_majority_id, from_settlement, to_majority_id, to_settlement, t_type, expected", (
    # находятся нитки с важными станциями и указанным типом транспорта
    (StationMajority.MAIN_IN_CITY_ID, True, StationMajority.MAIN_IN_CITY_ID, True, TransportType.TRAIN_ID, True),
    (StationMajority.MAIN_IN_CITY_ID, True, StationMajority.MAIN_IN_CITY_ID, False, TransportType.TRAIN_ID, True),
    (StationMajority.MAIN_IN_CITY_ID, False, StationMajority.MAIN_IN_CITY_ID, True, TransportType.TRAIN_ID, True),
    (StationMajority.MAIN_IN_CITY_ID, False, StationMajority.MAIN_IN_CITY_ID, False, TransportType.TRAIN_ID, True),
    # не находятся нитки с другим типом транспорта
    (StationMajority.MAIN_IN_CITY_ID, True, StationMajority.MAIN_IN_CITY_ID, True, TransportType.PLANE_ID, False),
    (StationMajority.MAIN_IN_CITY_ID, True, StationMajority.MAIN_IN_CITY_ID, False, TransportType.PLANE_ID, False),
    (StationMajority.MAIN_IN_CITY_ID, False, StationMajority.MAIN_IN_CITY_ID, True, TransportType.PLANE_ID, False),
    (StationMajority.MAIN_IN_CITY_ID, False, StationMajority.MAIN_IN_CITY_ID, False, TransportType.PLANE_ID, False),
    # не находятся нитки от неважных станций
    (StationMajority.STATION_ID, True, StationMajority.MAIN_IN_CITY_ID, True, TransportType.TRAIN_ID, False),
    (StationMajority.STATION_ID, True, StationMajority.MAIN_IN_CITY_ID, False, TransportType.TRAIN_ID, False),
    (StationMajority.NOT_IN_SEARCH_ID, False, StationMajority.MAIN_IN_CITY_ID, True, TransportType.TRAIN_ID, False),
    (StationMajority.NOT_IN_SEARCH_ID, False, StationMajority.MAIN_IN_CITY_ID, False, TransportType.TRAIN_ID, False),
    (StationMajority.MAIN_IN_CITY_ID, True, StationMajority.STATION_ID, True, TransportType.TRAIN_ID, False),
    (StationMajority.MAIN_IN_CITY_ID, True, StationMajority.NOT_IN_SEARCH_ID, False, TransportType.TRAIN_ID, False),
    (StationMajority.MAIN_IN_CITY_ID, False, StationMajority.STATION_ID, True, TransportType.TRAIN_ID, False),
    (StationMajority.MAIN_IN_CITY_ID, False, StationMajority.NOT_IN_SEARCH_ID, False, TransportType.TRAIN_ID, False),
))
def test_limit_conditions_filter_threads_qs(
    from_majority_id, from_settlement, to_majority_id, to_settlement, t_type, expected
):
    if from_settlement:
        from_point = create_settlement()
        from_station = create_station(majority=from_majority_id, settlement=from_point)
    else:
        from_point = from_station = create_station(majority=from_majority_id)

    if to_settlement:
        to_point = create_settlement()
        to_station = create_station(majority=to_majority_id, settlement=to_point)
    else:
        to_point = to_station = create_station(majority=to_majority_id)

    t_types = TransportType.objects.filter(id__in=[TransportType.BUS_ID, TransportType.TRAIN_ID])
    limit_conditions = LimitConditions(from_point, to_point, t_types=t_types)
    thread = create_thread(
        schedule_v1=[
            [None, 0, from_station],
            [10, None, to_station],
        ],
        t_type=t_type,
        __={'calculate_noderoute': True}
    )
    threads = limit_conditions.filter_threads_qs(RThread.objects.all())

    assert (thread in threads) == expected


@pytest.mark.dbuser
def test_limit_conditions_point_to_with_station2settlement():
    settlement_from = create_settlement()
    station_from = create_station(settlement=settlement_from)
    settlement_to = create_settlement()
    station_to = create_station()
    Station2Settlement.objects.create(station=station_to, settlement=settlement_to)
    thread = create_thread(
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ],
        __={'calculate_noderoute': True}
    )

    limit_conditions = LimitConditions(settlement_from, settlement_to)
    threads = limit_conditions.filter_threads_qs(RThread.objects.all())
    assert thread in threads


@pytest.mark.dbuser
def test_limit_conditions_point_from_with_station2settlement():
    settlement_from = create_settlement()
    station_from = create_station()
    Station2Settlement.objects.create(station=station_from, settlement=settlement_from)
    settlement_to = create_settlement()
    station_to = create_station(settlement=settlement_to)
    thread = create_thread(
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ],
        __={'calculate_noderoute': True}
    )

    limit_conditions = LimitConditions(settlement_from, settlement_to)
    threads = limit_conditions.filter_threads_qs(RThread.objects.all())
    assert thread in threads
