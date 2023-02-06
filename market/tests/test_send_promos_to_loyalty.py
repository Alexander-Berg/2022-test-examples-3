# coding: utf-8

import pytest
import copy

from protobuf_to_dict import protobuf_to_dict
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.promo.Promo_pb2 import PromoType, PromoPromotion
from market.proto.common.promo_pb2 import ESourceType
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data
from market.idx.yatf.utils.utils import create_timestamp_from_json

META_0 = {'timestamp': '2020-12-01T19:27:36Z'}
META_1 = {'timestamp': '2020-12-02T19:27:36Z'}

PROMO = {
    'primary_key': {
        'promo_id': '1',
        'business_id': 2,
        'source': ESourceType.PARTNER_SOURCE
    },
    'promo_general_info': {
        'meta': META_0,
        'promo_type': PromoType.PRICE_DROP,
    },
    'constraints': {
        'meta': META_0,
        'enabled': True,
        'start_date': 1606775557
    },
    'responsible': {
        'meta': META_0,
        'author': 'Medved'
    },
    'promotion': {
        'meta': META_0,
        'channel': [PromoPromotion.Channel.CATEGORY_ORDINARY_BANNER, PromoPromotion.Channel.PRODUCT_OF_DAY]
    },
    'mechanics_data': {
        'meta': META_0,
        'market_bonus': {
            'description': 'Some description'
        }
    },
    'additional_info': {
        'meta': META_0,
        'name': 'Super discount',
    }
}


def patch_metas(promo):
    fields_with_meta = ('promo_general_info', 'constraints', 'responsible', 'promotion', 'mechanics_data', 'additional_info')

    o = copy.deepcopy(promo)
    for f in fields_with_meta:
        if f in o and 'meta' in o[f]:
            ts = create_timestamp_from_json(o[f]['meta']['timestamp'])
            o[f]['meta'] = {'timestamp': {'seconds': ts.seconds}}
    return o


@pytest.fixture(scope='module')
def promos_datacamp_messages():
    return [message_from_data({'promos': {'promo': [PROMO]}}, DatacampMessage())]


# Входной для пайпера топик!
@pytest.fixture(scope='module')
def promos_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


# А вот это выходной топик. Не путать!
@pytest.fixture(scope='module')
def output_loyalty_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, promos_topic, output_loyalty_topic):
    cfg = {
        'logbroker': {
            'promos_topic': promos_topic.topic,
            'loyalty_promos_topic': output_loyalty_topic.topic
        },
        'general': {
            'color': 'white',
        }
    }

    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, promos_topic, output_loyalty_topic):
    resources = {
        'config': config,
        'promos_topic': promos_topic,
        'loyalty_promos_topic': output_loyalty_topic
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='function')
def main_promos_inserter(promos_datacamp_messages, piper, promos_topic):
    for message in promos_datacamp_messages:
        promos_topic.write(message.SerializeToString())

    wait_until(lambda: piper.promo_processed >= len(promos_datacamp_messages), timeout=60)


def test_write_promos_to_loyalty_topic(main_promos_inserter, piper, output_loyalty_topic):
    data = output_loyalty_topic.read(count=1)
    message = DatacampMessage()
    message.ParseFromString(data[0])
    actual = protobuf_to_dict(message)
    expected = {'promos': {'promo': [patch_metas(PROMO)]}}

    # Раскомментировать в случае ядерной бомбардировки
    # print(actual)
    # print(expected)
    assert actual == expected


def test_write_promos_to_loyalty_topic_after_update(main_promos_inserter, promos_topic, piper, output_loyalty_topic):
    promo_update = {
        'primary_key': {
            'promo_id': '1',
            'business_id': 2,
            'source': ESourceType.PARTNER_SOURCE
        },
        'responsible': {
            'meta': META_1,
            'author': 'Volk'
        },
    }
    # Записываем обновление
    message = message_from_data({'promos': {'promo': [promo_update]}}, DatacampMessage())
    promos_topic.write(message.SerializeToString())
    # Ждём когда будут обработано обновление
    wait_until(lambda: piper.promo_processed >= 2, timeout=60)

    expected_promo = copy.deepcopy(PROMO)
    expected_promo.update(promo_update)
    # Ожидаем, что в выходном топике будет и акция, и обновление
    expected = {'promos': {'promo': [patch_metas(expected_promo)]}}

    data = output_loyalty_topic.read(count=2)
    message = DatacampMessage()
    message.ParseFromString(data[1])
    actual = protobuf_to_dict(message)

    # Раскомментировать в случае ядерной бомбардировки
    # print(actual)
    # print(expected)
    assert actual == expected
