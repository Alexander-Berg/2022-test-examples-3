# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from common.models.currency import Price
from travel.rasp.wizards.train_wizard_api.direction.segments import TrainVariant
from travel.rasp.wizards.train_wizard_api.direction.sorting import BEST_TRAIN_OFFERS_SORTING
from travel.rasp.wizards.train_wizard_api.lib.tests_utils import make_places_group, make_train_segment
from travel.rasp.wizards.wizard_lib.tests_utils import utc_dt

pytestmark = pytest.mark.dbuser


def test_best_train_offers_sorting():
    without_price = TrainVariant(
        places_group=None,
        segment=make_train_segment(departure_local_dt=utc_dt(2000, 1, 1), arrival_local_dt=utc_dt(2000, 1, 2))
    )
    with_high_price = TrainVariant(
        places_group=make_places_group(price=Price(10000)),
        segment=make_train_segment(departure_local_dt=utc_dt(2000, 1, 1), arrival_local_dt=utc_dt(2000, 1, 2))
    )
    with_night_departure = TrainVariant(
        places_group=make_places_group(price=Price(1000)),
        segment=make_train_segment(departure_local_dt=utc_dt(2000, 1, 1, 1), arrival_local_dt=utc_dt(2000, 1, 2, 12))
    )
    with_night_arrival = TrainVariant(
        places_group=make_places_group(price=Price(1000)),
        segment=make_train_segment(departure_local_dt=utc_dt(2000, 1, 1, 12), arrival_local_dt=utc_dt(2000, 1, 2, 1))
    )
    best_offer = TrainVariant(
        places_group=make_places_group(price=Price(1000)),
        segment=make_train_segment(departure_local_dt=utc_dt(2000, 1, 1, 12), arrival_local_dt=utc_dt(2000, 1, 2, 12))
    )

    assert BEST_TRAIN_OFFERS_SORTING.apply((
        without_price, with_high_price, with_night_departure, with_night_arrival, best_offer
    )) == [
        best_offer, with_night_departure, with_night_arrival, with_high_price, without_price
    ]
