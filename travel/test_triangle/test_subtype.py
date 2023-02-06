# coding: utf-8

from StringIO import StringIO
from zipfile import ZipFile

import pytest

from common.models.schedule import RThread
from common.models.transport import TransportType, TransportSubtype
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.two_stage_import.admin import get_package_importer
from travel.rasp.admin.scripts.utils.import_file_storage import remove_schedule_temporary_today_dir
from tester.factories import create_supplier, create_station

TIMETABLE_CONTENT = u"""
маршрут и его номер; название маршрута; перевозчик; автобус; остановка; код остановки; расстояние; дни курсирования; тариф
                   ;                  ;           ;        ;          ;              ;           ; время           ;
                   ;                  ;           ;        ;          ;              ;           ;                 ;
Маршрут N1         ;A - B             ;          1;      БВ;          ;              ;           ;1234567          ; T
                   ;                  ;           ;        ;A         ;             A;           ;2:00             ;
                   ;                  ;           ;        ;B         ;             B;           ;3:00             ; 70
Конец маршрута     ;                  ;           ;        ;          ;              ;           ;                 ;
""".strip()


@pytest.mark.dbuser
def test_t_type_and_t_subtype():
    """ Проверяем, что t_type и t_subtype берутся из пакета """
    t_type_id = TransportType.WATER_ID
    t_subtype_id = TransportSubtype.SEA_ID
    supplier = create_supplier()
    station_a = create_station(title=u'A')
    station_b = create_station(title=u'B')
    StationMapping.objects.create(supplier=supplier, station=station_a, title=u'A', code=u'all_vendor_A')
    StationMapping.objects.create(supplier=supplier, station=station_b, title=u'B', code=u'all_vendor_B')
    tsi_package = create_tsi_package(package_type='triangle', supplier=supplier,
                                     t_type_id=t_type_id, t_subtype_id=t_subtype_id)
    zip_fileobj = StringIO()
    with ZipFile(zip_fileobj, 'w') as zipped:
        zipped.writestr('stations.csv', ' \n \n')
        zipped.writestr('carriers.csv', ' \n \n')
        zipped.writestr('timetable.csv', TIMETABLE_CONTENT.encode('utf-8'))
    zip_fileobj.seek(0)
    zip_fileobj.name = 'test_subtype.zip'
    tsi_package.package_file = zip_fileobj
    tsi_package.save()

    try:
        importer = get_package_importer(tsi_package)
        importer.reimport_package()

        thread = RThread.objects.get()
        assert thread.t_type_id == t_type_id
        assert thread.t_subtype_id == t_subtype_id
    finally:
        remove_schedule_temporary_today_dir(tsi_package)
