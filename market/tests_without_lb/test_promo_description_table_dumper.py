# coding: utf-8

import pytest
import mock
import yt.wrapper as yt
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import PromoDescriptionDumperEnv
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampPromoTable
)
from market.idx.datacamp.proto.promo.Promo_pb2 import PromoDescription
from market.pylibrary.proto_utils import message_from_data
from market.pylibrary.mindexerlib.recent_symlink_system import generation2unixtime
from hamcrest import assert_that, equal_to, has_entries, has_items
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf


OUTPUT_DIR = '//home/datacamp/promo_description_dumper_out'
NEW_GENERATION = '20200101_0000'

PROMOS = [
    {
        'primary_key': {
            'promo_id': '0'
        }
    }
]


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'color': 'white',
                'yt_home': '//home/datacamp'
            },
            'promo_description': {
                'enable_promo_description_table_dumper': True,
                'backup_dir': OUTPUT_DIR,
                'keep_count': 1,
                'keep_daily': 1,
                'keep_weekly': 1,
                'keep_montyly': 1
            },
            'yt': {
                'map_reduce_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
            }
        })
    return config


def gen_promo_row(promo_data):
    primary_key = promo_data['primary_key']

    o = {
        'promo_id': primary_key['promo_id']
    }

    if 'business_id' in primary_key:
        o['business_id'] = primary_key['business_id']

    if 'source' in primary_key:
        o['source'] = primary_key['source']

    o['promo'] = message_from_data(promo_data, PromoDescription()).SerializeToString()
    return o


@pytest.fixture(scope='module')
def promo_table_data():
    return [gen_promo_row(o) for o in PROMOS]


@pytest.fixture(scope='module')
def promo_description_table(yt_server, config, promo_table_data):
    return DataCampPromoTable(yt_server, config.yt_promo_tablepath, data=promo_table_data)


# Создаём выходные таблицы - чтобы проверить корректную очистку директории
@pytest.fixture(scope='module')
def history(yt_server, config):
    yt_client = yt_server.get_yt_client()
    tables = [
        '20190101_0000',
        '20190101_0001',
        '20190101_0002',
        '20190102_0000',
        '20190103_0000',
        '20190104_0000',
        '20190120_0000',
        '20190121_0000',
        '20190122_0000',
    ]

    for t in tables:
        yt_client.create('table', yt.ypath_join(OUTPUT_DIR, t), recursive=True)

    yt_server.get_yt_client().link(yt.ypath_join(OUTPUT_DIR, '20190122_0000'), yt.ypath_join(OUTPUT_DIR, 'recent'))


def create_generation_name_mock(template):
    return generation2unixtime(NEW_GENERATION)


@pytest.yield_fixture(scope='module')
def routines(
        yt_server,
        config,
        promo_description_table,
        history
):
    resources = {
        'promos_table': promo_description_table,
        'config': config,
    }
    with mock.patch('time.time', return_value=generation2unixtime(NEW_GENERATION)):
        with PromoDescriptionDumperEnv(yt_server, **resources) as routines_env:
            yield routines_env


def test_dumper(yt_server, routines):
    yt_client = yt_server.get_yt_client()
    tables = yt_client.list(OUTPUT_DIR)

    # I. Проверяем, что чистится директория
    assert_that(tables, equal_to(['20190122_0000', '20200101_0000', 'recent']))

    # II. Проверяем данные в табличке
    last_dumped_table_data = list(yt_client.read_table(yt.ypath_join(OUTPUT_DIR, 'recent')))
    assert_that(last_dumped_table_data, has_items(
        has_entries({
            'promo_id': '0',
            'business_id': None,
            'source': None,
            'promo': IsSerializedProtobuf(PromoDescription, {
                'primary_key': {
                    'promo_id': '0'
                }
            })
        })
    ))
