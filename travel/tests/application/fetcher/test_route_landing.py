# -*- encoding: utf-8 -*-
import mock
import pytest

from travel.proto.dicts.rasp.settlement_pb2 import TSettlement

from travel.avia.api_gateway.application.cache.cache_root import CacheRoot
from travel.avia.api_gateway.application.fetcher.route_landing.mapper import RouteLandingMapper, ERouteBlockType
from travel.avia.api_gateway.lib.landings.templater import LandingTemplater


def test_hotel_crosslinks_block():
    cache_root = mock.Mock(CacheRoot)
    landing_templator = mock.Mock(LandingTemplater)
    landing_templator.render = mock.Mock(return_value='Title')
    mapper = RouteLandingMapper(cache_root, landing_templator)
    to_settlement = TSettlement(Id=1, Slug='slug')
    hotel_crosslinks = {
        'hasData': True,
        'key': 'value',
    }

    result = mapper._hotel_crosslinks_block(hotel_crosslinks, to_settlement)

    assert result == {
        'type': ERouteBlockType.HOTELS_CROSS_SALE,
        'data': {
            'hasData': True,
            'title': 'Title',
            'key': 'value',
        }
    }


@pytest.mark.parametrize(
    'hotel_crosslinks',
    (
        None,
        {},
        {'hasData': False},
    ),
)
def test_hotel_crosslinks_block_empty(hotel_crosslinks):
    cache_root = mock.Mock(CacheRoot)
    landing_templator = mock.Mock(LandingTemplater)
    mapper = RouteLandingMapper(cache_root, landing_templator)
    to_settlement = TSettlement(Id=1, Slug='slug')

    result = mapper._hotel_crosslinks_block(hotel_crosslinks, to_settlement)

    assert result is None
