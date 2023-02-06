# coding: utf-8

import pytest
import yt

from hamcrest import assert_that, has_entries, equal_to, has_items, has_key, is_not

from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
import market.idx.datacamp.proto.tables.Partner_pb2 as Partner
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import EcomExportMergedOffersDumperEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.offer.OfferDelivery_pb2 as Delivery
import market.idx.datacamp.proto.offer.PartnerInfo_pb2 as PartnerInfo
from market.idx.datacamp.yatf.utils import create_meta

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampDeliveryBucketsTable,
    DataCampServiceOffersTable,
    DataCampPartnersTable
)
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.utils.utils import create_pb_timestamp

from market.proto.delivery.delivery_calc.delivery_calc_pb2 import DeliveryOptionsGroupRegion
from market.proto.delivery.delivery_yt.indexer_part_pb2 import CommonDeliveryOptionsBucket
from market.pylibrary.proto_utils import message_from_data

OUTPUT_DIR = '//home/market/production/ecom/export/offers/merged'

BUSINESS_FOR_INVISIBLE = 1090
SHOP_FOR_INVISIBLE = 1091
WAREHOUSE_FOR_INVISIBLE = 145

BASIC_OFFERS = [
    {
        'business_id': 1,
        'shop_sku': 'offer_a',
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_a',
                feed_id=1000
            ),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        offer_params=DTC.ProductYmlParams(
                            param=[DTC.OfferYmlParam(name='param1', value='value1')]
                        ),
                        url=DTC.StringValue(value='http://example.com'),
                        vendor=DTC.StringValue(value='datacamp_vendor'),
                        dimensions=DTC.PreciseDimensions(
                            height_mkm=10,
                            width_mkm=100,
                            length_mkm=50,
                        ),
                        weight=DTC.PreciseWeight(
                            value_mg=100000,
                        ),
                        name=DTC.StringValue(value='partner name'),
                        original_name=DTC.StringValue(value='original partner name'),
                        description=DTC.StringValue(value='partner description'),
                    ),
                    actual=DTC.ProcessedSpecification(
                        url=DTC.StringValue(value='https://actual_partner_url.com'),
                        title=DTC.StringValue(value='Actual partner name'),
                    )
                )
            ),
            tech_info=DTC.OfferTechInfo(
                last_parsing=DTC.ParserTrace(
                    feed_timestamp=create_pb_timestamp(300),
                    start_parsing=create_pb_timestamp(100),
                    end_parsing=create_pb_timestamp(300)
                )
            ),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    multi_actual={
                        'https://example.com/url1': DTC.NamespacePictures(
                            by_namespace={
                                'marketpic': DTC.MarketPicture(
                                    namespace='marketpic'
                                )
                            }
                        )
                    }
                )
            ),
            partner_info=PartnerInfo.PartnerInfo(
                cpa=4
            ),
            delivery=Delivery.OfferDelivery(
                calculator=Delivery.DeliveryCalculatorOptions(
                    delivery_bucket_ids=[1235],
                    pickup_bucket_ids=[1337]
                )
            )
        ).SerializeToString()
    },
    # не выгружаем оффер, т.к. нет сервисной части
    {
        'business_id': 1,
        'shop_sku': 'offer_b',
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_b',
                feed_id=1000
            )
        ).SerializeToString()
    },
    {
        'business_id': BUSINESS_FOR_INVISIBLE,
        'shop_sku': 'InvisibleOffer',
        'identifiers': DTC.OfferIdentifiers(
            business_id=BUSINESS_FOR_INVISIBLE,
            offer_id='InvisibleOffer',
            feed_id=1000,
        ).SerializeToString(),
        'tech_info': DTC.OfferTechInfo(
            last_parsing=DTC.ParserTrace(
                feed_timestamp=create_pb_timestamp(300),
                start_parsing=create_pb_timestamp(100),
                end_parsing=create_pb_timestamp(300),
            ),
        ).SerializeToString(),
        'meta': create_meta(10, scope=DTC.BASIC, data_source=DTC.PUSH_PARTNER_FEED).SerializeToString(),
        'status': DTC.OfferStatus(
            invisible=DTC.Flag(
                flag=True,
            ),
        ).SerializeToString(),
    },
]

SERVICE_OFFERS = [
    {
        'shop_id': 100,
        'business_id': 1,
        'shop_sku': 'offer_a',
        'warehouse_id': 145,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=1,
            offer_id='offer_a',
            shop_id=100,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.DIRECT_STANDBY, data_source=DTC.PUSH_PARTNER_FEED).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(flag=True, meta=DTC.UpdateMeta(source=DTC.PUSH_PARTNER_FEED))
            ]
        ).SerializeToString(),
        'tech_info': DTC.OfferTechInfo(
            last_parsing=DTC.ParserTrace(
                feed_timestamp=create_pb_timestamp(200),
                start_parsing=create_pb_timestamp(200),
                end_parsing=create_pb_timestamp(200)
            )
        ).SerializeToString(),
    },
    {
        'shop_id': SHOP_FOR_INVISIBLE,
        'business_id': BUSINESS_FOR_INVISIBLE,
        'shop_sku': 'InvisibleOffer',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=BUSINESS_FOR_INVISIBLE,
            offer_id='InvisibleOffer',
            warehouse_id=0,
            shop_id=SHOP_FOR_INVISIBLE,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, color=DTC.DIRECT_STANDBY, data_source=DTC.PUSH_PARTNER_FEED).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(flag=False, meta=DTC.UpdateMeta(source=DTC.PUSH_PARTNER_FEED))
            ]
        ).SerializeToString(),
        'tech_info': DTC.OfferTechInfo(
            last_parsing=DTC.ParserTrace(
                feed_timestamp=create_pb_timestamp(200),
                start_parsing=create_pb_timestamp(200),
                end_parsing=create_pb_timestamp(200)
            )
        ).SerializeToString(),
    },
]

ACTUAL_SERVICE_OFFERS = [
    {
        'shop_id': 100,
        'business_id': 1,
        'shop_sku': 'offer_a',
        'warehouse_id': 145,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=1,
            offer_id='offer_a',
            warehouse_id=145,
            shop_id=100,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.DIRECT_STANDBY, scope=DTC.SERVICE).SerializeToString()
    },
    {
        'shop_id': 100,
        'business_id': 1,
        'shop_sku': 'offer_b',
        'warehouse_id': 145,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=1,
            offer_id='offer_b',
            warehouse_id=145,
            shop_id=100,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.DIRECT_STANDBY, scope=DTC.SERVICE).SerializeToString()
    },
    {
        'shop_id': SHOP_FOR_INVISIBLE,
        'business_id': BUSINESS_FOR_INVISIBLE,
        'shop_sku': 'InvisibleOffer',
        'warehouse_id': WAREHOUSE_FOR_INVISIBLE,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=BUSINESS_FOR_INVISIBLE,
            offer_id='InvisibleOffer',
            warehouse_id=WAREHOUSE_FOR_INVISIBLE,
            shop_id=SHOP_FOR_INVISIBLE,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.DIRECT_STANDBY, scope=DTC.SERVICE).SerializeToString()
    },
]

PARTNERS = [
    {
        'shop_id': 100,
        'resolved_redirect_info': Partner.ResolvedRedirectInfo(
            items=[
                Partner.ResolvedRedirectInfoItem(original_host='http://example.com', target_host='http://redirected_example.com'),
            ]
        ).SerializeToString()
    },
    {
        'shop_id': SHOP_FOR_INVISIBLE,
        'resolved_redirect_info': Partner.ResolvedRedirectInfo(
            items=[
                Partner.ResolvedRedirectInfoItem(original_host='http://invisible.com', target_host='http://redirected_invisible.com'),
            ]
        ).SerializeToString()
    },
]

BLUE_BUCKETS_DATA = [
    {
        'bucket_id': 666,  # wont appear in the expected result
        'bucket': CommonDeliveryOptionsBucket(
            currency="RUR",
            delivery_opt_bucket_id=555555,
            delivery_option_group_regs=[
                DeliveryOptionsGroupRegion(region=6661),
                DeliveryOptionsGroupRegion(region=6662)
            ]
        ).SerializeToString()
    },
    {
        'bucket_id': 1235,
        'bucket': CommonDeliveryOptionsBucket(
            currency="RUR",
            delivery_opt_bucket_id=10280796,
            delivery_option_group_regs=[
                DeliveryOptionsGroupRegion(region=12353),
                DeliveryOptionsGroupRegion(region=12354)
            ]
        ).SerializeToString()
    },
    {
        'bucket_id': 1337,
        'bucket': CommonDeliveryOptionsBucket(
            currency="RUR",
            delivery_opt_bucket_id=10280796,
            delivery_option_group_regs=[
                DeliveryOptionsGroupRegion(region=13375),
                DeliveryOptionsGroupRegion(region=13376),
                DeliveryOptionsGroupRegion(region=13377),
            ]
        ).SerializeToString()
    },
]


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'color': 'white',
                'yt_home': '//home/datacamp/united'
            },
            'yt': {
                'turbo_out': 'turbo_out',
                'direct_out': 'direct_out',
            },
            'ecom_export': {
                'enable': True,
                'yt_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'merged_offers_dir': OUTPUT_DIR,
                'colors': 'DIRECT_SITE_PREVIEW,DIRECT_STANDBY,DIRECT_GOODS_ADS',
                'default_blue_buckets_path': '//home/indexer/blue_generations/recent/buckets',
                'enable_fill_regions_from_dcmp': True,
                'dump_consistent_snapshot': True,
                'consistent_snapshot_max_attempts': 10,
            }
        })
    return config


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    data = BASIC_OFFERS
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=data)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    data = SERVICE_OFFERS
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=data)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    data = ACTUAL_SERVICE_OFFERS
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=data)


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    data = PARTNERS
    return DataCampPartnersTable(yt_server, config.yt_partners_tablepath, data=data)


@pytest.fixture(scope='module')
def blue_buckets_table(yt_server, config):
    tablepath = '//home/indexer/blue_generations/recent/buckets'
    table = DataCampDeliveryBucketsTable(yt_server, tablepath, data=BLUE_BUCKETS_DATA)
    return table


@pytest.yield_fixture(scope='module')
def ecom_export(
        yt_server,
        config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        partners_table,
        blue_buckets_table
):
    resources = {
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_table': partners_table,
        'config': config,
        'blue_buckets_table': blue_buckets_table
    }
    with EcomExportMergedOffersDumperEnv(yt_server, **resources) as routines_env:
        yield routines_env


def test_ecom_export(ecom_export, yt_server):
    yt_client = yt_server.get_yt_client()

    records = list(yt_client.read_table(yt.wrapper.ypath_join(OUTPUT_DIR, 'recent')))

    assert_that(len(records), equal_to(1))
    assert_that(records, has_items(
        has_entries({
            'BusinessId': 1,
            'ShopId': 100,
            'OfferYabsId': 0,
            'WarehouseId': 145,
            'OfferId': 'offer_a',
            'MarketFeedId': 1000,
            'Data': IsSerializedProtobuf(ExportMessage, {
                'offer': {
                    'business_id': 1,
                    'feed_id': 1000,
                    'offer_id': 'offer_a',
                    'shop_id': 100,
                    'disable_status': has_key(DTC.PUSH_PARTNER_FEED),
                    'original_content': {
                        'params': [{
                            'name': 'param1',
                            'value': 'value1'
                        }],
                        'url': 'http://example.com',
                        'shop_vendor': 'datacamp_vendor',
                        'brutto_dimensions': {
                            'height_mkm': 10,
                            'width_mkm': 100,
                            'length_mkm': 50
                        },
                        'brutto_weight_in_grams': 100,
                        'name': 'original partner name',
                        'description': 'partner description',
                    },
                    'actual_content': {
                        'url': 'https://actual_partner_url.com',
                        'name': 'Actual partner name',
                    },
                    'redirect': {
                        'redirected_host_from_partners': 'http://redirected_example.com'
                    },
                    'service': {
                        'data_source': DTC.PUSH_PARTNER_FEED,
                    },
                    'tech_info': {
                        'last_parsing': {
                            'feed_timestamp': {
                                'seconds': 300
                            },
                            'start_parsing': {
                                'seconds': 200
                            },
                            'end_parsing': {
                                'seconds': 300
                            }
                        }
                    },
                    'multi_actual_pictures': has_key('https://example.com/url1'),
                    'original_regions_info': {
                        'regions': [12353, 12354],
                        'priority_regions': [],
                        'pickup_regions': [13375, 13376, 13377]
                    }
                }
            }),
        })
    ))


def test_skip_invisible_offers(ecom_export, yt_server, basic_offers_table, service_offers_table, actual_service_offers_table):
    yt_client = yt_server.get_yt_client()
    records = list(yt_client.read_table(yt.wrapper.ypath_join(OUTPUT_DIR, 'recent')))
    assert_that(records, is_not(has_items(
        has_entries({
            'BusinessId': BUSINESS_FOR_INVISIBLE,
            'ShopId': SHOP_FOR_INVISIBLE,
            'OfferId': 'InvisibleOffer',
        })
    )))

    basic_offers_table.load()
    service_offers_table.load()
    actual_service_offers_table.load()
    assert_that(basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_FOR_INVISIBLE,
                'offer_id': 'InvisibleOffer',
            },
            'status': {
                'invisible': {
                    'flag': True,
                },
            },
        }, DTC.Offer())
    ]))
    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_FOR_INVISIBLE,
                'offer_id': 'InvisibleOffer',
                'shop_id': SHOP_FOR_INVISIBLE,
            },
        }, DTC.Offer())
    ]))
    assert_that(actual_service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': BUSINESS_FOR_INVISIBLE,
                'offer_id': 'InvisibleOffer',
                'shop_id': SHOP_FOR_INVISIBLE,
            },
        }, DTC.Offer())
    ]))
