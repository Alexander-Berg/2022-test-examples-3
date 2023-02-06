# coding: utf-8

from __future__ import unicode_literals

from datetime import datetime, date

from django.core.files.base import ContentFile

from common.models.schedule import Supplier, RThread
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.unittests import replace_now
from cysix.tests.utils import CysixTestCase


class TestThreadSettingAllowToImport(CysixTestCase):
    def setUp(self):
        self.supplier = Supplier.objects.get(code='supplier_1')

        self.package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        TSISetting.objects.get_or_create(package=self.package)
        self.package.tsisetting.set_number = True
        self.package.tsisetting.save()

        super(TestThreadSettingAllowToImport, self).setUp()

    def reimport(self):
        factory = self.package.get_two_stage_factory()

        self.package.package_file = ContentFile(CYSIX_XML)

        importer = factory.get_two_stage_importer()
        importer.reimport_package()

    @replace_now(datetime(2015, 3, 1))
    def test_mask_is_not_exrapolatable(self):
        self.reimport()

        assert not RThread.objects.get(number='t-number').has_extrapolatable_mask

    @replace_now(datetime(2015, 3, 1))
    def test_mask_be_exrapolatabled(self):
        self.package.add_default_filters()
        package_filter = self.package.packagefilter_set.get(filter__code='text_schedule_and_extrapolation')
        package_filter.parameters.update_parameter_value('extrapolation_limit_length', 20)
        package_filter.save()

        self.package.tsisetting.max_forward_days = 100
        self.package.tsisetting.save()

        self.reimport()

        thread = RThread.objects.get(number='t-number-2')
        assert thread.has_extrapolatable_mask
        assert thread.get_mask()[date(2015, 3, 15)]
        assert not thread.get_mask()[date(2015, 3, 30)]

        package_filter = self.package.packagefilter_set.get(filter__code='text_schedule_and_extrapolation')
        package_filter.parameters.update_parameter_value('extrapolation_limit_length', 100)
        package_filter.save()

        self.reimport()

        thread = RThread.objects.get(number='t-number-2')
        assert thread.has_extrapolatable_mask
        assert thread.get_mask()[date(2015, 3, 10)]
        assert thread.get_mask()[date(2015, 3, 30)]
        assert thread.get_mask()[date(2015, 4, 20)]


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
      <thread title="t-title" number="t-number">
        <stoppoints>
          <stoppoint station_title="Станция 1" station_code="1" departure_time="13:00:00" />
          <stoppoint station_title="Станция 2" station_code="2" arrival_time="17:30:00" />
        </stoppoints>
        <schedules>
          <schedule period_start_date="2013-03-29" period_end_date="2015-03-29" days="1234567" times="13:00:00"/>
        </schedules>
      </thread>
      <thread title="t-title" number="t-number-2">
        <stoppoints>
          <stoppoint station_title="Станция 1" station_code="1" departure_time="13:00:00" />
          <stoppoint station_title="Станция 2" station_code="2" arrival_time="17:30:00" />
        </stoppoints>
        <schedules>
          <schedule days="2015-03-01;2015-03-02;2015-03-03;2015-03-04;2015-03-05;2015-03-06;2015-03-07;2015-03-08;2015-03-09;2015-03-10;2015-03-11;2015-03-12;2015-03-13;2015-03-14;2015-03-15;2015-03-16" times="13:00:00"/>
        </schedules>
      </thread>
     </threads>
  </group>
</channel>
""".strip()


class TestChangeHasExtrapolatableMaskAfterReimport(CysixTestCase):
    XML_WITHOUT_PERIOD_END = '''
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
          <thread title="t-title" number="t-number">
            <stoppoints>
              <stoppoint station_title="Станция 1" station_code="1" departure_time="13:00:00" />
              <stoppoint station_title="Станция 2" station_code="2" arrival_time="17:30:00" />
            </stoppoints>
            <schedules>
              <schedule period_start_date="2015-03-01" days="1234567" times="13:00:00"/>
            </schedules>
          </thread>
         </threads>
      </group>
    </channel>'''.strip()

    XML_WITH_PERIOD_END = '''
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
              <thread title="t-title" number="t-number">
                <stoppoints>
                  <stoppoint station_title="Станция 1" station_code="1" departure_time="13:00:00" />
                  <stoppoint station_title="Станция 2" station_code="2" arrival_time="17:30:00" />
                </stoppoints>
                <schedules>
                  <schedule period_start_date="2015-03-01" period_end_date="2015-03-30" days="1234567" times="13:00:00"/>
                </schedules>
              </thread>
             </threads>
          </group>
        </channel>'''.strip()

    def setUp(self):
        self.supplier = Supplier.objects.get(code='supplier_1')

        self.package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        TSISetting.objects.get_or_create(package=self.package)
        self.package.tsisetting.set_number = True
        self.package.tsisetting.save()

        super(TestChangeHasExtrapolatableMaskAfterReimport, self).setUp()

    def reimport(self, content):
        factory = self.package.get_two_stage_factory()

        self.package.package_file = ContentFile(content)

        importer = factory.get_two_stage_importer()
        importer.reimport_package()

    @replace_now(datetime(2015, 3, 1))
    def test_that_flag_removed_after_import(self):
        self.package.add_default_filters()
        package_filter = self.package.packagefilter_set.get(filter__code='text_schedule_and_extrapolation')
        package_filter.parameters.update_parameter_value('extrapolation_limit_length', 90)
        package_filter.save()

        self.package.tsisetting.max_forward_days = 90
        self.package.tsisetting.save()

        self.reimport(self.XML_WITHOUT_PERIOD_END)

        thread = RThread.objects.get(number='t-number')
        assert thread.has_extrapolatable_mask
        assert thread.get_mask()[date(2015, 3, 15)]
        assert thread.get_mask()[date(2015, 3, 30)]
        assert thread.get_mask()[date(2015, 4, 15)]

        self.reimport(self.XML_WITH_PERIOD_END)

        thread_after = RThread.objects.get(number='t-number')
        assert thread_after.id == thread.id
        assert not thread_after.has_extrapolatable_mask
        assert thread_after.get_mask()[date(2015, 3, 10)]
        assert thread_after.get_mask()[date(2015, 3, 30)]
        assert not thread_after.get_mask()[date(2015, 4, 20)]
