# -*- coding: utf-8 -*-

import mock

from datetime import datetime
from django.core.files.base import ContentFile

from common.models.schedule import RThread
from cysix.filters.cysix_xml_thread.skip_station_without_times import SkipStationWithoutTimes
from cysix.models import Filter, PackageFilter
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from tester.testcase import TestCase
from tester.factories import create_supplier, create_station
from tester.utils.datetime import replace_now


class SkipStationWithoutTimesTest(TestCase):
    @replace_now(datetime(2015, 9, 15))
    def test_replace_local_time_by_shift_integration(self):
        supplier = create_supplier()
        package = create_tsi_package(supplier=supplier)
        filter_, _created = Filter.objects.get_or_create(code='cysix_xml_thread.skip_station_without_times')
        PackageFilter.objects.create(filter=filter_, package=package, parameters=filter_.default_parameters, use=True)
        package.package_file = ContentFile(name=u'cysix.xml', content="""
<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
    <group code="1">
        <stations>
            <station code="1" title="1"/>
            <station code="2" title="2"/>
            <station code="3" title="3"/>
        </stations>
        <threads>
            <thread title="R-F" t_type="bus" number="RF_middle">
              <stoppoints>
                <stoppoint station_code="1" departure_time="10:00" arrival_shift=""/>
                <stoppoint station_code="3" arrival_time="" departure_time="" arrival_shift="" departure_shift="" />
                <stoppoint station_code="2" arrival_time="20:00" departure_time=""/>
              </stoppoints>
              <schedules>
                <schedule period_start_date="2015-09-01" period_end_date="2015-11-31" days="1234567"/>
              </schedules>
            </thread>
            <thread title="R-F" t_type="bus" number="RF_begin">
              <stoppoints>
                <stoppoint station_code="3" arrival_time="" departure_time="" arrival_shift="" departure_shift="" />
                <stoppoint station_code="1" departure_time="10:00" arrival_shift=""/>
                <stoppoint station_code="2" arrival_time="20:00" departure_time=""/>
              </stoppoints>
              <schedules>
                <schedule period_start_date="2015-09-01" period_end_date="2015-11-31" days="1234567"/>
              </schedules>
            </thread>
            <thread title="R-F" t_type="bus" number="RF_end">
              <stoppoints>
                <stoppoint station_code="1" departure_time="10:00" arrival_shift=""/>
                <stoppoint station_code="2" arrival_time="20:00" departure_time=""/>
                <stoppoint station_code="3" arrival_time="" departure_time="" arrival_shift="" departure_shift="" />
              </stoppoints>
              <schedules>
                <schedule period_start_date="2015-09-01" period_end_date="2015-11-31" days="1234567"/>
              </schedules>
            </thread>
        </threads>
    </group>
</channel>
            """.strip())

        station_1 = create_station(title='1', settlement=None)
        station_2 = create_station(title='2', settlement=None)
        station_3 = create_station(title='3', settlement=None)

        StationMapping.objects.create(supplier=supplier, station=station_1, code='1_vendor_1', title='1')
        StationMapping.objects.create(supplier=supplier, station=station_2, code='1_vendor_2', title='2')
        StationMapping.objects.create(supplier=supplier, station=station_3, code='1_vendor_3', title='3')

        factory = package.get_two_stage_factory()
        importer = factory.get_two_stage_importer()
        importer.reimport_package()

        assert RThread.objects.filter(route__two_stage_package=package).count() == 3
        for thread in RThread.objects.filter(route__two_stage_package=package):
            rtstations = list(thread.path)
            assert len(thread.path) == 2
            assert rtstations[0].station == station_1
            assert rtstations[1].station == station_2

        assert len(list(factory.get_data_provider().get_xml_thread_iter_for_middle_import())) == 3
        for cysix_xml_thread in factory.get_data_provider().get_xml_thread_iter_for_middle_import():
            assert len(cysix_xml_thread.supplier_rtstations) == 2
            assert cysix_xml_thread.supplier_rtstations[0].supplier_station.title == u'1'
            assert cysix_xml_thread.supplier_rtstations[1].supplier_station.title == u'2'

    def test_filter(self):
        filter_ = SkipStationWithoutTimes()

        xml_thread = mock.Mock()

        xml_thread.supplier_rtstations = [
            mock.Mock(order=1, arrival_time=None, departure_time=None,
                      arrival_minutes_shift=None, departure_minutes_shift=None),
            mock.Mock(order=2, arrival_time=None, departure_time=None,
                      arrival_minutes_shift=None, departure_minutes_shift=0),
            mock.Mock(order=3, arrival_time=None, departure_time=None,
                      arrival_minutes_shift=None, departure_minutes_shift=None),
            mock.Mock(order=4, arrival_time=50, departure_time=None,
                      arrival_minutes_shift=15, departure_minutes_shift=10),
            mock.Mock(order=5, arrival_time=None, departure_time=None,
                      arrival_minutes_shift=None, departure_minutes_shift=None),
        ]

        new_supplier_rtstations = filter_.apply(xml_thread).supplier_rtstations
        assert len(new_supplier_rtstations) == 2
        assert new_supplier_rtstations[0].order == 2
        assert new_supplier_rtstations[1].order == 4
