# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import pytest

from travel.rasp.library.python.common23.tester.factories import create_station
from travel.rasp.library.python.common23.models.tariffs.tester.factories import create_thread_tariff
from common.utils.date import RunMask

from travel.rasp.rasp_scripts.scripts.pathfinder.gen_maps import _gen_threadtariff_file, _fill_time_zone_map
from travel.rasp.rasp_scripts.scripts.pathfinder.tmpfiles import clean_temp, get_tmp_filepath


@clean_temp
@pytest.mark.dbuser
def test_gen_threadtariff_file():
    today = date(2015, 3, 1)
    tariff = create_thread_tariff(year_days='1' * RunMask.MASK_LENGTH, currency='RUR')
    file_path = get_tmp_filepath()
    _gen_threadtariff_file(today, file_path)
    with open(file_path, 'r') as file:
        line = file.readline()
        assert line[-1] == '\n'
        line = line.strip()
        assert line == '\t'.join(unicode(x) for x in (tariff.thread_uid,
                                                      tariff.station_from.id, tariff.station_to.id,
                                                      tariff.tariff, '1'*365, 'RUR'))


@clean_temp
@pytest.mark.dbuser
def test_gen_time_zone_file():
    today = date(2015, 3, 1)
    station = create_station()
    file_path = get_tmp_filepath()
    _fill_time_zone_map(today, file_path)
    with open(file_path, 'r') as file:
        line = file.readline()
        assert line[-1] == '\n'
        line = line.strip()
        assert line == unicode(station.time_zone) + ' 0' * 365
