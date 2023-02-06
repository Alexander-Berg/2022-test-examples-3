#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import pytest
from market.idx.yatf.utils.default_configs import PriceHistoryDefaultConfig

import market.idx.pylibrary.mindexer_core.price_history.price_history as price_history

from mock import patch


os.environ["YT_STUFF_MAX_START_RETRIES"] = "2"


def _create_table(yt_server, dir_path, table_name, blue):
    yt_client = yt_server.get_yt_client()
    yt_client.create(
        'map_node',
        dir_path,
        recursive=True,
        ignore_existing=True,
    )

    if blue:
        schema = [
            dict(type='uint64', name='msku'),
        ]
    else:
        schema = [
            dict(type='string', name='offer_id'),
        ]

    table_path = dir_path + '/' + table_name
    yt_client.remove(table_path, force=True)
    yt_client.create('table', table_path, attributes=dict(
        schema=schema
    ))
    return table_path


@pytest.fixture(autouse=True)
def patch_sql():
    with patch('market.pylibrary.mindexerlib.sql.get_generation_half_mode', return_value=False),\
            patch('market.pylibrary.mindexerlib.sql.get_generation_ok', return_value=True):
        yield


@pytest.fixture(scope='module')
def yt_token(yt_server):
    return yt_server.get_yt_client().config['token']


@pytest.fixture(scope='module')
def disabled_config(yt_server):
    config = PriceHistoryDefaultConfig(yt_server.get_server())
    config.yt_price_history_dir = None
    return config


def test_monitoring_disabled(yt_server, yt_token, disabled_config):
    """
        Проверяет возможные сообщения мониторинга:
        1. Цены выключены - Ок
    """

    assert(price_history._check_tables(disabled_config, blue_history=False, pricedrops=False, yt_token=yt_token) == '0;Ok - Disabled')


@pytest.fixture(scope='module')
def config(yt_server):
    return PriceHistoryDefaultConfig(yt_server.get_server())


def monitoring_tables(yt_server, yt_token, config, blue_history, pricedrops):
    """
        Проверяет возможные сообщения мониторинга:
        2. Нет дневных таблиц вообще - Error
        3. Нет вчерашней дневной таблицы вообще - Error
        4. Нет last_complete таблицы валидации - Error
    """

    history_dir = config.yt_blue_price_history_dir if blue_history else config.yt_price_history_dir

    if not pricedrops:
        assert(price_history._check_tables(config, blue_history=blue_history, pricedrops=pricedrops, yt_token=yt_token) ==
               '2;Error - No history table found for yesterday')

        _create_table(yt_server, history_dir, price_history.get_yyyymmdd(-1 if pricedrops else -2), blue=blue_history)
        assert(price_history._check_tables(config, blue_history=blue_history, pricedrops=pricedrops, yt_token=yt_token) ==
               '2;Error - No history table found for yesterday')

    _create_table(yt_server, history_dir, price_history.get_yyyymmdd(0 if pricedrops else -1), blue=blue_history)
    assert(price_history._check_tables(config, blue_history=blue_history, pricedrops=pricedrops, yt_token=yt_token) ==
           '2;Error - Last complete link is not found')

    # there is last_complete link, but there is no history price validation table from yesterday
    prices_validation_path = ('%s/hprices_pricedrops' if pricedrops else '%s/hprices') % history_dir
    _create_table(yt_server, prices_validation_path, price_history.get_yyyymmdd(-2), blue=blue_history)
    _create_table(yt_server, prices_validation_path, 'last_complete', blue=False)
    assert(price_history._check_tables(config, blue_history=blue_history, pricedrops=pricedrops, yt_token=yt_token) ==
           '2;Error - price validation path is not found for yesterday')


def test_monitoring_tables(yt_server, yt_token, config):
    monitoring_tables(yt_server, yt_token, config, blue_history=False, pricedrops=False)


def test_monitoring_pricedrops_tables(yt_server, yt_token, config):
    monitoring_tables(yt_server, yt_token, config, blue_history=False, pricedrops=True)


def test_blue_monitoring_tables(yt_server, yt_token, config):
    monitoring_tables(yt_server, yt_token, config, blue_history=True, pricedrops=False)


def test_blue_monitoring_pricedrops_tables(yt_server, yt_token, config):
    monitoring_tables(yt_server, yt_token, config, blue_history=True, pricedrops=True)


def _check_monitorings_contistency(yt_server, yt_token, config, pricedrops, date_from_today=-1):
    """
        Проверяет возможные сообщения мониторинга:
        5. Есть таблицы, но пустые - Ok (для тестов, например)
        6. Есть непустые таблицы с оферами, но пустые с валидацией - Error
        7. Есть непустые таблицы с оферами, но в таблицах с валидацией мало - Error
        8. Есть непустые таблицы с оферами, и в таблицах с валидацией нормально - Ok
        9. Есть непустые таблицы с оферами, и в таблицах с валидацией много - Ok
    """

    def create_offer(id='1'):
        return {'offer_id': id}

    prices_path = ('%s/hprices_pricedrops' if pricedrops else '%s/hprices') % (config.yt_price_history_dir)

    for i in range(-3, 0):
        _create_table(yt_server, config.yt_price_history_dir, price_history.get_yyyymmdd(i), blue=False)
    _create_table(yt_server, prices_path, price_history.get_yyyymmdd(date_from_today), blue=False)

    prices_path = _create_table(yt_server, prices_path, 'last_complete', blue=False)
    yt_client = yt_server.get_yt_client()
    assert(yt_client.exists(prices_path))

    expected_result = '0;Ok' if pricedrops else '2;Error - Yesterday\'s price history table is empty'

    offers_path = _create_table(yt_server, config.yt_genlog_gendir, '20190505_2020', blue=False)
    assert(price_history._check_tables(config, blue_history=False, pricedrops=pricedrops, yt_token=yt_token) == expected_result)

    yt_client.write_table(offers_path, [create_offer('1'), create_offer('2')])
    assert(price_history._check_tables(config, blue_history=False, pricedrops=pricedrops, yt_token=yt_token) == '2;Error - Offers count is too low')

    yt_client.write_table(prices_path, [create_offer('1')])
    assert(price_history._check_tables(config, blue_history=False, pricedrops=pricedrops, yt_token=yt_token) == '2;Error - Offers count is too low')

    yt_client.write_table(prices_path, [create_offer('1'), create_offer('2')])
    assert(price_history._check_tables(config, blue_history=False, pricedrops=pricedrops, yt_token=yt_token) == expected_result)

    yt_client.write_table(prices_path, [create_offer('1'),
                                        create_offer('2'),
                                        create_offer('3')])
    assert(price_history._check_tables(config, blue_history=False, pricedrops=pricedrops, yt_token=yt_token) == expected_result)


def test_monitoring_consistency(yt_server, yt_token, config):
    _check_monitorings_contistency(yt_server, yt_token, config, False)


def test_monitoring_pricedrops_consistency(yt_server, yt_token, config):
    _check_monitorings_contistency(yt_server, yt_token, config, True, date_from_today=0)


def _check_blue_monitotings_contistency(yt_server, yt_token, config, pricedrops, date_from_today=-1):
    """
        Проверяет возможные сообщения мониторинга:
        10. Есть таблицы, но пустые - Ok (для тестов, например)
        11. Есть непустые таблицы с msku, но пустые с валидацией - Error
        12. Есть непустые таблицы с msku, но в таблицах с валидацией msku меньше - Ok (валидная ситауция MARKETINDEXER-16675)
        13. Есть непустые таблицы с msku, и в таблицах с валидацией нормально - Ok
        14. Есть непустые таблицы с msku, и в таблицах с валидацией много - Ok
    """

    def create_msku(msku=10201):
        return {'msku': msku}

    prices_path = ('%s/hprices_pricedrops' if pricedrops else '%s/hprices') % (config.yt_blue_price_history_dir)

    for i in range(-3, 0):
        _create_table(yt_server, config.yt_blue_price_history_dir, price_history.get_yyyymmdd(i), blue=True)
    _create_table(yt_server, prices_path, price_history.get_yyyymmdd(date_from_today), blue=True)

    prices_path = _create_table(yt_server, prices_path, 'last_complete', blue=True)
    yt_client = yt_server.get_yt_client()
    assert(yt_client.exists(prices_path))

    expected_result = '0;Ok' if pricedrops else '2;Error - Yesterday\'s price history table is empty'

    offers_path = _create_table(yt_server, config.yt_genlog_buybox, '20190505_2020', blue=True)
    assert(price_history._check_tables(config, blue_history=True, pricedrops=pricedrops, yt_token=yt_token) == expected_result)

    yt_client.write_table(offers_path, [create_msku(10201), create_msku(10202)])
    assert(price_history._check_tables(config, blue_history=True, pricedrops=pricedrops, yt_token=yt_token) == '2;Error - Msku count is too low')

    yt_client.write_table(prices_path, [create_msku(10201)])
    assert(price_history._check_tables(config, blue_history=True, pricedrops=pricedrops, yt_token=yt_token) == expected_result)

    yt_client.write_table(prices_path, [create_msku(10201), create_msku(10202)])
    assert(price_history._check_tables(config, blue_history=True, pricedrops=pricedrops, yt_token=yt_token) == expected_result)

    yt_client.write_table(prices_path, [create_msku(10201),
                                        create_msku(10202),
                                        create_msku(10203)])
    assert(price_history._check_tables(config, blue_history=True, pricedrops=pricedrops, yt_token=yt_token) == expected_result)


def test_blue_monitoring_consistency(yt_server, yt_token, config):
    _check_blue_monitotings_contistency(yt_server, yt_token, config, False)


def test_blue_monitoring_pricedrops_consistency(yt_server, yt_token, config):
    _check_blue_monitotings_contistency(yt_server, yt_token, config, True, date_from_today=0)
