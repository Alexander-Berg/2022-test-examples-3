# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.train_api.tariffs.train.im.parser import filter_bus_segments


def create_raw_segment(train_number='100Я', transport_type='Train'):
    return {
        'TrainNumber': train_number,
        'TransportType': transport_type,
    }


@pytest.mark.parametrize('segments, expected_train_numbers', [
    ([create_raw_segment('1', 'Train')], ['1']),
    ([create_raw_segment('2', 'Bus')], []),
    ([create_raw_segment('3', 'BUS')], []),
    ([create_raw_segment('4', 'bUs')], []),
    ([create_raw_segment('5', ' bus')], []),
    ([create_raw_segment('6', 'bus ')], []),
    ([create_raw_segment('7', ' bUs ')], []),
    (
        [
            create_raw_segment('0', 'Bus'),
            create_raw_segment('1', 'Train'),
            create_raw_segment('2', 'Train'),
            create_raw_segment('3', 'Bus'),
            create_raw_segment('4', 'Bus'),
            create_raw_segment('5', 'Train'),
            create_raw_segment('6', 'Bus'),
            create_raw_segment('7', 'Bus'),
            create_raw_segment('8', 'Train'),
        ],
        [
            '1', '2', '5', '8'
        ]
    ),
])
def test_filter_bus_segments(segments, expected_train_numbers):
    train_numbers = [s['TrainNumber'] for s in filter_bus_segments(segments)]

    assert train_numbers == expected_train_numbers
