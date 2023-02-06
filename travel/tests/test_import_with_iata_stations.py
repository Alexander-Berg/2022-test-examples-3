# -*- coding: utf-8 -*-

from StringIO import StringIO

from common.models.geo import StationCode, Station
from common.models.schedule import RThread
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.scripts.utils.import_file_storage import remove_schedule_temporary_today_dir


class CysixImportTest(TestCase):
    class_fixtures = ['travel.rasp.admin.tester.fixtures.www:countries.yaml', 'travel.rasp.admin.tester.fixtures.www:regions.yaml',
                      'travel.rasp.admin.tester.fixtures.www:settlements.yaml', 'travel.rasp.admin.tester.fixtures.www:stations.yaml']

    @classmethod
    def teardown_class_rasp(cls):
        remove_schedule_temporary_today_dir(cls.package)

    @classmethod
    def setup_class_rasp(cls):
        StationCode.objects.create(station=Station.objects.get(id=9600370), code='SVX', system_id=4)
        StationCode.objects.create(station=Station.objects.get(id=9600366), code='LED', system_id=4)

        cls.package = create_tsi_package()

        fileobj = StringIO()

        fileobj.write(u"""<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" t_type="bus" station_code_system="iata" timezone="local">
  <group title="all" code="all">
    <stations>
      <station title="Пулково" code="LED" code_system="iata"/>
      <station title="Кольцово" code="SVX" code_system="iata"/>
    </stations>
    <threads>
      <thread title="К - Л" number="N1">
        <schedules>
          <schedule days="1234567"/>
        </schedules>
        <stoppoints>
          <stoppoint station_title="Кольцово" station_code="SVX" station_code_system="iata" departure_time="02:00:00"/>
          <stoppoint station_title="Пулково" station_code="LED" station_code_system="iata" arrival_time="03:00:00"/>
        </stoppoints>
      </thread>
    </threads>
  </group>
</channel>
        """.encode('utf-8'))

        fileobj.seek(0)
        fileobj.name = 'test_import_iata_stations.xml'

        cls.package.package_file = fileobj

        factory = cls.package.get_two_stage_factory()

        importer = factory.get_two_stage_importer()

        importer.reimport_package_into_middle_base()

        # Хак (обходим кэширование)
        # из-за @cache_method_result в импорте в промежуточную базу привязки еще нет
        # и из-за кэширования при импорте привязка не находится
        factory2 = cls.package.get_two_stage_factory()

        importer2 = factory2.get_two_stage_importer()

        importer2.reimport_package()

    def testImportIataStations(self):
        number = 'N1'

        threads = RThread.objects.filter(route__two_stage_package=self.package, hidden_number=number)

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        thread = threads[0]

        path = thread.path

        self.assertEqual(len(path), 2)

        self.assertEqual(path[0].station.id, 9600370)
        self.assertEqual(path[1].station.id, 9600366)
