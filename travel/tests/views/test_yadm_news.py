# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime

import pytest
from hamcrest import assert_that, has_entries, contains, contains_inanyorder
from django.test import Client

from common.models.factories import create_settlement, create_station, create_external_direction, create_info
from common.apps.info_center.models import YadmNewsItem
from common.models.geo import CityMajority

from travel.rasp.info_center.info_center.views.yadm_news import (
    _get_teaser_settlements, _get_geo_ids, _get_main_settlement
)


pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


def test_get_teaser_settlements():
    settlement1 = create_settlement(_geo_id=111, majority=CityMajority.CAPITAL_ID)
    settlement2 = create_settlement(_geo_id=112, majority=CityMajority.REGION_CAPITAL_ID)
    settlement3 = create_settlement(_geo_id=113, majority=CityMajority.COMMON_CITY_ID)
    station2 = create_station(settlement=settlement2)
    station3 = create_station(settlement=settlement3)
    direction2 = create_external_direction(base_station=station2)

    info = create_info(
        settlements=[settlement1]
    )

    settlements = _get_teaser_settlements(info)
    assert settlements == {settlement1}
    assert _get_geo_ids(settlements) == [111]
    assert _get_main_settlement(settlements) == settlement1

    info = create_info(
        stations=[station2, station3]
    )
    settlements = _get_teaser_settlements(info)
    assert settlements == {settlement2, settlement3}
    assert_that(_get_geo_ids(settlements), contains_inanyorder(112, 113))
    assert _get_main_settlement(settlements) == settlement2

    info = create_info(
        external_directions=[direction2],
    )
    settlements = _get_teaser_settlements(info)
    assert settlements == {settlement2}
    assert _get_geo_ids(settlements) == [112]
    assert _get_main_settlement(settlements) == settlement2

    info = create_info(
        settlements=[settlement1],
        stations=[station2],
        external_directions=[direction2]
    )
    settlements = _get_teaser_settlements(info)
    assert settlements == {settlement1, settlement2}
    assert_that(_get_geo_ids(settlements), contains_inanyorder(111, 112))
    assert _get_main_settlement(settlements) == settlement1

    info = create_info()
    settlements = _get_teaser_settlements(info)
    assert settlements == set()
    assert _get_geo_ids(settlements) == []
    assert _get_main_settlement(settlements) is None


def test_yadm_news():
    settlement1 = create_settlement(_geo_id=111, title_ru='Город1')
    settlement2 = create_settlement(_geo_id=112, title_ru='Город2')
    station2 = create_station(settlement=settlement2)

    create_info(
        settlements=[settlement1],
        yadm_news=[
            YadmNewsItem(
                id=11, title='title1', text='text1', importance=1, dt_created=datetime(2020, 7, 1, 8, 10, 0)
            ),
            YadmNewsItem(
                id=13, title='title3', text='text3', importance=2, dt_created=datetime(2020, 7, 3, 10, 30, 0)
            ),
        ]
    )

    create_info(
        stations=[station2],
        yadm_news=[
            YadmNewsItem(
                id=12, title='title2', text='text2', importance=0, dt_created=datetime(2020, 7, 2, 9, 20, 0)
            ),
        ]
    )

    response = Client().get('/yadm_news/?last_id=10')

    assert response.status_code == 200
    assert_that(json.loads(response.content), has_entries({
        'news': contains(
            {
                'id': 11,
                'geoIds': [111],
                'importance': 1,
                'title': 'title1',
                'text': 'Город1. 1 июля, 8:10. ЯНДЕКС.РАСПИСАНИЯ. text1',
                'code': 'suburban_message'
            },
            {
                'id': 12,
                'geoIds': [112],
                'importance': 0,
                'title': 'title2',
                'text': 'Город2. 2 июля, 9:20. ЯНДЕКС.РАСПИСАНИЯ. text2',
                'code': 'suburban_message'
            },
            {
                'id': 13,
                'geoIds': [111],
                'importance': 2,
                'title': 'title3',
                'text': 'Город1. 3 июля, 10:30. ЯНДЕКС.РАСПИСАНИЯ. text3',
                'code': 'suburban_message'
            },
        )
    }))

    response = Client().get('/yadm_news/?last_id=12')

    assert response.status_code == 200
    assert_that(json.loads(response.content), has_entries({
        'news': contains(
            {
                'id': 13,
                'geoIds': [111],
                'importance': 2,
                'title': 'title3',
                'text': 'Город1. 3 июля, 10:30. ЯНДЕКС.РАСПИСАНИЯ. text3',
                'code': 'suburban_message'
            }
        )
    }))

    response = Client().get('/yadm_news/?last_id=13')

    assert response.status_code == 200
    assert_that(json.loads(response.content), has_entries({
        'news': []
    }))
