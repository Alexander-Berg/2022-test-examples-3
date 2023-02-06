# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import timedelta

from common.models.currency import Price
from common.models.schedule import DeLuxeTrain
from travel.rasp.wizards.train_wizard_api.direction.promotion import BRAND_PROMOTION, DURATION_PROMOTION
from travel.rasp.wizards.train_wizard_api.lib.tests_utils import make_places_group, make_train_segment


def test_brand_promotion():
    train_brand = DeLuxeTrain()
    without_price = make_train_segment(train_brand=train_brand)
    without_brand = make_train_segment(places=[make_places_group(price=Price(1000))])
    promoted_1 = make_train_segment(places=[make_places_group(price=Price(2000))], train_brand=train_brand)
    promoted_2 = make_train_segment(places=[make_places_group(price=Price(3000))], train_brand=train_brand)
    not_promoted = make_train_segment(places=[make_places_group(price=Price(4000))], train_brand=train_brand)

    assert BRAND_PROMOTION.apply((
        without_price, without_brand, promoted_1, promoted_2, not_promoted
    )) == (
        promoted_1, promoted_2, without_price, without_brand, not_promoted
    )


def test_duration_promotion():
    without_price = make_train_segment(duration=timedelta(hours=1))
    promoted_1 = make_train_segment(duration=timedelta(hours=2), places=[make_places_group(price=Price(1000))])
    promoted_2 = make_train_segment(duration=timedelta(hours=3), places=[make_places_group(price=Price(1000))])
    not_promoted = make_train_segment(duration=timedelta(hours=4), places=[make_places_group(price=Price(1000))])

    # the promotion wouldn't fail when there are no offers with prices
    assert DURATION_PROMOTION.apply((without_price,)) == (without_price,)

    assert DURATION_PROMOTION.apply((
        without_price, not_promoted, promoted_2, promoted_1
    )) == (
        promoted_1, promoted_2, without_price, not_promoted
    )
