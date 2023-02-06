import pytest

from hamcrest import assert_that, has_length, equal_to
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.matchers.yt_rows_matchers import HasOffers
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.datacamp.dispatcher.yatf.resources.config import DispatcherConfig
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampServiceOffersTable
from market.idx.datacamp.controllers.stroller.yatf.utils import expected_offer, request

TIMESTAMP = '2021-08-01T15:55:55Z'
BLOCKED_BUSINESS = 1000


@pytest.fixture(scope='module')
def promo_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, 'topic-to-promo')
    return topic


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


@pytest.fixture(scope='module')
def dispatcher_config(
    yt_token,
    yt_server,
    log_broker_stuff,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    promo_topic,
    subscription_service_topic,
):
    cfg = DispatcherConfig()
    cfg.create_initializer(yt_server=yt_server, yt_token_path=yt_token.path)

    reader = cfg.create_lb_reader(log_broker_stuff, subscription_service_topic)
    unpacker = cfg.create_subscription_message_unpacker(service_select_subscribers='CMI_PROMO')
    dispatcher = cfg.create_subscription_dispatcher(
        basic_offers_table.table_path, service_offers_table.table_path, actual_service_offers_table.table_path
    )
    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, dispatcher)

    filter = cfg.create_subscription_filter('CMI_PROMO_SUBSCRIBER', extra_params={
        'Mode': 'MIXED',
        'ApplyBasicPriceToService': True,
    })
    sender = cfg.create_subscription_sender()
    lb_writer = cfg.create_lb_writer(log_broker_stuff, promo_topic)

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
    subscription_service_topic,
    promo_topic
):
    resources = {
        'dispatcher_config': dispatcher_config,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'subscription_service_topic': subscription_service_topic,
        'promo_topic': promo_topic
    }

    with DispatcherTestEnv(**resources) as env:
        env.verify()
        yield env


def test_set_united_offer_with_promo(yt_server, dispatcher, stroller, config, promo_topic):
    service_offers_table = DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath)
    service_offers_table.load()
    service_count_before_update = len(service_offers_table.data)

    timestamp = '2019-02-15T15:55:55Z'
    warehouse_id = None

    basic_offer = expected_offer(
        business_id=1,
        offer_id='o1',
        shop_id=2,
        warehouse_id=warehouse_id,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=timestamp,
        price=None,
        scope=DTC.BASIC,
    )
    response = request(stroller, '/v1/partners/{business_id}/offers/basic?offer_id={offer_id}', basic_offer, timestamp)
    assert_that(response, HasStatus(200))

    anaplan_promos = [{'id': 'promo_1'}]
    disabled_flag = {
        'meta': {
            'source': DTC.MARKET_IDX
            },
        'flag': False
    }

    timestamp = '2019-02-15T17:55:55Z'
    service_offer = expected_offer(
        business_id=1,
        offer_id='o1',
        shop_id=2,
        warehouse_id=warehouse_id,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=timestamp,
        price=None,
        scope=DTC.SERVICE,
        status=DTC.HIDDEN,
        anaplan_promos=anaplan_promos,
        disabled_flag=disabled_flag
    )
    service_offer['meta']['rgb'] = DTC.BLUE
    response = request(stroller, '/v1/partners/{business_id}/offers/services/{service_id}?offer_id={offer_id}', service_offer, timestamp)
    assert_that(response, HasStatus(200))

    meta = service_offer['meta']
    meta['scope'] = DTC.SERVICE
    meta['ts_created'] = timestamp

    service_offers_table.load()
    assert_that(len(service_offers_table.data), equal_to(service_count_before_update + 1))

    assert_that(service_offers_table.data, HasOffers([
        message_from_data({
            'identifiers': {
                'business_id': 1,
                'offer_id': 'o1',
                'shop_id': 2,
                'extra': {
                    'recent_business_id': 1,
                }
            },
            'meta': meta,
            'promos': {
                'anaplan_promos': {
                    'all_promos': {
                        'promos': anaplan_promos
                    }
                }
            },
        }, DTC.Offer())
    ]))

    # Check messages in promo topic.
    promo_messages = promo_topic.read(count=1)
    assert_that(promo_messages, has_length(1))

    assert_that(promo_messages[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    2: {
                        'identifiers': service_offer['identifiers'],
                        'promos': service_offer['promos'],
                    }
                })
            }]
        }]
    }))

    # Уже есть базовый оффер и сервисный (одного united offer-а). Присылаем обновление цены в базовой части
    basic_offer = expected_offer(
        business_id=1,
        offer_id='o1',
        shop_id=2,
        warehouse_id=warehouse_id,
        source=DTC.PUSH_PARTNER_OFFICE,
        ts=timestamp,
        price=300,
        scope=DTC.BASIC,
    )
    response = request(stroller, '/v1/partners/{business_id}/offers/basic?offer_id={offer_id}', basic_offer, timestamp)
    assert_that(response, HasStatus(200))

    # Ищем сообщение в топике promo
    promo_messages = promo_topic.read(count=1)
    assert_that(promo_messages, has_length(1))

    # Ожидаем, что сработает дозачитывание (параметр service_select_subscribers)
    #  и цена применится к сервисным частям (параметр ApplyBasicPriceToService=true)
    assert_that(promo_messages[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': basic_offer['identifiers']['business_id'],
                        'offer_id': basic_offer['identifiers']['offer_id'],
                    },
                },
                'service': IsProtobufMap({
                    2: {
                        'identifiers': service_offer['identifiers'],
                        # Включена опция ApplyBasicPriceToService - цена скопировалась из базовой части
                        'price': basic_offer['price'],
                    }
                })
            }]
        }]
    }))
