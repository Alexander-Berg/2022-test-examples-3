# -*- coding: utf-8 -*-

import pytest
from StringIO import StringIO

from common.utils.unicode_csv import UnicodeDictReader
from cysix.triangle.triangle2cysix import STATION_FIELDS, TriangleStation
from tester.factories import create_country

STATIONS_DATA = u"""
код остановки; название;               страна
1;             метро "Обводный канал"; Россия
2;             Ваалима, МАПП;          Финляндия
3;             по адресам, Таллин;     Эстония
""".strip()


@pytest.mark.dbuser
def test_stations_parser():
    create_country(title=u'Финляндия', code='FI')
    create_country(title=u'Эстония', code='EE')

    stations_fp = StringIO(STATIONS_DATA.encode('utf-8'))
    file_parser = UnicodeDictReader(stations_fp, fieldnames=STATION_FIELDS, encoding='utf-8',
                                    strip_values=True, delimiter=';')
    country_codes = set()
    # skip titles
    file_parser.next()
    for rowdict in file_parser:
        triangle_station = TriangleStation.create_from_rowdict(rowdict)
        country_codes.add(triangle_station.country_code)

    assert country_codes == {'RU', 'FI', 'EE'}
