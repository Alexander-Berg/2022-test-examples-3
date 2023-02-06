# -*- encoding: utf-8 -*-

from StringIO import StringIO
from zipfile import ZipFile

from common.models.schedule import Supplier
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.importinfo.two_stage_import.admin import get_package_importer
from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.scripts.utils.import_file_storage import remove_schedule_temporary_today_dir


module_fixtures = [
    'travel.rasp.admin.tester.fixtures.www:countries.yaml',
    'travel.rasp.admin.tester.fixtures.www:regions.yaml',
    'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
    'travel.rasp.admin.tester.fixtures.www:stations.yaml',

    'travel.rasp.admin.cysix.tests:triangle.yaml',
]


STATION_MAPPING_CONTENT = u"""
code;    title; group; good_title; station_id; settlement_id; region_id; country_id; majority_code; file_region_id; add_info
 100; Кольцово;      ;           ; 9600370   ;              ;     11162;        225;              ;               ;
 200;  Пулково;      ;           ; 9600366   ;              ;     10174;        225;              ;               ;
""".strip()


TIMETABLE_CONTENT = u"""
маршрут и его номер; название маршрута; перевозчик; автобус; остановка; код остановки; расстояние; дни курсирования; тариф
                   ;                  ;           ;        ;          ;              ;           ; время           ;
                   ;                  ;           ;        ;          ;              ;           ;                 ;
Маршрут N1         ;К - Л             ;          1;      БВ;          ;              ;           ;1234567          ; T
                   ;                  ;           ;        ;Кольцово  ;           100;           ;2:00             ;
                   ;                  ;           ;        ;Пулково   ;           200;           ;3:00             ; 70
Конец маршрута     ;                  ;           ;        ;          ;              ;           ;                 ;
""".strip()


class TriangleImportTestStationMappings(TestCase):
    class_fixtures = module_fixtures

    def setUp(self):
        super(TriangleImportTestStationMappings, self).setUp()

        self.tsi_package = TwoStageImportPackage.objects.get(title='test_tsi_package')
        TSISetting.objects.get_or_create(package=self.tsi_package)

        zip_fileobj = StringIO()
        with ZipFile(zip_fileobj, 'w') as zipped:
            zipped.writestr('stations.csv', ' \n \n')
            zipped.writestr('carriers.csv', ' \n \n')
            zipped.writestr('station_mapping.csv', STATION_MAPPING_CONTENT.encode('utf-8'))
            zipped.writestr('timetable.csv', TIMETABLE_CONTENT.encode('utf-8'))

        zip_fileobj.seek(0)
        zip_fileobj.name = 'test_station_mapping.zip'

        self.tsi_package.package_file = zip_fileobj
        self.tsi_package.save()

        remove_schedule_temporary_today_dir(self.tsi_package)

        importer = get_package_importer(self.tsi_package)

        importer.reimport_package_into_middle_base()

        # Хак (обходим кэширование)
        # из-за @cache_method_result в импорте в промежуточную базу привязки еще нет
        # и из-за кэширования при импорте привязка не находится
        importer2 = get_package_importer(self.tsi_package)

        importer2.reimport_package()

    def tearDown(self):
        remove_schedule_temporary_today_dir(self.tsi_package)

    def testStationMapping(self):
        test_supplier = Supplier.objects.get(code=u'_test')
        self.assertEqual(StationMapping.objects.get(code='all_yandex_9600366', supplier=test_supplier).title, u'Пулково')
        self.assertEqual(StationMapping.objects.get(code='all_yandex_9600370', supplier=test_supplier).title, u'Кольцово')
