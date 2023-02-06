# coding: utf-8

import os.path
from datetime import date, datetime

import pytest
from hamcrest import contains, assert_that
from hamcrest.library.collection.issequence_containinginanyorder import contains_inanyorder

from travel.rasp.admin.scripts.utils.import_file_storage import get_all_temporary_date_files, get_schedule_temporary_date_filepath, \
    get_all_temporary_dirs_by_dt, get_stored_supplier_codes, get_tablo_temporary_datetime_filepath
from tester.factories import create_supplier
from tester.utils.replace_setting import replace_setting


@pytest.yield_fixture(autouse=True)
def tmp_data_dir(tmpdir):
    datapath = tmpdir.mkdir('datapath')
    with replace_setting('DATA_PATH', str(datapath)):
        yield datapath


def touch(filepath):
    with open(filepath, 'a') as f:
        pass

    return filepath


@pytest.mark.dbuser
def test_get_all_temporary_date_files():
    supplier = create_supplier()
    filepath_v1_0105 = touch(get_schedule_temporary_date_filepath('v1.txt', supplier, date(2016, 1, 5)))
    filepath_v1_0106 = touch(get_schedule_temporary_date_filepath('v1.txt', supplier, date(2016, 1, 6)))
    filepath_v1_0101 = touch(get_schedule_temporary_date_filepath('v1.txt', supplier, date(2016, 1, 1)))
    touch(get_schedule_temporary_date_filepath('v2.txt', supplier, date(2016, 1, 7)))

    assert_that(get_all_temporary_date_files('v1.txt', supplier).items(), contains(
        (date(2016, 1, 6), filepath_v1_0106),
        (date(2016, 1, 5), filepath_v1_0105),
        (date(2016, 1, 1), filepath_v1_0101),
    ))


@pytest.mark.dbuser
def test_get_all_temporary_dirs_by_dt():
    """
    Тестирую так как это используется в rotate_temporary_data.py
    """
    supplier_1 = create_supplier()
    supplier_2 = create_supplier()

    def get_and_touch(supplier, dt):
        return touch(get_tablo_temporary_datetime_filepath('somefile.txt', supplier, dt))

    sup1_1_10_30 = get_and_touch(supplier_1, datetime(2016, 1, 1, 10, 30))
    sup1_2_10_30 = get_and_touch(supplier_1, datetime(2016, 1, 2, 10, 30))
    sup1_2_10_40 = get_and_touch(supplier_1, datetime(2016, 1, 2, 10, 40))

    sup2_1_10_30 = get_and_touch(supplier_2, datetime(2016, 1, 1, 10, 30))
    sup2_2_10_30 = get_and_touch(supplier_2, datetime(2016, 1, 2, 10, 30))
    sup2_2_10_40 = get_and_touch(supplier_2, datetime(2016, 1, 2, 10, 40))

    assert_that(get_stored_supplier_codes('tablo'), contains_inanyorder(
        supplier_1.code,
        supplier_2.code,
    ))

    assert_that(get_all_temporary_dirs_by_dt('tablo', supplier_1.code).items(), contains(
        (datetime(2016, 1, 2, 10, 40), os.path.dirname(sup1_2_10_40)),
        (datetime(2016, 1, 2, 10, 30), os.path.dirname(sup1_2_10_30)),
        (datetime(2016, 1, 1, 10, 30), os.path.dirname(sup1_1_10_30)),
    ))
    assert_that(get_all_temporary_dirs_by_dt('tablo', supplier_2.code).items(), contains(
        (datetime(2016, 1, 2, 10, 40), os.path.dirname(sup2_2_10_40)),
        (datetime(2016, 1, 2, 10, 30), os.path.dirname(sup2_2_10_30)),
        (datetime(2016, 1, 1, 10, 30), os.path.dirname(sup2_1_10_30)),
    ))



