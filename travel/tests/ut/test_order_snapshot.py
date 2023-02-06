# -*- coding: utf-8 -*-
from __future__ import print_function, unicode_literals

from pytest import raises
from travel.cpa.lib.order_snapshot import OrderSnapshot, OrderStatus


def test_as_dict():
    validate = OrderSnapshot.validate
    OrderSnapshot.validate = lambda self: None
    snapshot = OrderSnapshot()

    snapshot.travel_order_id = '1'
    d = snapshot.as_dict()
    assert '1' == d['travel_order_id']

    snapshot.status = OrderStatus.PENDING
    d = snapshot.as_dict()
    assert 'pending' == d['status']
    assert '4d5022617f53abe938cf4380e65209ef' == d['hash']

    snapshot._timestamp = 123
    snapshot.updated_at = 456
    snapshot.last_seen = 789
    d = snapshot.as_dict()
    assert '4d5022617f53abe938cf4380e65209ef' == d['hash']

    OrderSnapshot.validate = validate

    with raises(ValueError):
        snapshot = OrderSnapshot()
        snapshot.as_dict()


def test_attributes():
    snapshot = OrderSnapshot()
    snapshot.travel_order_id = '1'
    assert '1' == snapshot.travel_order_id

    with raises(AttributeError):
        print(snapshot.not_attribute)

    with raises(AttributeError):
        snapshot.not_attribute = None


def test_schema():
    snapshot = OrderSnapshot()

    schema = snapshot.get_logfeller_schema()
    assert 'VT_STRING' == schema['travel_order_id']

    schema = snapshot.get_yt_schema()
    assert 'uint64' == schema['created_at']
