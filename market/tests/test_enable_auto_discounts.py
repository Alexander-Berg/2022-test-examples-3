# coding: utf-8

from hamcrest import assert_that
import pytest

from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import OfferIdentifiers, OfferPrice, PriceBundle, UpdateMeta, Flag
from market.proto.common.common_pb2 import PriceExpression
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_price

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers

from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.idx.yatf.utils.utils import create_pb_timestamp


@pytest.fixture(scope='session')
def offers():
    experiment1 = [
        # ставим цену на офер
        DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=1,
                business_id=1,
                offer_id='1',
                warehouse_id=1,
            ),
            price=create_price(20, 100)
        ),
        # обновляем флаг enable_auto_discounts, price должна смержится
        DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=1,
                business_id=1,
                offer_id='1',
                warehouse_id=1,
            ),
            price=OfferPrice(
                enable_auto_discounts=Flag(
                    flag=True,
                    meta=UpdateMeta(
                        timestamp=create_pb_timestamp(200)
                    )
                )
            )
        ),
    ]

    experiment2 = [
        # ставим цену и флаг на офер
        DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=1,
                business_id=1,
                offer_id='2',
                warehouse_id=1,
            ),
            price=OfferPrice(
                basic=PriceBundle(
                    meta=UpdateMeta(
                        timestamp=create_pb_timestamp(100),
                    ),
                    binary_price=PriceExpression(
                        price=20 * 10**7,
                        id='RUR'
                    ),
                ),
                enable_auto_discounts=Flag(
                    flag=True,
                    meta=UpdateMeta(
                        timestamp=create_pb_timestamp(100)
                    )
                )
            )
        ),
        # обновляем флаг enable_auto_discounts, флаг должен сброситься, цена при этом не меняется
        DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=1,
                business_id=1,
                offer_id='2',
                warehouse_id=1,
            ),
            price=OfferPrice(
                enable_auto_discounts=Flag(
                    flag=False,
                    meta=UpdateMeta(
                        timestamp=create_pb_timestamp(200)
                    )
                )
            )
        ),
        # меняем цену, флаг при этом изменится не должен
        DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=1,
                business_id=1,
                offer_id='2',
                warehouse_id=1,
            ),
            price=create_price(30, 300)
        ),
    ]

    return experiment1 + experiment2


@pytest.fixture(scope='session')
def lbk_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, lbk_topic):
    cfg = {
        'general': {
            'color': 'blue',
        },
        'logbroker': {
            'offers_topic': lbk_topic.topic,
        },
    }

    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, lbk_topic):
    resources = {
        'config': config,
        'offers_topic': lbk_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def writer(offers, piper, lbk_topic):
    for offer in offers:
        lbk_topic.write(offer.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(offers), timeout=60)


def test_merge_price_and_flag(piper, writer):
    """
    experiment1:
    Проверяем, что piper корректно обработает независимое изменение price и enable_auto_discounts
    """

    # Проверяем, что все входные оферы, есть в выходной табличке
    assert_that(piper.service_offers_table.data,
                HasOffers(
                    [message_from_data({
                        'identifiers': {
                            'shop_id': 1,
                            'offer_id': '1',
                        },
                        'price': {
                            'basic': {
                                'meta': {
                                    'timestamp': create_pb_timestamp(100).ToJsonString()
                                },
                                'binary_price': {
                                    'price': 20 * 10**7,
                                    'id': 'RUR',
                                }
                            },
                            'enable_auto_discounts': {
                                'meta': {
                                    'timestamp': create_pb_timestamp(200).ToJsonString()
                                },
                                'flag': True
                            }
                        }
                    }, DTC.Offer())
                    ]),
                'Missing offers')


def test_update_price(piper, writer):
    """
    experiment2:
    Проверяем, что piper корректно обработает обновление цены и флага, если данные уже есть в хранилище
    """

    # Проверяем, что все входные оферы, есть в выходной табличке
    assert_that(piper.service_offers_table.data,
                HasOffers(
                    [message_from_data({
                        'identifiers': {
                            'shop_id': 1,
                            'offer_id': '2',
                        },
                        'price': {
                            'basic': {
                                'meta': {
                                    'timestamp': create_pb_timestamp(300).ToJsonString()
                                },
                                'binary_price': {
                                    'price': 30 * 10**7,
                                    'id': 'RUR',
                                }
                            },
                            'enable_auto_discounts': {
                                'meta': {
                                    'timestamp': create_pb_timestamp(200).ToJsonString()
                                },
                                'flag': False
                            }
                        }
                    }, DTC.Offer())]),
                'Missing offers')
