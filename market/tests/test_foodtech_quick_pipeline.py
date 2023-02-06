# coding: utf-8

import ctypes
import datetime
from hamcrest import assert_that
import pytest

from saas.protos.rtyserver_pb2 import TMessage as RtyMessage

from market.pylibrary.proto_utils import message_from_data

from market.proto.common.common_pb2 import PriceExpression
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.OfferPrice_pb2 import Vat as DatacampVat
from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
from market.idx.datacamp.proto.common.Types_pb2 import Currency
from market.idx.datacamp.proto.external.Offer_pb2 import (
    Offer as ExternalOffer,
    IdentifiedPrice,
    IdentifiedStatus,
    OfferPrice,
    Vat as ExternalVat
)
from market.idx.datacamp.proto.offer.OfferMeta_pb2 import Flag, UpdateMeta
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampPartnersTable,
)
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_pb_timestamp


def make_expected_ts(ts):
    time_pattern = "%Y-%m-%dT%H:%M:%SZ"
    return datetime.utcfromtimestamp(ts).strftime(time_pattern)


def make_identified_price(shop_id, price, vat, ts=0):
    return IdentifiedPrice(
        shop_id=shop_id,
        price=OfferPrice(price=price, currency=Currency.RUR),
        meta=UpdateMeta(timestamp=create_pb_timestamp(ts)),
        vat=vat
    )


def make_identified_status(shop_id, flag, ts=0):
    return IdentifiedStatus(
        shop_id=shop_id,
        disable_status={
            DTC.DataSource.PUSH_PARTNER_API: Flag(
                flag=flag,
                meta=UpdateMeta(timestamp=create_pb_timestamp(ts))
            )
        },
    )


EXISTING_OFFER_ID = 'EXISTING_OFFER_ID'  # этот оффер есть в Хранилище
MISSING_EXISTING_OFFER_ID = 'MISSING_EXISTING_OFFER_ID'  # этого оффера еще нет в Хранилище
BUSINESS_ID = 1
FEED1_ID = 11
FEED2_ID = 12
SHOP1_ID = 101
SHOP2_ID = 102

QOFFERS = [
    ExportMessage(
        offer=ExternalOffer(
            business_id=BUSINESS_ID,
            offer_id=EXISTING_OFFER_ID,
            shop_prices=[
                make_identified_price(SHOP1_ID, 456*10**7, ts=100500, vat=ExternalVat.VAT_10),
                make_identified_price(SHOP2_ID, 678*10**7, ts=100501, vat=ExternalVat.VAT_20),
            ],
            shop_statuses=[
                make_identified_status(SHOP2_ID, True, ts=100700)
            ]
        )
    ).SerializeToString(),
    ExportMessage(
        offer=ExternalOffer(
            business_id=BUSINESS_ID,
            offer_id=MISSING_EXISTING_OFFER_ID,
            shop_prices=[
                make_identified_price(SHOP1_ID, 789*10**7, ts=100800, vat=ExternalVat.NO_VAT),
            ],
            shop_statuses=[
                make_identified_status(SHOP1_ID, True, ts=100900)
            ]
        )
    ).SerializeToString(),
]


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=[
            {
                'shop_id': SHOP1_ID,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': SHOP1_ID,
                        'business_id': BUSINESS_ID,
                        'datafeed_id': FEED1_ID,
                        'united_catalog_status': 'NO',
                        'is_lavka': 'true',
                    }),
                ]),
            },
            {
                'shop_id': SHOP2_ID,
                'mbi': '\n\n'.join([
                    dict2tskv({
                        'shop_id': SHOP2_ID,
                        'business_id': BUSINESS_ID,
                        'datafeed_id': FEED2_ID,
                        'united_catalog_status': 'NO',
                        'is_lavka': 'true',
                    }),
                ]),
            },
        ],
    )


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=[
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=EXISTING_OFFER_ID),
            meta=create_meta(10, color=DTC.LAVKA, scope=DTC.SERVICE),
        ))])


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=EXISTING_OFFER_ID, shop_id=SHOP1_ID, feed_id=0),
            meta=create_meta(10, color=DTC.LAVKA, scope=DTC.SERVICE),
            price=DTC.OfferPrice(
                basic=DTC.PriceBundle(
                    binary_price=PriceExpression(
                        price=100 * 10**7,
                    ),
                    vat=DatacampVat.VAT_18
                )
            )
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=EXISTING_OFFER_ID, shop_id=SHOP2_ID, feed_id=0),
            meta=create_meta(10, color=DTC.LAVKA, scope=DTC.SERVICE),
            price=DTC.OfferPrice(
                basic=DTC.PriceBundle(
                    binary_price=PriceExpression(
                        price=100 * 10**7,
                    ),
                    vat=DatacampVat.VAT_18
                )
            )
        ))
    ])


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=EXISTING_OFFER_ID, shop_id=SHOP1_ID, feed_id=FEED1_ID, warehouse_id=0),
            meta=create_meta(10, color=DTC.LAVKA, scope=DTC.SERVICE),
        )),
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(business_id=BUSINESS_ID, offer_id=EXISTING_OFFER_ID, shop_id=SHOP2_ID, feed_id=FEED2_ID, warehouse_id=0),
            meta=create_meta(10, color=DTC.LAVKA, scope=DTC.SERVICE),
        ))
    ])


@pytest.fixture(scope='module')
def foodtech_quick_pipeline_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def rty_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'rty_topic')
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, foodtech_quick_pipeline_topic, rty_topic):
    cfg = {
        'logbroker': {
            'foodtech_quick_pipeline_topic': foodtech_quick_pipeline_topic.topic,  # input topic
            'rty_topic': rty_topic.topic,             # output topic
        },
        'general': {
            'color': 'white',
        }
    }
    return PiperConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.yield_fixture(scope='module')
def piper(
    yt_server,
    log_broker_stuff,
    config,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    partners_table,
    foodtech_quick_pipeline_topic,
    rty_topic
):
    resources = {
        'config': config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_table': partners_table,
        'foodtech_quick_pipeline_topic': foodtech_quick_pipeline_topic,
        'rty_topic': rty_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


def test_foodtech_quick_pipeline(rty_topic, foodtech_quick_pipeline_topic, piper, basic_offers_table, service_offers_table, actual_service_offers_table):
    def rty_values(rty_doc):
        """ Dict-shaped RTYy document's values """
        names = rty_doc.Factors.Names
        values = rty_doc.Factors.Values.Values
        assert len(names) == len(values)
        return {n: v.Value for n, v in zip(names, values)}

    def reconstruct_price(rty_doc):
        """ A bit of black wizardry,
            see https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/quick/library/doc_state/price.cpp?rev=r7137727#L85 """
        values = rty_values(rty_doc)
        assert 'price_low' in values and 'price_high' in values
        price_low = values['price_low']
        price_high = values['price_high']
        f_low = ctypes.c_float(price_low)
        f_high = ctypes.c_float(price_high)
        i_low = ctypes.c_uint.from_buffer(f_low).value
        i_high = ctypes.c_uint.from_buffer(f_high).value
        return (i_high << 32) | i_low

    def reconstruct_disabled(rty_doc):
        """ See https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/quick/library/doc_state/disabled.cpp?rev=r8425865#L11
        """
        values = rty_values(rty_doc)
        if 'offer_disabled' not in values:
            return 0
        f_disabled = ctypes.c_float(values['offer_disabled'])
        return ctypes.c_uint.from_buffer(f_disabled).value

    def reconstruct_disabled_ts(rty_doc):
        """ See https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/quick/library/doc_state/disabled.cpp?rev=r8425865#L11
        """
        values = rty_values(rty_doc)
        if 'offer_disabled_ts' not in values:
            return 0
        f_disabled_ts = ctypes.c_float(values['offer_disabled_ts'])
        return ctypes.c_uint.from_buffer(f_disabled_ts).value

    def parse_result(r):
        msg = RtyMessage()
        msg.ParseFromString(r)
        return msg

    for qoffers in QOFFERS:
        foodtech_quick_pipeline_topic.write(qoffers)
    wait_until(lambda: piper.united_offers_processed >= 1)

    result = rty_topic.read(count=2)
    messages = list(sorted((parse_result(r) for r in result), key=lambda m: m.Document.Url))
    assert len(messages) == 2

    # проверяем первый документ: цена + фиктивное скрытие с ts = 1
    assert messages[0].MessageType == RtyMessage.MODIFY_DOCUMENT
    assert messages[0].Document.Url == "11/EXISTING_OFFER_ID/"
    assert reconstruct_price(messages[0].Document) == 456 * 10**7
    assert reconstruct_disabled(messages[0].Document) != 0
    assert reconstruct_disabled_ts(messages[0].Document) == 1

    # проверяем второй документ: цена + скрытие с прикладным ts
    assert messages[1].MessageType == RtyMessage.MODIFY_DOCUMENT
    assert messages[1].Document.Url == "12/EXISTING_OFFER_ID/"
    assert reconstruct_price(messages[1].Document) == 678 * 10**7
    assert reconstruct_disabled(messages[1].Document) != 0
    assert reconstruct_disabled_ts(messages[1].Document) == 100700

    # проверяем, что в топике больше нет данных, которые мы можем вычитать
    assert_that(rty_topic, HasNoUnreadData())

    # проверяем, что не создается оффер с пустой базовой частью
    basic_offers_table.load()
    assert len(basic_offers_table.data) == 1

    service_offers_table.load()
    assert len(service_offers_table.data) == 2

    actual_service_offers_table.load()
    assert len(actual_service_offers_table.data) == 2

    # проверяем, что мы обновили статус, цену и не снесли vat
    assert_that(
        service_offers_table.data,
        HasOffers([
            message_from_data({
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': EXISTING_OFFER_ID,
                    'shop_id': SHOP1_ID,
                },
                'price': {
                    'basic': {
                        'binary_price': {
                            'price': 456 * 10**7
                        },
                        'vat': DatacampVat.VAT_10
                    },
                    'original_price_fields': {
                        'vat': {
                            'value': DatacampVat.VAT_10
                        },
                    }
                },
                'status': {
                    'disabled': [{
                        'flag': True,
                    }]
                }
            }, DTC.Offer()),
            message_from_data({
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': EXISTING_OFFER_ID,
                    'shop_id': SHOP2_ID,
                },
                'price': {
                    'basic': {
                        'binary_price': {
                            'price': 678 * 10**7
                        },
                        'vat': DatacampVat.VAT_20
                    },
                    'original_price_fields': {
                        'vat': {
                            'value': DatacampVat.VAT_20
                        },
                    }
                }
            }, DTC.Offer())
        ])
    )
