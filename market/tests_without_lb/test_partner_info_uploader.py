# coding: utf-8

from hamcrest import assert_that, not_, greater_than, has_items, has_entries, greater_than_or_equal_to
import pytest
import time

from market.idx.datacamp.proto.common.SchemaType_pb2 import PULL, PUSH, PULL_TO_PUSH
from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerAdditionalInfo
from market.idx.datacamp.routines.lib.tasks.sender_to_miner import yt_table_state_path
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.resources.yt_states_table_mock import YtStatesTableMock
from market.idx.datacamp.routines.yatf.test_env import PartnerInfoUploaderTestEnv
from market.idx.yatf.matchers.env_matchers import IsSerializedJson
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.shopsdat_matchers import ShopsDatTskvMatcher
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampPartersYtRows

from market.idx.yatf.resources.shops_dat import ShopsDat

from market.idx.yatf.utils.utils import create_pb_timestamp


NOW = int(time.time())
NEW_SHOPSDATA_MINING_TIME = 360

SHOPSDAT_1 = {
    'shop_id': 1,
    'datafeed_id': 101,
    'business_id': 10,
    'is_push_partner': True,
    'blue_status': 'REAL',
    'is_enabled': True,
}

SHOPSDAT_2 = {
    'shop_id': 2,
    'datafeed_id': 102,
    'business_id': 20,
    'is_push_partner': True,
    'blue_status': 'REAL',
    'is_enabled': True,
}

SHOPSDAT_7 = {
    'shop_id': 7,
    'datafeed_id': 107,
    'business_id': 70,
    'is_push_partner': False,
    'blue_status': 'REAL',
    'is_enabled': True,
}

SHOPSDAT_9 = {
    'shop_id': 9,
    'business_id': 90,
    'datafeed_id': 109,
    'is_push_partner': False,
    'is_enabled': True,
}

SHOPSDAT_10 = {
    '#shop_id': 10,
    'datafeed_id': 110,
    'business_id': 100,
    'is_push_partner': True,
    'blue_status': 'REAL',
    'is_enabled': False,
}

SHOPSDAT_11 = {
    'shop_id': 10,
    'datafeed_id': 111,
    'business_id': 100,
    'is_push_partner': True,
    'blue_status': 'REAL',
    'is_enabled': True,
}

SHOPSDAT_12 = {
    '#shop_id': 11,
    'datafeed_id': 112,
    'business_id': 110,
    'is_push_partner': True,
    'blue_status': 'REAL',
    'is_enabled': False,
}

SHOPSDAT_13 = {
    '#shop_id': 11,
    'datafeed_id': 113,
    'business_id': 110,
    'is_push_partner': True,
    'blue_status': 'REAL',
    'is_enabled': False,
    'is_tested': True,
}

SHOPSDAT_14 = {
    '#shop_id': 12,
    'business_id': 120,
    'datafeed_id': 114,
    'is_push_partner': True,
    'blue_status': 'REAL',
    'is_enabled': False,
}

SHOPSDAT_15 = {
    '#shop_id': 12,
    'business_id': 120,
    'datafeed_id': 115,
    'is_push_partner': True,
    'blue_status': 'REAL',
    'is_enabled': False,
}

SHOPSDAT_16 = {
    '#shop_id': 13,
    'business_id': 130,
    'datafeed_id': 116,
    'blue_status': 'REAL',
    'is_push_partner': True,
    'is_enabled': False,
    'is_tested': True,
}

SHOPSDAT_17 = {
    'shop_id': 14,
    'datafeed_id': 117,
    'business_id': 140,
    'is_push_partner': False,
    'blue_status': 'REAL',
    'is_enabled': True,
    'warehouse_id': 1
}

SHOPSDAT_18 = {
    'shop_id': 14,
    'datafeed_id': 118,
    'business_id': 140,
    'is_push_partner': False,
    'blue_status': 'REAL',
    'is_enabled': True,
    'warehouse_id': 2
}

SHOPSDAT_19 = {
    'shop_id': 15,
    'business_id': 150,
    'datafeed_id': 119,
    'blue_status': 'REAL',
    'is_push_partner': True,
    'is_enabled': False,
    'is_tested': True,
}

SHOPSDAT_20 = {
    'shop_id': 15,
    'datafeed_id': 120,
    'business_id': 150,
    'blue_status': 'REAL',
    'is_push_partner': True,
    'is_enabled': False,
    'is_tested': True,
}

SHOPSDAT_21 = {
    'shop_id': 16,
    'datafeed_id': 121,
    'business_id': 160,
    'blue_status': 'REAL',
    'is_push_partner': True,
    'is_enabled': False,
    'is_tested': True,
    'cpa': 'REAL',
}

SHOPSDAT_22 = {
    'shop_id': 17,
    'datafeed_id': 122,
    'business_id': 170,
    'blue_status': 'REAL',
    'is_push_partner': True,
    'is_enabled': False,
    'is_tested': True,
    'shop_delivery_currency': 'RUR',
}

SHOPSDAT_23 = {
    '#shop_id': 18,
    'business_id': 180,
    'datafeed_id': 123,
    'blue_status': 'REAL',
    'is_push_partner': True,
    'is_enabled': False,
    'is_alive': False,
    'shop_delivery_currency': 'RUR',
}

SHOPSDAT_24 = {
    '#shop_id': 18,
    'business_id': 180,
    'datafeed_id': 124,
    'blue_status': 'REAL',
    'is_push_partner': True,
    'is_enabled': False,
    'is_alive': True,
    'shop_delivery_currency': 'RUR',
}

SHOPSDAT_25 = {
    'shop_id': 19,
    'business_id': 190,
    'datafeed_id': 191,
    'blue_status': 'REAL',
    'is_push_partner': True,
    'is_enabled': True,
}

SHOPSDAT_26 = {
    'shop_id': 21,
    'business_id': 200,
    'datafeed_id': 201,
    'is_alive': True,
    'is_push_partner': True,
}

SHOPSDAT_27 = {
    'shop_id': 22,
    'business_id': 210,
    'datafeed_id': 211,
    'is_alive': True,
    'is_enabled': True,
    'is_push_partner': True,
}

SHOPSDAT_28 = {
    'shop_id': 23,
    'business_id': 220,
    'datafeed_id': 225,
    'blue_status': 'REAL',
    'is_alive': True,
    'is_enabled': True,
}

SHOPSDAT_29 = {
    'shop_id': 23,
    'business_id': 221,
    'datafeed_id': 226,
    'blue_status': 'REAL',
    'is_alive': True,
    'is_enabled': True,
}

SHOPSDAT_30 = {
    'shop_id': 2228904,
    'blue_status': 'REAL',
    'business_id': 757313,
    'datafeed_id': 2190353,
    'is_alive': True,
    'is_enabled': True,
    'is_push_partner': True,
    'warehouse_id': 298252,
}

SHOPSDAT = [
    # магазин в таблице выключен, в шопсдате включен -> после выгрузки должен быть включен
    SHOPSDAT_1,
    # синий пуш магазин, которого нету в таблице -> должен быть выгружен
    SHOPSDAT_2,
    # белый пуш магазин -> не должен быть выгружен
    {
        'shop_id': 4,
        'business_id': 40,
        'datafeed_id': 104,
        'is_push_partner': True,
        'is_enabled': True,
    },
    # синий пул магазин -> не должен быть выгружен
    {
        'shop_id': 5,
        'business_id': 50,
        'datafeed_id': 105,
        'is_enabled': True,
    },
    # виртуальный магазин -> должден быть выгружен
    {
        'shop_id': 6,
        'business_id': 60,
        'datafeed_id': 106,
        'ff_virtual': True,
        'is_enabled': True,
    },
    # магазин в табличке включен, находится в стадии переключения, в шопс дате он пул партнер
    SHOPSDAT_7,
    # магазин в табличке включен, его стадия переключения уже закончилась в состоянии PULL,
    # значение в шопс дат игнорируем -> выключаем
    {
        'shop_id': 8,
        'business_id': 80,
        'datafeed_id': 108,
        'is_push_partner': True,
        'is_enabled': True,
    },
    # магазин в табличке включен, его стадия переключения уже закончилась в состоянии PUSH,
    # значение в шопс дат игнорируем -> не выключаем, обновляем
    SHOPSDAT_9,
    # синий пуш магазин, у которого один фид включен, а один - выключен
    # должен появиться в таблице партнеров в статусе publish
    SHOPSDAT_10,
    SHOPSDAT_11,
    # синий пуш магазин, у которого один фид с статусе check, а один - выключен
    # должен появиться в таблице партнеров в статусе publish
    SHOPSDAT_12,
    SHOPSDAT_13,
    # синий пуш магазин, у которого 2 выключеных склада
    # должен появиться в таблице партнеров в статусе disable
    SHOPSDAT_14,
    SHOPSDAT_15,
    # синий пуш магазин, по нему нет изменений в шопс дате,
    # но в таблице партнеров он сейчас в статсусе check - статус должен изменится на publish
    SHOPSDAT_16,
    # магазин в процессе переключения, после обработки комплит фидов
    SHOPSDAT_17,
    SHOPSDAT_18,
    # магазин, для которого появились новые фиды в шопс дате
    SHOPSDAT_19,
    SHOPSDAT_20,
    # магазин, для которого произошли существенные изменения в шопс дате
    SHOPSDAT_21,
    # магазин, для которого произошли НЕ существенные изменения в шопс дате
    SHOPSDAT_22,
    # магазин с is_alive фидом - должен получиться статус publish в таблице
    SHOPSDAT_23,
    SHOPSDAT_24,
    # магазин, у которого убили брата
    SHOPSDAT_25,
    SHOPSDAT_26,
    SHOPSDAT_27,
    # магазины с одним shop_id и разным business_id
    SHOPSDAT_28,
    SHOPSDAT_29,
    # https://st.yandex-team.ru/MARKETINDEXER-48146
    SHOPSDAT_30,
]

DIRECT_SHOPSDAT_1 = {
    'shop_id': 1001,
    'datafeed_id': 10010,
    'is_enabled': True,
    'is_push_partner': True,
    'direct_status': 'REAL',
    'business_id': 1001001
}

EXPECTED_DIRECT_SHOPSDAT_1 = {
    'shop_id': 1001,
    'datafeed_id': 10010,
    'is_enabled': True,
    'is_push_partner': True,
    'direct_status': 'REAL',
    'tariff': 'FREE',
    'regions': 10000,
    'business_id': 1001001,
    'vertical_share': True
}

DIRECT_SHOPSDAT_2 = {
    '#shop_id': 1002,
    'business_id': 1002001,
    'datafeed_id': 10020,
    'is_enabled': True,
    'is_push_partner': True,
    'direct_status': 'REAL'
}

VERTICAL_SHOPSDAT_1 = {
    'shop_id': 3335,
    'datafeed_id': 33350,
    'is_enabled': True,
    'is_push_partner': True,
    'business_id': 33353335,
    'vertical_share': True
}

VERTICAL_SHOPSDAT_2 = {
    'shop_id': 3435,
    'datafeed_id': 34350,
    'is_enabled': True,
    'is_push_partner': True,
    'business_id': 34353435,
    'blue_status': 'REAL',
    'vertical_share': False
}

EXPECTED_VERTICAL_SHOPSDAT_1 = {
    'shop_id': 3335,
    'datafeed_id': 33350,
    'is_enabled': True,
    'is_push_partner': True,
    'tariff': 'FREE',
    'regions': 10000,
    'business_id': 33353335,
    'vertical_share': True
}

EXPECTED_VERTICAL_SHOPSDAT_2 = {
    'shop_id': 3435,
    'datafeed_id': 34350,
    'is_enabled': True,
    'is_push_partner': True,
    'business_id': 34353435,
    'blue_status': 'REAL',
    'vertical_share': False
}

NOT_EXPECTED_VERTICAL_SHOPSDAT = {
    'shop_id': 3435,
    'datafeed_id': 34350,
    'is_enabled': True,
    'is_push_partner': True,
    'tariff': 'FREE',
    'regions': 10000,
    'business_id': 34353435,
    'blue_status': 'REAL',
    'vertical_share': False
}

DIRECT_SHOPSDAT = [
    DIRECT_SHOPSDAT_1,
    VERTICAL_SHOPSDAT_1,
    VERTICAL_SHOPSDAT_2
]

FOREIGN_SHOPSDAT = [
    {
        'shop_id': 2222,
        'datafeed_id': 22222,
        'busines_id': 222222,
        'is_enabled': True,
        'foreign_partner': 'REAL',
    }
]

FOODTECH_SHOPSDAT = [
    {
        'shop_id': 2002,
        'datafeed_id': 20022002,
        'is_enabled': True,
        'is_push_partner': True,
        'tariff': 'FREE',
        'regions': 10000,
        'business_id': 20022002,
        'is_lavka': True
    }
]


def fixed_time():
    return NOW


@pytest.fixture(scope='module')
def config(yt_server):
    cfg = {
        'general': {
            'color': 'white',
        },
        'patches': {
            'forced_vertical_shops': [1001]
        },
    }
    return RoutinesConfigMock(
        yt_server=yt_server,
        config=cfg)


@pytest.fixture(scope='module')
def shopsdat(config):
    return ShopsDat(filename=config.shopsdat,
                    shops=SHOPSDAT)


@pytest.fixture(scope='module')
def direct_shopsdat(config):
    return ShopsDat(filename=config.direct_shopsdat,
                    shops=DIRECT_SHOPSDAT)


@pytest.fixture(scope='module')
def foodtech_shopsdat(config):
    return ShopsDat(filename=config.foodtech_shopsdat,
                    shops=FOODTECH_SHOPSDAT)


@pytest.fixture(scope='module')
def foreign_shopsdat(config):
    return ShopsDat(filename=config.foreign_shopsdat,
                    shops=FOREIGN_SHOPSDAT)


@pytest.fixture(scope='module')
def partners_table_data():
    return [
        # магазин в таблице выключен, в шопсдате включен -> после выгрузки должен быть включен
        {
            'shop_id': 1,
            'status': 'disable',
            'partner_additional_info': {
                'disabled_since_ts': create_pb_timestamp().ToJsonString(),
            },
        },
        # магазин в таблице включен, но в шопсдате его нет -> после выгрузки должен быть удален
        {
            'shop_id': 3,
            'status': 'publish',
            'mbi': {
                'shop_id': 3,
                'business_id': 30,
                'datafeed_id': 103,
            },
        },
        # магазин включен и находится в стадии переключения
        {
            'shop_id': 7,
            'status': 'publish',
            'partner_additional_info': {
                'partner_change_schema_process': {
                    'start_ts': create_pb_timestamp().ToJsonString(),
                }
            },
            'mbi': {
                'shop_id': 7,
                'business_id': 70,
                'datafeed_id': 107,
                'is_enabled': True,
            }
        },
        # магазин включен, но его стадия переключения завершилась в состоянии PULL
        {
            'shop_id': 8,
            'status': 'publish',
            'partner_additional_info': {
                'partner_schema': PULL,
            },
            'mbi': {
                'shop_id': 8,
                'business_id': 80,
                'datafeed_id': 108,
                'is_enabled': True,
            }
        },
        # магазин включен и его стадия переключения завершилась в состоянии PULL
        {
            'shop_id': 9,
            'status': 'publish',
            'partner_additional_info': {
                'partner_schema': PUSH,
            },
            'mbi': {
                'shop_id': 9,
                'business_id': 90,
                'datafeed_id': 109,
                'is_enabled': True,
            }
        },
        # синий пул-магазин в статусе check - должен стать publish
        {
            'shop_id': 13,
            'status': 'check',
            'mbi': {
                '#shop_id': 13,
                'datafeed_id': 116,
                'business_id': 130,
                'blue_status': 'REAL',
                'is_push_partner': True,
                'is_enabled': False,
                'is_tested': True,
            }
        },
        # комплит фиды обработаны, но флаг индексации не выставлен
        {
            'shop_id': 14,
            'status': 'disabled',
            'partner_additional_info': {
                'partner_schema': PULL,
                'partner_change_schema_process': {
                    'start_ts': create_pb_timestamp().ToJsonString(),
                    'change_schema_type': PULL_TO_PUSH,
                    'is_ready_to_index_from_datacamp': False,
                    'parsed_complete_feeds': [1, 2],
                }
            },
        },
        # магазин, для которого появился новый фид, должен получить специальный ts про переобогащение вне расписания
        {
            'shop_id': 15,
            'status': 'publish',
            'mbi': {
                'shop_id': 15,
                'business_id': 150,
                'datafeed_id': 119,
                'blue_status': 'REAL',
                'is_push_partner': True,
                'is_enabled': False,
                'is_tested': True,
            }
        },
        # магазин, для которого произошли изменения во важным параметрам,
        # должен получить специальный ts про переобогащение вне расписания
        {
            'shop_id': 16,
            'status': 'publish',
            'mbi': {
                'shop_id': 16,
                'business_id': 160,
                'datafeed_id': 121,
                'blue_status': 'REAL',
                'is_push_partner': True,
                'is_enabled': False,
                'is_tested': True,
                'cpa': 'NO',
            }
        },
        # магазин, для которого произошли изменения во НЕ важным параметрам,
        # НЕ должен получить специальный ts про переобогащение вне расписания
        {
            'shop_id': 17,
            'status': 'publish',
            'mbi': {
                'shop_id': 17,
                'business_id': 170,
                'datafeed_id': 122,
                'blue_status': 'REAL',
                'is_push_partner': True,
                'is_enabled': False,
                'is_tested': True,
                'shop_delivery_currency': 'EUR',
            }
        },
        # в новой шопсдате придет флажок is_active для одного из фидов
        # магазин станет publish
        {
            'shop_id': 18,
            'status': 'disabled',
            'mbi': [{
                '#shop_id': 18,
                'datafeed_id': 123,
                'business_id': 180,
                'blue_status': 'REAL',
                'is_push_partner': True,
                'is_enabled': False,
                'shop_delivery_currency': 'EUR',
            }, {
                '#shop_id': 18,
                'business_id': 180,
                'datafeed_id': 124,
                'blue_status': 'REAL',
                'is_push_partner': True,
                'is_enabled': False,
                'shop_delivery_currency': 'EUR',
            }]
        },
        #
        {
            'shop_id': 19,
            'status': 'publish',
            'mbi': [{
                'shop_id': 19,
                'business_id': 190,
                'datafeed_id': 191,
            }, {
                'shop_id': 20,
                'business_id': 190,
                'datafeed_id': 192,
            }]
        },
        # магазин, у которого пропадает is_enabled,
        # должен обновиться соответствующий ts
        {
            'shop_id': 21,
            'status': 'publish',
            'mbi': {
                'shop_id': 21,
                'business_id': 200,
                'datafeed_id': 201,
                'is_enabled': True,
            },
            'partner_additional_info': {
                'last_change_status_ts': create_pb_timestamp().ToJsonString(),
            }
        },
        # магазин, у которого появляется is_enabled,
        # должен сброситься соответствующий ts
        {
            'shop_id': 22,
            'status': 'publish',
            'mbi': {
                'shop_id': 22,
                'business_id': 210,
                'datafeed_id': 211,
            },
            'partner_additional_info': {
                'last_change_status_ts': create_pb_timestamp().ToJsonString(),
                'disabled_since_ts': create_pb_timestamp(100).ToJsonString(),
            }
        },
        # https://st.yandex-team.ru/MARKETINDEXER-48146
        {
            'shop_id': 2228904,
            'status': 'disable',
            'mbi': {
                '#shop_id': 2228904,
                'blue_status': 'REAL',
                'business_id': 757313,
                'datafeed_id': 2190353,
                # 'is_alive': True, - это поле придет в обновлении shops.dat
                # 'is_enabled': True, - это поле придет в обновлении shops.dat
                'is_push_partner': True,
                'warehouse_id': 298252,
            },
            'partner_additional_info': {
                'last_change_status_ts': create_pb_timestamp(100).ToJsonString(),
                'disabled_since_ts': create_pb_timestamp(100).ToJsonString(),
            }
        },
    ]


@pytest.fixture(scope='module')
def states_table(yt_server, config):
    tablepath = yt_table_state_path(config)
    return YtStatesTableMock(yt_server, tablepath, data=[{
        'key': 30,
        'state': '{}'
    }, {
        'key': 190,
        'state': '{}'
    }])


@pytest.yield_fixture(scope='module')
def partner_info_uploader(monkeymodule, yt_server, config, partners_table, states_table, shopsdat, direct_shopsdat, foodtech_shopsdat, foreign_shopsdat):
    resources = {
        'config': config,
        'partners_table': partners_table,
        'states_table': states_table,
        'shopsdat': shopsdat,
        'direct_shopsdat': direct_shopsdat,
        'foodtech_shopsdat': foodtech_shopsdat,
        'foreign_shopsdat': foreign_shopsdat,
    }
    with monkeymodule.context() as m:
        m.setattr("market.idx.datacamp.routines.lib.tasks.partner_info_uploader.get_mbi_params_dramatically_changed_ts", fixed_time)
        with PartnerInfoUploaderTestEnv(yt_server, **resources) as partner_info_uploader_env:
            partner_info_uploader_env.verify()
            partners_table.load()
            yield partner_info_uploader_env


def test_partner_info_uploader_update(partner_info_uploader, partners_table):
    """Проверяем, что магазин, который был выключен в таблице и включен в шопсдате, после выгрузке станет включенным в таблице"""
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 1,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_1]),
                'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                    'last_change_status_ts': {
                        'seconds': greater_than(0),
                    }
                })
            }]
        ))


@pytest.mark.parametrize("shop_id,shopsdat_id", [(2, SHOPSDAT_2), (1001, EXPECTED_DIRECT_SHOPSDAT_1)])
def test_partner_info_uploader_new(partner_info_uploader, partners_table, shop_id, shopsdat_id):
    """Проверяем, что новый магазин, которого не был в шопсдате добавиться в таблицу"""
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': shop_id,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([shopsdat_id]),
            }]
        ))


@pytest.mark.parametrize("shop_id,shopsdat_id", [(3335, EXPECTED_VERTICAL_SHOPSDAT_1), (3435, EXPECTED_VERTICAL_SHOPSDAT_2)])
def test_partner_info_uploader_vertical(partner_info_uploader, partners_table, shop_id, shopsdat_id):
    """Проверяем, что новый вертикальный магазин, которого не был в шопсдате, добавится в таблицу"""
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': shop_id,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([shopsdat_id]),
            }]
        ))


@pytest.mark.parametrize("shop_id,shopsdat_id", [(3435, NOT_EXPECTED_VERTICAL_SHOPSDAT)])
def test_partner_info_uploader_not_vertical(partner_info_uploader, partners_table, shop_id, shopsdat_id):
    """Проверяем, что новый не вертикальный магазин, которого не был в шопсдате,
    добавится в таблицу без параметров regions и tariff"""
    for table in [partners_table]:
        assert_that(table.data, not_(HasDatacampPartersYtRows(
            [{
                'shop_id': shop_id,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([shopsdat_id]),
            }]
        )))


def test_partner_info_uploader_disable(partner_info_uploader, partners_table, states_table):
    """Проверяем, что магазин, который включен в таблице, но отсутствует в шопсдате(выклчюен), удалится в таблице"""
    for table in [partners_table]:
        assert_that(table.data, not_(HasDatacampPartersYtRows(
            [{
                'shop_id': 3,
            }]
        )))

    states_table.load()
    assert_that(states_table.data, not_(has_items(*[
        has_entries({
            'key': 30,
        }),
    ])))


def test_partner_info_uploader_filter_pull(partner_info_uploader, partners_table):
    """Проверяем, что пул магазины не выгружаются в таблицу, если мы не в united-режиме"""
    for table in [partners_table]:
        assert_that(table.data, not_(HasDatacampPartersYtRows(
            [{
                'shop_id': 5,
            }]
        )))


def test_partner_info_uploader_virtual(partner_info_uploader, partners_table):
    """Проверяем, что вирутальные магазины выгружаются в таблицу"""
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 6,
                'status': 'publish',
            }]
        ))


def test_partner_info_uploader_switching_schema(partner_info_uploader, partners_table):
    """Проверяем, что магазины которые находятся в режиме переключения обновляются в таблице"""
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 7,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_7]),
            }]
        ))


def test_partner_info_uploader_finish_switchin_schema_in_pull(partner_info_uploader, partners_table):
    """Проверяем, что магазины которые закончили стадию переключения в пул схеме будут удалены"""
    for table in [partners_table]:
        assert_that(table.data, not_(HasDatacampPartersYtRows(
            [{
                'shop_id': 8,
            }]
        )))


def test_partner_info_uploader_finish_switchin_schema_in_push(partner_info_uploader, partners_table):
    """Проверяем, что магазины которые закончили стадию переключения в пуш схеме не будут отключены"""
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 9,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_9]),
            }]
        ))


def test_partner_info_uploader_push_partner_with_one_disabled_warehouse(partner_info_uploader, partners_table):
    """Проверяем, что магазин, у которого один склад отключен, все равно
    попадет в партнерскую таблицу со статусом publish
    """
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 10,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_10, SHOPSDAT_11]),
            }]
        ))


def test_partner_info_uploader_push_partner_with_check_and_disabled_warehouses(partner_info_uploader, partners_table):
    """Проверяем, что магазин, у которого один склад отключен, а другой находится на пш,
    попадет в партнерскую таблицу со статусом publish
    """
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 11,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_12, SHOPSDAT_13]),
            }]
        ))


def test_partner_info_uploader_push_partner_with_two_disabled_warehouses(partner_info_uploader, partners_table):
    """Проверяем, что магазин, у которого оба склада отключены,
    попадет в партнерскую таблицу со статусом disable
    """
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 12,
                'status': 'disable',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_14, SHOPSDAT_15]),
            }]
        ))


def test_partner_info_uploader_change_status(partner_info_uploader, partners_table):
    """Проверяем, что магазин, который был в статусе check, перейдет в статус publish
    """
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 13,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_16]),
            }]
        ))


def test_partner_info_uploader_set_is_ready_to_index_flag(partner_info_uploader, partners_table):
    """Проверяем, что магазин, который был в статусе check, перейдет в статус publish
    """
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 14,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_17, SHOPSDAT_18]),
                'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                    'partner_change_schema_process': {
                        'is_ready_to_index_from_datacamp': True,
                    }
                })
            }]
        ))


def test_partner_info_uploader_for_disabled_direct(partner_info_uploader, partners_table):
    """Проверяем, что магазин, который отключен в shops.dat не окажется в таблице партнеров
    """
    for table in [partners_table]:
        assert_that(table.data, not_(HasDatacampPartersYtRows(
            [{
                'shop_id': 1002,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([DIRECT_SHOPSDAT_2]),
            }]
        )))


def test_partner_info_uploader_for_foodtech(partner_info_uploader, partners_table):
    """Проверяем, что магазин из eats-and-lavka-partners.dat окажется в таблице партнеров
    """
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 2002,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher(FOODTECH_SHOPSDAT)
            }]
        ))


def test_partner_info_uploader_for_foreign_market(partner_info_uploader, partners_table):
    """Проверяем, что магазин из foreign-shops.dat окажется в таблице партнеров
    """
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 2222,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher(FOREIGN_SHOPSDAT)
            }]
        ))


def test_new_partners_will_be_deferrely_mined(partner_info_uploader, states_table, partners_table):
    """Проверяем, что магазины, которые
     - новые
     - не имели mbi_info в таблице партнеров
     будут перемайнены через период времени, равный времени обновления динамического кеша в майнере"""
    states_table.load()
    business_ids = [10, 20, 1001001, 60, 100, 110, 120, 140]
    for business_id in business_ids:
        assert_that(states_table.data, has_items(*[
            has_entries({
                'key': business_id,
                'state': IsSerializedJson({
                    'deferred_ts': greater_than_or_equal_to(NOW + NEW_SHOPSDATA_MINING_TIME)
                })
            }),
        ]))


def test_old_partners_and_not_push_will_not_be_force_reparsed(partner_info_uploader, states_table, partners_table):
    """Проверяем, что магазины, которые
    - уже были в таблице с mbi_info
    - не попадают в таблицу партнеров
    не будут отправлены на принудительное обогащение"""
    states_table.load()
    business_ids = [50, 70, 80, 90, 130, 1002001]
    for shop_id in business_ids:
        assert_that(states_table.data, not_(has_items(*[
            has_entries({
                'key': shop_id,
            }),
        ])))


def test_partners_with_new_feed_get_special_mining_ts(partner_info_uploader, states_table, partners_table):
    """Проверяем, что магазины, по которым появился новый фид в шопс дате,
     получат специальный ts для переоброгащения вне расписания"""
    states_table.load()
    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 150,
            'state': IsSerializedJson({
                'mbi_params_dramatically_changed': NOW
            })
        }),
    ]))

    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 15,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_19, SHOPSDAT_20]),
            }]
        ))


def test_partners_with_important_changes_get_special_mining_ts(partner_info_uploader, states_table, partners_table):
    """Проверяем, что магазины, по которым произошли сильные изменения в шопс дате,
     получат специальный ts для переоброгащения вне расписания"""
    states_table.load()
    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 160,
            'state': IsSerializedJson({
                'mbi_params_dramatically_changed': NOW
            })
        }),
    ]))

    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 16,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_21]),
            }]
        ))


def test_partners_with_not_important_changes_get_no_special_mining_ts(partner_info_uploader, states_table, partners_table):
    """Проверяем, что магазины, по которым произошли НЕ сильные изменения в шопс дате,
     НЕ получат специальный ts для переоброгащения вне расписания"""
    states_table.load()
    assert_that(states_table.data, not_(has_items(*[
        has_entries({
            'key': 170,
        }),
    ])))

    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 17,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_22]),
            }]
        ))


def test_disabled_partner_with_recently_active_feed_is_publish(partner_info_uploader, states_table, partners_table):
    """Проверяем, что магазин, у которого в таблице был статус disabled, а в шопсдате пришел флажок is_active
    хотя бы для одного фида, меняет статус в таблице на publish"""
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 18,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_23, SHOPSDAT_24]),
            }]
        ))


def test_partner_with_deleted_shop(partner_info_uploader, states_table, partners_table):
    """Бизнес, у которого удалили один из шопов должен остаться в таблице майнинга"""
    assert_that(partners_table.data, HasDatacampPartersYtRows(
        [{
            'shop_id': 19,
            'status': 'publish',
            'mbi': ShopsDatTskvMatcher([SHOPSDAT_25]),
        }]
    ))

    states_table.load()
    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 190,
        }),
    ]))


def test_mbi_disabled_shop_ts(partner_info_uploader, states_table, partners_table):
    """Проверяем, что магазин, у которого в таблице в mbi был is_enabled = true, а в шопсдате этот флаг пропал,
    получает disabled_since_ts"""
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 21,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_26]),
                'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                    'disabled_since_ts': {
                        'seconds': greater_than(0),
                    }
                })
            }]
        ))


def test_mbi_disabled_shop_ts_cleared(partner_info_uploader, states_table, partners_table):
    """Проверяем, что магазин, у которого в таблице в mbi не было is_enabled = true, а в шопсдате этот флаг появился,
    удаляет disabled_since_ts"""
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 22,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_27]),
                'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                    'disabled_since_ts': None
                })
            }]
        ))


def test_partner_with_different_business_id(partner_info_uploader, states_table, partners_table):
    """Проверяем, что магазин, у которого в шопсдате для одного shop_id существует несколько business_id,
    загружает только один шопсдат"""
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 23,
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_28])
            }]
        ))
        assert_that(table.data, not_(HasDatacampPartersYtRows(
            [{
                'shop_id': 23,
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_29])
            }]
        )))


def test_MARKETINDEXER_48146(partner_info_uploader, states_table, partners_table):
    """Проверяем, что изменение is_alive и is_enable приводят к отправке на перемайнинг"""
    for table in [partners_table]:
        assert_that(table.data, HasDatacampPartersYtRows(
            [{
                'shop_id': 2228904,
                'status': 'publish',
                'mbi': ShopsDatTskvMatcher([SHOPSDAT_30]),
            }]
        ))
    states_table.load()
    assert_that(states_table.data, has_items(*[
        has_entries({
            'key': 757313,
            'state': IsSerializedJson({
                'mbi_params_dramatically_changed': NOW
            })
        }),
    ]))
