# coding: utf-8

import pytest
from hamcrest import assert_that

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.api.DatacampMessage_pb2 as DTC_API
import market.idx.datacamp.proto.offer.UnitedOffer_pb2 as UTC
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_update_meta
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampBasicOffersTable, DataCampServiceOffersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data

OLD_DATE_TS = 1
NEW_DATE_TS = 100
VERY_OLD_DATE_TS = 0

BASIC_TABLE_DATA = [
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id='T1000'),
            content=DTC.OfferContent(
                binding=DTC.ContentBinding(
                    goods_cvdups_mapping=DTC.GoodsMapping(
                        meta=create_update_meta(OLD_DATE_TS),
                        model_id=0,
                        sku_id=2,
                    )
                )
            )
        )
    )
]

SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1),
        )
    )
    for offer_id in ('T1000',)
]

ACTUAL_SERVICE_TABLE_DATA = [
    offer_to_service_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=1, offer_id=offer_id, shop_id=1),
        )
    )
    for offer_id in ('T1000',)
]


def create_datacamp_message(basic_offer):
    return DTC_API.DatacampMessage(united_offers=[UTC.UnitedOffersBatch(offer=[UTC.UnitedOffer(basic=basic_offer)])])


def create_response(offer_id, model_id, sku_id, date):
    return create_datacamp_message(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id=offer_id,
            ),
            content=DTC.OfferContent(
                binding=DTC.ContentBinding(
                    goods_cvdups_mapping=DTC.GoodsMapping(
                        meta=create_update_meta(date),
                        model_id=model_id,
                        sku_id=sku_id
                    )
                )
            )
        )
    )

CVDUPS_RESPONSES = [
    create_response('T1000', 1, 3, NEW_DATE_TS),
    create_response('T1000', 2, 4, VERY_OLD_DATE_TS),
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


@pytest.fixture(scope='module')
def cvdups_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, cvdups_topic):
    cfg = {
        'general': {
            'color': 'white',
            'batch_size': 10,
        },
        'logbroker': {
            'cvdups_topic': cvdups_topic.topic,
        }
    }
    return PiperConfigMock(yt_server=yt_server, log_broker_stuff=log_broker_stuff, config=cfg)


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    cvdups_topic,
    actual_service_offers_table,
    service_offers_table,
    basic_offers_table,
):
    resources = {
        'config': config,
        'cvdups_topic': cvdups_topic,
        'actual_service_offers_table': actual_service_offers_table,
        'service_offers_table': service_offers_table,
        'basic_offers_table': basic_offers_table,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_update_cvdups_content_from_topic(piper, cvdups_topic):
    for response in CVDUPS_RESPONSES:
        cvdups_topic.write(response.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(CVDUPS_RESPONSES), timeout=60)

    assert_that(piper.basic_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'T1000',
            },
            'content': {
                'binding': {
                    'goods_cvdups_mapping': {
                        'model_id': 1,
                        'sku_id': 3
                    }
                }
            }
        }, DTC.Offer())]))
