# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date

import mock
from hamcrest import assert_that, has_properties, contains, has_entries

from common.tester.utils.replace_setting import replace_setting
from common.apps.im_logs.models import get_im_train_car_types, YT_IM_TRAIN_CAR_TYPES_TABLES_DIR, im_reducer


def test_im_reducer():
    keys = {
        'TrainInfo__TrainName': 'train',
        'Car__TrainNumber': 'number',
        'Car__CarSubType': 'sub_type'
    }
    recs = [{'field': 1}, {'field': 2}, {'field': 3}]

    result = list(im_reducer(keys, recs))
    assert_that(result, contains(has_entries({
        'train_name': 'train',
        'train_number': 'number',
        'car_sub_type': 'sub_type',
        'im_records_count': 3
    })))


@replace_setting('YT_PROXY', 'proxy')
@replace_setting('YT_TOKEN', 'token')
def test_get_im_train_car_types():
    rows = [
        {
            'train_name': 'имя'.encode('utf-8'),
            'train_number': 'номер'.encode('utf-8'),
            'car_sub_type': 'вагон'.encode('utf-8'),
            'im_records_count': 10,
        },
        {
            'train_name': 'еще имя'.encode('utf-8'),
            'train_number': 'еще номер'.encode('utf-8'),
            'car_sub_type': 'еще вагон'.encode('utf-8'),
            'im_records_count': 20,
        },
        {
            'train_name': None
        }
    ]

    with mock.patch('common.apps.im_logs.models.get_all_yt_table_rows', return_value=rows) as m_get_all_yt_table_rows:
        im_types = list(get_im_train_car_types(date(2021, 5, 26), date(2021, 5, 27)))

    assert m_get_all_yt_table_rows.mock_calls == [
        mock.call('{}2021-05-27'.format(YT_IM_TRAIN_CAR_TYPES_TABLES_DIR)),
        mock.call('{}2021-05-26'.format(YT_IM_TRAIN_CAR_TYPES_TABLES_DIR))
    ]

    assert_that(im_types, contains(
        has_properties({
            'train_name': 'имя',
            'train_number': 'номер',
            'wagon': 'вагон',
            'records_count': 10,
        }),
        has_properties({
            'train_name': 'еще имя',
            'train_number': 'еще номер',
            'wagon': 'еще вагон',
            'records_count': 20,
        }),
        has_properties({
            'train_name': 'имя',
            'train_number': 'номер',
            'wagon': 'вагон',
            'records_count': 10,
        }),
        has_properties({
            'train_name': 'еще имя',
            'train_number': 'еще номер',
            'wagon': 'еще вагон',
            'records_count': 20
        })
    ))
