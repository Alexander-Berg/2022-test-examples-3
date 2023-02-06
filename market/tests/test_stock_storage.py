# coding: utf-8

from hamcrest import assert_that
import pytest
from datetime import datetime

from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferStatus,
    OfferIdentifiers,
    OfferMeta,
    OfferOrderProperties,
    UpdateMeta,
    Flag,
    MARKET_STOCK,
    MARKET_IDX,
    MARKET_ABO,
    BLUE,
)
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import ChangeOfferRequest
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock

from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampServiceOffersTable, DataCampPartnersTable

from market.idx.yatf.matchers.yt_rows_matchers import HasOffers

from market.pylibrary.proto_utils import message_from_data
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row

BUSINESS_ID = 1000
SHOP_ID = 1
WAREHOUSE_ID = 111
FEED_ID = 111000
CREATE_TS = 10
CURRENT_TS = 500
FUTURE_TS = 1000
SS_TS = 700
time_pattern = "%Y-%m-%dT%H:%M:%SZ"


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    SHOPS = [
        {
            'shop_id': SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_ID,
                    'warehouse_id': WAREHOUSE_ID,
                    'datafeed_id': FEED_ID,
                    'business_id': BUSINESS_ID
                }),
            ]),
            'status': 'publish'
        }
    ]

    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=SHOPS
    )


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    def create_update_meta(source, ts_seconds):
        meta = UpdateMeta()
        meta.source = source
        meta.timestamp.FromSeconds(ts_seconds)

        return meta

    def create_status(disabled_list):
        status = []
        for disabled in disabled_list:
            status.append(Flag(
                flag=disabled[0],
                meta=create_update_meta(disabled[1], disabled[2])
            ))
        return OfferStatus(disabled=status)

    def create_order_properties(order_method, source, ts_seconds):
        return OfferOrderProperties(
            order_method=order_method,
            meta=create_update_meta(source, ts_seconds)
        )

    offer_meta = create_meta(CREATE_TS, BLUE)

    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=[
        offer_to_service_row(
            DatacampOffer(
                identifiers=OfferIdentifiers(
                    shop_id=SHOP_ID,
                    offer_id='ssku.to.be.enabled',
                    business_id=BUSINESS_ID,
                    warehouse_id=WAREHOUSE_ID
                ),
                meta=offer_meta
            )),
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=SHOP_ID,
                offer_id='ssku.to.be.disabled',
                business_id=BUSINESS_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            meta=offer_meta
        )),
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=SHOP_ID,
                offer_id='ssku.to.be.enabled.and.preorder',
                business_id=BUSINESS_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            meta=offer_meta
        )),
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=SHOP_ID,
                offer_id='ssku.disabled.in.table.by.other.source.to.be.disabled.by.stock',
                business_id=BUSINESS_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            meta=offer_meta,
            status=create_status([(True, MARKET_ABO, CURRENT_TS)])
        )),
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=SHOP_ID,
                offer_id='ssku.enabled.in.table.to.be.disabled.and.preorder',
                business_id=BUSINESS_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            meta=offer_meta,
            status=create_status([(False, MARKET_STOCK, CURRENT_TS)]),
            order_properties=create_order_properties(
                OfferOrderProperties.AVAILABLE_FOR_ORDER,
                MARKET_STOCK,
                CURRENT_TS,
            )
        )),
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=SHOP_ID,
                offer_id='ssku.disabled.in.table.to.be.enabled.and.preorder',
                business_id=BUSINESS_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            meta=offer_meta,
            status=create_status([(True, MARKET_STOCK, CURRENT_TS)]),
            order_properties=create_order_properties(
                OfferOrderProperties.AVAILABLE_FOR_ORDER,
                MARKET_STOCK,
                CURRENT_TS,
            )
        )),
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=SHOP_ID,
                offer_id='ssku.enabled.in.table.not.to.be.disabled.with.older.ts.and.preorder',
                business_id=BUSINESS_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            meta=offer_meta,
            status=create_status([(False, MARKET_STOCK, FUTURE_TS)]),
            order_properties=create_order_properties(
                OfferOrderProperties.AVAILABLE_FOR_ORDER,
                MARKET_STOCK,
                CURRENT_TS,
            )
        )),
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=SHOP_ID,
                offer_id='ssku.enabled.in.table.not.to.be.disabled.with.older.ts.and.preorder',
                business_id=BUSINESS_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            meta=offer_meta,
            status=create_status([(False, MARKET_STOCK, FUTURE_TS)]),
            order_properties=create_order_properties(
                OfferOrderProperties.AVAILABLE_FOR_ORDER,
                MARKET_STOCK,
                CURRENT_TS,
            )
        )),
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=SHOP_ID,
                offer_id='ssku.disabled.in.table.not.to.be.enabled.and.preorder.with.older.ts',
                business_id=BUSINESS_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            meta=offer_meta,
            status=create_status([(True, MARKET_STOCK, CURRENT_TS)]),
            order_properties=create_order_properties(
                OfferOrderProperties.AVAILABLE_FOR_ORDER,
                MARKET_STOCK,
                FUTURE_TS,
            )
        )),
        offer_to_service_row(DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=SHOP_ID,
                offer_id='ssku.with.stock.count',
                business_id=BUSINESS_ID,
                warehouse_id=WAREHOUSE_ID
            ),
            meta=offer_meta,
        )),
    ])


@pytest.fixture(scope='module')
def ss_topic_data():
    def make_ss_data(shop_id, offer_id, disabled, preorder, ts, available=None):
        # NB: Stock storage шлет метку времени в legacy формате (ts_ms)
        meta = UpdateMeta(
            source=MARKET_STOCK,
            ts_ms=ts * 1000
        )

        offer = DatacampOffer(
            identifiers=OfferIdentifiers(
                shop_id=shop_id,
                offer_id=offer_id,
                warehouse_id=WAREHOUSE_ID,
                feed_id=FEED_ID,
            ),
            meta=OfferMeta(
                rgb=BLUE
            ),
        )

        if disabled is not None:
            flag = offer.status.disabled.add()
            flag.flag = disabled
            flag.meta.CopyFrom(meta)

        if preorder is not None:
            order_method = OfferOrderProperties.PRE_ORDERED if preorder else OfferOrderProperties.AVAILABLE_FOR_ORDER
            offer.order_properties.order_method = order_method
            offer.order_properties.meta.CopyFrom(meta)

        if available is not None:
            offer.stock_info.market_stocks.count = available
            offer.stock_info.market_stocks.meta.CopyFrom(meta)

        return offer

    return [
        # Оффер выключается по стокам, проставляются параметры заказа
        make_ss_data(
            shop_id=SHOP_ID,
            offer_id='ssku.to.be.disabled',
            disabled=True,
            preorder=False,
            ts=SS_TS,
        ),
        # Оффер включается по стокам, проставляются параметры заказа
        make_ss_data(
            shop_id=SHOP_ID,
            offer_id='ssku.to.be.enabled',
            disabled=False,
            preorder=False,
            ts=SS_TS,
        ),
        # Оффер включается по стокам, проставляются параметры предзаказа
        make_ss_data(
            shop_id=SHOP_ID,
            offer_id='ssku.to.be.enabled.and.preorder',
            disabled=False,
            preorder=True,
            ts=SS_TS,
        ),
        # Не существующий в таблице оффер включается по стокам, проставляются параметры предзаказа (добавляется в
        # таблицу с установленным скрытием MARKET_IDX, т.к. не готов к индексации)
        make_ss_data(
            shop_id=SHOP_ID,
            offer_id='ssku.to.be.enabled.but.absent.in.storage',
            disabled=False,
            preorder=False,
            ts=SS_TS,
        ),
        # Оффер выключается по стокам, скрытия из других источников сохраняются
        make_ss_data(
            shop_id=SHOP_ID,
            offer_id='ssku.disabled.in.table.by.other.source.to.be.disabled.by.stock',
            disabled=True,
            preorder=False,
            ts=SS_TS,
        ),
        # Оффер, включенный по стокам, должен быть выключен
        make_ss_data(
            shop_id=SHOP_ID,
            offer_id='ssku.enabled.in.table.to.be.disabled.and.preorder',
            disabled=True,
            preorder=True,
            ts=SS_TS,
        ),
        # Оффер, выключенный по стокам, должен быть включен
        make_ss_data(
            shop_id=SHOP_ID,
            offer_id='ssku.disabled.in.table.to.be.enabled.and.preorder',
            disabled=False,
            preorder=True,
            ts=SS_TS,
        ),
        # Оффер, включенный по стокам, не выключается, если метка времени старая
        make_ss_data(
            shop_id=SHOP_ID,
            offer_id='ssku.enabled.in.table.not.to.be.disabled.with.older.ts.and.preorder',
            disabled=True,
            preorder=True,
            ts=SS_TS,
        ),
        # Оффер, выключенный по стокам, включается, но предзаказ не меняется, т.к. метка времени старая
        make_ss_data(
            shop_id=SHOP_ID,
            offer_id='ssku.disabled.in.table.not.to.be.enabled.and.preorder.with.older.ts',
            disabled=False,
            preorder=False,
            ts=SS_TS,
        ),
        # Оффер с информаций о количестве товара на складе
        make_ss_data(
            shop_id=SHOP_ID,
            offer_id='ssku.with.stock.count',
            disabled=None,
            preorder=None,
            ts=SS_TS,
            available=5,
        ),
    ]


@pytest.fixture(scope='session')
def stock_storage_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='session')
def config(yt_server, log_broker_stuff, stock_storage_topic):
    cfg = {
        'general': {
            'color': 'blue',
        },
        'logbroker': {
            'stock_storage_topic': stock_storage_topic.topic,
        },
    }
    return PiperConfigMock(
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, stock_storage_topic, actual_service_offers_table, partners_table):
    resources = {
        'config': config,
        'actual_service_offers_table': actual_service_offers_table,
        'partners_table': partners_table,
        'stock_storage_topic': stock_storage_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.yield_fixture(scope='module')
def workflow(piper, stock_storage_topic, ss_topic_data):
    for ss in ss_topic_data:
        request = ChangeOfferRequest()
        request.offer.extend([ss])
        stock_storage_topic.write(request.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(ss_topic_data), timeout=60)
    wait_until(lambda: piper.stock_storage_processed >= len(ss_topic_data), timeout=60)


def make_expected_dict(ssku, business_id, disabled_list, preorder, available=None, shop_id=None, warehouse_id=None):
    result = {
        'identifiers': {
            'business_id': business_id,
            'offer_id': ssku,
        }
    }
    if shop_id is not None:
        result['identifiers']['shop_id'] = shop_id
    if warehouse_id is not None:
        result['identifiers']['warehouse_id'] = warehouse_id

    if disabled_list is not None:
        result['status'] = {
            'disabled': [
                {
                    'flag': disabled[0],
                    'meta': {
                        'source': disabled[1],
                        'timestamp': datetime.utcfromtimestamp(disabled[2]).strftime(time_pattern)
                    }
                } for disabled in disabled_list
            ]
        }

    if preorder is not None:
        result['order_properties'] = {
            'meta': {
                'source': MARKET_STOCK,
                'timestamp': datetime.utcfromtimestamp(preorder[1]).strftime(time_pattern)
            },
            'order_method': OfferOrderProperties.PRE_ORDERED if preorder[0]
            else OfferOrderProperties.AVAILABLE_FOR_ORDER
        }

    if available is not None:
        result['stock_info'] = {
            'market_stocks': {
                'meta': {
                    'source': MARKET_STOCK,
                    'timestamp': datetime.utcfromtimestamp(available[1]).strftime(time_pattern)
                },
                'count': available[0]
            }
        }

    return result


@pytest.mark.parametrize("expected", [
    make_expected_dict(
        ssku='ssku.to.be.disabled',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=[(True, MARKET_STOCK, SS_TS)],
        preorder=(False, SS_TS),
    ),
    make_expected_dict(
        ssku='ssku.to.be.enabled',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=[(False, MARKET_STOCK, SS_TS)],
        preorder=(False, SS_TS),
    ),
    make_expected_dict(
        ssku='ssku.to.be.enabled.and.preorder',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=[(False, MARKET_STOCK, SS_TS)],
        preorder=(True, SS_TS),
    ),
    make_expected_dict(
        ssku='ssku.disabled.in.table.by.other.source.to.be.disabled.by.stock',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=[(True, MARKET_STOCK, SS_TS), (True, MARKET_ABO, CURRENT_TS)],
        preorder=(False, SS_TS),
    ),
    make_expected_dict(
        ssku='ssku.enabled.in.table.to.be.disabled.and.preorder',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=[(True, MARKET_STOCK, SS_TS)],
        preorder=(True, SS_TS),
    ),
    make_expected_dict(
        ssku='ssku.disabled.in.table.to.be.enabled.and.preorder',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=[(False, MARKET_STOCK, SS_TS)],
        preorder=(True, SS_TS),
    ),
    make_expected_dict(
        ssku='ssku.enabled.in.table.not.to.be.disabled.with.older.ts.and.preorder',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=[(False, MARKET_STOCK, FUTURE_TS)],
        preorder=(True, SS_TS),
    ),
    make_expected_dict(
        ssku='ssku.disabled.in.table.not.to.be.enabled.and.preorder.with.older.ts',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=[(False, MARKET_STOCK, SS_TS)],
        preorder=(False, FUTURE_TS),
    ),
    make_expected_dict(
        ssku='ssku.to.be.enabled.but.absent.in.storage',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=[(False, MARKET_STOCK, SS_TS), (True, MARKET_IDX, SS_TS)],
        preorder=(False, SS_TS),
    ),
    make_expected_dict(
        ssku='ssku.with.stock.count',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=None,
        preorder=None,
        available=(5, SS_TS),
    ),
])
def test_stock_storage_update_offer(workflow, piper, expected):
    """
    Проверяем, что piper читает стоки из топика и корректно обновляет/создает офферы
    """
    assert_that(piper.actual_service_offers_table.data, HasOffers([message_from_data(expected, DatacampOffer())]))


@pytest.mark.parametrize("expected", [
    make_expected_dict(
        ssku='ssku.to.be.disabled',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=[(True, MARKET_STOCK, SS_TS)],
        preorder=(False, SS_TS),
    ),
    make_expected_dict(
        ssku='ssku.with.stock.count',
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        disabled_list=None,
        preorder=None,
        available=(5, SS_TS),
    ),
])
def test_stock_storage_create_united_offer(workflow, piper, expected):
    """
    Проверяем, что piper:
      - создает офферы со скрытием и предзаказом в ActualServiceOffers
      - создает пустой оффер в BasicOffers
      - создает офферы в ServiceOffers
    """
    assert_that(piper.service_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID,
            'shop_id': SHOP_ID,
            'offer_id': expected['identifiers']['offer_id'],
        }
    }, DatacampOffer())]))
    assert_that(piper.actual_service_offers_table.data, HasOffers([message_from_data(
        expected, DatacampOffer())]))
    assert_that(piper.basic_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': BUSINESS_ID,
            'offer_id': expected['identifiers']['offer_id'],
        },
        'status': {
            'disabled': [],
        },
    }, DatacampOffer())]))
