# -*- coding: utf-8 -*-
from StringIO import StringIO
from cysix.models import Filter, PackageFilter
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import (TwoStageImportPackage, TwoStageImportThread, CysixGroupFilter,
                                                TSISetting)
from tester.factories import create_supplier, create_station
from tester.testcase import TestCase


cysix_template = u"""<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" t_type="bus" station_code_system="vendor" timezone="start_station">
  <group title="all" code="all">
    <stations>
      <station title="A" code="A" />
      <station title="{second_station_title}" code="B" />
      <station title="C" code="C" />
    </stations>
    <threads>
      <thread title="A - C">
        <schedules><schedule days="1234567" /></schedules>
        <stoppoints>
          <stoppoint station_title="А" station_code="A" departure_time="08:00"/>
          <stoppoint station_title="{second_station_title}" station_code="B" departure_time="08:30"/>
          <stoppoint station_title="C" station_code="C" arrival_time="09:30"/>
        </stoppoints>
      </thread>
    </threads>
  </group>
</channel>
"""


class TestMappingByCode(TestCase):
    def test_mapping_by_code_with_use_thread_info(self):
        supplier = create_supplier()
        package = create_tsi_package(supplier=supplier)
        package.tsisetting.filter_by_group = True
        package.tsisetting.save()
        filter_ = Filter.objects.get(code='allow_station_mapping_by_code')
        PackageFilter.objects.create(package=package, filter=filter_, parameters=filter_.default_parameters, use=True)
        CysixGroupFilter.objects.create(package=package, code='all', title='all', tsi_middle_available=True,
                                        use_thread_in_station_code=True)

        station_a = create_station(title=u'A')
        station_b = create_station(title=u'B')
        station_c = create_station(title=u'C')

        StationMapping.objects.create(supplier=supplier, station=station_a, title=u'A', code=u'all_vendor_A_A - C_')
        StationMapping.objects.create(supplier=supplier, station=station_b, title=u'B', code=u'all_vendor_B_A - C_')
        StationMapping.objects.create(supplier=supplier, station=station_c, title=u'C', code=u'all_vendor_C_A - C_')

        fileobj = StringIO()
        fileobj.write(cysix_template.format(second_station_title=u'B-chaneged').encode('utf-8'))
        fileobj.seek(0)
        fileobj.name = 'cysix.xml'
        package.package_file = fileobj

        importer = package.get_two_stage_factory().get_two_stage_importer()
        importer.reimport_package_into_middle_base()

        assert TwoStageImportThread.objects.all().count() == 1

        tsi_thread = TwoStageImportThread.objects.all()[0]

        station_ids = [s.id for s in tsi_thread.get_stations()]
        assert station_ids == [station_a.id, station_b.id, station_c.id]
