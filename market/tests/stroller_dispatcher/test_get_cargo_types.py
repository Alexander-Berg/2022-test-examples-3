# coding: utf-8

import pytest
import six
import time
from hamcrest import assert_that, equal_to

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.api.SyncCommon_pb2 as SyncCommon

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.utils import create_meta, create_update_meta, dict2tskv
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row

from market.idx.yatf.test_envs.saas_env import SaasEnv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf

BUSINESS_ID = 1
BUSINESS_ID_NO_CARGO_TYPES = 2
BUSINESS_ID_MULTIPLE_OFFERS = 7
OFFER_ID_1 = 'test_offer_1'
OFFER_ID_2 = 'test_offer_2'
SHOP_ID = 3


@pytest.fixture(scope='module')
def partners(config):
    return [
        {
            'shop_id': SHOP_ID,
            'mbi': '\n\n'.join([
                dict2tskv({
                    'shop_id': SHOP_ID,
                    'business_id': business_id,
                    'united_catalog_status': 'SUCCESS',
                }),
            ]),
            'status': 'publish'
        }
    for business_id in [BUSINESS_ID, BUSINESS_ID_NO_CARGO_TYPES, BUSINESS_ID_MULTIPLE_OFFERS]]


@pytest.fixture(scope='module')
def basic_offers():
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=business_id,
                offer_id=OFFER_ID_1,
            ),
            price=DTC.OfferPrice(
                basic=DTC.PriceBundle(
                    binary_price=DTC.PriceExpression(price=10),
                    meta=create_update_meta(10)
                )),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        vendor=DTC.StringValue(
                            value="Old vendor",
                            meta=create_update_meta(10)
                        ),
                    )
                )),
            meta=create_meta(10),
        )) for business_id in [BUSINESS_ID, BUSINESS_ID_NO_CARGO_TYPES, BUSINESS_ID_MULTIPLE_OFFERS]
    ] + [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID_MULTIPLE_OFFERS,
                offer_id=OFFER_ID_2,
            ),
            price=DTC.OfferPrice(
                basic=DTC.PriceBundle(
                    binary_price=DTC.PriceExpression(price=10),
                    meta=create_update_meta(10)
                )),
            content=DTC.OfferContent(
                partner=DTC.PartnerContent(
                    original=DTC.OriginalSpecification(
                        vendor=DTC.StringValue(
                            value="Old vendor",
                            meta=create_update_meta(10)
                        ),
                    )
                )),
            meta=create_meta(10),
        ))
    ]


@pytest.fixture(scope='module')
def service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=business_id,
                offer_id=OFFER_ID_1,
                shop_id=SHOP_ID,
                warehouse_id=0,
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
            status=DTC.OfferStatus(
                publish_by_partner=DTC.AVAILABLE,
                united_catalog=DTC.Flag(
                    flag=True,
                    meta=create_update_meta(10)
                )
            )
        )) for business_id in [BUSINESS_ID, BUSINESS_ID_NO_CARGO_TYPES, BUSINESS_ID_MULTIPLE_OFFERS]
    ] + [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID_MULTIPLE_OFFERS,
                offer_id=OFFER_ID_2,
                shop_id=SHOP_ID,
                warehouse_id=0,
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
            status=DTC.OfferStatus(
                publish_by_partner=DTC.AVAILABLE,
                united_catalog=DTC.Flag(
                    flag=True,
                    meta=create_update_meta(10)
                )
            )
        )),
    ]


@pytest.fixture(scope='module')
def actual_service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=business_id,
                offer_id=OFFER_ID_1,
                shop_id=SHOP_ID,
                warehouse_id=0,
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
            status=DTC.OfferStatus(
                publish=DTC.AVAILABLE,
            )
        )) for business_id in [BUSINESS_ID, BUSINESS_ID_NO_CARGO_TYPES, BUSINESS_ID_MULTIPLE_OFFERS]
    ] + [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID_MULTIPLE_OFFERS,
                offer_id=OFFER_ID_2,
                shop_id=SHOP_ID,
                warehouse_id=0,
            ),
            meta=create_meta(10, scope=DTC.SERVICE),
            status=DTC.OfferStatus(
                publish=DTC.AVAILABLE,
            )
        ))
    ]


@pytest.fixture(scope='module')
def saas():
    with SaasEnv(cluster_config='cluster_1be_internal.cfg', config_patch={'Server.Components': ['INDEX,DDK,MAKEUP']}) as saas:
        yield saas


@pytest.fixture(scope='module')
def dispatcher_config(
    yt_token,
    yt_server,
    log_broker_stuff,
    subscription_service_topic,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    saas
):
    cfg = DispatcherConfig()
    cfg.create_initializer(
        yt_server=yt_server,
        yt_token_path=yt_token.path
    )

    lb_reader = cfg.create_lb_reader(log_broker_stuff, subscription_service_topic)
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        basic_offers_table.table_path,
        service_offers_table.table_path,
        actual_service_offers_table.table_path
    )
    filter = cfg.create_subscription_filter('SAAS_SUBSCRIBER', extra_params={
        'Mode': 'ORIGINAL',
        'UseActualServiceFields': True,
        'OneServicePerUnitedOffer': False,
        'FillOnlyAffectedOffers': False,
        'IgnoreBlueOffersWithoutContent': True,
        'EnableIntegralStatusTrigger': True,
        'Color': 'UNKNOWN_COLOR;WHITE;BLUE',
    })
    converter = cfg.create_united_saas_docs_converter(
        service_offers_table.table_path,
        actual_service_offers_table.table_path
    )
    sender = cfg.create_united_saas_sender(saas)

    cfg.create_link(lb_reader, unpacker)
    cfg.create_link(unpacker, dispatcher)
    cfg.create_link(dispatcher, filter)
    cfg.create_link(filter, converter)
    cfg.create_link(converter, sender)

    return cfg


@pytest.yield_fixture(scope='module')
def dispatcher(
        dispatcher_config,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        subscription_service_topic,
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'subscription_service_topic': subscription_service_topic,
    }
    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


@pytest.yield_fixture()
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
        saas
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            shopsdat_cacher=True,
            partners_table=partners_table,
            basic_offers_table=basic_offers_table,
            service_offers_table=service_offers_table,
            actual_service_offers_table=actual_service_offers_table,
            local_saas=saas
    ) as stroller_env:
        yield stroller_env


def _get_cargo_types(stroller, business_id, shop_id):
    response = stroller.get('/v1/partners/{}/offers/services/{}/cargotypes'.format(business_id, shop_id))
    assert_that(response, HasStatus(200))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))
    return response


def _send_cargo_types_update_to_saas(stroller, business_id, offer_id, shop_id, cargo_types):

    def _create_cargo_types(cargo_types, update_ts=None):
        result = DTC.I32ListValue(
            meta=create_update_meta(update_ts) if update_ts else None
        )
        for cargo_type in cargo_types:
            result.value.append(cargo_type)
        return result

    offer = DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=business_id,
            offer_id=offer_id,
            shop_id=shop_id,
            warehouse_id=0
        ),
        meta=create_meta(10, scope=DTC.BASIC),
        content=DTC.OfferContent(
            master_data=DTC.MarketMasterData(
                cargo_type=_create_cargo_types(cargo_types=cargo_types, update_ts=20)
            )
        )
    )
    response = stroller.post('/v1/partners/{}/offers/basic?offer_id={}'.format(business_id, offer_id), data=offer.SerializeToString())
    assert_that(response, HasStatus(200))
    time.sleep(5)  # подождем немного, чтобы доки добавились в Saas


def test_default_shop_with_cargo_types(dispatcher, stroller):
    """Проверяем самый простой случай оффера с некоторым набором карготипов"""

    _send_cargo_types_update_to_saas(
        stroller=stroller,
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        offer_id=OFFER_ID_1,
        cargo_types=[6, 7, 8, 9, 10]
    )
    response = _get_cargo_types(stroller, business_id=BUSINESS_ID, shop_id=SHOP_ID)
    assert_that(response.data, IsSerializedProtobuf(SyncCommon.GetInt32ListValueResponse, {'values': [6, 7, 8, 9, 10]}))


def test_shop_with_no_cargo_types(dispatcher, stroller):
    """Проверяем оффер с пустым списком карготипов"""

    _send_cargo_types_update_to_saas(
        stroller=stroller,
        business_id=BUSINESS_ID_NO_CARGO_TYPES,
        shop_id=SHOP_ID,
        offer_id=OFFER_ID_1,
        cargo_types=[]
    )
    response = _get_cargo_types(stroller, business_id=BUSINESS_ID_NO_CARGO_TYPES, shop_id=SHOP_ID)
    assert_that(six.ensure_str(response.data), equal_to(''))


def test_multiple_offers_in_shop(dispatcher, stroller):
    """
    Проверяем магазин с несколькими офферами с разными карготипами.
    Должны получить их все.
    """

    _send_cargo_types_update_to_saas(
        stroller=stroller,
        business_id=BUSINESS_ID_MULTIPLE_OFFERS,
        shop_id=SHOP_ID,
        offer_id=OFFER_ID_1,
        cargo_types=[5, 6, 7]
    )
    _send_cargo_types_update_to_saas(
        stroller=stroller,
        business_id=BUSINESS_ID_MULTIPLE_OFFERS,
        shop_id=SHOP_ID,
        offer_id=OFFER_ID_2,
        cargo_types=[8, 9]
    )

    response = _get_cargo_types(stroller, business_id=BUSINESS_ID_MULTIPLE_OFFERS, shop_id=SHOP_ID)
    assert_that(response.data, IsSerializedProtobuf(SyncCommon.GetInt32ListValueResponse, {'values': [5, 6, 7, 8, 9]}))


def test_invalid_shop_id(dispatcher, stroller):
    """Проверяем запрос для несуществующего id магазина"""

    invalid_shop_id = 1234
    response = _get_cargo_types(stroller, business_id=BUSINESS_ID, shop_id=invalid_shop_id)
    assert_that(six.ensure_str(response.data), equal_to(''))


def test_invalid_business_id(dispatcher, stroller):
    """Проверяем запрос для несуществующего id бизнеса"""

    invalid_business_id = 12345
    response = _get_cargo_types(stroller, business_id=invalid_business_id, shop_id=SHOP_ID)
    assert_that(six.ensure_str(response.data), equal_to(''))
