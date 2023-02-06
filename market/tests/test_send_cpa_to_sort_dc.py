# -*- coding: utf-8 -*-

import pytest
from hamcrest import assert_that

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv

# Protos.
from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer
from market.idx.datacamp.proto.offer.OfferMeta_pb2 import (
    OfferMeta,
    UpdateMeta,
    UNKNOWN_SOURCE,
)
from market.idx.datacamp.proto.offer.OfferPrice_pb2 import OfferPrice, PriceBundle
from market.idx.datacamp.proto.offer.PartnerInfo_pb2 import PartnerInfo
from market.idx.datacamp.proto.offer.OfferIdentifiers_pb2 import (
    OfferIdentifiers,
    OfferExtraIdentifiers
)
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.proto.common.common_pb2 import PriceExpression
from google.protobuf.timestamp_pb2 import Timestamp


BUSINESS_ID = 1
SHOP_ID = 1
WAREHOUSE_ID = 1
MSKU = 1
MODEL_ID = 1
OFFER_ID = 'o1'
WARE_MD5 = 'w1'
WARE_MD5_2 = 'w2'
FAKE_LINK = 'https://www.fake.market.yandex.ru/{}/{}/{}/{}'.format(BUSINESS_ID, OFFER_ID, SHOP_ID, WAREHOUSE_ID)
MARKET_BLUE_LINK = 'https://market.yandex.ru/product/{}?offerid={}&sku={}'.format(MODEL_ID, WARE_MD5_2, MSKU)
MARKET_DSBS_LINK = 'https://market.yandex.ru/offer/' + WARE_MD5_2


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def sort_dc_cpa_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'sort_dc_cpa_topic')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, sort_dc_cpa_topic):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'sort_dc_cpa_topic': sort_dc_cpa_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, datacamp_messages_topic, sort_dc_cpa_topic):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'sort_dc_cpa_topic': sort_dc_cpa_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def push_to_dcmp_and_wait_until_processed(
    piper,    # type: PiperTestEnv
    topic,    # type: LbkTopic
    message,  # type: DatacampMessage
):
    processed_before_cnt = piper.united_offers_processed
    topic.write(message.SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= processed_before_cnt)


def test_send_cpa_to_sort_dc(
    piper,                    # type: PiperTestEnv
    datacamp_messages_topic,  # type: LbkTopic
    sort_dc_cpa_topic,        # type: LbkTopic
    log_broker_stuff,
):
    timestamp = 1
    export_msg = ExportMessage()

    # Build datacamp message.
    msg_blue = DatacampMessage(
        united_offers=[
            UnitedOffersBatch(
                offer=[
                    UnitedOffer(
                        basic=Offer(
                            identifiers=OfferIdentifiers(
                                business_id=BUSINESS_ID,
                                offer_id=OFFER_ID,
                            ),
                            meta=OfferMeta(
                                rgb=DTC.BLUE,
                            ),
                        ),
                        service={
                            SHOP_ID: Offer(
                                identifiers=OfferIdentifiers(
                                    business_id=BUSINESS_ID,
                                    offer_id=OFFER_ID,
                                    shop_id=SHOP_ID,
                                    warehouse_id=WAREHOUSE_ID,
                                    extra=OfferExtraIdentifiers(
                                        ware_md5=WARE_MD5
                                    )
                                ),
                                meta=OfferMeta(
                                    rgb=DTC.BLUE,
                                ),
                            ),
                        }
                    ),
                ]
            ),
        ],
    )

    msg_dsbs = DatacampMessage(
        united_offers=[
            UnitedOffersBatch(
                offer=[
                    UnitedOffer(
                        basic=Offer(
                            identifiers=OfferIdentifiers(
                                business_id=BUSINESS_ID,
                                offer_id=OFFER_ID,
                            ),
                            meta=OfferMeta(
                                rgb=DTC.WHITE,
                            ),
                        ),
                        service={
                            SHOP_ID: Offer(
                                identifiers=OfferIdentifiers(
                                    business_id=BUSINESS_ID,
                                    offer_id=OFFER_ID,
                                    shop_id=SHOP_ID,
                                    warehouse_id=WAREHOUSE_ID,
                                    extra=OfferExtraIdentifiers(
                                        ware_md5=WARE_MD5
                                    )
                                ),
                                meta=OfferMeta(
                                    rgb=DTC.WHITE,
                                ),
                                partner_info=PartnerInfo(
                                    is_dsbs=True,
                                )
                            ),
                        }
                    ),
                ]
            ),
        ],
    )

    # Initial push of the blue offer.
    push_to_dcmp_and_wait_until_processed(
        piper=piper,
        topic=datacamp_messages_topic,
        message=msg_blue,
    )
    export_msg.ParseFromString(sort_dc_cpa_topic.read(count=1, wait_timeout=1)[0])
    # Check that initial creation of the offer was sent to sortDC.
    assert export_msg.offer.offer_id == OFFER_ID

    # Initial push of the dsbs offer.
    push_to_dcmp_and_wait_until_processed(
        piper=piper,
        topic=datacamp_messages_topic,
        message=msg_dsbs,
    )
    export_msg.ParseFromString(sort_dc_cpa_topic.read(count=1, wait_timeout=1)[0])
    # Check that initial creation of the offer was sent to sortDC.
    assert export_msg.offer.offer_id == OFFER_ID

    # Change ware_md5 for blue.
    msg_blue.united_offers[0].offer[0].service[SHOP_ID].identifiers.extra.ware_md5 = WARE_MD5_2
    msg_dsbs.united_offers[0].offer[0].service[SHOP_ID].identifiers.extra.ware_md5 = WARE_MD5_2

    push_to_dcmp_and_wait_until_processed(
        piper=piper,
        topic=datacamp_messages_topic,
        message=msg_blue,
    )
    export_msg.ParseFromString(sort_dc_cpa_topic.read(count=1, wait_timeout=1)[0])
    # Check that update of the ware_md5 was sent to sortDC.
    assert export_msg.offer.ware_md5 == WARE_MD5_2
    assert export_msg.original_url == FAKE_LINK
    assert export_msg.market_content.market_b2c_url == MARKET_BLUE_LINK

    # Change ware_md5 for dsbs.
    msg_blue.united_offers[0].offer[0].service[SHOP_ID].identifiers.extra.ware_md5 = WARE_MD5_2
    msg_dsbs.united_offers[0].offer[0].service[SHOP_ID].identifiers.extra.ware_md5 = WARE_MD5_2

    push_to_dcmp_and_wait_until_processed(
        piper=piper,
        topic=datacamp_messages_topic,
        message=msg_dsbs,
    )
    export_msg.ParseFromString(sort_dc_cpa_topic.read(count=1, wait_timeout=1)[0])
    # Check that update of the ware_md5 was sent to sortDC.
    assert export_msg.offer.ware_md5 == WARE_MD5_2
    assert export_msg.original_url == FAKE_LINK
    assert export_msg.market_content.market_b2c_url == MARKET_DSBS_LINK

    # Change price.
    timestamp += 1
    msg_blue.united_offers[0].offer[0].service[SHOP_ID].price.CopyFrom(
        OfferPrice(
            basic=PriceBundle(
                meta=UpdateMeta(
                    timestamp=Timestamp(seconds=timestamp),
                    source=UNKNOWN_SOURCE,
                ),
                binary_price=PriceExpression(
                    price=100,
                )
            )
        )
    )
    # Change price.
    msg_dsbs.united_offers[0].offer[0].service[SHOP_ID].price.CopyFrom(
        OfferPrice(
            basic=PriceBundle(
                meta=UpdateMeta(
                    timestamp=Timestamp(seconds=timestamp),
                    source=UNKNOWN_SOURCE,
                ),
                binary_price=PriceExpression(
                    price=100,
                )
            )
        )
    )
    push_to_dcmp_and_wait_until_processed(
        piper=piper,
        topic=datacamp_messages_topic,
        message=msg_blue,
    )
    # Check that update of the price was not sent to sortDC
    # because there is no subscription for the price field.
    assert_that(sort_dc_cpa_topic, HasNoUnreadData())

    push_to_dcmp_and_wait_until_processed(
        piper=piper,
        topic=datacamp_messages_topic,
        message=msg_dsbs,
    )
    # Check that update of the price was not sent to sortDC
    # because there is no subscription for the price field.
    assert_that(sort_dc_cpa_topic, HasNoUnreadData())
