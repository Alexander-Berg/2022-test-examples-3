# coding: utf-8

import logging
import pytest

from market.idx.datacamp.proto.common.SchemaType_pb2 import PULL_TO_PUSH, PUSH, PUSH_TO_PULL, PULL
from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerAdditionalInfo, PartnerChangeSchemaProcess
from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.utils.utils import create_timestamp_from_json


from market.idx.pylibrary.datacamp.partners_table_utils import (
    is_real_push_partner,
    is_real_pull_partner,
    get_partner_type
)

PARTNERS_TABLE_PATH = "//home/partners_table_path"
UNKNOWN_SHOP_ID = 10101

PARTNERS_DATA = [
    # партнер не в переходном периоде
    # партнер не имеет целевой схемы
    # партнер в shops.dat записан как push
    {
        'shop_id': 101,
        'partners_table_data': {
            'shop_id': 101,
            'mbi': dict2tskv({
                'shop_id': 101,
                'supplier_type': 1,
                'datafeed_id': 1001,
                'is_push_partner': True
            })
        },
        'is_push_partner_shops_dat': True,
    },
    # партнер в переходном периоде pull->push
    # партнер не имеет целевой схемы
    # партнер в shops.dat записан как pull
    {
        'shop_id': 102,
        'partners_table_data': {
            'shop_id': 102,
            'mbi': dict2tskv({
                'shop_id': 102,
                'supplier_type': 1,
                'datafeed_id': 1002,
                'is_push_partner': False
            }),
            'partner_additional_info': PartnerAdditionalInfo(
                partner_change_schema_process=PartnerChangeSchemaProcess(
                    start_ts=create_timestamp_from_json("2019-10-08T15:59:36Z"),
                    change_schema_type=PULL_TO_PUSH,
                )
            ).SerializeToString(),
        },
        'is_push_partner_shops_dat': False,
    },
    # у партнера оконченный переходный период
    # партнер не имеет целевой схемы
    # партнер в shops.dat записан как push
    {
        'shop_id': 103,
        'partners_table_data': {
            'shop_id': 103,
            'mbi': dict2tskv({
                'shop_id': 103,
                'supplier_type': 1,
                'datafeed_id': 1003,
                'is_push_partner': True
            }),
            'partner_additional_info': PartnerAdditionalInfo(
                partner_change_schema_process=PartnerChangeSchemaProcess(
                    start_ts=create_timestamp_from_json("2019-10-08T15:59:36Z"),
                    change_schema_type=PULL_TO_PUSH,
                    finish_ts=create_timestamp_from_json("2019-10-08T16:59:36Z"),
                )
            ).SerializeToString(),
        },
        'is_push_partner_shops_dat': True,
    },
    # партнер не в переходном периоде
    # партнер имеет целевую схему = push
    # партнер в shops.dat записан как pull
    {
        'shop_id': 104,
        'partners_table_data': {
            'shop_id': 104,
            'mbi': dict2tskv({
                'shop_id': 104,
                'supplier_type': 1,
                'datafeed_id': 1001,
                'is_push_partner': False
            }),
            'partner_additional_info': PartnerAdditionalInfo(
                partner_schema=PUSH,
            ).SerializeToString(),
        },
        'is_push_partner_shops_dat': False,
    },
    # партнер не в переходном периоде
    # партнер не имеет целевой схемы
    # партнер в shops.dat записан как pull
    {
        'shop_id': 105,
        'partners_table_data': {
            'shop_id': 105,
            'mbi': dict2tskv({
                'shop_id': 105,
                'supplier_type': 1,
                'datafeed_id': 1005,
                'is_push_partner': False
            })
        },
        'is_push_partner_shops_dat': False,
    },
    # партнер в переходном периоде push->pull
    # партнер не имеет целевой схемы
    # партнер в shops.dat записан как push
    {
        'shop_id': 106,
        'partners_table_data': {
            'shop_id': 106,
            'mbi': dict2tskv({
                'shop_id': 106,
                'supplier_type': 1,
                'datafeed_id': 1006,
                'is_push_partner': True
            }),
            'partner_additional_info': PartnerAdditionalInfo(
                partner_change_schema_process=PartnerChangeSchemaProcess(
                    start_ts=create_timestamp_from_json("2019-10-08T15:59:36Z"),
                    change_schema_type=PUSH_TO_PULL,
                )
            ).SerializeToString(),
        },
        'is_push_partner_shops_dat': True,
    },
    # у партнера оконченный переходный период
    # партнер не имеет целевой схемы
    # партнер в shops.dat записан как pull
    {
        'shop_id': 107,
        'partners_table_data': {
            'shop_id': 107,
            'mbi': dict2tskv({
                'shop_id': 107,
                'supplier_type': 1,
                'datafeed_id': 1007,
                'is_push_partner': False
            }),
            'partner_additional_info': PartnerAdditionalInfo(
                partner_change_schema_process=PartnerChangeSchemaProcess(
                    start_ts=create_timestamp_from_json("2019-10-08T15:59:36Z"),
                    change_schema_type=PUSH_TO_PULL,
                    finish_ts=create_timestamp_from_json("2019-10-08T16:59:36Z"),
                )
            ).SerializeToString(),
        },
        'is_push_partner_shops_dat': False,
    },
    # партнер не в переходном периоде
    # партнер имеет целевую схему = pull
    # партнер в shops.dat записан как push
    {
        'shop_id': 108,
        'partners_table_data': {
            'shop_id': 108,
            'mbi': dict2tskv({
                'shop_id': 108,
                'supplier_type': 1,
                'datafeed_id': 1008,
                'is_push_partner': True
            }),
            'partner_additional_info': PartnerAdditionalInfo(
                partner_schema=PULL,
            ).SerializeToString(),
        },
        'is_push_partner_shops_dat': True,
    },
]


@pytest.yield_fixture()
def yt_client(yt_server):
    return yt_server.get_yt_client()


@pytest.fixture()
def partners_table(yt_server):
    return DataCampPartnersTable(yt_server,
                                 PARTNERS_TABLE_PATH,
                                 data=[partners_data['partners_table_data'] for partners_data in PARTNERS_DATA])


@pytest.yield_fixture()
def test_env(yt_server, partners_table):
    resources = {
        'partners_table': partners_table,
    }

    with BaseEnv(**resources) as base_env:
        base_env.verify()
        yield base_env


def _is_real_push_partner(shop_id, yt_client, is_push_in_shopsdat, expected_is_push_partner, partners_table_path=PARTNERS_TABLE_PATH):
    actual_is_push_partner = is_real_push_partner(
        yt_client=yt_client,
        is_push_in_shopsdat=is_push_in_shopsdat,
        shop_id=shop_id,
        yt_partners_tablepath=partners_table_path,
        log=logging.getLogger()
    )

    assert actual_is_push_partner == expected_is_push_partner


def _is_real_pull_partner(shop_id, yt_client, is_pull_is_shops_dat, expected_is_pull_partner, partners_table_path=PARTNERS_TABLE_PATH):
    actual_is_pull_partner = is_real_pull_partner(
        yt_client=yt_client,
        is_pull_in_shopsdat=is_pull_is_shops_dat,
        shop_id=shop_id,
        yt_partners_tablepath=partners_table_path,
        log=logging.getLogger()
    )

    assert actual_is_pull_partner == expected_is_pull_partner


def _test_partner_type(expected_partner_type, shop_id, yt_client, partners_table_path=PARTNERS_TABLE_PATH):
    partner_type = get_partner_type(
        yt_client=yt_client,
        shop_id=shop_id,
        yt_partners_tablepath=partners_table_path,
        log=logging.getLogger()
    )

    assert partner_type == expected_partner_type


def _get_shop_data(shop_id):
    shop = [shop for shop in PARTNERS_DATA if shop['shop_id'] == shop_id]
    assert shop
    return shop[0]


def test_push_partner_not_in_change_schema_process_no_partner_schema(yt_client, test_env):
    """Проверяем результат для партнера не находящегося в процессе смены схемы, без целевой схемы,
    только со значением в shops.dat == push => это push партнер"""
    shop = _get_shop_data(101)
    _is_real_push_partner(shop['shop_id'], yt_client, shop['is_push_partner_shops_dat'], True)
    _is_real_pull_partner(shop['shop_id'], yt_client, not shop['is_push_partner_shops_dat'], False)


def test_push_partner_in_change_schema_process(yt_client, test_env):
    """Проверяем результаты для партнера находящегося в процессе смены схемы (pull->push), без целевой схемы,
    со значением в shops.dat == pull => это push партнер"""
    shop = _get_shop_data(102)
    _is_real_push_partner(shop['shop_id'], yt_client, shop['is_push_partner_shops_dat'], True)
    _is_real_pull_partner(shop['shop_id'], yt_client, not shop['is_push_partner_shops_dat'], False)


def test_push_partner_finished_change_schema_process_no_partner_schema(yt_client, test_env):
    """Проверяем результаты для партнера завершившего процесс смены схемы, без целевой схемы,
    со значением в shops.dat == push => это push партнер"""
    shop = _get_shop_data(103)
    _is_real_push_partner(shop['shop_id'], yt_client, shop['is_push_partner_shops_dat'], True)
    _is_real_pull_partner(shop['shop_id'], yt_client, not shop['is_push_partner_shops_dat'], False)


def test_get_change_schema_process_for_not_existing_parnter(yt_client, test_env):
    """Проверяем, что функция нормально отрабатывает при запросе несуществующего в таблице партнера"""
    not_existing_shop_id = 999
    _is_real_push_partner(not_existing_shop_id, yt_client, False, False)
    _is_real_push_partner(not_existing_shop_id, yt_client, True, True)
    _is_real_pull_partner(not_existing_shop_id, yt_client, False, False)
    _is_real_pull_partner(not_existing_shop_id, yt_client, True, True)


def test_get_change_schema_process_for_wrong_table_path(yt_client, test_env):
    """Проверяем, что функция нормально отрабатывает если путь до таблицы плохой"""
    shop_id = 102
    _is_real_push_partner(shop_id, yt_client, False, False, partners_table_path="//wrong/wrong/wrong_path")
    _is_real_push_partner(shop_id, yt_client, True, True, partners_table_path="//wrong/wrong/wrong_path")
    _is_real_pull_partner(shop_id, yt_client, False, False, partners_table_path="//wrong/wrong/wrong_path")
    _is_real_pull_partner(shop_id, yt_client, True, True, partners_table_path="//wrong/wrong/wrong_path")


def test_push_partner_not_in_change_schema_process_with_partner_schema(yt_client, test_env):
    """Проверяем результат для партнера не находящегося в процессе смены схемы, с целевой схемой (PUSH),
    со значением в shops.dat == pull => это push партнер"""
    shop = _get_shop_data(104)
    _is_real_push_partner(shop['shop_id'], yt_client, shop['is_push_partner_shops_dat'], True)
    _is_real_pull_partner(shop['shop_id'], yt_client, not shop['is_push_partner_shops_dat'], False)


def test_pull_partner_not_in_change_schema_process_no_partner_schema(yt_client, test_env):
    """Проверяем результат для партнера не находящегося в процессе смены схемы, без целевой схемы,
    только со значением в shops.dat == pull => это pull партнер"""
    shop = _get_shop_data(105)
    _is_real_pull_partner(shop['shop_id'], yt_client, not shop['is_push_partner_shops_dat'], True)
    _is_real_push_partner(shop['shop_id'], yt_client, shop['is_push_partner_shops_dat'], False)


def test_pull_partner_in_change_schema_process(yt_client, test_env):
    """Проверяем результаты для партнера находящегося в процессе смены схемы (push->pull), без целевой схемы,
    со значением в shops.dat == push => это pull партнер"""
    shop = _get_shop_data(106)
    _is_real_pull_partner(shop['shop_id'], yt_client, not shop['is_push_partner_shops_dat'], True)
    _is_real_push_partner(shop['shop_id'], yt_client, shop['is_push_partner_shops_dat'], False)


def test_pull_partner_finished_change_schema_process_no_partner_schema(yt_client, test_env):
    """Проверяем результаты для партнера завершившего процесс смены схемы, без целевой схемы,
    со значением в shops.dat == pull => это pull партнер"""
    shop = _get_shop_data(107)
    _is_real_push_partner(shop['shop_id'], yt_client, not shop['is_push_partner_shops_dat'], True)
    _is_real_push_partner(shop['shop_id'], yt_client, shop['is_push_partner_shops_dat'], False)


def test_pull_partner_not_in_change_schema_process_with_partner_schema(yt_client, test_env):
    """Проверяем результат для партнера не находящегося в процессе смены схемы, с целевой схемой (pull),
    со значением в shops.dat == push => это pull партнер"""
    shop = _get_shop_data(108)
    _is_real_pull_partner(shop['shop_id'], yt_client, not shop['is_push_partner_shops_dat'], True)
    _is_real_push_partner(shop['shop_id'], yt_client, shop['is_push_partner_shops_dat'], False)


def test_pull_partner_type_in_change_process(yt_client, test_env):
    """Проверяем, что для партнера в переходном периоде push->pull будет возвращен тип PULL"""
    _test_partner_type(expected_partner_type=PULL, shop_id=106, yt_client=yt_client)


def test_push_partner_type_in_change_process(yt_client, test_env):
    """Проверяем, что для партнера в переходном периоде pull->push будет возвращен тип PUSH"""
    _test_partner_type(expected_partner_type=PUSH, shop_id=102, yt_client=yt_client)


def test_pull_partner_type_with_partner_schema(yt_client, test_env):
    """Проверяем, что для партнера с целевой схемой pull будет возвращен тип PULL"""
    _test_partner_type(expected_partner_type=PULL, shop_id=108, yt_client=yt_client)


def test_push_partner_type_with_partner_schema(yt_client, test_env):
    """Проверяем, что для партнера с целевой схемой push будет возвращен тип PUSH"""
    _test_partner_type(expected_partner_type=PUSH, shop_id=104, yt_client=yt_client)


def test_pull_partner_type_with_shops_dat(yt_client, test_env):
    """Проверяем, что для партнера со значением pull в шопс дате будет возвращен тип PULL"""
    _test_partner_type(expected_partner_type=PULL, shop_id=105, yt_client=yt_client)


def test_push_partner_type_with_shops_dat(yt_client, test_env):
    """Проверяем, что для партнера со значением push в шопс дате будет возвращен тип PUSH"""
    _test_partner_type(expected_partner_type=PUSH, shop_id=101, yt_client=yt_client)


def test_partner_type_not_in_partners_table(yt_client, test_env):
    """Проверяем, что для партнера, которого нет в таблице партнеров будет возвращено значение по умолчанию (PULL)"""
    _test_partner_type(expected_partner_type=PULL, shop_id=UNKNOWN_SHOP_ID, yt_client=yt_client)
