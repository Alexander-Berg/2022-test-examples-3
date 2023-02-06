# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import assert_that, all_of
from hamcrest.library.collection.isdict_containingkey import has_key

from common.tester.factories import create_station
from travel.rasp.train_api.train_partners.base.train_details.serialization import TrainDetailsQuerySchema
from travel.rasp.train_api.train_purchase.core.models import TrainPartner


@pytest.mark.dbuser
def test_make_ufs_query_without_express_codes():
    station_from = create_station()
    station_to = create_station()

    schema = TrainDetailsQuerySchema()
    data, errors = schema.load({
        'stationFrom': station_from.id,
        'stationTo': station_to.id,
        'when': '2000-01-01',
        'number': '100'
    })
    assert_that(errors, all_of(has_key('stationFrom'), has_key('stationTo')))


@pytest.mark.dbuser
def test_make_ufs_query_with_express_codes():
    express_from = '100500'
    express_to = '200300'
    station_from = create_station(__={'codes': {'express': express_from}})
    station_to = create_station(__={'codes': {'express': express_to}})

    schema = TrainDetailsQuerySchema()
    ufs_query, errors = schema.load({
        'stationFrom': station_from.id,
        'stationTo': station_to.id,
        'when': '2000-01-01T00:00:00',
        'number': '100'
    })
    assert not errors

    assert ufs_query.station_from == station_from
    assert ufs_query.station_to == station_to
    assert ufs_query.express_from == express_from
    assert ufs_query.express_to == express_to
    assert ufs_query.when == ufs_query.railway_dt == datetime(2000, 1, 1)
    assert ufs_query.number == '100'


@pytest.mark.dbuser
def test_make_im_query_with_provider():
    express_from = '100500'
    express_to = '200300'
    station_from = create_station(__={'codes': {'express': express_from}})
    station_to = create_station(__={'codes': {'express': express_to}})

    schema = TrainDetailsQuerySchema()
    im_query, errors = schema.load({
        'stationFrom': station_from.id,
        'stationTo': station_to.id,
        'when': '2000-01-01T00:00:00',
        'number': '100',
        'partner': 'im',
        'provider': 'P2',
    })

    assert not errors
    assert im_query.station_from == station_from
    assert im_query.station_to == station_to
    assert im_query.express_from == express_from
    assert im_query.express_to == express_to
    assert im_query.when == im_query.railway_dt == datetime(2000, 1, 1)
    assert im_query.number == '100'
    assert im_query.partner == TrainPartner.IM
    assert im_query.provider == 'P2'
