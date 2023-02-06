# coding=utf-8
from __future__ import unicode_literals

import pytest

from travel.avia.subscriptions.app.model.storage import UpsertAction


def test_upsert_1(PromoSubscription, session):
    PromoSubscription = PromoSubscription(session)
    PromoSubscription.create(code='travel_news', national_version='com', language='en')
    with pytest.raises(Exception):
        PromoSubscription.create(code='travel_news', national_version='com', language='en')
    PromoSubscription.session.rollback()
    promo_1_3 = PromoSubscription.get(code='travel_news', national_version='com', language='en')
    assert promo_1_3 is None

    promo_1_4 = PromoSubscription.create(code='travel_news', national_version='com', language='en')
    promo_1_5 = PromoSubscription.get(code='travel_news', national_version='com', language='en')
    assert promo_1_5 == promo_1_4
    assert promo_1_5 is promo_1_4

    action, promo_1_6 = PromoSubscription.upsert(
        where=dict(code='travel_news', national_version='com', language='en'),
        values=dict(language='ru'),
    )
    assert action == UpsertAction.UPDATE
    assert promo_1_6.language == 'ru'
    assert promo_1_5 is promo_1_6
    promo_1_7 = PromoSubscription.get(code='travel_news', national_version='com', language='en')
    assert promo_1_7 is None
    promo_1_8 = PromoSubscription.get(code='travel_news', national_version='com', language='ru')
    assert promo_1_8 is promo_1_6


def test_upsert_2(PromoSubscription, session):
    PromoSubscription = PromoSubscription(session)
    action, promo_1_1 = PromoSubscription.upsert(
        where=dict(code='travel_news', national_version='com', language='en'),
    )
    assert action == UpsertAction.INSERT
    promo_1_5 = PromoSubscription.get(code='travel_news', national_version='com', language='en')
    assert promo_1_5 == promo_1_1
    assert promo_1_5 is promo_1_1

    action, promo_1_6 = PromoSubscription.upsert(
        where=dict(code='travel_news', national_version='com', language='en'),
        values=dict(language='ru'),
    )
    assert action == UpsertAction.UPDATE
    assert promo_1_6.language == 'ru'
    assert promo_1_5
    promo_1_7 = PromoSubscription.get(code='travel_news', national_version='com', language='en')
    assert promo_1_7 is None
    promo_1_8 = PromoSubscription.get(code='travel_news', national_version='com', language='ru')
    assert promo_1_8 is promo_1_6


def test_crud(PromoSubscription, TravelVertical, UserAuthType, session):
    PromoSubscription = PromoSubscription(session)
    TravelVertical = TravelVertical(session)
    UserAuthType = UserAuthType(session)
    promo_1_0 = PromoSubscription.get(code='travel_news', national_version='com', language='en')
    assert promo_1_0 is None
    promo_1_1 = PromoSubscription.create(code='travel_news', national_version='com', language='en')
    assert promo_1_1.id == 1
    assert promo_1_1.code == 'travel_news'
    assert promo_1_1.national_version == 'com'
    assert promo_1_1.language == 'en'
    promo_1_2 = PromoSubscription.get(code='travel_news', national_version='com', language='en')
    assert promo_1_2.id == 1
    assert promo_1_2.code == 'travel_news'
    assert promo_1_2.national_version == 'com'
    assert promo_1_2.language == 'en'

    travel_vertical_1_0 = TravelVertical.get_or_create(name='avia')
    assert travel_vertical_1_0.name == 'avia'

    user_auth_type_1_0 = UserAuthType.get_or_create(name='session')
    assert user_auth_type_1_0.name == 'session'
