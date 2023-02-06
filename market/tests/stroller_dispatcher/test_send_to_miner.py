import pytest
from hamcrest import assert_that, equal_to

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row, offer_to_service_row
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.SyncChangeOffer_pb2 import FullOfferResponse
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.datacamp.controllers.stroller.yatf.utils import assert_miner_topic_data, DisabledFlag, shops_request, prepare_expected_with_flags, request_with_price
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller


BUSINESS_ID = 1000
SHOP_ID = 1
WAREHOUSE_ID = 6000

OFFERS = [
    (
        'T1001',
        DTC.AVAILABLE,
        None
    ),
]


@pytest.fixture(scope='module')
def partners():
    return [
    {
        'shop_id': SHOP_ID,
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': SHOP_ID,
                'warehouse_id': WAREHOUSE_ID,
                'datafeed_id': 100,
                'business_id': BUSINESS_ID,
                'is_discounts_enabled': 'true',
            }),
        ]),
    }
]


@pytest.fixture(scope='module')
def basic_offers():
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=offer_id,
            ),
            meta=create_meta(10, scope=DTC.BASIC),
        )) for offer_id, _, _ in OFFERS
    ]


@pytest.fixture(scope='module')
def service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=offer_id,
                shop_id=SHOP_ID,
                warehouse_id=0,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
            price=price,
            status=DTC.OfferStatus(
                publish_by_partner=status
            ) if status else None,
        )) for offer_id, status, price in OFFERS
    ]


@pytest.fixture(scope='module')
def actual_service_offers():
    return [
        offer_to_service_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=BUSINESS_ID,
                offer_id=offer_id,
                shop_id=SHOP_ID,
                warehouse_id=WAREHOUSE_ID,
            ),
            meta=create_meta(10, color=DTC.BLUE, scope=DTC.SERVICE),
            status=DTC.OfferStatus(
                publish=status,
            ) if status else None
        )) for offer_id, status, _ in OFFERS
    ]


@pytest.fixture(scope='module')
def miner_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'topic-to-miner')
    return topic


@pytest.fixture(scope='module')
def dispatcher_config(
    yt_token,
    yt_server,
    log_broker_stuff,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    miner_topic,
    subscription_service_topic,
):
    cfg = DispatcherConfig()
    cfg.create_initializer(yt_server=yt_server, yt_token_path=yt_token.path, extra_params={
        'SubscriptionOverrides': {
            'MINER_SUBSCRIBER': {
                'price.basic.binary_price': 'ST_EXISTENCE_TRIGGER'
            }
        }
    })

    reader = cfg.create_lb_reader(log_broker_stuff, subscription_service_topic)
    unpacker = cfg.create_subscription_message_unpacker()
    dispatcher = cfg.create_subscription_dispatcher(
        basic_offers_table.table_path, service_offers_table.table_path, actual_service_offers_table.table_path
    )
    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, dispatcher)

    filter = cfg.create_subscription_filter('MINER_SUBSCRIBER', extra_params={
        'Mode': 'MIXED',
        'Color': 'UNKNOWN_COLOR;WHITE;BLUE;TURBO;LAVKA;EDA;DIRECT;DIRECT_SITE_PREVIEW;DIRECT_STANDBY;DIRECT_GOODS_ADS;DIRECT_SEARCH_SNIPPET_GALLERY',
    })
    enricher = cfg.create_miner_enricher()
    sender = cfg.create_subscription_sender()
    lb_writer = cfg.create_lb_writer(log_broker_stuff, miner_topic)

    cfg.create_link(dispatcher, filter)
    cfg.create_link(filter, enricher)
    cfg.create_link(enricher, sender)
    cfg.create_link(sender, lb_writer)

    return cfg


@pytest.yield_fixture(scope='module')
def dispatcher(
    dispatcher_config,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    subscription_service_topic,
    miner_topic
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'subscription_service_topic': subscription_service_topic,
        'miner_topic': miner_topic
    }

    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table,
        basic_offers_table,
        service_offers_table,
        actual_service_offers_table,
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
    ) as stroller_env:
        yield stroller_env


def test_single_push(dispatcher, stroller, miner_topic):
    '''Проверяет:
    - офер выключается и включается при добавлении одного флага
    - пуш источники равноправны и пишутся поверх друг друга
    - оффер отправляется на переобогащение в miner
    '''
    processed_before_update = dispatcher.subscription_dispatcher_processed
    disabled = DisabledFlag(flag=True, source=DTC.PUSH_PARTNER_OFFICE, timestamp='2019-02-15T15:55:55Z')
    response = shops_request(stroller, shop_id=SHOP_ID, offer_id='T1001', warehouse_id=WAREHOUSE_ID, flag=disabled)
    assert_that(response, HasStatus(200))
    expected_data = prepare_expected_with_flags(SHOP_ID, 'T1001', WAREHOUSE_ID, [disabled])
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))

    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= processed_before_update + 1, timeout=10)
    data = miner_topic.read(count=1, wait_timeout=60)
    assert_miner_topic_data(data, BUSINESS_ID, SHOP_ID, 'T1001', WAREHOUSE_ID)

    enabled = DisabledFlag(flag=False, source=DTC.PUSH_PARTNER_OFFICE, timestamp='2019-02-15T16:11:11Z')
    response = shops_request(stroller, shop_id=SHOP_ID, offer_id='T1001', warehouse_id=WAREHOUSE_ID, flag=enabled)
    assert_that(response, HasStatus(200))
    expected_data = prepare_expected_with_flags(SHOP_ID, 'T1001', WAREHOUSE_ID, flags=[enabled])
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))


def test_send_offer_to_miner_after_creation(dispatcher, stroller, miner_topic):
    """Проверяем, что после создания, оффер записан в топик, который читает miner"""
    processed_before_update = dispatcher.subscription_dispatcher_processed
    disabled = DisabledFlag(flag=True, source=DTC.PUSH_PARTNER_OFFICE, timestamp='2019-02-15T15:55:55Z')
    response = shops_request(
        stroller, shop_id=SHOP_ID, offer_id='NewOffer02', warehouse_id=WAREHOUSE_ID, flag=disabled,
        send_ids_only_by_uri=True, send_disable_only_by_uri=True
    )
    assert_that(response, HasStatus(200))
    expected_data = prepare_expected_with_flags(
        SHOP_ID, 'NewOffer02', WAREHOUSE_ID, [disabled],
        send_disable_only_by_uri=True
    )
    assert_that(response.data, IsSerializedProtobuf(FullOfferResponse, expected_data))

    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= processed_before_update + 1, timeout=10)
    data = miner_topic.read(count=1, wait_timeout=60)
    assert_miner_topic_data(data, BUSINESS_ID, SHOP_ID, 'NewOffer02', WAREHOUSE_ID)


def test_send_offer_to_miner_after_price_update(dispatcher, stroller, miner_topic):
    """Проверяем, что после изменения цены офер записал в топик, который читает miner"""
    processed_before_update = dispatcher.subscription_dispatcher_processed
    source = DTC.PUSH_PARTNER_API
    timestamp = '2019-04-18T15:58:00Z'
    shop_id = SHOP_ID
    offer_id = 'T1001'
    price = 6000000000

    response = request_with_price(stroller, shop_id=shop_id, offer_id=offer_id, warehouse_id=WAREHOUSE_ID, price=price, source=source, ts=timestamp)
    assert_that(response, HasStatus(200))

    # Проверяем, что оффер отправлен в miner
    wait_until(lambda: dispatcher.subscription_dispatcher_processed >= processed_before_update + 1, timeout=10)
    data = miner_topic.read(count=1, wait_timeout=60)
    assert_miner_topic_data(data, BUSINESS_ID, shop_id, offer_id, WAREHOUSE_ID)
