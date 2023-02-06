# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from hamcrest import assert_that, has_properties, contains_inanyorder

from common.models.geo import Country
from common.models.transport import TransportType
from common.tester.factories import (
    create_country, create_settlement, create_station, create_thread
)
from common.tester.testcase import TestCase
from common.utils.settlement import get_connected_stations, get_related_stations, get_main_stations


create_thread = create_thread.mutate(__={'calculate_noderoute': True})


class TestSettlementStations(TestCase):
    def test_connected_stations(self):
        settlement = create_settlement(country=Country.RUSSIA_ID)

        st_bus1 = create_station(
            t_type=TransportType.BUS_ID, settlement=settlement, majority=1, type_choices='schedule'
        )
        st_bus2 = create_station(
            t_type=TransportType.BUS_ID, settlement=settlement, majority=2, type_choices='schedule'
        )
        create_station(t_type=TransportType.BUS_ID, settlement=settlement, majority=3, type_choices='schedule')
        create_station(t_type=TransportType.BUS_ID, settlement=settlement, majority=2, type_choices='')

        st_sub1 = create_station(
            t_type=TransportType.TRAIN_ID, settlement=settlement, majority=1, type_choices='suburban'
        )
        create_station(t_type=TransportType.TRAIN_ID, settlement=settlement, majority=4, type_choices='suburban')
        create_station(t_type=TransportType.TRAIN_ID, settlement=settlement, majority=2, type_choices='train')

        settlement2 = create_settlement(country=Country.RUSSIA_ID)

        assert set(get_connected_stations(settlement,  2, TransportType.BUS_ID)) == {st_bus1, st_bus2}
        assert get_connected_stations(settlement, 2, TransportType.SUBURBAN_ID) == [st_sub1]
        assert get_connected_stations(settlement2, 2, TransportType.SUBURBAN_ID) == []

    def test_related_stations(self):
        settlement = create_settlement(country=Country.RUSSIA_ID)

        st_bus1 = create_station(t_type=TransportType.BUS_ID, majority=1, type_choices='schedule')
        st_bus2 = create_station(t_type=TransportType.BUS_ID, majority=2, type_choices='schedule')
        st_bus3 = create_station(t_type=TransportType.BUS_ID, majority=3, type_choices='schedule')
        st_bus4 = create_station(t_type=TransportType.BUS_ID, majority=2, type_choices='')

        settlement.related_stations.create(station=st_bus1)
        settlement.related_stations.create(station=st_bus2)
        settlement.related_stations.create(station=st_bus3)
        settlement.related_stations.create(station=st_bus4)

        st_sub1 = create_station(t_type=TransportType.TRAIN_ID, majority=1, type_choices='suburban')
        st_sub4 = create_station(t_type=TransportType.TRAIN_ID, majority=4, type_choices='suburban')
        st_sub5 = create_station(t_type=TransportType.TRAIN_ID, majority=2, type_choices='train')

        settlement.related_stations.create(station=st_sub1)
        settlement.related_stations.create(station=st_sub4)
        settlement.related_stations.create(station=st_sub5)

        settlement2 = create_settlement(country=Country.RUSSIA_ID)

        assert set(get_related_stations(settlement, 2, TransportType.BUS_ID)) == {st_bus1, st_bus2}
        assert get_related_stations(settlement, 3, TransportType.SUBURBAN_ID) == [st_sub1]
        assert get_related_stations(settlement2, 2, TransportType.SUBURBAN_ID) == []

        train_station = create_station(t_type='train', settlement=settlement2, majority=2, type_choices='train')
        settlement2.related_stations.create(station=train_station)
        assert get_related_stations(settlement2, 2) == [train_station]

        train_and_suburban_station = create_station(
            t_type='train', settlement=settlement2, majority=2, type_choices='train,suburban'
        )
        settlement3 = create_settlement(country=Country.RUSSIA_ID)
        settlement3.related_stations.create(station=train_and_suburban_station)

        stations = get_related_stations(settlement3, 2)
        assert_that(stations, contains_inanyorder(
            has_properties({'t_type': has_properties({'code': 'suburban'})}),
            has_properties({'t_type': has_properties({'code': 'train'})})
        ))

    def test_main_stations(self):
        settlement = create_settlement(country=Country.RUSSIA_ID)

        st_connected1 = create_station(
            t_type=TransportType.BUS_ID, settlement=settlement, majority=1, type_choices='schedule'
        )
        st_connected2 = create_station(
            t_type=TransportType.BUS_ID, settlement=settlement, majority=1, type_choices='schedule'
        )
        st_related = create_station(t_type=TransportType.BUS_ID, majority=1, type_choices='schedule')
        settlement.related_stations.create(station=st_related)

        assert set(get_main_stations(settlement, TransportType.BUS_ID)) == {st_connected1, st_connected2, st_related}

    def test_foreign(self):
        """
        Город не входит в 'наши страны'.
        В результат попадает только аэропорт. Автобусная, ж/д станции не попадают в результат.
        """
        country = create_country(id=84, title='США')
        settlement = create_settlement(country=country)

        create_station(t_type=TransportType.BUS_ID, settlement=settlement, majority=2, type_choices='schedule')
        create_station(t_type=TransportType.TRAIN_ID, settlement=settlement, majority=2, type_choices='train')
        create_station(t_type=TransportType.WATER_ID, settlement=settlement, majority=2, type_choices='schedule')
        airport = create_station(t_type=TransportType.PLANE_ID, settlement=settlement, majority=2, type_choices='tablo')

        assert get_connected_stations(settlement, 2) == [airport]

    def test_lithuania(self):
        """
        Город входит в 'наши страны'. В результат попадают все станции.
        """
        country = create_country(id=Country.LITVA_ID)
        settlement = create_settlement(country=country)

        bus_station = create_station(
            t_type=TransportType.BUS_ID, settlement=settlement, majority=2, type_choices='schedule'
        )
        train_station = create_station(
            t_type=TransportType.TRAIN_ID, settlement=settlement, majority=2, type_choices='train,suburban'
        )
        water_station = create_station(
            t_type=TransportType.WATER_ID, settlement=settlement, majority=2, type_choices='schedule'
        )
        airport = create_station(
            t_type=TransportType.PLANE_ID, settlement=settlement, majority=2, type_choices='tablo'
        )

        stations = get_connected_stations(settlement, 2)
        assert_that(stations, contains_inanyorder(
            has_properties({'t_type': has_properties({'code': 'suburban'}), 'id': train_station.id}),
            has_properties({'t_type': has_properties({'code': 'train'}), 'id': train_station.id}),
            has_properties({'t_type': has_properties({'code': 'bus'}), 'id': bus_station.id}),
            has_properties({'t_type': has_properties({'code': 'plane'}), 'id': airport.id}),
            has_properties({'t_type': has_properties({'code': 'water'}), 'id': water_station.id})
        ))
