# coding: utf-8

import pytest

from hamcrest import assert_that, has_items
from datetime import datetime, timedelta

from market.idx.pylibrary.taxes.taxes import (
    EVat,
    ETaxSystem
)
from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv

from market.proto.common.common_pb2 import PriceExpression
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.utils.utils import create_timestamp_from_json


def make_offer_data(ssku, shop_id, warehouse_id, dimensions, vat, buckets, business_id, classifier_good_id, classifier_magic_id):
    return {
        'identifiers': {
            'business_id': business_id,
            'shop_id': shop_id,
            'offer_id': ssku,
            'warehouse_id': warehouse_id,
            'extra': {
                'classifier_good_id': classifier_good_id,
                'classifier_magic_id2': classifier_magic_id,
                'ware_md5': 'ware_md5',
            }
        },
        'rgb': DTC.BLUE,
        'dimensions': dimensions,
        'vat': vat,
        'price': 100500,
        'buckets': buckets,
    }


def make_offer_key(ssku, shop_id):
    return '{}-{}'.format(shop_id, ssku)


@pytest.fixture(scope="module")
def offers():
    dimensions = {
        'width': 1.0,
        'height': 2.0,
        'length': 3.0,
        'weight': 4.0
    }
    exceeded_dimensions = {
        'width': 50.0,
        'height': 50.0,
        'length': 51.0,
        'weight': 4.0
    }
    exceeded_weight = {
        'width': 1.0,
        'height': 2.0,
        'length': 3.0,
        'weight': 21.0
    }

    buckets = {
        'delivery_bucket_ids': [1],
        'pickup_bucket_ids': [2],
        'post_bucket_ids': [3],
    }

    return {
        make_offer_key(t[0], t[1]): make_offer_data(
            ssku=t[0],
            shop_id=t[1],
            warehouse_id=t[2],
            dimensions=t[3],
            vat=t[4],
            buckets=t[5],
            business_id=t[6],
            classifier_good_id=t[7],
            classifier_magic_id=t[8]
        ) for _, t in enumerate([
            ('ssku.with.sizes', 111, 11100, dimensions, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.with.exceeded.dimensions', 111, 11100, exceeded_dimensions, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.with.exceeded.weight', 111, 11100, exceeded_weight, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.with.sizes.without.buckets', 111, 11100, dimensions, EVat.VAT_20.value, None, 12345, '1', '1'),
            ('ssku.with.sizes.but.no.vat', 111, 11100, dimensions, EVat.UNDEFINED.value, buckets, 12345, '1', '1'),
            ('ssku.with.sizes.without.warehouse', 111, None, dimensions, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.without.sizes.without.warehouse', 111, None, None, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.without.sizes', 111, 11100, None, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.without.sizes.without.buckets', 111, 11100, None, EVat.VAT_20.value, None, 12345, '1', '1'),
            ('ssku.without.sizes.with.ignore.stocks', 222, 22200, None, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.without.sizes.with.ignore.stocks.but.no.vat', 222, 22200, None, EVat.UNDEFINED.value, buckets, 12345, '1', '1'),
            ('ssku.without.sizes.with.ignore.stocks.without.warehouse', 222, None, None, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.with.sizes.and.vat.but.without.tax.system', 333, 33300, dimensions, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.with.sizes.and.vat.but.invalid.tax.system', 444, 44400, dimensions, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.with.crossdock.exceeded.dimensions', 555, 55500, exceeded_dimensions, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.with.crossdock.exceeded.weight', 555, 55500, exceeded_weight, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.with.crossdock.normal.dimensions', 555, 55500, dimensions, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.without.classifier_good_id', 666, 66600, dimensions, EVat.VAT_20.value, buckets, 12345, None, '1'),
            ('ssku.without.classifier_magic_id', 777, 77700, dimensions, EVat.VAT_20.value, buckets, 12345, '1', None),
            ('ssku.with.sizes.and.invalid.vat.for.npd.tax.system', 888, 88800, dimensions, EVat.VAT_20.value, buckets, 12345, '1', '1'),
            ('ssku.with.sizes.and.valid.vat.for.npd.tax.system', 888, 88800, dimensions, EVat.NO_VAT.value, buckets, 12345, '1', '1'),
        ])
    }


def get_offer_identifiers(offers, shop_id, ssku):
    offer_key = make_offer_key(ssku, shop_id)
    if offer_key in offers:
        return offers[offer_key]['identifiers']
    else:
        raise Exception('Offer with key {} not found'.format(offer_key))


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': 11111,
            'mbi': dict2tskv({
                'business_id': 12345,
                'shop_id': 11111,
                'datafeed_id': 22222,
                'united_catalog_status': 'SUCCESS',
                'blue_status': 'REAL',
            }),
            'status': 'publish'
        },
        {
            'shop_id': 111,
            'mbi':  dict2tskv({
                'business_id': 12345,
                'shop_id': 111,
                'datafeed_id': 1110,
                'warehouse_id': 11100,
                'ff_program': 'REAL',
                'ff_virtual_id': 11111,
                'direct_shipping': True,
                'tax_system': ETaxSystem.OSN.value,
                'united_catalog_status': 'SUCCESS',
                'blue_status': 'REAL',
            }),
            'status': 'publish'
        },
        {
            'shop_id': 222,
            'mbi':  dict2tskv({
                'business_id': 12345,
                'shop_id': 222,
                'datafeed_id': 2220,
                'warehouse_id': 22200,
                'ignore_stocks': True,
                'tax_system': ETaxSystem.OSN.value,
                'united_catalog_status': 'SUCCESS',
                'blue_status': 'REAL',
            }),
            'status': 'publish'
        },
        {
            'shop_id': 333,
            'mbi': dict2tskv({
                'business_id': 12345,
                'shop_id': 333,
                'datafeed_id': 3330,
                'warehouse_id': 33300,
                'ff_program': 'REAL',
                'ff_virtual_id': 11111,
                'direct_shipping': True,
                'united_catalog_status': 'SUCCESS',
                'blue_status': 'REAL',
            }),
            'status': 'publish'
        },
        {
            'shop_id': 444,
            'mbi': dict2tskv({
                'business_id': 12345,
                'shop_id': 444,
                'datafeed_id': 4440,
                'warehouse_id': 44400,
                'ff_program': 'REAL',
                'ff_virtual_id': 11111,
                'direct_shipping': True,
                'tax_system': ETaxSystem.USN.value,
                'united_catalog_status': 'SUCCESS',
                'blue_status': 'REAL',
            }),
            'status': 'publish'
        },
        {
            'shop_id': 555,
            'mbi': dict2tskv({
                'business_id': 12345,
                'shop_id': 555,
                'datafeed_id': 5550,
                'warehouse_id': 55500,
                'ff_program': 'REAL',
                'ff_virtual_id': 11111,
                'direct_shipping': False,
                'tax_system': ETaxSystem.OSN.value,
                'united_catalog_status': 'SUCCESS',
                'blue_status': 'REAL',
            }),
            'status': 'publish'
        },
        {
            'shop_id': 666,
            'mbi': dict2tskv({
                'business_id': 12345,
                'shop_id': 666,
                'datafeed_id': 6660,
                'warehouse_id': 66600,
                'ff_program': 'REAL',
                'ff_virtual_id': 11111,
                'direct_shipping': False,
                'tax_system': ETaxSystem.OSN.value,
                'united_catalog_status': 'SUCCESS',
                'blue_status': 'REAL',
            }),
            'status': 'publish'
        },
        {
            'shop_id': 777,
            'mbi': dict2tskv({
                'business_id': 12345,
                'shop_id': 777,
                'datafeed_id': 7770,
                'warehouse_id': 77700,
                'ff_program': 'REAL',
                'ff_virtual_id': 11111,
                'direct_shipping': False,
                'tax_system': ETaxSystem.OSN.value,
                'united_catalog_status': 'SUCCESS',
                'blue_status': 'REAL',
            }),
            'status': 'publish'
        },
        {
            'shop_id': 888,
            'mbi': dict2tskv({
                'business_id': 12345,
                'shop_id': 888,
                'datafeed_id': 8880,
                'warehouse_id': 88800,
                'ff_program': 'REAL',
                'ff_virtual_id': 11111,
                'direct_shipping': True,
                'tax_system': ETaxSystem.NPD.value,
                'united_catalog_status': 'SUCCESS',
                'blue_status': 'REAL',
            }),
            'status': 'publish'
        },
    ]


@pytest.fixture(scope='module')
def miner_config(
        yt_server,
        log_broker_stuff,
        input_topic,
        output_topic,
        yt_token,
        partner_info_table_path,
):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    shopsdat_enricher = cfg.create_shopsdat_enricher()
    offer_validator = cfg.create_offer_validator()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, shopsdat_enricher)
    cfg.create_link(shopsdat_enricher, offer_validator)
    cfg.create_link(offer_validator, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(
        yt_server,
        miner_config,
        input_topic, output_topic,
        partner_info_table_path,
        partner_data
):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=partner_data)
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.yield_fixture(scope='module')
def workflow(miner, input_topic, output_topic, offers):
    old_time = (datetime.utcnow() - timedelta(minutes=45)).strftime("%Y-%m-%dT%H:%M:%SZ")
    for _, offer in list(offers.items()):
        dimensions = None
        weight = None
        if offer['dimensions'] is not None:
            dimensions = DTC.PreciseDimensions(
                length_mkm=int(offer['dimensions']['length']*10000),
                width_mkm=int(offer['dimensions']['width']*10000),
                height_mkm=int(offer['dimensions']['height']*10000),
            )
            weight = DTC.PreciseWeight(
                value_mg=int(offer['dimensions']['weight']*1000000)
            )
        delivery = None
        if offer['buckets'] is not None:
            delivery = DTC.OfferDelivery(
                calculator=DTC.DeliveryCalculatorOptions(
                    delivery_bucket_ids=offer['buckets']['delivery_bucket_ids'],
                    pickup_bucket_ids=offer['buckets']['pickup_bucket_ids'],
                    post_bucket_ids=offer['buckets']['post_bucket_ids'],
                )
            )

        message = DatacampMessage(united_offers=[UnitedOffersBatch(
            offer=[UnitedOffer(
                basic=DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=offer['identifiers']['business_id'],
                        offer_id=offer['identifiers']['offer_id'],
                        extra=DTC.OfferExtraIdentifiers(
                            classifier_good_id=offer['identifiers']['extra']['classifier_good_id'],
                            classifier_magic_id2=offer['identifiers']['extra']['classifier_magic_id2'],
                        ),
                    ),
                    content=DTC.OfferContent(
                        binding=DTC.ContentBinding(
                            approved=DTC.Mapping(
                                market_sku_id=123
                            )
                        ),
                        master_data=DTC.MarketMasterData(
                            dimensions=dimensions,
                            weight_gross=weight,
                        )
                    ),
                ),
                service={offer['identifiers']['shop_id']: DTC.Offer(
                    price=DTC.OfferPrice(
                        basic=DTC.PriceBundle(
                            binary_price=PriceExpression(
                                price=int(offer['price'] * 10**7)
                            ),
                            vat=offer['vat'],
                        )
                    ),
                    identifiers=DTC.OfferIdentifiers(
                        business_id=offer['identifiers']['business_id'],
                        shop_id=offer['identifiers']['shop_id'],
                        offer_id=offer['identifiers']['offer_id'],
                        warehouse_id=offer['identifiers']['warehouse_id'],
                        extra=DTC.OfferExtraIdentifiers(
                            ware_md5=offer['identifiers']['extra']['ware_md5'],
                        ),
                    ),
                    delivery=delivery,
                    status=DTC.OfferStatus(
                        disabled=[
                            DTC.Flag(
                                flag=False,
                                meta=DTC.UpdateMeta(
                                    source=DTC.MARKET_IDX,
                                    timestamp=create_timestamp_from_json(old_time)
                                )
                            )
                        ]
                    ),
                    meta=DTC.OfferMeta(
                        rgb=offer['rgb']
                    ),
                    partner_info=DTC.PartnerInfo(
                        supplier_id=offer['identifiers']['shop_id']
                    )
                )}
            )]
        )])

        input_topic.write(message.SerializeToString())

    yield output_topic.read(count=len(offers))


@pytest.mark.parametrize("shop_id, ssku", [
    (111, 'ssku.with.sizes'),
    (111, 'ssku.with.exceeded.dimensions'),
    (111, 'ssku.with.exceeded.weight'),
    (111, 'ssku.with.sizes.without.buckets'),
    (222, 'ssku.without.sizes.with.ignore.stocks'),
    (555, 'ssku.with.crossdock.normal.dimensions'),
    (888, 'ssku.with.sizes.and.valid.vat.for.npd.tax.system'),
])
def test_valid_offer(workflow, offers, shop_id, ssku):
    """ Проверяем, что MARKET_IDX устанавливаются в false (в disabled и has_gone):
         - для офферов с ВГХ
         - для не кроссдок офферов с ВГХ больше чем лимиты
         - для офферов без ВГХ, но с опцией ignore_stocks в shopsdat для склада, указанного в оффере
    """
    identifiers = get_offer_identifiers(offers, shop_id, ssku)
    assert_that(workflow, has_items(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': identifiers['business_id'],
                        'offer_id': identifiers['offer_id'],
                    },
                },
                'service': IsProtobufMap({
                    identifiers['shop_id']: {
                        'identifiers': {
                            'shop_id': identifiers['shop_id'],
                            'business_id': identifiers['business_id'],
                            'offer_id': identifiers['offer_id'],
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': False,
                                    'meta': {
                                        'source': DTC.MARKET_IDX
                                    }
                                }
                            ],
                            'has_gone': {
                                'flag': False,
                                'meta': {
                                    'source': DTC.MARKET_IDX
                                }
                            },
                        },
                    },
                })
            }]
        }]})))


@pytest.mark.parametrize("shop_id, ssku", [
    (111, 'ssku.with.sizes.but.no.vat'),
    (111, 'ssku.without.sizes'),
    (111, 'ssku.without.sizes.without.buckets'),
    (111, 'ssku.without.sizes.without.warehouse'),
    (111, 'ssku.with.sizes.without.warehouse'),
    (222, 'ssku.without.sizes.with.ignore.stocks.but.no.vat'),
    (222, 'ssku.without.sizes.with.ignore.stocks.without.warehouse'),
    (333, 'ssku.with.sizes.and.vat.but.without.tax.system'),
    (444, 'ssku.with.sizes.and.vat.but.invalid.tax.system'),
    (555, 'ssku.with.crossdock.exceeded.dimensions'),
    (555, 'ssku.with.crossdock.exceeded.weight'),
    (666, 'ssku.without.classifier_good_id'),
    (777, 'ssku.without.classifier_magic_id'),
    (888, 'ssku.with.sizes.and.invalid.vat.for.npd.tax.system'),
])
def test_invalid_offer(workflow, offers, shop_id, ssku):
    """ Проверяем, что MARKET_IDX устанавливаются в true (только в disabled):
         - для офферов с ВГХ, но без vat
         - для офферов без ВГХ
         - для офферов без ВГХ, и у которых нет бакетов
         - для офферов без ВГХ, и у которых не указан склад
         - для офферов с ВГХ, у которых не указан склад
         - для офферов с ВГХ, но у которых нет бакетов
         - для офферов без ВГХ, с опцией ignore_stocks в shopsdat для склада, указанного в оффере, но без vat
         - для офферов без ВГХ, без указания склада, но с опцией ignore_stocks в shopsdat для какого-то склада магазина
         - для офферов с ВГХ и vat, но без tax_system
         - для офферов с ВГХ, и с vat неподходящей tax_system
         - для crossdock офферов с ВГХ больше предельных значений
    """
    identifiers = get_offer_identifiers(offers, shop_id, ssku)
    assert_that(workflow, has_items(IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': identifiers['business_id'],
                        'offer_id': identifiers['offer_id'],
                    },
                },
                'service': IsProtobufMap({
                    identifiers['shop_id']: {
                        'identifiers': {
                            'shop_id': identifiers['shop_id'],
                            'business_id': identifiers['business_id'],
                            'offer_id': identifiers['offer_id'],
                        },
                        'status': {
                            'disabled': [
                                {
                                    'flag': True,
                                    'meta': {
                                        'source': DTC.MARKET_IDX
                                    }
                                }
                            ],
                            'has_gone': None,
                        },
                    },
                })
            }]
        }]})))
