# coding: utf-8

from __future__ import unicode_literals


import mock
import pytest

from common.models.schedule import Company, RThread
from travel.rasp.admin.importinfo.models.af import AFScheduleFile
from travel.rasp.admin.scripts.schedule import re_import_af_schedule
from travel.rasp.admin.scripts.schedule.re_import_af_schedule import reimport_af
from tester.factories import create_supplier, create_station, create_region
from tester.utils.datetime import replace_now


thread_3333_content = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3333" startstationparent="000001"
          dates="2015-06-23;2015-06-24" changemode="insert">
    <stations>
      <station esrcode="000001" stname="1"
               arrival_time="" stop_time=""
               departure_time="12:00" minutes_from_start="0" />
      <station esrcode="000002" stname="2"
               arrival_time="12:40" stop_time="10"
               departure_time="12:50" minutes_from_start="40" />
      <station esrcode="000003" stname="3"
               arrival_time="13:00" stop_time=""
               departure_time="" minutes_from_start="60" />
    </stations>
  </thread>
</channel>
""".strip()

thread_3334_content = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread t_type="suburban" number="3334" startstationparent="000001"
          dates="2015-06-23;2015-06-24" changemode="insert">
    <stations>
      <station esrcode="000001" stname="1"
               arrival_time="" stop_time=""
               departure_time="13:00" minutes_from_start="0" />
      <station esrcode="000002" stname="2"
               arrival_time="12:40" stop_time="10"
               departure_time="13:50" minutes_from_start="40" />
      <station esrcode="000003" stname="3"
               arrival_time="14:00" stop_time=""
               departure_time="" minutes_from_start="60" />
    </stations>
  </thread>
</channel>
""".strip()


@pytest.mark.dbuser
class TestReimport(object):
    def setup_data(self):
        Company.objects.create(id=112, title='РЖД')
        create_supplier(id=4, code='af')
        create_station(id=1101, t_type='suburban', __={'codes': {'esr': '000001'}})
        create_station(id=1102, t_type='suburban', __={'codes': {'esr': '000002'}})
        create_station(id=1103, t_type='suburban', __={'codes': {'esr': '000003'}})
        self.region = create_region()

    @replace_now('2015-06-01')
    def test_add_import(self):
        self.setup_data()
        schedule_file_1 = AFScheduleFile.objects.create(region=self.region, schedule_file_name='1.xml',
                                                        schedule_file=thread_3333_content)
        with mock.patch.object(re_import_af_schedule, 'flags', {'maintenance': False}):
            reimport_af('suburban', add_import=True)

        assert RThread.objects.filter(supplier__code='af').count() == 1
        assert RThread.objects.filter(supplier__code='af').get().number == '3333'

        schedule_file_1.delete()

        AFScheduleFile.objects.create(region=self.region, schedule_file_name='2.xml',
                                      schedule_file=thread_3334_content)
        with mock.patch.object(re_import_af_schedule, 'flags', {'maintenance': False}):
            reimport_af('suburban', add_import=True)

        assert RThread.objects.filter(supplier__code='af').count() == 2
        assert {t.number for t in RThread.objects.filter(supplier__code='af')} == {'3333', '3334'}

    @replace_now('2015-06-01')
    def test_re_import(self):
        self.setup_data()
        schedule_file_1 = AFScheduleFile.objects.create(region=self.region, schedule_file_name='1.xml',
                                                        schedule_file=thread_3333_content)
        with mock.patch.object(re_import_af_schedule, 'flags', {'maintenance': False}):
            reimport_af('suburban', add_import=False)

        assert RThread.objects.filter(supplier__code='af').count() == 1
        assert RThread.objects.filter(supplier__code='af').get().number == '3333'

        schedule_file_1.delete()

        AFScheduleFile.objects.create(region=self.region, schedule_file_name='2.xml',
                                      schedule_file=thread_3334_content)
        with mock.patch.object(re_import_af_schedule, 'flags', {'maintenance': False}):
            reimport_af('suburban', add_import=False)

        assert RThread.objects.filter(supplier__code='af').count() == 1
        assert RThread.objects.filter(supplier__code='af').get().number == '3334'
