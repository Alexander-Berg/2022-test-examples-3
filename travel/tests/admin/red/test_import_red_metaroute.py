# -*- coding: utf-8 -*-

from datetime import time

import httpretty
import pytest

import library.python.resource
from travel.rasp.admin.admin.red.metaimport import RedMetaRouteImporter
from travel.rasp.admin.admin.red.models import Package, MetaRoute, MetaRouteStation
from common.models.schedule import Route, RThread
from common.models.transport import TransportType
from common.tester.factories import create_supplier, create_station
from common.tester.testcase import TestCase
from common.tester.utils.datetime import replace_now
from travel.rasp.admin.lib.unittests.check_thread_mixin import CheckThreadMixin

from travel.rasp.library.python.common23.tester.helpers.mask_description import run_mask_from_mask_description
from travel.rasp.library.python.common23.date import environment


@pytest.mark.usefixtures('http_allowed')
class TestImportRedMetaroute(TestCase, CheckThreadMixin):
    def test_import(self):
        red_package = Package.objects.create(title=u'Красный тестовый пакет')
        red_metaroute = MetaRoute.objects.create(
            id=66,
            package=red_package,
            title=u'Красный тестовый рейс',
            scheme=u'7.00',
            t_type_id=TransportType.BUS_ID,
            supplier=create_supplier(id=66),
        )
        MetaRouteStation.objects.create(
            metaroute=red_metaroute,
            station=create_station(id=8901),
            arrival=None,
            departure=0,
            order=0,
        )
        MetaRouteStation.objects.create(
            metaroute=red_metaroute,
            station=create_station(id=8902),
            arrival=10,
            departure=None,
            order=1,
        )

        importer = RedMetaRouteImporter(red_metaroute)
        importer.import_metaroute()

        routes = list(Route.objects.filter(red_metaroute=red_metaroute))
        assert len(routes) == 1

        threads = list(routes[0].rthread_set.all())
        assert len(threads) == 1

        thread = threads[0]
        assert thread.tz_start_time == time(7, 0)

        expected_uid = 'empty_f8901t8902_r66_66-1-4d5c8520bc0928162a84cf96f9f29bb6-26d1de31cbca4cf0533448976e8e7990'
        assert thread.import_uid == expected_uid, (
            u'Поменялась логика генерации import_uid! '
            u'Правьте данный assert, только если логика генерации import_uid была изменена сознательно.'
        )

        original_import_uid = thread.import_uid
        assert original_import_uid == thread.gen_import_uid(), u'При перегенерации import_uid должен сохранятся'

    @httpretty.activate
    @replace_now('2015-01-01 00:00:00')
    def test_unite_threads(self):
        httpretty.register_uri(
            httpretty.GET,
            uri='https://calendar.yandex.ru/export/holidays.xml?start_date=2014-01-01&end_date=2014-12-31&country_id=225&out_mode=all',  # noqa
            content_type='text/xml; charset=utf-8',
            body=library.python.resource.find('tester/data/yandex_calendar_2014_rus.xml')
        )
        httpretty.register_uri(
            httpretty.GET,
            uri='https://calendar.yandex.ru/export/holidays.xml?start_date=2015-01-01&end_date=2015-12-31&country_id=225&out_mode=all',  # noqa
            content_type='text/xml; charset=utf-8',
            body=library.python.resource.find('tester/data/yandex_calendar_2015_rus.xml')
        )

        today = environment.today()
        red_package = Package.objects.create(title=u'Красный тестовый пакет')
        red_metaroute = MetaRoute.objects.create(
            package=red_package,
            title=u'Красный тестовый рейс',
            scheme=u'7.00(р), 7.00(в)',
            t_type_id=TransportType.BUS_ID,
            supplier=create_supplier(),
        )
        MetaRouteStation.objects.create(
            metaroute=red_metaroute,
            station=create_station(),
            arrival=None,
            departure=0,
            order=0,
        )
        MetaRouteStation.objects.create(
            metaroute=red_metaroute,
            station=create_station(),
            arrival=10,
            departure=None,
            order=1,
        )

        common_mask = run_mask_from_mask_description(u'''
 январь 2015
 пн   вт   ср   чт   пт   сб   вс
               # 1  # 2  # 3  # 4
# 5  # 6  # 7  # 8  # 9  #10  #11
#12  #13  #14  #15  #16  #17  #18
#19  #20  #21  #22  #23  #24  #25
#26  #27  #28  #29  #30  #31
        ''')
        common_mask.set_today(today)

        importer = RedMetaRouteImporter(red_metaroute)
        importer.import_metaroute()

        routes = list(Route.objects.filter(red_metaroute=red_metaroute))
        assert len(routes) == 1

        threads = list(routes[0].rthread_set.all())
        assert len(threads) == 1

        thread = threads[0]
        assert thread.tz_start_time == time(7, 0)

        assert common_mask & thread.get_mask(today) == common_mask

    def test_apply_base_stations(self):
        base_station = create_station(is_base=True)
        red_package = Package.objects.create(title=u'Красный тестовый пакет')
        red_metaroute = MetaRoute.objects.create(
            package=red_package,
            title=u'Красный тестовый рейс',
            scheme=u'7.00',
            t_type_id=TransportType.BUS_ID,
            supplier=create_supplier(),
        )
        MetaRouteStation.objects.create(metaroute=red_metaroute, station=create_station(), arrival=None, departure=0, order=0)
        MetaRouteStation.objects.create(metaroute=red_metaroute, station=base_station, arrival=10, departure=11, order=1)
        MetaRouteStation.objects.create(metaroute=red_metaroute, station=create_station(), arrival=20, departure=None, order=2)
        importer = RedMetaRouteImporter(red_metaroute)

        # по-умолчанию логика базовых автовокзалов работает
        importer.import_metaroute()
        path = list(RThread.objects.get(route__red_metaroute=red_metaroute).path)
        assert path[0].in_station_schedule
        assert path[0].is_searchable_from
        assert not path[1].in_station_schedule
        assert not path[1].is_searchable_from
        assert not path[2].in_station_schedule
        assert not path[2].is_searchable_from

        # отключаем логику базовых автовокзалов
        red_metaroute.apply_base_stations = False
        importer.import_metaroute()
        path = list(RThread.objects.get(route__red_metaroute=red_metaroute).path)
        assert path[0].in_station_schedule
        assert path[0].is_searchable_from
        assert path[1].in_station_schedule
        assert path[1].is_searchable_from
        assert path[2].in_station_schedule
        assert path[2].is_searchable_from
