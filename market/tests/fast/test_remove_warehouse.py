# coding: utf-8
'''
Тест на проверку ручки stroller удаления склада у магазина.
Пример вызова ручки:
curl -X POST --header "X-Ya-Service-Ticket: $(cat tvm_ticket)" \
    "http://datacamp.white.tst.vs.market.yandex.net/v1/partners/${BUSINESS_ID}/services/${SHOP_ID}/remove_warehouse?format=json&warehouse_id=${WH_ID}"

, где
    business_id - id партнёра,
    shop_id - id магазина,
    warehouse_id - id склада, который удаляем

В результате работы ручки мы должны отправить в топик следующие данные:
identifiers - различные индентификаторы оффера у которого мы удаляем склад
status - поле с флагом removed и meta, ult
    removed - это пометка для рутин, чтобы они прошлись и удалили актуальную сервисную часть.
        Если она проставлена в True, то мы не берём этот оффер в следующее поколение.
    meta - meta инфа операции проставления флага (кто и когда это сделал).
stock_info - поле с полем partner_stocks внутри. У partner_stocks есть 2 поля: count и meta.
    сount - это количество товара на складе. Мы его долны обнулить в этой ручке. Это нужно, чтобы доехали быстроданные.
    meta - meta инфа операции проставления флага (кто и когда это сделал).
'''


import pytest
import six
from hamcrest import (
    assert_that,
    has_length,
    equal_to,
    has_entries,
    has_entry,
    has_property,
    has_key,
    greater_than
)
from protobuf_to_dict import protobuf_to_dict


import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.proto.common.common_pb2 import EComponent
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus, HasSerializedDatacampMessages
from market.idx.datacamp.yatf.utils import dict2tskv, create_meta, create_update_meta
from market.idx.pylibrary.datacamp.conversion import offer_to_service_row

HANDLE_VERSION = 'v1'

BUSINESS_ID = 1
SHOP_ID = 1

OFFER_ID_FOR_REMOVE = 'o1'
OFFER_ID_FOR_KEEP = 'o2'

FEED_ID_FOR_REMOVE = 10
FEED_ID_FOR_KEEP = 20

WAREHOUSE_ID_FOR_REMOVE = 145
WAREHOUSE_ID_FOR_KEEP = 147

PARTNERS_TABLE_DATA = [
    {
        'shop_id': 1,
        'mbi': '\n\n'.join([
            dict2tskv({
                'shop_id': SHOP_ID,
                'warehouse_id': WAREHOUSE_ID_FOR_REMOVE,
                'datafeed_id': FEED_ID_FOR_REMOVE,
                'business_id': BUSINESS_ID
            }),
            dict2tskv({
                'shop_id': SHOP_ID,
                'warehouse_id': WAREHOUSE_ID_FOR_KEEP,
                'datafeed_id': FEED_ID_FOR_KEEP,
                'business_id': BUSINESS_ID
            })
        ])
    }
]

ACTUAL_SERVICE_OFFERS_TABLE_DATA = [
    offer_to_service_row(DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=offer['business_id'],
            offer_id=offer['offer_id'],
            shop_id=SHOP_ID,
            feed_id=offer['feed_id'],
            warehouse_id=offer['warehouse_id']
        ),
        stock_info=DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                count=100,
                meta=create_update_meta(10, source=DTC.MARKET_MBI),
            ),
        ),
        meta=create_meta(10, scope=DTC.SERVICE),
    )) for offer in [
        {
            'business_id': BUSINESS_ID,
            'offer_id': OFFER_ID_FOR_REMOVE,
            'feed_id': FEED_ID_FOR_REMOVE,
            'warehouse_id': WAREHOUSE_ID_FOR_REMOVE
        },
        {
            'business_id': BUSINESS_ID,
            'offer_id': OFFER_ID_FOR_KEEP,
            'feed_id': FEED_ID_FOR_KEEP,
            'warehouse_id': FEED_ID_FOR_KEEP
        }
    ]
]


@pytest.fixture(scope='module')
def partners():
    return PARTNERS_TABLE_DATA


@pytest.fixture(scope='module')
def actual_service_offers():
    return ACTUAL_SERVICE_OFFERS_TABLE_DATA


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        partners_table,
        actual_service_offers_table,
        united_offers_topic,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            shopsdat_cacher=True,
            partners_table=partners_table,
            actual_service_offers_table=actual_service_offers_table,
            united_offers_topic=united_offers_topic,
    ) as stroller_env:
        yield stroller_env


@pytest.yield_fixture(scope='module')
def response_remove_warehouse(stroller):
    return stroller.post('/{ver}/partners/{business_id}/services/{shop_id}/remove_warehouse?warehouse_id={wh_id}'.format(
        business_id=BUSINESS_ID,
        shop_id=SHOP_ID,
        wh_id=WAREHOUSE_ID_FOR_REMOVE,
        ver=HANDLE_VERSION
    ))


@pytest.yield_fixture(scope='module')
def data_from_topic(response_remove_warehouse, united_offers_topic):
    return united_offers_topic.read(count=1)


@pytest.yield_fixture(scope='module')
def proto_from_topic(data_from_topic):
    '''
    Проверяем, что нам пришло одно сообщение в топик
    '''
    assert_that(data_from_topic, has_length(1))

    message = DatacampMessage()
    message.ParseFromString(data_from_topic[0])

    return message


@pytest.yield_fixture(scope='module')
def actual_part(proto_from_topic):
    '''
    Проверяем:
        - что к нам пришёл только 1 оффер на удаление;
        - у этого оффера только одна актуальная сервисная часть.
    '''
    assert_that(proto_from_topic, has_property('united_offers'))
    assert_that(proto_from_topic.united_offers, has_length(1))

    assert_that(proto_from_topic.united_offers[0], has_property('offer'))
    assert_that(proto_from_topic.united_offers[0].offer, has_length(1))

    assert_that(proto_from_topic.united_offers[0].offer[0], has_property('actual'))
    assert_that(proto_from_topic.united_offers[0].offer[0].actual, has_length(1))

    assert_that(proto_from_topic.united_offers[0].offer[0].actual, has_key(1))
    return proto_from_topic.united_offers[0].offer[0].actual[1]


@pytest.yield_fixture(scope='module')
def warehouse_from_actual_part(actual_part):
    '''
    Проверяем, что в актуальной сервисоной части есть склад, который мы хотим удалить
    '''
    assert_that(actual_part, has_property('warehouse'))
    assert_that(actual_part.warehouse, has_key(WAREHOUSE_ID_FOR_REMOVE))

    return protobuf_to_dict(actual_part.warehouse[WAREHOUSE_ID_FOR_REMOVE])


def test_basic_identifiers(data_from_topic):
    """ Проверяем, что передаются базовые идентификаторы, т.к. piper без них данные не принимает """
    assert_that(data_from_topic, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': OFFER_ID_FOR_REMOVE,
                    },
                    'meta': {
                        'scope': DTC.BASIC
                    }
                },
            }]
        }]
    }]))


def test_response_remove_warehouse(response_remove_warehouse):
    '''
    Проверяем:
        - что ручка отдаёт 200.
        - в ответе есть соббщение о том сколько оффером мы удалили с определённого склада.
    '''
    assert_that(response_remove_warehouse, HasStatus(200))
    assert_that(
        six.ensure_str(response_remove_warehouse.data),
        equal_to('{} offers were removed from warehouse {}'.format(1, WAREHOUSE_ID_FOR_REMOVE))
    )


def test_identifiers(warehouse_from_actual_part):
    '''
    Проверка поля identifiers для оффера, у которого мы хотим оторвать привязку к складу
    '''
    assert_that(warehouse_from_actual_part, has_entries({
        'identifiers': has_entries({
            'shop_id': SHOP_ID,
            'feed_id': FEED_ID_FOR_REMOVE,
            'warehouse_id': WAREHOUSE_ID_FOR_REMOVE,
            'offer_id': OFFER_ID_FOR_REMOVE,
            'business_id': BUSINESS_ID
        })
    }))


def test_status(warehouse_from_actual_part):
    '''
    Проверка поля status для оффера, у которого мы хотим оторвать привязку к складу.
    Тут обязательно должен быть проставлен флаг removed и заполнена meta
    '''
    assert_that(warehouse_from_actual_part, has_entries({
        'status': has_entries({
            'removed': has_entries({
                'flag': True,
                'meta': has_entries({
                    'source': int(DTC.PUSH_PARTNER_OFFICE),
                    'applier': int(EComponent.STROLLER),
                    'timestamp': has_entry('seconds', greater_than(0))
                })
            })
        })
    }))


def test_stock_info(warehouse_from_actual_part):
    '''
    Проверка поля stock_info для оффера, у которого мы хотим оторвать привязку к складу.
    Тут обязательно должено быть указано количество для оффера 0 (его нет на складе) и заполнена meta
    '''
    assert_that(warehouse_from_actual_part, has_entries({
        'stock_info': has_entries({
            'partner_stocks': has_entries({
                'count': 0,
                'meta': has_entries({
                    'source': int(DTC.PUSH_PARTNER_OFFICE),
                    'applier': int(EComponent.STROLLER),
                    'timestamp': has_entry('seconds', greater_than(0))
                    })
                })
            })
        }))
