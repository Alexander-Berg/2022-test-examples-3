# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.conf import settings

from common.dynamic_settings.default import conf
from common.models.geo import Country
from common.models.transport import TransportType
from common.tester.factories import create_rthread_segment, create_station, create_thread, create_country
from common.tester.testcase import TestCase

from travel.rasp.morda_backend.morda_backend.search.search.data_layer.backend import add_railway_sales_limit


@pytest.mark.dbuser
def test_train_usual_sales_limit():
    from_express_code = 'express_from'
    to_express_code = 'express_to'
    station_from = create_station(__=dict(codes={'express': from_express_code}))
    station_to = create_station(__=dict(codes={'express': to_express_code}))
    usual_train_segment = create_rthread_segment(
        station_from=station_from, station_to=station_to,
        thread=create_thread(t_type=TransportType.TRAIN_ID)
    )

    add_railway_sales_limit(segments=[usual_train_segment])

    assert usual_train_segment.sales_limit_in_days == conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES


@pytest.mark.dbuser
@pytest.mark.parametrize('from_national_version, to_national_version, depth_ru, depth_ua', [
    ('ua', 'ua', conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES, settings.UKRMINTRANS_TRAIN_DEFAULT_DEPTH_OF_SALES),
    ('ua', 'ru', conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES, settings.UKRMINTRANS_TRAIN_DEFAULT_DEPTH_OF_SALES),
    ('ru', 'ua', conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES, settings.UKRMINTRANS_TRAIN_DEFAULT_DEPTH_OF_SALES),
    ('ru', 'ru', conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES, conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES),
    ('ru', 'kz', conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES, conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES),
])
def test_national_version_train_sales_limit(from_national_version, to_national_version, depth_ru, depth_ua):
    countries = {
        'ua': create_country(id=Country.UKRAINE_ID),
        'kz': create_country(id=Country.KAZAKHSTAN_ID),
        'ru': Country.objects.get(id=Country.RUSSIA_ID)
    }

    from_express_code = 'express_from'
    to_express_code = 'express_to'
    station_from = create_station(__=dict(codes={'express': from_express_code}),
                                  country=countries[from_national_version])
    station_to = create_station(__=dict(codes={'express': to_express_code}),
                                country=countries[to_national_version])
    usual_train_segment = create_rthread_segment(station_from=station_from, station_to=station_to,
                                                 thread=create_thread(t_type=TransportType.TRAIN_ID))

    add_railway_sales_limit(segments=[usual_train_segment], national_version='ru')
    assert usual_train_segment.sales_limit_in_days == depth_ru

    add_railway_sales_limit(segments=[usual_train_segment], national_version='ua')
    assert usual_train_segment.sales_limit_in_days == depth_ua


@pytest.mark.dbuser
def test_suburban_sales_limit():
    station_from = create_station()
    station_to = create_station()
    suburban_segment = create_rthread_segment(
        station_from=station_from, station_to=station_to,
        thread=create_thread(t_type=TransportType.SUBURBAN_ID)
    )
    add_railway_sales_limit(segments=[suburban_segment])

    assert suburban_segment.sales_limit_in_days == conf.SUBURBAN_ORDER_DEFAULT_DEPTH_OF_SALES


@pytest.mark.dbuser
def test_not_railway_no_sales_limit():
    from_express_code = 'express_from'
    to_express_code = 'express_to'
    station_from = create_station(__=dict(codes={'express': from_express_code}))
    station_to = create_station(__=dict(codes={'express': to_express_code}))
    bus_segment = create_rthread_segment(
        station_from=station_from, station_to=station_to,
        thread=create_thread(t_type=TransportType.BUS_ID)
    )

    add_railway_sales_limit(segments=[bus_segment])

    assert not hasattr(bus_segment, 'sales_limit_in_days')


class TestNumberOfQueries(TestCase):
    def test_number_of_queries(self):
        create_country(id=Country.UKRAINE_ID)
        station_a = create_station(__=dict(codes={'express': 'a'}))
        station_b = create_station(__=dict(codes={'express': 'b'}))

        station_a_ua = create_station(country=Country.UKRAINE_ID, __=dict(codes={'express': 'a_ua'}))
        station_b_ua = create_station(country=Country.UKRAINE_ID, __=dict(codes={'express': 'b_ua'}))

        def create_segments(number_of_segments):
            return [
                create_rthread_segment(station_from=station_a, station_to=station_b,
                                       thread=create_thread(t_type=TransportType.TRAIN_ID))
                for _ in range(number_of_segments // 2)
            ] + [
                create_rthread_segment(station_from=station_a_ua, station_to=station_b_ua,
                                       thread=create_thread(t_type=TransportType.TRAIN_ID))
                for _ in range(number_of_segments // 2)
            ]

        segments = create_segments(20)

        with self.assertNumQueries(0):
            add_railway_sales_limit(segments=segments)
            add_railway_sales_limit(segments=segments, national_version='ua')
