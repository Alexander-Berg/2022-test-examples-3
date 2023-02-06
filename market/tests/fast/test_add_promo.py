# coding: utf-8

import pytest
import copy
from hamcrest import assert_that, equal_to
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.proto.promo.Promo_pb2 import PromoDescription, PromoType
from market.idx.datacamp.proto.api.SyncGetPromo_pb2 import UpdatePromoBatchRequest, UpdatePromoBatchResponse
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampPromoRows
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from protobuf_to_dict import protobuf_to_dict
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage


@pytest.fixture(scope='module')
def output_loyalty_topic(config, log_broker_stuff):
    topic = LbkTopic(log_broker_stuff, topic=config.loyalty_promos_topic)
    return topic


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        promo_table,
        output_loyalty_topic,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            promo_table=promo_table,
            loyalty_promos_topic=output_loyalty_topic
    ) as stroller_env:
        yield stroller_env


def patch_metas(promo):
    fields_with_meta = ('promo_general_info', 'constraints', 'responsible', 'promotion', 'mechanics_data', 'additional_info')

    o = copy.deepcopy(promo)
    for f in fields_with_meta:
        if f in o and 'meta' in o[f]:
            ts = create_timestamp_from_json(o[f]['meta']['timestamp'])
            o[f]['meta'] = {'timestamp': {'seconds': ts.seconds}}
    return o


def test_add_and_update_promo(stroller, output_loyalty_topic):
    def check_promo_in_output_topic(expected_promo):
        data = output_loyalty_topic.read(count=1)
        message = DatacampMessage()
        message.ParseFromString(data[0])
        datacamp_message_dict_from_loyalty_topic = protobuf_to_dict(message)
        assert datacamp_message_dict_from_loyalty_topic == {'promos': {'promo': [expected_promo]}}

    meta = {'timestamp': '2020-12-01T19:27:36Z'}
    promo = {
        'primary_key': {
            'business_id': 2,
            'source': 2,
            'promo_id': '1',
        },
        'promo_general_info': {
            'meta': meta,
            'promo_type': PromoType.BLUE_CASHBACK,
        },
        'mechanics_data': {
            'meta': meta,
            'market_bonus': {
                'description': 'Some description'
            }
        },
    }
    message = message_from_data(promo, PromoDescription())
    expected_promo = patch_metas(promo)
    # Закидываем акцию в ручку
    response = stroller.post(path='/v1/add_promo', data=message.SerializeToString())

    assert_that(response, HasStatus(200))
    assert_that(len(stroller.promo_table.data), equal_to(1), 'Incorrect number of promos in a promo table.')
    # Проверяем, что всё правильно в таблице
    assert_that(stroller.promo_table.data,
                HasDatacampPromoRows(
                    [
                        {
                            'business_id': 2,
                            'source': 2,
                            'promo_id': '1',
                            'promo': IsSerializedProtobuf(PromoDescription, expected_promo),
                        }
                    ]),
                'Missing promos')

    # Проверяем выходной топик
    check_promo_in_output_topic(expected_promo)

    # Готовим update
    meta_update = {'timestamp': '2020-12-02T19:27:36Z'}
    promo_update = {
        'primary_key': {
            'business_id': 2,
            'source': 2,
            'promo_id': '1',
        },
        'mechanics_data': {
            'meta': meta_update,
            'market_bonus': {
                'description': 'Some NEW !!! description'
            }
        },
    }
    message = message_from_data(promo_update, PromoDescription())
    expected_promo = copy.deepcopy(promo)
    expected_promo.update(promo_update)
    expected_promo = patch_metas(expected_promo)
    # Закидываем update
    response = stroller.post(path='/v1/add_promo', data=message.SerializeToString())
    assert_that(response, HasStatus(200))

    # Проверяем, что в таблице обновились данные
    assert_that(stroller.promo_table.data,
                HasDatacampPromoRows(
                    [
                        {
                            'business_id': 2,
                            'source': 2,
                            'promo_id': '1',
                            'promo': IsSerializedProtobuf(PromoDescription, expected_promo),
                        }
                    ]),
                'Missing promos')

    # Проверяем выходной топик
    check_promo_in_output_topic(expected_promo)


def test_update_promo_batch(stroller, output_loyalty_topic):
    business_id = 3
    promos = [{
        'primary_key': {
            'business_id': 3,
            'source': 1,
            'promo_id': promo_id,
        },
        'promo_general_info': {
            'meta': {'timestamp': '2020-12-01T19:27:36Z'},
            'promo_type': PromoType.BLUE_CASHBACK,
        },
    } for promo_id in ['1', '2', '3']]
    message = message_from_data({
        'promos': {
            'promo': promos
        }
    }, UpdatePromoBatchRequest())

    # Пишем
    response = stroller.post(path='/v1/partners/{}/promos'.format(business_id), data=message.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(UpdatePromoBatchResponse, {
        'updated_promos': {
            'promo': [patch_metas(promo) for promo in promos]
        }
    }))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))

    # Проверяем наличие в таблице
    assert_that(
        stroller.promo_table.data,
        HasDatacampPromoRows(
            [
                {
                    'business_id': 3,
                    'source': 1,
                    'promo_id': promo['primary_key']['promo_id'],
                    'promo': IsSerializedProtobuf(PromoDescription, patch_metas(promo)),
                } for promo in promos
            ]),
        'Missing promos'
    )

    # Проверяем выходной топик
    data = output_loyalty_topic.read(count=1)
    message = DatacampMessage()
    message.ParseFromString(data[0])
    datacamp_message_dict_from_loyalty_topic = protobuf_to_dict(message)
    # Порядок в батче не определён, поэтому не сравниваем на равенство
    assert list(datacamp_message_dict_from_loyalty_topic.keys()) == ['promos']
    assert len(datacamp_message_dict_from_loyalty_topic['promos']['promo']) == 3

    patched_promos = [patch_metas(p) for p in promos]
    for promo_from_topic in datacamp_message_dict_from_loyalty_topic['promos']['promo']:
        assert promo_from_topic in patched_promos
