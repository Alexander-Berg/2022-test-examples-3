#!/usr/bin/env python
# coding: utf-8
import pytest
import os
import xml.etree.ElementTree as ET

import yatest.common

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.offers.yatf.utils.fixtures import default_genlog, default_shops_dat, default_blue_genlog, delivery_offset_dict
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord

from market.idx.yatf.resources.shops_outlet import ShopsOutlet
from market.idx.offers.yatf.resources.offers_indexer.delivery_service_flags import DeliveryServiceFlags
from market.idx.offers.yatf.resources.offers_indexer.categories_aviability import CategoriesAviability
from market.idx.yatf.resources.shops_dat import ShopsDat

from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

from hamcrest import assert_that


def offers_available_on_store():
    offer1 = default_genlog(     # Good offer 1
        ware_md5='t+HYw9KglGtvW5itMzVbdA',
        original_sku='SOMESKUFROMUCBZZ13',
        shop_sku='vgvjc49coiv1mc5vb21d',
        fulfillment_shop_id=5,  # Does not exist in shop_outlets
        category_id=91491,
        shop_id=1,
        delivery_offset=delivery_offset_dict(days_from=1, days_to=1),
        flags=OfferFlags.PICKUP | OfferFlags.STORE,
    )

    offer2 = default_genlog(     # Good offer 2
        ware_md5='09lEaAKkQll1XTaaaaaaaQ',
        original_sku='SOMESKUFROMUCBZZ14',
        shop_sku='mqct01s3b3fvsv89eipk',
        fulfillment_shop_id=7,  # Exists in shop_outlets
        category_id=91491,
        shop_id=1,
        url='http://good.url/',
        DeliveryCalcGeneration=1000,
        delivery_offset=delivery_offset_dict(days_from=1, days_to=2),
        flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE | OfferFlags.IS_FULFILLMENT,
    )
    return [offer1, offer2]


def offers_not_available_on_store():
    offer3 = default_genlog(     # Offer with shop_sku which doesn't exists in sku-availability.pbuf.sn
        ware_md5='kP3oC5KjARGI5f9EEkNGtA',
        original_sku='SOMESKUFROMUCBZZ15',
        shop_sku='shop_sku_without_stock',
        fulfillment_shop_id=5,
        shop_id=1,
        url='http://candidate.url/',
        delivery_offset=delivery_offset_dict(days_from=1, days_to=3),
        flags=OfferFlags.PICKUP | OfferFlags.STORE,
    )
    offer4 = default_genlog(     # Offer without shop_sku
        ware_md5='zP3oN5KjARGI5f9EEkNGtA',
        shop_id=1,
        delivery_offset=delivery_offset_dict(days_from=1, days_to=4),
        flags=OfferFlags.PICKUP | OfferFlags.STORE,
    )
    offer5 = default_genlog(     # Offer without fulfillment_shop_id
        ware_md5='KklEaAKkQll1XTaaabbbbQ',
        original_sku='SOMESKUFROMUCBZZ14',
        shop_sku='mqct01s3b3fvsv89eipk',
        shop_id=1,
        delivery_offset=delivery_offset_dict(days_from=1, days_to=5),
        flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.IS_FULFILLMENT,
    )
    offer6 = default_genlog(     # Offer with exists shop_sku, but from different shop_id
        ware_md5='s+HYw9KglGtvW5itMzVbdA',
        original_sku='SOMESKUFROMUCBZZ13',
        shop_sku='vgvjc49coiv1mc5vb21d',
        fulfillment_shop_id=5,
        category_id=91491,
        shop_id=10,
        delivery_offset=delivery_offset_dict(days_from=1, days_to=6),
        flags=OfferFlags.PICKUP | OfferFlags.STORE,
    )

    return [offer3, offer4, offer5, offer6]


def offers_for_ff_light():
    offer = default_genlog(
        ware_md5='mdf5C5KjARGI5f9EEkN65w',
        shop_sku='ff_light_sku',
    )
    return [offer]


@pytest.fixture(scope="module")
def genlog_rows():
    return (
        offers_available_on_store() +
        offers_not_available_on_store() +
        offers_for_ff_light()
    )


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


def generate_outlet(outlet_id, outlet_type):
    telephone = ET.Element('Telephone')
    ET.SubElement(telephone, 'CountryCode').text = '2353'
    ET.SubElement(telephone, 'CityCode').text = '32131231'
    ET.SubElement(telephone, 'TelephoneNumber').text = '132-321312312312'
    ET.SubElement(telephone, 'TelephoneType').text = 'PHONE'

    worktime = ET.Element('WorkingTime')
    ET.SubElement(worktime, 'WorkingDaysFrom').text = '1'
    ET.SubElement(worktime, 'WorkingDaysTill').text = '7'
    ET.SubElement(worktime, 'WorkingHoursFrom').text = '8:00'
    ET.SubElement(worktime, 'WorkingHoursTill').text = '18:00'

    outlet = ET.Element('outlet')
    ET.SubElement(outlet, 'DeliveryServicePointId').text = 'SPB-{}'.format(outlet_id)
    ET.SubElement(outlet, 'PointId').text = str(outlet_id)
    ET.SubElement(outlet, 'ShopPointId').text = str(outlet_id)
    ET.SubElement(outlet, 'PointName').text = u'Точка {} {}'.format(outlet_id, outlet_type)
    ET.SubElement(outlet, 'Type').text = outlet_type
    ET.SubElement(outlet, 'LocalityName').text = u'Санкт-Петербург'
    ET.SubElement(outlet, 'ThoroughfareName').text = u'Невский проспект'
    ET.SubElement(outlet, 'PremiseNumber').text = '68'
    ET.SubElement(outlet, 'GpsCoord').text = '30.345045,59.933347'
    ET.SubElement(outlet, 'RegionId').text = '213'
    outlet.append(telephone)
    outlet.append(worktime)

    return outlet


@pytest.fixture(scope="module")
def shop_outlets():
    outlet_info = ET.Element('OutletInfo')
    delivery_services = ET.SubElement(outlet_info, 'delivery-services')

    delivery_service_103 = ET.Element('delivery-service', attrib={'id': '103'})
    delivery_service_103.append(generate_outlet(123, 'depot'))
    delivery_services.append(delivery_service_103)

    delivery_service_104 = ET.Element('delivery-service', attrib={'id': '104'})
    delivery_service_104.append(generate_outlet(120, 'post_term'))
    delivery_services.append(delivery_service_104)

    shops = ET.SubElement(outlet_info, 'shops')

    shop = ET.Element('Shop', attrib={'id': '7'})
    shop.append(ET.Element('shop-delivery-service', attrib={'id': '103'}))
    shop.append(ET.Element('shop-delivery-service', attrib={'id': '104'}))
    shop.append(generate_outlet(130, 'depot'))
    shops.append(shop)

    return ShopsOutlet(outlet_info)


@pytest.fixture(scope="module")
def delivery_service_flags_json(shop_outlets):
    shop_outlets.write(yatest.common.build_path())
    assert os.path.exists(shop_outlets.path)
    return DeliveryServiceFlags(shop_outlets.path)


@pytest.fixture(scope="module")
def categories_aviability():
    categories = CategoriesAviability()
    categories.add(103, 91491, True)
    categories.add(104, 91491, True)
    return categories


@pytest.fixture(scope="module")
def custom_shops_dat():
    ff_shop = default_shops_dat()
    ff_shop['name'] = 'StandartShop'
    ff_shop['shop_id'] = 1
    ff_shop['ff_feed_id'] = 1
    ff_shop['ff_shop_id'] = 7
    ff_shop['ff_program'] = 'REAL'

    ff_virtual_shop = default_shops_dat()
    ff_virtual_shop['name'] = 'VirtualFulfillmentShop'
    ff_virtual_shop['shop_id'] = 7
    ff_virtual_shop['datafeed_id'] = 1
    ff_virtual_shop['priority_regions'] = '19328'
    ff_virtual_shop['ff_virtual'] = True
    ff_virtual_shop['domain'] = 'test.virtual-domain.ru'

    return ShopsDat(shops=[ff_virtual_shop, ff_shop])


def common_workflow(yt_server, genlog_table, shop_outlets, delivery_service_flags_json, categories_aviability, custom_shops_dat):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'shops_outlet_v5_mmap': shop_outlets,
        'delivery_service_flags_json': delivery_service_flags_json,
        'categories_availability_tsv': categories_aviability,
        'shops_dat': custom_shops_dat,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        return env


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, shop_outlets, delivery_service_flags_json, categories_aviability, custom_shops_dat):
    yield common_workflow(yt_server, genlog_table, shop_outlets, delivery_service_flags_json, categories_aviability, custom_shops_dat)


def test_fulfillment_regions_redefinition(workflow):
    """
    Этот тест проверяет, что для фулфилментовского офера
    в генлог попали регионы виртуального магазина
    """
    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'url': 'http://good.url/',
                'shop_sku': 'mqct01s3b3fvsv89eipk',
                'int_geo_regions': [],
                'priority_regions': '',
                'int_regions': [1],
            }
        )
    )


@pytest.fixture(scope="module")
def blue_genlog_rows():
    blue_old_fulfillment_offer = default_blue_genlog(
        ware_md5='09lEaAKkQll1XTaaaaaaaQ',
        original_sku='SOMESKUFROMUCBZZ14',
        shop_sku='mqct01s3b3fvsv89eipk',
        fulfillment_shop_id=7,  # Exists in shop_outlets
        category_id=91491,
        shop_id=1,
        supplier_id=2,
        shop_name='shop_name',
        supplier_name='supplierName',
        # https://a.yandex-team.ru/arc/commit/r9032170#file-/trunk/arcadia/market/idx/offers/lib/loaders/load_biz_logic.cpp:R713
        url='https://market.yandex.ru/product/888?offerid=09lEaAKkQll1XTaaaaaaaQ&sku=777',
        DeliveryCalcGeneration=1000,
        market_sku=777,
        model_id=888,
        flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.IS_FULFILLMENT
    )

    msku = default_blue_genlog(
        ware_md5='t+HYw9KglGtvW5itMzVbdA',
        original_sku='SOMESKUFROMUCBZZ13',
        shop_sku='vgvjc49coiv1mc5vb21d',
        fulfillment_shop_id=5,  # Does not exist in shop_outlets
        category_id=91491,
        shop_id=1,
        is_fake_msku_offer=True,
        market_sku=777,
        model_id=888,
        flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.IS_FULFILLMENT
    )

    blue_offer = default_blue_genlog(
        ware_md5='411792e0f02e2ad1f36f09',
        original_sku='SOMESKUFROMUCBZZ15',
        shop_sku='blue_offer_SKU',
        fulfillment_shop_id=4,
        category_id=91491,
        shop_id=1,
        market_sku=777,
        model_id=888,
    )
    blue_offer['flags'] = OfferFlags.AVAILABLE | OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.IS_FULFILLMENT

    premium_partner_offer = default_blue_genlog(
        ware_md5='f532c61b8aba2e26217438',
        original_sku='SOMESKUFROMUCBZZ15',
        shop_sku='premium_offer_SKU',
        fulfillment_shop_id=7,
        supplier_name='premiumPartner',
        url='http://golden.url/',
        category_id=91491,
        shop_id=1,
        market_sku=777,
        model_id=888,
        flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE
    )

    return [blue_old_fulfillment_offer, msku, blue_offer, premium_partner_offer]


@pytest.fixture(scope="module")
def blue_genlog_table(yt_server, blue_genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), blue_genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def blue_workflow(yt_server, blue_genlog_table, shop_outlets, delivery_service_flags_json, categories_aviability, custom_shops_dat):
    yield common_workflow(yt_server, blue_genlog_table, shop_outlets, delivery_service_flags_json, categories_aviability, custom_shops_dat)


def test_blue_offer_in_genlog(blue_workflow):
    """
    Этот тест проверяет, что для синего офера, который есть в файле наличий, в генлог попадает запись
    """
    #   Флаг HAS_GONE выставляется, т.к. этот вариант fulfillment считаем устаревшим
    expected_flags = OfferFlags.DEPOT
    expected_flags |= OfferFlags.AVAILABLE
    expected_flags |= OfferFlags.STORE
    expected_flags |= OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.CPC
    expected_flags |= OfferFlags.IS_FULFILLMENT
    expected_flags |= OfferFlags.BLUE_OFFER

    assert_that(
        blue_workflow,
        HasGenlogRecord(
            {
                'url': 'https://market.yandex.ru/product/888?offerid=09lEaAKkQll1XTaaaaaaaQ&sku=777',
                'shop_sku': "mqct01s3b3fvsv89eipk",
                'is_blue_offer': True,       # офер помечен синим
                'flags': expected_flags,     # и в флагах офера есть нужные
                'market_sku': 777,
                'supplier_id': 2,            # оригинальный идентификатор поставщика
                'supplier_name': 'supplierName',  # имя поставщика
            }
        )
    )


def test_msku_is_in_genlog(blue_workflow):
    """
    Этот тест проверяет, что фэйковые МСКУ-офера попадают в генлоги
    """

    assert_that(
        blue_workflow,
        HasGenlogRecord(
            {
                'shop_sku': 'vgvjc49coiv1mc5vb21d',
            }
        )
    )


def test_fulfillment_flag_fake_msku(blue_workflow):
    """
    Проверка того, что для MSKU синего маркета IS_FULFILLMENT не заполняется (он не актуален для них)
    """
    expected_flags = OfferFlags.PICKUP
    expected_flags |= OfferFlags.STORE
    expected_flags |= OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.CPC
    expected_flags |= OfferFlags.MARKET_SKU

    assert_that(blue_workflow,
                HasGenlogRecord({'shop_sku': 'vgvjc49coiv1mc5vb21d', 'flags': expected_flags}))


def test_fulfillment_flag_blue_offer(blue_workflow):
    """
    Для обычных синих офферов IS_FULFILLMENT = True, т.е. мы сами исполняем заказ
    """
    expected_flags = OfferFlags.PICKUP
    expected_flags |= OfferFlags.AVAILABLE
    expected_flags |= OfferFlags.STORE
    expected_flags |= OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.IS_FULFILLMENT
    expected_flags |= OfferFlags.CPC
    expected_flags |= OfferFlags.BLUE_OFFER

    assert_that(blue_workflow,
                HasGenlogRecord({'shop_sku': 'blue_offer_SKU', 'flags': expected_flags}))


def test_fulfillment_flag_premium_blue_offer(blue_workflow):
    """
    Для синих офферов от "золотых" партнёров IS_FULFILLMENT == False, исполняют заказ они
    """
    expected_flags = OfferFlags.PICKUP
    expected_flags |= OfferFlags.AVAILABLE
    expected_flags |= OfferFlags.STORE
    expected_flags |= OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.CPC
    expected_flags |= OfferFlags.BLUE_OFFER

    assert_that(blue_workflow,
                HasGenlogRecord({'shop_sku': 'premium_offer_SKU', 'flags': expected_flags}))
