# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime

from common.models.currency import Price
from travel.rasp.wizards.train_wizard_api.direction.logger import adapt_train_direction_segments
from travel.rasp.wizards.train_wizard_api.lib.tests_utils import make_places_group, make_train_segment


def make_place(coach_type):
    return make_places_group(
        coach_type=coach_type,
        count=666,
        max_seats_in_the_same_car=666,
        price=Price(999, 'RUB'),
    )


def make_segment(places, updated_at):
    return make_train_segment(electronic_ticket=True, places=places, facilities_ids=[], updated_at=updated_at)


def test_adapt_train_direction_segments_without_price():
    assert adapt_train_direction_segments([
        make_segment(places=None, updated_at=None),
        make_segment(places=None, updated_at=None),
        make_segment(places=None, updated_at=None),
    ]) == {
        'all_segments_count': 3,
        'max_updated_at': None,
        'min_updated_at': None,
        'sold_out_segments_count': 0,
        'with_price_segments_count': 0,
        'without_price_info_segments_count': 3,
    }


def test_adapt_train_direction_segments_sold_out():
    assert adapt_train_direction_segments([
        make_segment(places=None, updated_at=datetime(2000, 9, 1)),
        make_segment(places=None, updated_at=datetime(2020, 9, 1)),
        make_segment(places=None, updated_at=datetime(2010, 9, 1)),
    ]) == {
        'all_segments_count': 3,
        'max_updated_at': 1598918400,  # GMT: Tuesday, September 1, 2020 12:00:00 AM
        'min_updated_at': 967766400,  # GMT: Friday, September 1, 2000 12:00:00 AM
        'sold_out_segments_count': 3,
        'with_price_segments_count': 0,
        'without_price_info_segments_count': 0,
    }


def test_adapt_train_direction_segments_with_places():
    assert adapt_train_direction_segments([
        make_segment(places=[make_place('common')], updated_at=datetime(2000, 9, 1)),
        make_segment(places=None, updated_at=datetime(2020, 9, 1)),
        make_segment(places=[make_place('common'), make_place('plazkarte')], updated_at=datetime(2010, 9, 1)),
    ]) == {
        'all_segments_count': 3,
        'common_segments_count': 2,
        'max_updated_at': 1598918400,  # GMT: Tuesday, September 1, 2020 12:00:00 AM
        'min_updated_at': 967766400,  # GMT: Friday, September 1, 2000 12:00:00 AM
        'plazkarte_segments_count': 1,
        'sold_out_segments_count': 1,
        'with_price_segments_count': 2,
        'without_price_info_segments_count': 0,
    }
