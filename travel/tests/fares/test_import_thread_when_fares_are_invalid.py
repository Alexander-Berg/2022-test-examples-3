# coding: utf-8

from __future__ import unicode_literals

import pytest
from django.core.files.base import ContentFile

from common.models.schedule import Supplier, RThread
from common.models.tariffs import ThreadTariff
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.logs import get_collector_context
from tester.factories import create_currency


@pytest.mark.dbuser
@pytest.mark.usefixtures('setup_cysix_test_case')
class TestMaskEveryNDay(object):
    def setup(self):
        create_currency(code='USD')
        self.supplier = Supplier.objects.get(code='supplier_1')
        self.package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        TSISetting.objects.get_or_create(package=self.package)

    def reimport(self):
        factory = self.package.get_two_stage_factory()

        self.package.package_file = ContentFile(CYSIX_XML)

        importer = factory.get_two_stage_importer()
        importer.reimport_package()

    def test_import_thread_when_fares_are_invalid(self):
        with get_collector_context() as log_cpllector:
            self.reimport()
            log_messages = log_cpllector.get_collected(clean=False).strip().splitlines(False)

        thread = RThread.objects.get()
        assert not ThreadTariff.objects.filter(thread_uid=thread.uid).exists()

        expected_log_message = (
            'ERROR: '
            'Пропускаем тарифы для '
            '<CysixXmlThread: title="t-title" number="1" sourceline=24 <Group title="" code="group1" sourceline=10>> '
            'Станции <StopPoint: station_title="" station_code="invalid-code" station_code_system="vendor"> '
            'нет в блоке stations'
        )
        assert expected_log_message in log_messages


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
    <fares>
      <fare code="1">
        <price price="1" currency="USD">
          <stop_from station_code="1"/>
          <stop_to   station_code="invalid-code"/>
        </price>
      </fare>
    </fares>
    <threads>
      <thread title="t-title" number="1">
        <stoppoints>
          <stoppoint station_title="Станция 1" station_code="1" departure_time="13:00:00" />
          <stoppoint station_title="Станция 2" station_code="2" arrival_time="17:30:00" />
        </stoppoints>
        <schedules>
          <schedule period_start_date="2016-10-01" days="1234567" times="13:00:00"/>
        </schedules>
        <fares><fare code="1" days="1234567" /></fares>
      </thread>
     </threads>
  </group>
</channel>
""".strip()
