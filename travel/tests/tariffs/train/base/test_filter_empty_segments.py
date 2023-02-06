# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.train_api.tariffs.train.base.models import TrainSegment
from travel.rasp.train_api.tariffs.train.base.query import filter_empty_segments


@pytest.mark.dbuser
@pytest.mark.parametrize('classes, broken_classes, expected_to_remain', (
    (None, None, False),
    ({}, None, False),
    (None, {}, False),
    ({}, {}, False),
    ({'soft': 'something'}, {}, True),
    ({'soft': 'something'}, None, True),
    ({}, {'soft': 'something'}, True),
    (None, {'soft': 'something'}, True),
    ({'soft': 'something'}, {}, True),
    ({'soft': 'something'}, {'soft': 'something'}, True),
))
def test_filter_empty_segments_with_reason(classes, broken_classes, expected_to_remain):
    segment = TrainSegment()
    segment.tariffs['classes'] = classes
    segment.tariffs['broken_classes'] = broken_classes

    result_segments = filter_empty_segments([segment], include_reason_for_missing_prices=True)

    if expected_to_remain:
        assert result_segments == [segment]
    else:
        assert result_segments == []


@pytest.mark.dbuser
@pytest.mark.parametrize('classes, broken_classes, expected_to_remain', (
    (None, None, False),
    ({}, None, False),
    (None, {}, False),
    ({}, {}, False),
    ({'soft': 'something'}, {}, True),
    ({'soft': 'something'}, None, True),
    ({}, {'soft': 'something'}, False),
    (None, {'soft': 'something'}, False),
    ({'soft': 'something'}, {}, True),
    ({'soft': 'something'}, {'soft': 'something'}, True),
))
def test_filter_empty_segments(classes, broken_classes, expected_to_remain):
    segment = TrainSegment()
    segment.tariffs['classes'] = classes
    segment.tariffs['broken_classes'] = broken_classes

    result_segments = filter_empty_segments([segment], include_reason_for_missing_prices=False)

    if expected_to_remain:
        assert result_segments == [segment]
    else:
        assert result_segments == []
