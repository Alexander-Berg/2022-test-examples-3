# -*- coding: utf-8 -*-

from datetime import datetime

from django.core.files.base import ContentFile

from common.models.schedule import RThread
from common.models.transport import TransportType
from cysix.base import CysixCompanyFinder
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import CompanyMapping, StationMapping
from travel.rasp.admin.lib.unittests import LogHasMessageMixIn, replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase
from tester.factories import create_station, create_company


xml_content = u"""<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
    <group code="g1">
        <stations>
            <station code="1" title="Начало"/>
            <station code="2" title="Конец"/>
        </stations>
        <carriers>
            <carrier title="" code="АУ" code_system="sirena"/>

            <carrier title="" code="АУ" code_system="iata"/>
            <carrier title="aaa" code="22" code_system="iata"/>

            <carrier title="" code="АУ" code_system="icao"/>

            <carrier title="1" code="1" code_system="vendor"/>
            <carrier title="2" code="2" code_system="vendor"/>
            <carrier title="3" code="3" code_system="vendor" url="https://create.vendor.carrier.ru" />
            <carrier title="   " code="5" code_system="vendor"/>

            <carrier title="33" code="3" code_system="local"/>
        </carriers>
        <threads>
            <thread number="sirena_with_title" carrier_code="АУ" carrier_code_system="sirena" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

            <thread number="iata_duplicate" carrier_code="22" carrier_code_system="iata" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

            <thread number="sirena_without_title" carrier_code="АУ1" carrier_code_system="sirena" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

            <thread number="iata_with_title" carrier_code="АУ" carrier_code_system="iata" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule period_start_date="2013-02-04" period_end_date="2015-02-18" days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

            <thread number="iata_without_title" carrier_code="АУ1" carrier_code_system="iata" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

            <thread number="icao_with_title" carrier_code="АУ" carrier_code_system="icao" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

            <thread number="icao_without_title" carrier_code="АУ1" carrier_code_system="icao" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

            <thread number="mapping_vendor_carrier" carrier_code="1" carrier_code_system="vendor" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

            <thread number="existed_title_vendor_carrier" carrier_code="2" carrier_code_system="vendor" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

            <thread number="create_vendor_carrier" carrier_code="3" carrier_code_system="vendor" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

            <thread number="not_found_ref_vendor" carrier_code="4" carrier_code_system="vendor" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

            <thread number="test_bad_title" carrier_code="5" carrier_code_system="vendor" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

        <!-- local -->
        <thread number="create_local" carrier_code="3" carrier_code_system="local" title="A - B">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_shift="2000"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="08:30:00"/>
              </schedules>
            </thread>

        </threads>
    </group>
</channel>"""


class CompanyFinderTest(TestCase, LogHasMessageMixIn):
    @replace_now(datetime(2013, 2, 4))
    def setUp(self):
        super(CompanyFinderTest, self).setUp()

        self.package = create_tsi_package()
        supplier = self.package.supplier

        first_station = create_station(title=u'Начало')
        last_station = create_station(title=u'Конец')
        StationMapping.objects.create(station=first_station, code='g1_vendor_1', title=u'Начало', supplier=supplier)
        StationMapping.objects.create(station=last_station, code='g1_vendor_2', title=u'Конец', supplier=supplier)
        company = create_company(title='1')
        self.company_mapping = CompanyMapping.objects.create(title='1', code='g1_vendor_1',
                                                             supplier=supplier, company=company)
        self.companies = {
            'sirena_with_title': create_company(title='AY', sirena_id=u'АУ'),
            'sirena_without_title': create_company(title='AY', sirena_id=u'АУ1'),
            'iata_with_title': create_company(title='AY', iata=u'АУ'),
            'iata_without_title': create_company(title='AY', iata=u'АУ1'),
            'icao_with_title': create_company(title='AY', icao=u'АУ'),
            'icao_without_title': create_company(title='AY', icao=u'АУ1'),
            'existed_title_vendor_carrier': create_company(title='2', t_type=TransportType.BUS_ID)
        }
        create_company(title='AY', iata='22')
        create_company(title='AY', iata='22')

        self.factory = self.package.get_two_stage_factory()
        self.package.package_file = ContentFile(name='cysix.xml', content=xml_content)
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package()

    def testAll(self):
        self._testSupportedSystems()
        self._testMappingVendorCarrier()
        self._testExistedTitleVendorCarrier()
        self._testCreateVendorCarrier()
        self._testNotFoundRefVendor()
        self._testBadTitleVendor()
        self._testCreateLocal()

    def _testSupportedSystems(self):
        for system in CysixCompanyFinder.SUPPORTED_CODE_SYSTEMS:
            number_code = '%s_with_title' % system
            company = self.companies[number_code]
            thread = RThread.objects.get(hidden_number=number_code)
            self.assertEqual(thread.company, company)

            number_code = '%s_without_title' % system
            company = self.companies[number_code]
            thread = RThread.objects.get(hidden_number=number_code)
            self.assertEqual(thread.company, company)

        self.assert_log_has_message(u"ERROR: Несколько компании с кодом iata 22")
        self.assertTrue(CompanyMapping.objects.get(title='aaa', code='g1_iata_22'))

    def _testMappingVendorCarrier(self):
        number_code = 'mapping_vendor_carrier'
        thread = RThread.objects.get(hidden_number=number_code)
        self.assertEqual(thread.company, self.company_mapping.company)

    def _testExistedTitleVendorCarrier(self):
        number_code = 'existed_title_vendor_carrier'
        company = self.companies[number_code]
        thread = RThread.objects.get(hidden_number=number_code)
        self.assertTrue(CompanyMapping.objects.filter(title=u"2", code="g1_vendor_2"))
        self.assertEqual(thread.company, company)

    def _testCreateVendorCarrier(self):
        number_code = 'create_vendor_carrier'
        mapping = CompanyMapping.objects.get(title=u"3", code="g1_vendor_3")
        thread = RThread.objects.get(hidden_number=number_code)
        self.assertEqual(thread.company, mapping.company)
        self.assertEqual(thread.company.url, 'https://create.vendor.carrier.ru')

    def _testNotFoundRefVendor(self):
        self.assert_log_has_message(u'ERROR: Пропускаем <CysixXmlThread: title="A - B" number="not_found_ref_vendor" sourceline=124 <Group title="" code="g1" sourceline=3>>: Не нашли в справочнике перевозчика с кодом 4 система vendor')

    def _testBadTitleVendor(self):
        self.assert_log_has_message(u'ERROR: Пропускаем <CysixXmlThread: title="A - B" number="test_bad_title" sourceline=134 <Group title="" code="g1" sourceline=3>>: Не указано название у перевозчика с кодом 5 система vendor')

    def _testCreateLocal(self):
        number_code = 'create_local'
        mapping = CompanyMapping.objects.get(title=u"33", code="g1_local")
        thread = RThread.objects.get(hidden_number=number_code)
        self.assertEqual(thread.company, mapping.company)
