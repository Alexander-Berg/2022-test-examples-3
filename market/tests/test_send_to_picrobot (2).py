# coding: utf-8

import pytest
from hamcrest import assert_that, has_items

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.subscription.subscription_pb2 as SUBS
from market.idx.datacamp.picrobot.proto.event_pb2 import TImageRequest

from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.matchers.matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.datacamp.yatf.utils import create_update_meta
from market.pylibrary.proto_utils import message_from_data, proto_path_from_str_path

VERTICAL_BUSINESS_ID=55
VERTICAL_SHOP_ID=555
VERTICAL_OFFER_ID='5555'


@pytest.fixture(scope='module')
def basic_offers_table_data():
    return [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000'),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        meta=create_update_meta(0),
                        source=[
                            DTC.SourcePicture(url='https://firstpicture/'),
                            DTC.SourcePicture(url='secondpicture/'),
                            DTC.SourcePicture(url='https://thirdpicture/'),
                            DTC.SourcePicture(url='http://httpurl/'),
                        ]
                    )
                )
            ),
        ),
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T2000'),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        meta=create_update_meta(0),
                        source=[
                            DTC.SourcePicture(url='2000url1/'),
                            DTC.SourcePicture(url='https://2000url2/'),
                            DTC.SourcePicture(url='2000url3/'),
                            DTC.SourcePicture(url='https://2000url4/'),
                            DTC.SourcePicture(url='https://newsourceurl/'),
                            DTC.SourcePicture(url='new2url/'),
                        ],
                    )
                )
            ),
        ),
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T3000'),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        source=[
                            DTC.SourcePicture(url='https://3000url1/'),
                            DTC.SourcePicture(url='3000url3/'),
                            DTC.SourcePicture(url='3000url2/'),
                        ]
                    )
                )
            ),
        )
    ] + [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=VERTICAL_BUSINESS_ID, offer_id=VERTICAL_OFFER_ID),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        source=[
                            DTC.SourcePicture(url='https://vertical/sourceurl/'),
                        ]
                    )
                )
            ),
        )
    ]


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1),
        ) for offer_id in ('T1000', 'T2000', 'T3000')
    ] + [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=2),
            meta=DTC.OfferMeta(rgb=DTC.DIRECT_SEARCH_SNIPPET_GALLERY)
        ) for offer_id in ('T1000', 'T2000', 'T3000')
    ] + [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=3),
            meta=DTC.OfferMeta(rgb=DTC.DIRECT_SEARCH_SNIPPET_GALLERY)
        ) for offer_id in ('T1000', 'T2000', 'T3000')
    ] + [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=VERTICAL_BUSINESS_ID, offer_id=VERTICAL_OFFER_ID, shop_id=VERTICAL_SHOP_ID),
            meta=DTC.OfferMeta(rgb=DTC.VERTICAL_GOODS_ADS)
        )
    ]


@pytest.fixture(scope='module')
def actual_service_offers_table_data():
    return [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1),
        ) for offer_id in ('T1000', 'T2000', 'T3000')
    ] + [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=2),
            meta=DTC.OfferMeta(rgb=DTC.DIRECT_SEARCH_SNIPPET_GALLERY)
        ) for offer_id in ('T1000', 'T2000', 'T3000')
    ] + [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=3),
            meta=DTC.OfferMeta(rgb=DTC.DIRECT_SEARCH_SNIPPET_GALLERY)
        ) for offer_id in ('T1000', 'T2000', 'T3000')
   ] + [
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=VERTICAL_BUSINESS_ID, offer_id=VERTICAL_OFFER_ID, shop_id=VERTICAL_SHOP_ID),
            meta=DTC.OfferMeta(rgb=DTC.VERTICAL_GOODS_ADS)
        )
    ]


SUBSCRIPTION_MESSAGES = [
    message_from_data({
        'subscription_request': [
            {
                'business_id': 1,
                'offer_id': 'T1000',
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'pictures.partner.original')
                    ],
                    'new_picture_urls': [
                        'https://firstpicture/',
                        'secondpicture/',
                        'https://thirdpicture/',
                        'http://httpurl/',
                    ]
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
    message_from_data({
        'subscription_request': [
            {
                'business_id': 1,
                'offer_id': 'T2000',
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'pictures.partner.original')
                    ],
                    'new_picture_urls': [
                        'https://newsourceurl/',
                        'new2url/',
                    ]
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
    message_from_data({
        'subscription_request': [
            {
                'business_id': 1,
                'offer_id': 'T3000',
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'pictures.partner.original')
                    ],
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
    message_from_data({
        'subscription_request': [
            {
                'business_id': VERTICAL_BUSINESS_ID,
                'offer_id': VERTICAL_OFFER_ID,
                'basic': {
                    'updated_fields': [
                        proto_path_from_str_path(DTC.Offer, 'meta.picrobot_force_send')
                    ]
                }
            }
        ]
    }, SUBS.UnitedOffersSubscriptionMessage()),
]


@pytest.fixture(scope='module')
def dispatcher_config(
        yt_token,
        yt_server,
        log_broker_stuff,
        input_topic,
        output_topic,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table
):
    cfg = DispatcherConfig()
    cfg.create_initializer(
        yt_server=yt_server,
        yt_token_path=yt_token.path
    )

    lb_reader = cfg.create_lb_reader(log_broker_stuff, input_topic)
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        basic_offers_table.table_path,
        service_offers_table.table_path,
        actual_service_offers_table.table_path
    )
    filter = cfg.create_subscription_filter('PICROBOT_SUBSCRIBER', extra_params={
        'OfferPictureUrlsAsSet': 'true',
        'Color': 'UNKNOWN_COLOR;WHITE;DIRECT_SEARCH_SNIPPET_GALLERY;VERTICAL_GOODS_ADS',
        'Mode': 'ORIGINAL',
        'SendConsistentOffersOnly': 'false',
        'OfferPictureUrlsAsSet': 'true',
        'OneServicePerUnitedOffer': 'false',
    })
    sender = cfg.create_picrobot_sender(extra_params={
        'PicrobotVerticalMdsNamespace': 'goods_pic',
    })
    lb_writer = cfg.create_lb_writer(log_broker_stuff, output_topic)

    cfg.create_link(lb_reader, unpacker)
    cfg.create_link(unpacker, dispatcher)
    cfg.create_link(dispatcher, filter)
    cfg.create_link(filter, sender)
    cfg.create_link(sender, lb_writer)

    return cfg


@pytest.yield_fixture(scope='module')
def dispatcher(
        dispatcher_config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        input_topic,
        output_topic,
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'input_topic': input_topic,
        'output_topic': output_topic,
    }
    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


def test_filtering(dispatcher, input_topic, output_topic):
    for message in SUBSCRIPTION_MESSAGES:
        input_topic.write(message.SerializeToString())

    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= len(SUBSCRIPTION_MESSAGES), timeout=10)

    sent_data = output_topic.read(count=13)

    assert_that(
        sent_data,
        has_items(
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://firstpicture/', 'MdsNamespace': 'marketpictesting'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://secondpicture/', 'MdsNamespace': 'marketpictesting'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://thirdpicture/', 'MdsNamespace': 'marketpictesting'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://newsourceurl/', 'MdsNamespace': 'marketpictesting'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://new2url/', 'MdsNamespace': 'marketpictesting'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://httpurl/', 'MdsNamespace': 'marketpictesting'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://firstpicture/', 'MdsNamespace': 'marketpictestingdirect'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://secondpicture/', 'MdsNamespace': 'marketpictestingdirect'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://thirdpicture/', 'MdsNamespace': 'marketpictestingdirect'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://newsourceurl/', 'MdsNamespace': 'marketpictestingdirect'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://new2url/', 'MdsNamespace': 'marketpictestingdirect'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'http://httpurl/', 'MdsNamespace': 'marketpictestingdirect'}),
            IsSerializedProtobuf(TImageRequest, {'Url': 'https://vertical/sourceurl/', 'MdsNamespace': 'goods_pic'}),
        ),
    )

    # Проеряем, что топик опустел
    assert_that(output_topic, HasNoUnreadData())
