#!/usr/bin/env python
# coding: utf-8
import pytest
import os
import xml.etree.ElementTree as ET
from hamcrest import assert_that

import yatest.common

from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv

from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
    default_shops_dat,
    delivery_offset_dict,
    get_binary_ware_md5
)
from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasSkuRecord
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable

from market.idx.yatf.resources.shops_outlet import ShopsOutlet
from market.idx.offers.yatf.resources.offers_indexer.delivery_service_flags import DeliveryServiceFlags
from market.idx.offers.yatf.resources.offers_indexer.categories_aviability import CategoriesAviability
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


def offers_available_on_store():
    offer1 = default_genlog(     # Good offer 1
        ware_md5='t+HYw9KglGtvW5itMzVbdA',
        binary_ware_md5=get_binary_ware_md5('t+HYw9KglGtvW5itMzVbdA'),
        original_sku='SOMESKUFROMUCBZZ13',
        shop_sku='vgvjc49coiv1mc5vb21d',
        fulfillment_shop_id=5,  # Does not exist in shop_outlets
        category_id=91491,
        shop_id=1,
        delivery_offset=delivery_offset_dict(days_from=1, days_to=1)
    )

    offer2 = default_genlog(     # Good offer 2
        ware_md5='09lEaAKkQll1XTaaaaaaaQ',
        binary_ware_md5=get_binary_ware_md5('09lEaAKkQll1XTaaaaaaaQ'),
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
        binary_ware_md5=get_binary_ware_md5('kP3oC5KjARGI5f9EEkNGtA'),
        original_sku='SOMESKUFROMUCBZZ15',
        shop_sku='shop_sku_without_stock',
        fulfillment_shop_id=5,
        shop_id=1,
        url='http://candidate.url/',
        delivery_offset=delivery_offset_dict(days_from=1, days_to=3)
    )
    offer4 = default_genlog(     # Offer without shop_sku
        ware_md5='zP3oN5KjARGI5f9EEkNGtA',
        binary_ware_md5=get_binary_ware_md5('zP3oN5KjARGI5f9EEkNGtA'),
        shop_id=1,
        delivery_offset=delivery_offset_dict(days_from=1, days_to=4)
    )
    offer5 = default_genlog(     # Offer without fulfillment_shop_id
        ware_md5='KklEaAKkQll1XTaaabbbbQ',
        binary_ware_md5=get_binary_ware_md5('KklEaAKkQll1XTaaabbbbQ'),
        original_sku='SOMESKUFROMUCBZZ14',
        shop_sku='mqct01s3b3fvsv89eipk',
        shop_id=1,
        delivery_offset=delivery_offset_dict(days_from=1, days_to=5)
    )
    offer6 = default_genlog(     # Offer with exists shop_sku, but from different shop_id
        ware_md5='s+HYw9KglGtvW5itMzVbdA',
        binary_ware_md5=get_binary_ware_md5('s+HYw9KglGtvW5itMzVbdA'),
        original_sku='SOMESKUFROMUCBZZ13',
        shop_sku='vgvjc49coiv1mc5vb21d',
        fulfillment_shop_id=5,
        category_id=91491,
        shop_id=10,
        delivery_offset=delivery_offset_dict(days_from=1, days_to=6)
    )

    return [offer3, offer4, offer5, offer6]


def offers_for_ff_light():
    offer = default_genlog(
        ware_md5='mdf5C5KjARGI5f9EEkN65w',
        binary_ware_md5=get_binary_ware_md5('mdf5C5KjARGI5f9EEkN65w'),
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


@pytest.yield_fixture(scope="module")
def offers_processor_workflow(yt_server, shop_outlets, delivery_service_flags_json, categories_aviability, custom_shops_dat, genlog_table):
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
        env.verify()
        yield env


def offer_to_record(offer_dict):
    fields = [
        "shop_id",
        "is_blue_offer",
        "delivery_bucket_ids",
        "pickup_bucket_ids",
        "post_bucket_ids",
        "fulfillment_shop_id",
        "flags",
        "contex_info",
        "market_sku",
        "shop_sku",
        "delivery_offset",
        "fulfillment_flags",
        "cpa",

        "binary_ware_md5",
    ]

    record_dict = {}
    for field in fields:
        record_dict[field] = offer_dict.get(field)
    return make_gl_record(**record_dict)


@pytest.yield_fixture(scope="module")
def genlog_dumper(offers_processor_workflow):
    records = []
    for offer in offers_processor_workflow.genlog_dicts:
        records.append(offer_to_record(offer))

    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'REGIONAL_DELIVERY',
            '--dumper', 'WARE_MD5'
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(records)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def ordered_offers(genlog_dumper, genlog_rows):
    offset_by_md5 = {}

    for i, _ in enumerate(genlog_rows):
        offset_by_md5[genlog_dumper.ware_md5.get_ware_md5(i)] = i

    return sorted(genlog_rows, key=lambda offer : offset_by_md5[offer['ware_md5']])


def test_filtering_by_availability(genlog_dumper, ordered_offers):
    expected = []

    for offset, offer in enumerate(ordered_offers):
        if 'shop_sku' not in offer:
            continue

        delivery_offset_str = "0-0"
        if 'delivery_offset' in offer:
            delivery_offset = offer['delivery_offset']
            delivery_offset_str = str(delivery_offset.get('days_from')) + "-" + str(delivery_offset.get('days_to'))

        expected.append([
            ('OfferOffset', str(offset)),
            ('SKU', ''),
            ('ShopSKU', offer['shop_sku']),
            ('RefShopId', '0'),
            ('CalendarId', '4294967295'),
            ('Buckets', ''),
            ("DeliveryServiceFlags (2 ^ (bit's index))", ''),
            ("OfferFlags ([bit's index]bit's value)", '[0]0 [3]0 [7]0 [15]0 [24]0 [26]0'),
            ('Cpa', '0'),
            ('DeliveryOffset (from-to)', delivery_offset_str),
        ])

    assert_that(genlog_dumper,
                HasSkuRecord('4', expected),
                u'offer_sku.mmap contains expected document')
