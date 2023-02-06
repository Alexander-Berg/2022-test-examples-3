# -*- coding: utf-8 -*-

from datetime import time, date
from StringIO import StringIO

from lxml import etree

from common.models.geo import Station
from common.models.schedule import RThread
from travel.rasp.library.python.common23.date import environment
from common.utils.date import RunMask
from tester.factories import create_thread, create_supplier
from tester.testcase import TestCase
from tester.utils.datetime import replace_now

from travel.rasp.admin.scripts.schedule.update_af_suburban import update_threads
from stationschedule.models import ZTablo2


delay_xml_template = u"""
<?xml version="1.0" encoding="utf-8"?>
<channel>
  <thread thread="{uid}" dates="2011-03-12"
    changemode="delay"
    delay_type="delay"
    delay_comment_ru="comment_ru"
    delay_comment_uk="comment_uk"
    delay_value="10"
    supplier="test"
  />

  <thread thread="{uid}" dates="2011-03-19"
    changemode="delay"
    delay_type="cancel"
    delay_comment_ru="comment"
    supplier="test"
  />

  <thread t_type="suburban" number="6645" startstationparent="100100"
    dates="2011-03-26"
    changemode="delay"
    supplier="test">

    <stations>
      <station esrcode="100100" stname="Первая" delay_type="none" />
      <station esrcode="200200" stname="Вторая" delay_type="delay" delay_arrival="10" delay_departure="20" delay_comment_ru="delay" />
      <station esrcode="300300" stname="Третья" delay_type="cancel" delay_comment_uk="cancel" />
    </stations>
  </thread>
</channel>""".strip()


class TestAfDelays(TestCase):
    @replace_now('2011-03-03 0:00:00')
    def setUp(self):
        super(TestAfDelays, self).setUp()
        create_supplier(code='af')

        dates = [
            # сб вс
            date(2011, 3, 5), date(2011, 3, 6),
            date(2011, 3, 12), date(2011, 3, 13),
            date(2011, 3, 19), date(2011, 3, 20),
            date(2011, 3, 26), date(2011, 3, 27),
        ]
        tz_msk = 'Europe/Moscow'
        thread = create_thread(
            uid=u'6645WW_1_test', number=u'6645', t_type='suburban', supplier={'code': 'test'}, time_zone=tz_msk,
            tz_start_time=time(10, 0), year_days=str(RunMask(days=dates, today=environment.today())), schedule_v1=[
                [None, 0, {'title': u'Первая', 't_type': 'suburban', 'majority': 'in_tablo', 'time_zone': tz_msk,
                           'settlement': {'majority': 1}, '__': {'codes': {'esr': '100100'}}}],
                [120, 130, {'title': u'Вторая', 't_type': 'suburban', 'majority': 'in_tablo', 'time_zone': tz_msk,
                            'settlement': {'majority': 1}, '__': {'codes': {'esr': '200200'}}}],
                [240, None, {'title': u'Третья', 't_type': 'suburban', 'majority': 'in_tablo', 'time_zone': tz_msk,
                             'settlement': {'majority': 1}, '__': {'codes': {'esr': '300300'}}}],
            ]
        )
        thread.route.route_uid = thread.gen_route_uid(use_start_station=True)
        thread.gen_uid()
        thread.save()
        thread.route.save()

        delay_xml = delay_xml_template.format(uid=thread.uid)
        self.thread_els = etree.parse(StringIO(delay_xml.encode('utf-8'))).findall('.//thread')

    @replace_now('2011-03-03 0:00:00')
    def check_tablo_has(self, check_list):
        for variant in check_list:
            self.assertTrue(ZTablo2.objects.filter(**variant), repr(variant))

    @replace_now('2011-03-03 0:00:00')
    def test_thread_delay(self):
        thread = RThread.objects.get(number=u'6645')
        station_1 = Station.get_by_code('esr', '100100')
        station_2 = Station.get_by_code('esr', '200200')
        station_3 = Station.get_by_code('esr', '300300')

        today = environment.today()

        update_threads(self.thread_els[0], 'aaa.xml', today)

        self.check_tablo_has([
            {'original_arrival': None, 'original_departure': '2011-03-12 10:00:00',
             'arrival':          None, 'departure':          '2011-03-12 10:00:00',
             'real_arrival':     None, 'real_departure':     '2011-03-12 10:10:00',
             'thread': thread, 'station': station_1,
             'comment_ru': u'comment_ru', 'comment_uk': u'comment_uk'
             },

            {'original_arrival': '2011-03-12 12:00:00', 'original_departure': '2011-03-12 12:10:00',
             'arrival':          '2011-03-12 12:00:00', 'departure':          '2011-03-12 12:10:00',
             'real_arrival':     '2011-03-12 12:10:00', 'real_departure':     '2011-03-12 12:20:00',
             'thread': thread, 'station': station_2,
             'comment_ru': u'comment_ru', 'comment_uk': u'comment_uk'
             },

            {'original_arrival': '2011-03-12 14:00:00', 'original_departure': None,
             'arrival':          '2011-03-12 14:00:00', 'departure':          None,
             'real_arrival':     '2011-03-12 14:10:00', 'real_departure':     None,
             'thread': thread, 'station': station_3,
             'comment_ru': u'comment_ru', 'comment_uk': u'comment_uk'
             }
        ])

    @replace_now('2011-03-03 0:00:00')
    def test_thread_cancel(self):
        thread = RThread.objects.get(number=u'6645')
        station_1 = Station.get_by_code('esr', '100100')
        station_2 = Station.get_by_code('esr', '200200')
        station_3 = Station.get_by_code('esr', '300300')

        today = environment.today()

        update_threads(self.thread_els[1], 'aaa.xml', today)

        self.check_tablo_has([
            {'original_arrival': None, 'original_departure': '2011-03-19 10:00:00',
             'arrival':          None, 'departure':          '2011-03-19 10:00:00',
             'arrival_cancelled': True, 'departure_cancelled': True,
             'thread': thread, 'station': station_1,
             },

            {'original_arrival': '2011-03-19 12:00:00', 'original_departure': '2011-03-19 12:10:00',
             'arrival':          '2011-03-19 12:00:00', 'departure':          '2011-03-19 12:10:00',
             'arrival_cancelled': True, 'departure_cancelled': True,
             'thread': thread, 'station': station_2,
             },

            {'original_arrival': '2011-03-19 14:00:00', 'original_departure': None,
             'arrival':          '2011-03-19 14:00:00', 'departure':          None,
             'arrival_cancelled': True, 'departure_cancelled': True,
             'thread': thread, 'station': station_3,
             }
        ])

    @replace_now('2011-03-03 0:00:00')
    def test_per_station(self):
        thread = RThread.objects.get(number=u'6645')
        station_1 = Station.get_by_code('esr', '100100')
        station_2 = Station.get_by_code('esr', '200200')
        station_3 = Station.get_by_code('esr', '300300')

        today = environment.today()

        update_threads(self.thread_els[2], 'aaa.xml', today)

        self.assertFalse(ZTablo2.objects.filter(station=station_1))

        self.check_tablo_has([
            {'original_arrival': '2011-03-26 12:00:00', 'original_departure': '2011-03-26 12:10:00',
             'arrival':          '2011-03-26 12:00:00', 'departure':          '2011-03-26 12:10:00',
             'real_arrival':     '2011-03-26 12:10:00', 'real_departure':     '2011-03-26 12:30:00',
             'arrival_cancelled': False, 'departure_cancelled': False,
             'thread': thread, 'station': station_2,
             'comment_ru': 'delay'
             },

            {'original_arrival': '2011-03-26 14:00:00', 'original_departure': None,
             'arrival':          '2011-03-26 14:00:00', 'departure':          None,
             'arrival_cancelled': True, 'departure_cancelled': True,
             'thread': thread, 'station': station_3,
             'comment_uk': 'cancel'
             }
        ])
