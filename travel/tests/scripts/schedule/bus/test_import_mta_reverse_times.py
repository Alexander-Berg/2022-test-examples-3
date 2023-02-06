# -*- coding: utf-8 -*-

from StringIO import StringIO

import freezegun
import pytest

from common.models.geo import Region, Country
from common.models.schedule import RThread
from common.utils.date import MSK_TIMEZONE
from travel.rasp.admin.lib.unittests.check_thread_mixin import CheckThreadMixin
from travel.rasp.admin.scripts.schedule.bus.import_mta import import_mta_from_files
from tester.factories import create_supplier
from tester.testcase import TestCase


stops_xml = u"""<?xml version="1.0" encoding="utf-8"?>
<stop_list>
    <stop stop_name="Луховицы (Вокзал)" stop_code="939879236" stop_city="Луховицы" area_name="Луховицкий" area_code="16" domain_name="Московская" domain_code="1"/>
    <stop stop_name="Молзавод" stop_code="-1132177432" stop_city="Луховицы" area_name="Луховицкий" area_code="16" domain_name="Московская" domain_code="1"/>
    <stop stop_name="Лесная" stop_code="919052374" stop_city="Луховицы" area_name="Луховицкий" area_code="16" domain_name="Московская" domain_code="1"/>
</stop_list>
"""

schedule_xml = u"""<?xml version="1.0" encoding="utf-8"?>
<route_list>
<route route_number="58" route_title="Ловцы – Коломна (а/в Голутвин)">
<reis period_start="01-05-2015" period_end="31-12-2015" days_of_week="12345--">
    <reis_point stop_code="939879236" time="7:15:00"/>
    <reis_point stop_code="-1132177432" time="7:12:00"/>
    <reis_point stop_code="919052374" time="7:10:00"/>
</reis>
</route>
</route_list>
"""


@pytest.yield_fixture(scope='module', autouse=True)
def replace_now():
    with freezegun.freeze_time('2015-01-01'):
        yield


class TestImportMTAReverseTimes(TestCase, CheckThreadMixin):
    def test_import_mta_reverse_times(self):
        supplier = create_supplier(code='mta')
        russia = Country.objects.get(code='RU')
        Region.objects.create(pk=1, title='Москва и Московская область', country=russia, time_zone=MSK_TIMEZONE)

        schedule_file = StringIO(schedule_xml.encode('utf-8'))
        stops_file = StringIO(stops_xml.encode('utf-8'))
        import_mta_from_files(schedule_file, stops_file)

        threads = RThread.objects.filter(supplier=supplier)
        assert threads.count() == 1

        self.assertThreadStopsTimes(threads[0], [
            [None, 0, MSK_TIMEZONE],
            [5, 6, MSK_TIMEZONE],
            [10, None, MSK_TIMEZONE],
        ])
