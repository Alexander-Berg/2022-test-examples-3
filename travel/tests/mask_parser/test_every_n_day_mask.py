# coding: utf-8

from __future__ import unicode_literals

from datetime import datetime, date

import pytest
from django.core.files.base import ContentFile

from common.models.schedule import Supplier, RThread
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from tester.utils.datetime import replace_now


@pytest.mark.dbuser
@pytest.mark.usefixtures('setup_cysix_test_case')
@pytest.mark.parametrize('now', [datetime(2016, 11, 1), datetime(2016, 10, 1)])
class TestMaskEveryNDay(object):
    def setup(self):
        self.supplier = Supplier.objects.get(code='supplier_1')
        self.package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        setting, _created = TSISetting.objects.get_or_create(package=self.package)

        setting.set_number = True
        setting.max_forward_days = 300
        setting.save()

    def reimport(self):
        factory = self.package.get_two_stage_factory()

        self.package.package_file = ContentFile(CYSIX_XML)

        importer = factory.get_two_stage_importer()
        importer.reimport_package()

    def test_mask(self, now):
        with replace_now(now):
            self.reimport()

            dates = RThread.objects.get(number="1").get_run_date_list()
            assert date(2016, 11, 2) in dates
            assert date(2016, 11, 3) not in dates
            assert date(2016, 11, 4) in dates

            dates = RThread.objects.get(number="2").get_run_date_list()
            assert date(2016, 11, 2) in dates
            assert date(2016, 11, 3) not in dates
            assert date(2016, 11, 4) not in dates
            assert date(2016, 11, 5) not in dates
            assert date(2016, 11, 6) in dates


CYSIX_XML = """
<?xml version='1.0' encoding='utf8'?>
<channel
  t_type="bus"
  carrier_code_system="local"
  version="1.0"
  station_code_system="vendor"
  timezone="local"
  vehicle_code_system="local"
>
  <group code="group1">
    <stations>
      <station code="1" title="Станция 1" />
      <station code="2" title="Станция 2" />
    </stations>
    <threads>
      <thread title="t-title" number="1">
        <stoppoints>
          <stoppoint station_title="Станция 1" station_code="1" departure_time="13:00:00" />
          <stoppoint station_title="Станция 2" station_code="2" arrival_time="17:30:00" />
        </stoppoints>
        <schedules>
          <schedule period_start_date="2016-10-01" days="через день" times="13:00:00"/>
        </schedules>
      </thread>
      <thread title="t-title" number="2">
        <stoppoints>
          <stoppoint station_title="Станция 1" station_code="1" departure_time="12:00:00" />
          <stoppoint station_title="Станция 2" station_code="2" arrival_time="17:30:00" />
        </stoppoints>
        <schedules>
          <schedule period_start_date="2016-10-01" days="через 3 дня" times="12:00:00"/>
        </schedules>
      </thread>
     </threads>
  </group>
</channel>
""".strip()
