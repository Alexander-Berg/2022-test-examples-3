# coding: utf-8

import pytest
import yt.wrapper as yt
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.tables.Partner_pb2 as Partner

from hamcrest import all_of, assert_that, has_entries, has_items, has_length, not_none, is_not
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import ResolvedRedirectTrackerEnv

from market.idx.datacamp.yatf.utils import (
    create_pb_timestamp,
    create_meta
)
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.pylibrary.proto_utils import message_from_data

BUSINESS_FOR_INVISIBLE = 1090
SHOP_FOR_INVISIBLE = 1091
WAREHOUSE_FOR_INVISIBLE = 145


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'yt_home': '//home/datacamp/united',
            },
            'redirect_tracker': {
                'enable': True,
                'yt_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'colors': 'DIRECT_GOODS_ADS,DIRECT_STANDBY,DIRECT_SEARCH_SNIPPET_GALLERY',
                'output_dir': 'redirect_tracker',
                'output_rows_limit': 50,
            }
        }
    )
    return config


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
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
            ).SerializeToString()
        },
        {
            'business_id': 1,
            'shop_sku': 'offer_b',
            'offer': DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    offer_id='offer_b',
                    feed_id=1000
                ),
                content=DTC.OfferContent(
                    partner=DTC.PartnerContent(
                        original=DTC.OriginalSpecification(
                            offer_params=DTC.ProductYmlParams(
                                param=[DTC.OfferYmlParam(name='param1', value='value1')]
                            ),
                            url=DTC.StringValue(value='http://example2.com'),
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


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
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
            'shop_id': 100,
            'business_id': 1,
            'shop_sku': 'offer_b',
            'warehouse_id': 145,
            'outlet_id': 0,
            'identifiers': DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_b',
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


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
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


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        {
            'shop_id': 100,
            'mbi': {
                'shop_id': 100,
                'business_id': 1
            },
            'resolved_redirect_info': Partner.ResolvedRedirectInfo(
                items=[
                    Partner.ResolvedRedirectInfoItem(original_host='http://example.com', target_host='http://redirected_example.com'),
                ]
            ).SerializeToString(),
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


@pytest.yield_fixture(scope='module')
def resolved_redirect_tracker(
        yt_server,
        config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        partners_table
):
    resources= {
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_table': partners_table,
        'config': config
    }
    with ResolvedRedirectTrackerEnv(yt_server, **resources) as routines_env:
        yield routines_env


def test_resolved_redirect_tracker(yt_server, config, resolved_redirect_tracker):
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join(config.redirect_tracker_output_dir, 'recent')))
    assert_that(results, all_of(
        has_length(1),
        has_items(
            has_entries({
                'feed_ts': 200,
                'offer': IsSerializedProtobuf(DTC.Offer, {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'offer_a',
                        'shop_id': 100,
                    },
                    'meta': {
                        'scope': DTC.SERVICE,
                        'redirect_resolved_flag': {
                            'flag': True,
                            'meta': {
                                'timestamp': not_none(),
                            }
                        },
                    },
                }),
            }),
        )
    ))


def test_skip_invisible_offers(yt_server, config, resolved_redirect_tracker, basic_offers_table, service_offers_table, actual_service_offers_table):
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join(config.redirect_tracker_output_dir, 'recent')))
    assert_that(results, is_not(has_items(
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
