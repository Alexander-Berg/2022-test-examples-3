# -*- coding: utf-8 -*-

from datetime import date, datetime, time
from StringIO import StringIO

import freezegun
import pytest

from common.models.geo import Region, Country
from common.models.schedule import RThread, Route
from travel.rasp.library.python.common23.date import environment
from common.utils.date import MSK_TIMEZONE, daterange
from travel.rasp.admin.importinfo.models import OriginalThreadData
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage
from travel.rasp.admin.lib.unittests.check_thread_mixin import CheckThreadMixin
from route_search.models import ZNodeRoute2
from travel.rasp.admin.scripts.schedule.bus.import_mta import import_mta_from_files
from tester.factories import create_supplier, create_thread, create_station
from tester.testcase import TestCase

stops_xml = u"""<?xml version="1.0" encoding="utf-8"?>
<stop_list>
  <stop stop_name="ст. Железнодорожная" stop_code="111" stop_city="Железнодорожный" area_name="Балашихинский" area_code="2" domain_name="Московская" domain_code="1"/>
  <stop stop_name="д. Чёрное" stop_code="222" stop_city="Балашиха" area_name="Балашихинский" area_code="2" domain_name="Московская" domain_code="1"/>
  <stop stop_name="м/р Купавна" stop_code="333" stop_city="Железнодорожный" area_name="Балашихинский" area_code="2" domain_name="Московская" domain_code="1"/>
</stop_list>
"""

schedule_xml_template = u"""<?xml version="1.0" encoding="utf-8"?>
<route_list>
<route route_number="{number}" internal_number="287482721" route_title="Название маршрута">
  <reis reistype="0" period_start="01-01-2015" period_end="31-12-2015" days_of_week="4" rasptype=" 1 ">
    <reis_point stop_code="111" time="6:35:00"/>
    <reis_point stop_code="222" time="6:44:00"/>
    <reis_point stop_code="333" time="6:55:00"/>
  </reis>
</route>
</route_list>
"""

schedule_xml = schedule_xml_template.format(number=56)


@pytest.yield_fixture(scope='module', autouse=True)
def replace_now():
    with freezegun.freeze_time('2015-01-01'):
        yield


class TestImportMTA(TestCase, CheckThreadMixin):
    def test_import_mta(self):
        supplier = create_supplier(pk=44, code='mta')
        russia = Country.objects.get(code='RU')
        region = Region.objects.create(title=u'Москва и Московская область', country=russia, time_zone=MSK_TIMEZONE)

        station_a = create_station(pk=12001, region=region, country=russia, title=u'ст. Железнодорожная')
        StationMapping.objects.create(supplier=supplier, title=u'ст. Железнодорожная', station=station_a, code='111')
        station_b = create_station(pk=12002, region=region, country=russia, title=u'д. Чёрное')
        StationMapping.objects.create(supplier=supplier, title=u'д. Чёрное', station=station_b, code='222')
        station_c = create_station(pk=12003, region=region, country=russia, title=u'м/р Купавна')
        StationMapping.objects.create(supplier=supplier, title=u'м/р Купавна', station=station_c, code='333')

        schedule_file = StringIO(schedule_xml.encode('utf-8'))
        stops_file = StringIO(stops_xml.encode('utf-8'))
        import_mta_from_files(schedule_file, stops_file)

        routes = Route.objects.filter(supplier=supplier)
        assert routes.count() == 1

        threads = RThread.objects.filter(supplier=supplier)
        assert threads.count() == 1

        thread = threads[0]
        dates = thread.get_mask(today=environment.today()).dates()
        dates = {day for day in dates if day.month == 1 and day.year == 2015}
        assert dates == {day for day in daterange(date(2015, 1, 1), date(2015, 2, 1)) if day.isoweekday() == 4}
        assert thread.tz_start_time == time(6, 35)
        assert thread.time_zone == MSK_TIMEZONE

        self.assertThreadStopsTimes(thread, [
            [None, 0, MSK_TIMEZONE],
            [8, 9, MSK_TIMEZONE],
            [20, None, MSK_TIMEZONE]
        ])

        origin_data = OriginalThreadData.objects.get(thread=thread)
        assert origin_data.raw

        expected_import_uid = '56_f12001t12003_44-1-5e21ee8316bd28831bc8727fdde1b370-8638fe7db3567c8ae351c922a618c1fc'
        assert thread.import_uid == expected_import_uid, (
            u'Поменялась логика генерации import_uid! '
            u'Правьте данный assert, только если логика генерации import_uid была изменена сознательно.'
        )

        original_import_uid = thread.import_uid
        assert original_import_uid == thread.gen_import_uid(), u'При перегенерации import_uid должен сохранятся'

    def test_import_mta_with_dm(self):
        """
        Проверяем, что переимпорт МТА не удаляет нитки, импортированные из пакета, у которых поставщик тоже МТА
        """
        dm = create_supplier(code='dm')
        package = TwoStageImportPackage.objects.create(title=u'Test Package', supplier=dm)
        mta = create_supplier(code='mta')
        thread = create_thread(supplier=mta, route={'two_stage_package': package, 'supplier': mta,
                                                    'script_protected': False})
        russia = Country.objects.get(code='RU')
        Region.objects.create(pk=1, title=u'Москва и Московская область', country=russia, time_zone=MSK_TIMEZONE)

        schedule_file = StringIO(schedule_xml.encode('utf-8'))
        stops_file = StringIO(stops_xml.encode('utf-8'))

        routes = Route.objects.filter(supplier=mta)
        assert routes.count() == 1
        assert routes[0].two_stage_package is not None
        assert routes[0].supplier == mta
        assert not routes[0].script_protected

        threads = RThread.objects.filter(supplier=mta)
        assert threads.count() == 1
        assert threads[0].supplier == mta

        import_mta_from_files(schedule_file, stops_file)

        routes = Route.objects.filter(supplier=mta)
        assert routes.count() == 2

        threads = RThread.objects.filter(supplier=mta)
        assert threads.count() == 2

    def test_import_mta_with_routes_in_db(self):
        """
        Проверяем переимпорт МТА, когда в базе уже есть нитки МТА
        """
        mta = create_supplier(code='mta')
        russia = Country.objects.get(code='RU')
        Region.objects.create(pk=1, title=u'Москва и Московская область', country=russia, time_zone=MSK_TIMEZONE)

        existed_thread = create_thread(
            supplier=mta,
            route={'supplier': mta, 'script_protected': False, 'two_stage_package': None},
            __={'calculate_noderouteadmin': True}
        )

        schedule_file = StringIO(schedule_xml.encode('utf-8'))
        stops_file = StringIO(stops_xml.encode('utf-8'))
        import_mta_from_files(schedule_file, stops_file)

        assert Route.objects.filter(supplier=mta).count() == 1
        assert RThread.objects.filter(supplier=mta).count() == 1
        assert not ZNodeRoute2.objects.filter(supplier_id=mta.id, two_stage_package_id=None).exists()

        threads = RThread.objects.filter(supplier=mta)
        assert threads[0].id != existed_thread.id


@pytest.mark.dbuser
@pytest.mark.parametrize('number', ('5', '42', '1017'))
def test_import_with_different_numbers(number):
    schedule_xml = schedule_xml_template.format(number=number)

    supplier = create_supplier(pk=44, code='mta')
    russia = Country.objects.get(code='RU')
    region = Region.objects.create(pk=1, title=u'Москва и Московская область', country=russia, time_zone=MSK_TIMEZONE)

    schedule_file = StringIO(schedule_xml.encode('utf-8'))
    stops_file = StringIO(stops_xml.encode('utf-8'))
    import_mta_from_files(schedule_file, stops_file)

    thread = RThread.objects.get()
    assert thread.number == number
