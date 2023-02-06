# coding: utf-8

from __future__ import unicode_literals

from datetime import datetime

from django.core.files.base import ContentFile

from common.models.geo import Station
from common.models.schedule import Supplier, RThread
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSIThreadSetting, TSISetting
from travel.rasp.admin.lib.unittests import replace_now
from cysix.tests.utils import CysixTestCase


class TestThreadSettingAllowToImport(CysixTestCase):
    def setUp(self):
        self.supplier = Supplier.objects.get(code='supplier_1')

        self.package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        TSISetting.objects.get_or_create(package=self.package)
        self.package.tsisetting.set_number = True
        self.package.tsisetting.save()

        self.package.package_file = ContentFile(CYSIX_XML)
        factory = self.package.get_two_stage_factory()

        xml_thread = list(
            factory.get_data_provider().get_xml_thread_iter_for_middle_import()
        )[0]

        supplier_route = xml_thread.get_supplier_routes()[0]

        self.path_key = supplier_route.get_path_key()

        super(TestThreadSettingAllowToImport, self).setUp()

    def reimport(self):
        factory = self.package.get_two_stage_factory()

        self.package.package_file = ContentFile(CYSIX_XML)

        importer = factory.get_two_stage_importer()
        importer.reimport_package()

    @replace_now(datetime(2013, 5, 15))
    def testNotAllowToImport(self):
        setting = TSIThreadSetting.objects.create(package=self.package, path_key=self.path_key)
        setting.allow_to_import = False
        setting.save()

        self.reimport()

        self.assertRaises(RThread.DoesNotExist, RThread.objects.get, number='t-number')

    @replace_now(datetime(2013, 5, 15))
    def testAllowToImport(self):
        setting = TSIThreadSetting.objects.create(package=self.package, path_key=self.path_key)
        setting.allow_to_import = True
        setting.save()

        self.reimport()

        self.assertTrue(RThread.objects.get(number='t-number'))

    @replace_now(datetime(2013, 5, 15))
    def testDefaultSetting(self):
        TSIThreadSetting.objects.filter(package=self.package, path_key=self.path_key).delete()

        self.reimport()

        self.assertTrue(RThread.objects.get(number='t-number'))


class TestThreadSettingTimezoneOverride(CysixTestCase):
    def setUp(self):
        station_3 = Station.objects.get(title=u'Станция 3')
        station_3.settlement = self.ekb_settlement
        station_3.time_zone = 'Asia/Yekaterinburg'
        station_3.save()

        self.supplier = Supplier.objects.get(code='supplier_1')

        self.package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        TSISetting.objects.get_or_create(package=self.package)
        self.package.tsisetting.set_number = True
        self.package.tsisetting.save()

        self.package.package_file = ContentFile(CYSIX_XML)
        factory = self.package.get_two_stage_factory()

        xml_thread = list(
            factory.get_data_provider().get_xml_thread_iter_for_middle_import()
        )[0]

        supplier_route = xml_thread.get_supplier_routes()[0]

        self.path_key = supplier_route.get_path_key()

        super(TestThreadSettingTimezoneOverride, self).setUp()

    def reimport(self, timezone_override):
        factory = self.package.get_two_stage_factory()

        setting = TSIThreadSetting.objects.create(package=self.package, path_key=self.path_key)
        setting.timezone_override = timezone_override
        setting.save()

        self.package.package_file = ContentFile(CYSIX_XML)

        importer = factory.get_two_stage_importer()
        importer.reimport_package()

    @replace_now(datetime(2013, 5, 15))
    def testNoneOverride(self):
        self.reimport(timezone_override='none')

        thread = RThread.objects.get(number='t-number')

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        self.assertEqual(path[0].time_zone, 'Europe/Moscow')
        self.assertEqual(path[1].time_zone, 'Europe/Moscow')
        self.assertEqual(path[2].time_zone, 'Asia/Yekaterinburg')

    @replace_now(datetime(2013, 5, 15))
    def testLocalOverride(self):
        self.reimport(timezone_override='local')

        thread = RThread.objects.get(number='t-number')

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        self.assertEqual(path[0].time_zone, 'Europe/Moscow')
        self.assertEqual(path[1].time_zone, 'Europe/Moscow')
        self.assertEqual(path[2].time_zone, 'Asia/Yekaterinburg')

    @replace_now(datetime(2013, 5, 15))
    def testStartStationOverride(self):
        self.reimport(timezone_override='start_station')

        thread = RThread.objects.get(number='t-number')

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        self.assertEqual(path[0].time_zone, 'Europe/Moscow')
        self.assertEqual(path[1].time_zone, 'Europe/Moscow')
        self.assertEqual(path[2].time_zone, 'Europe/Moscow')

    @replace_now(datetime(2013, 5, 15))
    def testSpecifiedTZStationOverride(self):
        self.reimport(timezone_override='Europe/Kiev')

        thread = RThread.objects.get(number='t-number')

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        self.assertEqual(path[0].time_zone, 'Europe/Kiev')
        self.assertEqual(path[1].time_zone, 'Europe/Kiev')
        self.assertEqual(path[2].time_zone, 'Europe/Kiev')


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
      <station code="3" title="Станция 3" />
    </stations>
    <vehicles>
      <vehicle code="2" title="ПАЗ-3205"/>
    </vehicles>
    <carriers>
      <carrier code="3" title="Carrier-title"/>
    </carriers>
    <threads>
      <thread title="t-title" number="t-number" carrier_code="3" vehicle_code="2">
        <stoppoints>
          <stoppoint station_title="Станция 1" station_code="1" departure_time="13:00:00" />
          <stoppoint station_title="Станция 2" station_code="2" arrival_time="14:10:00" departure_time="14:11:00" />
          <stoppoint station_title="Станция 3" station_code="3" arrival_time="17:30:00" />
        </stoppoints>
        <schedules>
          <schedule period_start_date="2013-03-29" period_end_date="2015-03-29" days="135" times="13:00:00"/>
        </schedules>
      </thread>
     </threads>
  </group>
</channel>
""".strip()
