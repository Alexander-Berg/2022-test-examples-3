# coding: utf8
from market.idx.datacamp.system_offers.lib.system_offer import SystemOffer
from market.pylibrary.mindexerlib import util


def test_create_assortment_system_offer():
    timestamp = util.now()
    identifiers = {
        "business_id": 1,
        "feed_id": 2,
        "offer_id": "test_offer"
    }
    fake_context = type('ConfigSection', (object,), {})()
    setattr(fake_context, 'identifiers', identifiers)
    setattr(fake_context, 'check_name', 'test_check')

    system_offer = SystemOffer(fake_context)
    united_offer = system_offer.create_offer_for_update(timestamp)

    assert united_offer.basic.identifiers.business_id == 1
    assert united_offer.basic.identifiers.offer_id == "test_offer"

    description = united_offer.basic.content.partner.original.description
    assert description.value == hex(timestamp)
    assert description.meta.timestamp.seconds == timestamp

    assert not united_offer.service
    assert not united_offer.actual


def test_create_full_system_offer():
    timestamp = util.now()
    identifiers = {
        "business_id": 1,
        "feed_id": 2,
        "shop_id": 3,
        "warehouse_id": 4,
        "offer_id": "test_offer"
    }
    fake_context = type('ConfigSection', (object,), {})()
    setattr(fake_context, 'identifiers', identifiers)
    setattr(fake_context, 'check_name', 'test_check')

    system_offer = SystemOffer(fake_context)
    united_offer = system_offer.create_offer_for_update(timestamp)

    assert united_offer.basic.identifiers.business_id == 1
    assert united_offer.basic.identifiers.offer_id == "test_offer"

    original = united_offer.basic.content.partner.original
    assert original.description.value == hex(timestamp)
    assert original.description.meta.timestamp.seconds == timestamp
    assert original.name.value

    assert united_offer.service[3].identifiers.feed_id == 2
    assert united_offer.service[3].identifiers.shop_id == 3
    print(united_offer.service[3].status.disabled)
    assert united_offer.service[3].status.disabled[0].flag

    assert united_offer.actual[3].warehouse[4].identifiers.warehouse_id == 4
