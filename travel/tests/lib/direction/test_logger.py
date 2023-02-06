# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from common.models.currency import Price
from travel.rasp.wizards.suburban_wizard_api.lib.direction.logger import adapt_suburban_direction_segments
from travel.rasp.wizards.suburban_wizard_api.lib.direction.segments import SuburbanSegment


def make_segment(**kwargs):
    return SuburbanSegment(*(kwargs.get(field) for field in SuburbanSegment._fields))


def test_adapt_suburban_direction_segments_without_price():
    assert adapt_suburban_direction_segments([
        make_segment(), make_segment(), make_segment()
    ]) == {
        'all_segments_count': 3,
        'without_price_info_segments_count': 3,
        'with_price_segments_count': 0,
    }


def test_adapt_suburban_direction_segments_with_prices():
    assert adapt_suburban_direction_segments([
        make_segment(price=Price(100)), make_segment(), make_segment(price=Price(200))
    ]) == {
        'all_segments_count': 3,
        'without_price_info_segments_count': 1,
        'with_price_segments_count': 2,
    }
