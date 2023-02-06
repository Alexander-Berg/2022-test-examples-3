# coding: utf-8

from hamcrest import assert_that, has_items
import pytest

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.OfferMeta_pb2 import DIRECT_STANDBY, WHITE

from market.mbi.proto.SamovarContext_pb2 import SamovarContext, FeedInfo
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.yatf.matchers.matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.datacamp.yatf.utils import create_update_meta
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row, offer_to_basic_row

from robot.protos.crawl.compatibility.feeds_pb2 import TFeedExt


BUSINESS_ID = 1
SHOP_ID = 1
OFFER_ID = 'T1000'  # оффер DIRECT_STANDBY разметили семплом редиректа => отправился
OFFER_ID_2 = 'T2000'  # оффер не DIRECT_STANDBY разметили семплом редиректа => не отправился
OFFER_ID_3 = 'T3000'  # в размеченном семплированием оффере DIRECT_STANDBY поменялся урл => отправили
OFFER_ID_4 = 'T4000'  # в не размеченном семплированием оффере DIRECT_STANDBY поменялся урл => не отправили
OFFER_ID_5 = 'T5000'  # белый оффер получил платформу DIRECT_STANDBY => отправили

BASIC_TABLE_DATA = [
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID),
        )
    ),
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_2),
        )
    ),
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_3),
        )
    ),
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_4),
        )
    ),
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_5),
        )
    ),
]

SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID, shop_id=SHOP_ID),
            meta=DTC.OfferMeta(rgb=DIRECT_STANDBY),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        url=DTC.StringValue(value='http://www.first.com', meta=create_update_meta(10))
                    )
                )
            ),
        )
    ),
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_2, shop_id=SHOP_ID),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        url=DTC.StringValue(value='http://www.second.com', meta=create_update_meta(10))
                    )
                )
            ),
        )
    ),
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_3, shop_id=SHOP_ID),
            meta=DTC.OfferMeta(rgb=DIRECT_STANDBY),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        url=DTC.StringValue(value='http://www.third.com', meta=create_update_meta(10))
                    )
                )
            ),
            status=DTC.OfferStatus(
                is_sampled_for_redirect=DTC.Flag(
                    flag=True,
                    meta=create_update_meta(10),
                )
            )
        )
    ),
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_4, shop_id=SHOP_ID),
            meta=DTC.OfferMeta(rgb=DIRECT_STANDBY),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        url=DTC.StringValue(value='http://www.fourth.com', meta=create_update_meta(10))
                    )
                )
            ),
        )
    ),
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_5, shop_id=SHOP_ID),
            meta=DTC.OfferMeta(rgb=WHITE),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        url=DTC.StringValue(value='http://www.fifth.com', meta=create_update_meta(10))
                    )
                )
            ),
            status=DTC.OfferStatus(
                is_sampled_for_redirect=DTC.Flag(
                    flag=True,
                    meta=create_update_meta(10),
                )
            )
        )
    ),
]

ACTUAL_SERVICE_TABLE_DATA = []


def create_datacamp_message(offer):
    return DatacampMessage(united_offers=[
        UnitedOffersBatch(offer=[UnitedOffer(basic=DTC.Offer(identifiers=DTC.OfferIdentifiers(
            offer_id=offer.identifiers.offer_id, business_id=offer.identifiers.business_id)),
            service={SHOP_ID: offer})])
    ])


UPDATED_OFFER_MESSAGES = [
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID, shop_id=SHOP_ID),
            meta=DTC.OfferMeta(rgb=DIRECT_STANDBY),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        url=DTC.StringValue(value='http://www.first.com', meta=create_update_meta(20))
                    )
                )
            ),
            status=DTC.OfferStatus(
                is_sampled_for_redirect=DTC.Flag(
                    flag=True,
                    meta=create_update_meta(20),
                )
            )
        )
    ),
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_2, shop_id=SHOP_ID),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        url=DTC.StringValue(value='http://www.second.com', meta=create_update_meta(20))
                    )
                )
            ),
            status=DTC.OfferStatus(
                is_sampled_for_redirect=DTC.Flag(
                    flag=True,
                    meta=create_update_meta(20),
                )
            )
        )
    ),
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_3, shop_id=SHOP_ID),
            meta=DTC.OfferMeta(rgb=DIRECT_STANDBY),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        url=DTC.StringValue(value='http://www.third.com/new', meta=create_update_meta(20))
                    )
                )
            ),
            status=DTC.OfferStatus(
                is_sampled_for_redirect=DTC.Flag(
                    flag=True,
                    meta=create_update_meta(20),
                )
            )
        )
    ),
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_4, shop_id=SHOP_ID),
            meta=DTC.OfferMeta(rgb=DIRECT_STANDBY),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        url=DTC.StringValue(value='http://www.fourth.com/new', meta=create_update_meta(20))
                    )
                )
            ),
        )
    ),
    create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=OFFER_ID_5, shop_id=SHOP_ID),
            meta=DTC.OfferMeta(platforms={
                DIRECT_STANDBY: True
            })
        )
    ),
]


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=BASIC_TABLE_DATA)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=SERVICE_TABLE_DATA)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(
        yt_server, config.yt_actual_service_offers_tablepath, data=ACTUAL_SERVICE_TABLE_DATA
    )


@pytest.fixture(scope='session')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def samovar_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, samovar_topic):
    cfg = {
        'general': {
            'color': 'white',
            'batch_size': 10,
        },
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
        },
        'samovar': {
            'topic': samovar_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    datacamp_messages_topic,
    samovar_topic,
    actual_service_offers_table,
    service_offers_table,
    basic_offers_table,
):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'samovar_topic': samovar_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_send(piper, datacamp_messages_topic, samovar_topic):
    for message in UPDATED_OFFER_MESSAGES:
        datacamp_messages_topic.write(message.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(UPDATED_OFFER_MESSAGES), timeout=60)

    sent_data = samovar_topic.read(count=3)
    assert_that(
        sent_data,
        has_items(
            IsSerializedProtobuf(TFeedExt, {
                'FeedName': 'datacamp-bannerland-offers-redirects',
                'Url': 'http://www.first.com',
                'FeedContext': {
                    'BytesValue': SamovarContext(
                        feeds=[FeedInfo(businessId=BUSINESS_ID, shopId=SHOP_ID, campaignType="REDIRECT", directStandBy=True)]
                    ).SerializeToString()
                }
            }),
        ),
    )
    assert_that(
        sent_data,
        has_items(
            IsSerializedProtobuf(TFeedExt, {
                'FeedName': 'datacamp-bannerland-offers-redirects',
                'Url': 'http://www.third.com/new',
                'FeedContext': {
                    'BytesValue': SamovarContext(
                        feeds=[FeedInfo(businessId=BUSINESS_ID, shopId=SHOP_ID, campaignType="REDIRECT", directStandBy=True)]
                    ).SerializeToString()
                }
            }),
        ),
    )
    assert_that(
        sent_data,
        has_items(
            IsSerializedProtobuf(TFeedExt, {
                'FeedName': 'datacamp-bannerland-offers-redirects',
                'Url': 'http://www.fifth.com',
                'FeedContext': {
                    'BytesValue': SamovarContext(
                        feeds=[FeedInfo(businessId=BUSINESS_ID, shopId=SHOP_ID, campaignType="REDIRECT", directStandBy=True)]
                    ).SerializeToString()
                }
            }),
        ),
    )

    assert_that(samovar_topic, HasNoUnreadData())
