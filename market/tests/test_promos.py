# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.promo.Promo_pb2 import PromoDescription, PromoType, PromoPromotion
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampPromoRows
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampPromoTable,
)
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.proto.common.promo_pb2 import ESourceType
from market.pylibrary.proto_utils import message_from_data


META_0 = {'timestamp': '2020-12-01T19:27:36Z'}
META_1 = {'timestamp': '2020-12-02T19:27:36Z'}

PROMOS = [
    {
        'primary_key': {
            'promo_id': '0'
        }
    },
    {
        'primary_key': {
            'promo_id': '0',
            'business_id': 1
        }
    },
    {
        'primary_key': {
            'promo_id': '0',
            'business_id': 2,
            'source': ESourceType.PARTNER_SOURCE
        }
    },
    {
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
            'channel': [
                PromoPromotion.Channel.CATEGORY_ORDINARY_BANNER,
                PromoPromotion.Channel.PRODUCT_OF_DAY
            ]
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
    },
]


def patch_metas(promos):
    fields_with_meta = ('promo_general_info', 'constraints', 'responsible', 'promotion', 'mechanics_data', 'additional_info')

    # Хакерство их-за timestamp-ов
    o = list(promos)
    for promo in o:
        for f in fields_with_meta:
            if f in promo and 'meta' in promo[f]:
                ts = create_timestamp_from_json(promo[f]['meta']['timestamp'])
                promo[f]['meta'] = {'timestamp': {'seconds': ts.seconds}}
    return o


@pytest.fixture(scope='module')
def output_promos():
    return patch_metas(PROMOS)


@pytest.fixture(scope='module')
def promos_table(yt_server, config):
    return DataCampPromoTable(
        yt_server,
        config.yt_promo_tablepath,
    )


@pytest.fixture(scope='module')
def promos_datacamp_messages():
    return [message_from_data({'promos': {'promo': PROMOS}}, DatacampMessage())]


@pytest.fixture(scope='module')
def promos_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, promos_topic):
    cfg = {
        'logbroker': {
            'promos_topic': promos_topic.topic
        },
        'general': {
            'color': 'white',
        }
    }

    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, promos_topic):
    resources = {
        'config': config,
        'promos_topic': promos_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def main_promos_inserter(promos_datacamp_messages, piper, promos_topic):
    for message in promos_datacamp_messages:
        promos_topic.write(message.SerializeToString())

    wait_until(lambda: piper.promo_processed >= len(PROMOS), timeout=60)


# Проверка того, что акции корректно записываются из топика в таблицу
def test_write_new_promos(main_promos_inserter, piper, output_promos):
    assert_that(
        piper.promos_table.data,
        HasDatacampPromoRows(
            [
                {
                    'business_id': promo['primary_key']['business_id'] if 'business_id' in promo['primary_key'] else 0,
                    'source': promo['primary_key']['source'] if 'source' in promo['primary_key'] else 0,
                    'promo_id': promo['primary_key']['promo_id'],
                    'promo': IsSerializedProtobuf(PromoDescription, promo),
                }
                for promo in output_promos
            ]
        ),
        'Missing promos'
    )


# Проверка того, что акции корректно обновляются из топика
def test_update_promos(main_promos_inserter, piper, promos_topic, promos_datacamp_messages, output_promos, promos_table):
    # Проводим тест после того как обработаны сообщения из main_promos_inserter
    promo = {
        'primary_key': {
            'promo_id': 'SomePromo',
            'business_id': 3,
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
            'author': 'Cat'
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
    # Записали в топик оригинальные данные
    message = message_from_data({'promos': {'promo': [promo]}}, DatacampMessage())
    promos_topic.write(message.SerializeToString())
    wait_until(lambda: piper.promo_processed >= len(PROMOS) + 1, timeout=60)

    # Обновляем все поля + добавляем что-то ещё
    promo_update = {
        'primary_key': {
            'promo_id': 'SomePromo',
            'business_id': 3,
            'source': ESourceType.PARTNER_SOURCE
        },
        'promo_general_info': {
            'meta': META_1,
            'promo_type': PromoType.BLUE_CASHBACK,
        },
        'constraints': {
            'meta': META_1,
            'enabled': False,
            'start_date': 1606775559,
            'offers_matching_rules': [
                {
                    'category_restriction': {
                        'promo_category': [{
                            'id': 10
                        }]
                    }
                }
            ]
        },
        'responsible': {
            'meta': META_1,
            'author': 'Dog'
        },
        'promotion': {
            'meta': META_1,
            'channel': [PromoPromotion.Channel.PRODUCT_OF_DAY]
        },
        'mechanics_data': {
            'meta': META_1,
            'market_bonus': {
                'description': 'Some description222'
            }
        },
        'additional_info': {
            'meta': META_1,
            'name': 'Super discount333',
        }
    }

    # Записываем обновление
    message = message_from_data({'promos': {'promo': [promo_update]}}, DatacampMessage())
    promos_topic.write(message.SerializeToString())
    # Ждём когда будут обработаны ещё 2 наших сообщения (акция + обновление)
    wait_until(lambda: piper.promo_processed >= len(PROMOS) + 2, timeout=60)

    updated_promo = dict(promo)
    updated_promo.update(promo_update)

    expected_promos = list(output_promos)
    # В этом тесте мы закинули одну акцию и обновление к ней.
    expected_promos.extend(patch_metas([updated_promo]))

    for p in expected_promos:
        assert_that(
            piper.promos_table.data,
            HasDatacampPromoRows(
                [
                    {
                        'business_id': p['primary_key']['business_id'] if 'business_id' in p['primary_key'] else 0,
                        'source': p['primary_key']['source'] if 'source' in p['primary_key'] else 0,
                        'promo_id': p['primary_key']['promo_id'],
                        'promo': IsSerializedProtobuf(PromoDescription, p),
                    }
                ]
            ),
            'Missing promos'
        )
