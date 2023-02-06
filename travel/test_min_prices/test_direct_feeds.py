# -*- coding: utf-8 -*-
import mock
import pytest
from lxml import etree

from common.models.geo import Country, Region
from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_station, create_country, create_region, create_thread
from route_search.models import ZNodeRoute2
from travel.rasp.tasks.min_prices import direct_feeds
from travel.rasp.tasks.min_prices.direct_feeds import make_station_settlement_map, TrainMinPriceCache, generate_xml, get_routes

pytestmark = [pytest.mark.dbuser]


def test_make_station_settlement_map():
    country = create_country()
    region = create_region()
    settlement1 = create_settlement(country=country, region=region)
    settlement2 = create_settlement(country=country, region=region)
    station11 = create_station(country=country, region=region, settlement=settlement1,
                               t_type=TransportType.TRAIN_ID, majority=u'main_in_city')
    station12 = create_station(country=country, region=region, settlement=settlement1,
                               t_type=TransportType.TRAIN_ID, majority=u'main_in_city')
    station21 = create_station(country=country, region=region, settlement=settlement2,
                               t_type=TransportType.TRAIN_ID, majority=u'main_in_city')
    map = make_station_settlement_map()
    assert map[str(station11.id)] == settlement1.id
    assert map[str(station12.id)] == settlement1.id
    assert map[str(station21.id)] == settlement2.id


def test_min_price_cache():
    settlement1 = create_settlement()
    settlement2 = create_settlement()
    settlement3 = create_settlement()
    min_price_cache = TrainMinPriceCache(2, 'table_path')
    with mock.patch.object(direct_feeds.yt, 'wrapper') as m_yt:
        m_yt.read_table.return_value = [
            {
                'settlement_from_id': str(settlement1.id),
                'settlement_to_id': str(settlement2.id),
                'price': 511.1,
                'date_forward': '2018-11-09',
            },
            {
                'settlement_from_id': str(settlement1.id),
                'settlement_to_id': str(settlement2.id),
                'price': 522.2,
                'date_forward': '2018-11-10',
            },
            {
                'settlement_from_id': str(settlement2.id),
                'settlement_to_id': str(settlement1.id),
                'price': 2500.2,
                'date_forward': '2018-11-10',
            },
        ]
        min_price_cache.init_prices()
    assert min_price_cache._min_price_cache
    assert min_price_cache.get_price_by_settlements(settlement1, settlement2) == (511.1, True)
    assert min_price_cache.get_price_by_settlements(settlement2, settlement1) == (2500.2, False)
    assert min_price_cache.get_price_by_settlements(settlement3, settlement2) == (0, False)


def test_get_routes():
    region1 = create_region(country=Country.RUSSIA_ID)
    region_klg = create_region(id=Region.KALININGRAD_REGION_ID, country=Country.RUSSIA_ID)
    uganda = create_country()
    region_uganda = create_region(country=uganda)
    settlement1 = create_settlement(country=Country.RUSSIA_ID, region=region1)
    settlement2 = create_settlement(country=Country.RUSSIA_ID, region=region1)
    settlement_klg = create_settlement(country=Country.RUSSIA_ID, region=region_klg)
    settlement_uganda = create_settlement(country=uganda, region=region_uganda)
    station1 = create_station(settlement=settlement1, t_type=TransportType.TRAIN_ID)
    station2 = create_station(settlement=settlement2, t_type=TransportType.TRAIN_ID)
    station_klg = create_station(settlement=settlement_klg, t_type=TransportType.TRAIN_ID)
    station_uganda = create_station(settlement=settlement_uganda, t_type=TransportType.TRAIN_ID)
    thread = create_thread(t_type=TransportType.TRAIN_ID)
    ZNodeRoute2(
        route_id=thread.route_id,
        thread=thread,
        t_type_id=thread.t_type_id,
        settlement_from=settlement1,
        station_from=station1,
        settlement_to=settlement2,
        station_to=station2,
    ).save()
    ZNodeRoute2(
        route_id=thread.route_id,
        thread=thread,
        t_type_id=thread.t_type_id,
        settlement_from=settlement1,
        station_from=station1,
        settlement_to=settlement_klg,
        station_to=station_klg,
    ).save()
    ZNodeRoute2(
        route_id=thread.route_id,
        thread=thread,
        t_type_id=thread.t_type_id,
        settlement_from=settlement2,
        station_from=station2,
        settlement_to=settlement_uganda,
        station_to=station_uganda,
    ).save()
    routes = list(get_routes())
    assert len(routes) == 1
    assert routes[0] == (settlement1, settlement2)


def test_generate_xml():
    region1 = create_region(country=Country.RUSSIA_ID)
    settlement1 = create_settlement(country=Country.RUSSIA_ID, region=region1, title='NormalTitle')
    settlement2 = create_settlement(country=Country.RUSSIA_ID, region=region1, title='SmlTle')
    settlement3 = create_settlement(country=Country.RUSSIA_ID, region=region1, title='Big-big Title')
    settlement4 = create_settlement(country=Country.RUSSIA_ID, region=region1,
                                    title='Very Big Title We Must Ignore This')
    min_price_cache = TrainMinPriceCache(2, 'table_path')
    min_price_cache._min_price_cache = {
        (str(settlement1.id), str(settlement2.id)): TrainMinPriceCache.TrainMinPrice(dates={'1', '2'}, min_price=500.1),
        (str(settlement2.id), str(settlement1.id)): TrainMinPriceCache.TrainMinPrice(dates={'1'}, min_price=600.2),
    }
    with mock.patch.object(direct_feeds, 'get_routes') as m_get_routes:
        m_get_routes.return_value = [
            (settlement1, settlement2),
            (settlement2, settlement1),
            (settlement1, settlement3),
            (settlement3, settlement1),
            (settlement4, settlement1),
        ]
        xml_str = generate_xml(min_price_cache)
    assert xml_str.startswith("<?xml version='1.0' encoding='UTF-8'?>")
    root = etree.XML(xml_str)

    def get_offer_value(c1, c2, element):
        return root.xpath('/yml_catalog/shop/offers/offer[@id="{}-{}"]/{}'.format(c1.point_key, c2.point_key, element))

    def check_offer_name(c1, c2):
        name = get_offer_value(c1, c2, 'name')[0].text
        assert c1.title in name
        assert c2.title in name

    assert get_offer_value(settlement1, settlement2, 'price')[0].text == '500.1'
    assert get_offer_value(settlement1, settlement2, 'categoryId')[0].text == direct_feeds.EVERYDAY_CATEGORY.id
    check_offer_name(settlement1, settlement2)
    assert get_offer_value(settlement1, settlement2, '@available')[0] == 'true'
    assert get_offer_value(settlement2, settlement1, 'price')[0].text == '600.2'
    assert get_offer_value(settlement2, settlement1, 'categoryId')[0].text == direct_feeds.NOT_EVERYDAY_CATEGORY.id
    assert get_offer_value(settlement2, settlement1, '@available')[0] == 'true'
    check_offer_name(settlement2, settlement1)
    assert get_offer_value(settlement1, settlement3, 'price')[0].text == '0.0'
    assert get_offer_value(settlement1, settlement3, '@available')[0] == 'false'
    check_offer_name(settlement1, settlement3)
    assert get_offer_value(settlement3, settlement1, 'price')[0].text == '0.0'
    assert get_offer_value(settlement3, settlement1, '@available')[0] == 'false'
    check_offer_name(settlement3, settlement1)
    assert not get_offer_value(settlement4, settlement1, '@id')
