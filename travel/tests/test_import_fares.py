# -*- coding: utf-8 -*-
import datetime

from django.core.files.base import ContentFile

from common.models.schedule import RThread
from common.models.tariffs import ThreadTariff
from travel.rasp.library.python.common23.date import environment
from common.utils.date import RunMask
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.lib.unittests import LogHasMessageMixIn, MaskComparisonMixIn
from tester.factories import create_supplier, create_station, create_currency, create_settlement
from tester.testcase import TestCase
from tester.utils.datetime import replace_now
from cysix.tests.utils import get_test_filepath


class CysixImportFaresTest(TestCase, LogHasMessageMixIn, MaskComparisonMixIn):
    def setUp(self):
        create_currency(code='USD')
        self.station_a = create_station(t_type='bus', title=u'Начало')
        self.station_b = create_station(t_type='bus', title=u'Конец')
        self.station_c = create_station(t_type='bus', title=u'Еще дальше', settlement=create_settlement())
        supplier = create_supplier()
        StationMapping.objects.create(station=self.station_a, code='g1_vendor_1', title=u'Начало', supplier=supplier)
        StationMapping.objects.create(station=self.station_b, code='g1_vendor_2', title=u'Конец', supplier=supplier)
        StationMapping.objects.create(station=self.station_c, code='g1_vendor_3', title=u'Еще дальше', supplier=supplier)
        self.package = create_tsi_package(supplier=supplier)
        self.factory = self.package.get_two_stage_factory()

        super(CysixImportFaresTest, self).setUp()

    @replace_now(datetime.datetime(2013, 2, 4))
    def testImportFares(self):
        with open(get_test_filepath('data', 'test_import_fares', 'threads.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(threads), 1)

        fares = ThreadTariff.objects.filter(thread_uid=threads[0].uid)

        self.assertEqual(len(fares), 3)

        for price in fares:
            if price.tariff == 12 and price.currency == u'USD':
                self.assertEqual(price.station_from.title, u"Начало")
                self.assertEqual(price.station_to.title, u"Конец")

            elif price.tariff == 20 and price.currency == u'USD':
                self.assertEqual(price.station_from.title, u"Начало")
                self.assertEqual(price.station_to.title, u"Еще дальше")

            elif price.tariff == 9 and price.currency == u'USD':
                self.assertEqual(price.station_from.title, u"Конец")
                self.assertEqual(price.station_to.title, u"Еще дальше")

            else:
                self.fail(u"Других цен быть не должно, а есть '%s'" % price)

    @replace_now(datetime.datetime(2013, 2, 4))
    def testInvalidFareCode(self):
        with open(get_test_filepath('data', 'test_import_fares', 'invalid_fare_code_in_thread.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        self.assert_log_has_message(u'ERROR: Строка 29. '
                                 u'Файл общего xml-формата содержит неизвестный код тарифа "1" '
                                 u'в гуппе "<Group title="" code="bashautotrans">"')

    @replace_now(datetime.datetime(2013, 2, 4))
    def testImportFareLinks(self):
        with open(get_test_filepath('data', 'test_import_fares', 'threads_fare_links.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        # fare_code
        thread = RThread.objects.get(route__two_stage_package=self.package, hidden_number='102')

        fares = ThreadTariff.objects.filter(thread_uid=thread.uid)

        self.assertEqual(len(fares), 1)

        for price in fares:
            if price.tariff == 12 and price.currency == u'USD':
                self.assertEqual(price.station_from.title, u'Начало')
                self.assertEqual(price.station_to.title, u'Конец')

        # fare_code overriden by farelinks
        thread = RThread.objects.get(route__two_stage_package=self.package, hidden_number='103')

        fares = ThreadTariff.objects.filter(thread_uid=thread.uid)

        self.assertEqual(len(fares), 2)

        weekday_fares = None
        weekend_fares = None

        for fare in fares:
            if fare.tariff == 50:
                weekend_fares = fare

            if fare.tariff == 12:
                weekday_fares = fare

        thread_mask = RunMask(thread.year_days, today=environment.today())
        weekday_mask = RunMask(weekday_fares.year_days, today=environment.today())
        weekend_mask = RunMask(weekend_fares.year_days, today=environment.today())

        self.assertFalse(weekday_mask - thread_mask)
        self.assertFalse(weekend_mask - thread_mask)

        weekdays = u"""
          февраль 2013
         пн   вт   ср   чт   пт   сб   вс
                              1    2    3
        # 4  # 5  # 6  # 7  # 8    9   10
        #11  #12  #13  #14  #15   16   17
        #18   19   20   21   22   23   24
         25   26   27   28
        """

        weekends = u"""
          февраль 2013
         пн   вт   ср   чт   пт   сб   вс
                              1    2    3
          4    5    6    7    8  # 9  #10
         11   12   13   14   15  #16  #17
         18   19   20   21   22   23   24
         25   26   27   28
        """

        self.assert_mask_equal_description(weekday_mask, weekdays)
        self.assert_mask_equal_description(weekend_mask, weekends)

    @replace_now(datetime.datetime(2013, 2, 4))
    def testEmptyPriceInFare(self):
        """ Проверяем, что даже с пустой ценой в тарифах нитка импортируется """

        with open(get_test_filepath('data', 'test_import_fares', 'empty_price_in_fare.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(threads), 1)

        self.assertFalse(ThreadTariff.objects.all().exists())

    @replace_now(datetime.datetime(2013, 2, 4))
    def testInvalidFare(self):
        """ Проверяем, что даже с ошибкой в тарифах нитка импортируется """

        with open(get_test_filepath('data', 'test_import_fares', 'invalid_fare.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        threads = RThread.objects.filter(route__two_stage_package=self.package)

        self.assertEqual(len(threads), 1)

    @replace_now(datetime.datetime(2013, 2, 4))
    def testInvalidFareLink(self):
        """ Проверяем, что даже с ошибкой в тарифах нитка импортируется """

        with open(get_test_filepath('data', 'test_import_fares', 'invalid_fare_link.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        threads = RThread.objects.filter(route__two_stage_package=self.package)

        self.assertEqual(len(threads), 1)

    @replace_now(datetime.datetime(2013, 2, 4))
    def testFareDaysDelimiter(self):
        with open(get_test_filepath('data', 'test_import_fares', 'fare_days_delimeter.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        threads = RThread.objects.filter(route__two_stage_package=self.package)

        self.assertEqual(len(threads), 6)

        for thread in threads:
            fares = ThreadTariff.objects.filter(thread_uid=thread.uid)

            self.assertEqual(len(fares), 1)

            fare_mask = RunMask(fares[0].year_days, today=environment.today())

            number = thread.hidden_number

            if number in ('days-delimiter', 'days-delimiter-2', 'days-delimiter-3'):
                self.assert_mask_equal_description(
                    fare_mask,
                    u"""
 февраль 2013
 пн   вт   ср   чт   пт   сб   вс
                      1    2    3
  4  # 5  # 6  # 7    8    9   10
 11   12   13   14   15   16   17
 18   19   20   21   22   23   24
 25   26   27   28
                     """)

            elif number in ('exclude-days-delimiter', 'exclude-days-delimiter-2', 'exclude-days-delimiter-3'):
                self.assert_mask_equal_description(
                    fare_mask,
                    u"""
 февраль 2013
 пн   вт   ср   чт   пт   сб   вс
                      1    2    3
# 4    5    6    7  # 8  # 9  #10
#11  #12  #13  #14  #15  #16  #17
#18   19   20   21   22   23   24
 25   26   27   28
                      """)

            else:
                self.fail(u'Неизвестный номер нитки %s' % number)

    @replace_now(datetime.datetime(2013, 2, 4))
    def test_fill_thread_tariff(self):
        xml_data = u"""
        <?xml version='1.0' encoding='utf-8'?>
        <channel version="1.0" station_code_system="vendor" t_type="bus" timezone="Europe/Moscow">
            <group code="g1">
                <stations>
                    <station code="1" title="Начало"/>
                    <station code="2" title="Конец"/>
                    <station code="3" title="Еще дальше"/>
                </stations>
                <fares>
                    <fare code="1">
                        <price price="10" currency="USD">
                            <stop_from station_code="2"/>
                            <stop_to   station_code="3"/>
                        </price>
                    </fare>
                </fares>
                <threads>
                    <thread number="number" title="Начало - Конец" t_type="bus" fare_code="1">
                      <stoppoints>
                        <stoppoint station_code="1"/>
                        <stoppoint station_code="2" departure_time="01:00" timezone="Europe/Moscow" departure_day_shift="1"/>
                        <stoppoint station_code="3" arrival_time="02:00" timezone="Europe/Moscow" arrival_day_shift="1"/>
                      </stoppoints>
                      <schedules>
                        <schedule period_start_date="2013-02-04" period_end_date="2013-02-18" days="1234567" times="23:30"/>
                      </schedules>
                    </thread>
                </threads>
            </group>
        </channel>
        """.strip()
        self.package.package_file = ContentFile(content=xml_data.encode('utf-8'), name='cysix.xml')

        importer = self.package.get_two_stage_factory().get_two_stage_importer()
        importer.reimport_package()

        thread = RThread.objects.get()
        tariff = ThreadTariff.objects.get()
        assert tariff.thread_uid == thread.uid
        assert tariff.station_from == self.station_b
        assert tariff.station_to == self.station_c
        assert tariff.settlement_from == self.station_b.settlement
        assert tariff.settlement_to == self.station_c.settlement
        assert tariff.year_days == thread.year_days
        assert tariff.tariff == 10
        assert tariff.currency == 'USD'
        assert tariff.time_zone_from == 'Europe/Moscow'
        assert tariff.time_zone_to == 'Europe/Moscow'
        assert tariff.time_from == datetime.time(1, 0)
        assert tariff.time_to == datetime.time(2, 0)
        assert tariff.duration == 60
        assert tariff.supplier == thread.supplier
        assert tariff.number == 'number'
        assert tariff.t_type == thread.t_type

        tariff_mask = RunMask(tariff.year_days_from, today=environment.today())
        thread_mask = RunMask(thread.year_days, today=environment.today())
        assert tariff_mask == thread_mask.shifted(1)  # сдвиг отличный от нуля потому, что наступили другие сутки
