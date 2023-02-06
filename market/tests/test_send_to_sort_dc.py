# -*- coding: utf-8 -*-
import pytest
from hamcrest import assert_that

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv

# Protos.
from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer
from market.idx.datacamp.proto.offer.OfferMeta_pb2 import (
    OfferMeta,
    UpdateMeta,
    Flag,
    UNKNOWN_SOURCE,
    TSortDCExportContext,
)
from market.idx.datacamp.proto.offer.OfferPrice_pb2 import OfferPrice, PriceBundle
from market.idx.datacamp.proto.offer.OfferStatus_pb2 import OfferStatus
from market.idx.datacamp.proto.offer.OfferIdentifiers_pb2 import OfferIdentifiers
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.proto.common.common_pb2 import PriceExpression
from google.protobuf.timestamp_pb2 import Timestamp


BUSINESS_ID = 1
SHOP_ID = 1
WAREHOUSE_ID = 1
OFFER_ID = 'o1'


@pytest.fixture(scope='module')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def sort_dc_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'sort_dc_topic')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, datacamp_messages_topic, sort_dc_topic):
    cfg = {
        'logbroker': {
            'datacamp_messages_topic': datacamp_messages_topic.topic,
            'sort_dc_topic': sort_dc_topic.topic,
        },
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, datacamp_messages_topic, sort_dc_topic):
    resources = {
        'config': config,
        'datacamp_messages_topic': datacamp_messages_topic,
        'sort_dc_topic': sort_dc_topic,
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


def test_send_to_sort_dc(
    piper,                    # type: PiperTestEnv
    datacamp_messages_topic,  # type: LbkTopic
    sort_dc_topic,            # type: LbkTopic
    log_broker_stuff,
):
    timestamp = 1
    export_msg = ExportMessage()

    sortdc_export = TSortDCExportContext(
        meta=UpdateMeta(
            timestamp=Timestamp(seconds=timestamp)
        ),
        export_items=[
            TSortDCExportContext.TExportItem(
                url_hash_first=4386651286405210899,
                url_hash_second=14947835558944293137
            )
        ]
    )

    # Build datacamp message.
    msg = DatacampMessage(
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
                                rgb=DTC.VERTICAL_GOODS_ADS,
                            ),
                        ),
                        service={
                            SHOP_ID: Offer(
                                identifiers=OfferIdentifiers(
                                    business_id=BUSINESS_ID,
                                    offer_id=OFFER_ID,
                                    shop_id=SHOP_ID,
                                    warehouse_id=WAREHOUSE_ID,
                                ),
                                meta=OfferMeta(
                                    rgb=DTC.VERTICAL_GOODS_ADS,
                                    sortdc_context=sortdc_export,
                                ),
                                status=OfferStatus(
                                    disabled=[
                                        Flag(
                                            meta=UpdateMeta(
                                                timestamp=Timestamp(seconds=timestamp),
                                                source=UNKNOWN_SOURCE,
                                            ),
                                            flag=False,
                                        ),
                                    ],
                                ),
                            ),
                        }
                    ),
                ]
            ),
        ],
    )

    # Initial push of the offer.
    push_to_dcmp_and_wait_until_processed(
        piper=piper,
        topic=datacamp_messages_topic,
        message=msg,
    )
    export_msg.ParseFromString(sort_dc_topic.read(count=1, wait_timeout=1)[0])
    # Check that initial creation of the offer was sent to sortDC.
    assert export_msg.offer.offer_id == OFFER_ID

    # Change disabled status.
    timestamp += 1
    msg.united_offers[0].offer[0].service[SHOP_ID].status.disabled[0].meta.timestamp.seconds = timestamp
    msg.united_offers[0].offer[0].service[SHOP_ID].status.disabled[0].flag = True
    msg.united_offers[0].offer[0].service[SHOP_ID].meta.sortdc_context.meta.timestamp.seconds = timestamp
    push_to_dcmp_and_wait_until_processed(
        piper=piper,
        topic=datacamp_messages_topic,
        message=msg,
    )
    export_msg.ParseFromString(sort_dc_topic.read(count=1, wait_timeout=1)[0])
    # Check that update of the disabled status was sent to sortDC.
    assert export_msg.offer.offer_id == OFFER_ID
    assert len(export_msg.offer.sortdc_context.export_items) == 1
    assert export_msg.offer.sortdc_context.export_items[0].url_hash_first == 4386651286405210899
    assert export_msg.offer.sortdc_context.export_items[0].url_hash_second == 14947835558944293137

    # Change price.
    timestamp += 1
    msg.united_offers[0].offer[0].service[SHOP_ID].price.CopyFrom(
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
        message=msg,
    )
    # Check that update of the price was not sent to sortDC
    # because there is no subscription for the price field.
    assert_that(sort_dc_topic, HasNoUnreadData())
