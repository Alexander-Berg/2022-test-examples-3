# coding: utf-8

from market.idx.pylibrary.offer_flags.flags import DisabledFlags, OfferFlags


def test_disabled_flags():
    assert DisabledFlags.PULL_PARTNER_FEED == 1 << 1


def test_offer_flags():
    assert OfferFlags.DF_NONE == 0
