#!/usr/bin/python
# -*- coding: utf-8 -*-

import unittest
import datetime
from pytz import utc, timezone
import time
import os
import re
from subprocess import CalledProcessError

from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig
import yatest.common
from market.pylibrary.yatestwrap.yatestwrap import source_path
from market.idx.pylibrary.mindexer_core.shows_updater.shows_updater import ShowsDataYtUpdater, ShowsDataYtChecker
from market.idx.pylibrary.mindexer_core.vendor_category_shows.vendor_category_shows import VendorCategoryShows


def config_stub(yt_stuff):
    class TestConfig(object):
        def __init__(self, yt_stuff):
            self.yt_proxy_primary = yt_stuff.get_server()
            self.yt_proxy = self.yt_proxy_primary
            self.yt_cluster_primary = self.yt_proxy_primary.split('.', 1)[0]
            self.yt_cluster = self.yt_cluster_primary
            self.yt_log_level = 'Debug'
            self.shows_yt_dir = '//shows_dst'
            self.shows_mstat_yt_dir = '//shows_src'
            self.clicks_mstat_yt_dir = '//shows_src'
            self.shows_enabled = True
            self.shows_days_to_aggregate = 5
            self.shows_missed_tables_to_load_num = 2
            self.is_production = False
            self.shows_aggregator_bin = yatest.common.binary_path('market/idx/generation/shows-aggregator-yt/shows-aggregator-yt')
            self.shows_aggregator_geopath = source_path('market/idx/marketindexer/tests/data/geo.c2p')
            self.yt_tokenpath = 'somethere'

            # unused, but must present
            self.yt_proxy_reserve = ''
            self.yt_cluster_reserve = ''
            self.yt_pool_batch = ''
            self.yql_tokenpath = ''
            self.yt_genlog_proxy = ''
            self.mitype = ''
            self.is_testing = False
            self.is_mir = False

    return TestConfig(yt_stuff)


class YqlExecutorStub(object):
    '''Класс для имитации выполнения YQL-запросов
    TODO: YQL-local тесты
    '''
    def __init__(self, yt_client):
        self.yt_client = yt_client

    def yql_waiting_execute(self, yql_tokenpath, query):
        '''Имитация YQL-запросов
        '''
        src_pattern = 'FROM \\`(.*?)\\`'
        src_table = re.findall(src_pattern, query, re.MULTILINE | re.DOTALL)
        if not src_table or len(src_table) == 0:
            raise Exception('Yql query: src table not set')

        # Исключение, имитирующее запрос в несуществующую таблицу
        # При текущем устройстве shows_updater оно невозможно
        if not self.yt_client.exists(src_table[0]):
            raise Exception('Yql query: src table not found: %s', src_table[0])

        dst_pattern = 'INSERT INTO \\`(.*?)\\`'
        dst_table = re.findall(dst_pattern, query, re.MULTILINE | re.DOTALL)
        if not dst_table or len(dst_table) == 0:
            raise Exception('Yql query: dst table not set')
        # Создадим таблицу и пометим ее, как созданную yql-запросом
        self.yt_client.create(
            'table',
            dst_table[0],
            ignore_existing=True,
            recursive=True,
        )
        self.yt_client.set_attribute(dst_table[0], 'yql_stub_table', True)


class TestShowsUpdater(unittest.TestCase):
    '''Тестрование shows_updater (без YQL-запросов)
    '''

    @classmethod
    def setUpClass(cls):
        root = yatest.common.output_path('yt_shows_test')
        os.mkdir(root)
        config = YtConfig(yt_work_dir=root)
        cls.yt_stuff = YtStuff(config=config)
        cls.yt_stuff.start_local_yt()

    @classmethod
    def tearDownClass(cls):
        cls.yt_stuff.stop_local_yt()

    def setUp(self):
        yt = self.yt_stuff.get_yt_client()
        yt.remove('//shows_src', force=True, recursive=True)
        yt.remove('//shows_dst', force=True, recursive=True)
        indexer_group = "idm-group:69548"
        groups = yt.list("//sys/groups")
        if indexer_group not in groups:
            yt.create("group", attributes={"name": "idm-group:69548"}, force=True)

    def prepare_tables_for_loading_test(self):
        '''Подготовим начальные условия для тестов получения данных из MSTAT
        '''

        # Создадим некоторые таблицы mstat
        yt = self.yt_stuff.get_yt_client()
        mstat_tables = [
            '//shows_src/2017-06-22',
            '//shows_src/2017-06-20',
            '//shows_src/2017-06-19',
            '//shows_src/2017-06-16',
            '//shows_src/2017-06-15',
        ]
        for table in mstat_tables:
            yt.create('table', table, ignore_existing=True, recursive=True)

        yt.remove('//shows_dst', force=True, recursive=True)

        # Создадим некоторые таблицы индексатора
        indexer_tables = [
            '//shows_dst/products/2017-06-20',
            '//shows_dst/offers/2017-06-20',
        ]
        for table in indexer_tables:
            yt.create('table', table, ignore_existing=True, recursive=True)
            yt.set_attribute(table, 'yql_stub_table', False)

    def test_current_day_shows_products_ok(self):
        '''Успешная выгрузку таблицы за заданный день
        '''
        self.prepare_tables_for_loading_test()
        date_to_process = datetime.datetime(2017, 6, 22, 00, 00)
        yt = self.yt_stuff.get_yt_client()

        ShowsDataYtUpdater(
            entity='products',
            config=config_stub(self.yt_stuff),
            yt_client=yt,
            yql_executor=YqlExecutorStub(yt_client=yt)
        )._load_current_day_shows(date_to_process=date_to_process)
        self.assertTrue(yt.exists('//shows_dst/products/2017-06-22'))
        self.assertEqual(yt.get('//shows_dst/products/2017-06-22/@yql_stub_table'), True)

    def test_current_day_shows_products_src_not_found(self):
        '''Искомая таблица отсутствует у MSTAT
        '''
        self.prepare_tables_for_loading_test()
        date_to_process = datetime.datetime(2017, 6, 21, 00, 00)
        yt = self.yt_stuff.get_yt_client()

        # Успешная выгрузка таблица за заданный день
        ShowsDataYtUpdater(
            entity='products',
            config=config_stub(self.yt_stuff),
            yt_client=yt,
            yql_executor=YqlExecutorStub(yt_client=yt)
        )._load_current_day_shows(date_to_process=date_to_process)
        # В результате таблицы нет, но не падаем
        self.assertFalse(yt.exists('//shows_dst/products/2017-06-21'))

    def test_current_day_shows_products_dst_exists(self):
        '''Заданая таблица уже есть в индексаторе, не перезаписываем
        '''
        self.prepare_tables_for_loading_test()
        date_to_process = datetime.datetime(2017, 6, 20, 00, 00)
        yt = self.yt_stuff.get_yt_client()

        # Успешная выгрузка таблица за заданный день
        ShowsDataYtUpdater(
            entity='products',
            config=config_stub(self.yt_stuff),
            yt_client=yt,
            yql_executor=YqlExecutorStub(yt_client=yt)
        )._load_current_day_shows(date_to_process=date_to_process)
        self.assertTrue(yt.exists('//shows_dst/products/2017-06-20'))
        self.assertEqual(yt.get('//shows_dst/products/2017-06-20/@yql_stub_table'), False)

    def test_current_day_shows_offers_ok(self):
        '''Успешная выгрузку таблицы для офферов за заданный день
        '''
        self.prepare_tables_for_loading_test()
        date_to_process = datetime.datetime(2017, 6, 22, 00, 00)
        yt = self.yt_stuff.get_yt_client()

        ShowsDataYtUpdater(
            entity='offers',
            config=config_stub(self.yt_stuff),
            yt_client=yt,
            yql_executor=YqlExecutorStub(yt_client=yt)
        )._load_current_day_shows(date_to_process=date_to_process)
        self.assertTrue(yt.exists('//shows_dst/offers/2017-06-22'))
        self.assertEqual(yt.get('//shows_dst/offers/2017-06-22/@yql_stub_table'), True)

    def test_missing_days_products(self):
        '''Проверяем загрузку отсутствующих таблиц для products
        '''
        self.prepare_tables_for_loading_test()
        date_to_process = datetime.datetime(2017, 6, 22, 00, 00)
        yt = self.yt_stuff.get_yt_client()

        ShowsDataYtUpdater(
            entity='products',
            config=config_stub(self.yt_stuff),
            yt_client=yt,
            yql_executor=YqlExecutorStub(yt_client=yt)
        )._load_missing_days_shows(date_to_process=date_to_process)
        # проверяем таблицы за 5 суток, начиная с 2017-06-21
        self.assertFalse(yt.exists('//shows_dst/products/2017-06-22'))
        # такой таблицы не было у mstat
        self.assertFalse(yt.exists('//shows_dst/products/2017-06-21'))
        # таблица была в индексаторе, не перезаписавается
        self.assertTrue(yt.exists('//shows_dst/products/2017-06-20'))
        self.assertEqual(yt.get('//shows_dst/products/2017-06-20/@yql_stub_table'), False)
        # таблица была выгружена из mstat
        self.assertEqual(yt.get('//shows_dst/products/2017-06-19/@yql_stub_table'), True)
        # слишком старая таблица, она нам не нужна
        self.assertFalse(yt.exists('//shows_dst/products/2017-06-16'))

    def test_missing_days_offers(self):
        '''Проверяем загрузку отсутствующих таблиц для offers
        '''
        self.prepare_tables_for_loading_test()
        date_to_process = datetime.datetime(2017, 6, 20, 00, 00)
        yt = self.yt_stuff.get_yt_client()

        ShowsDataYtUpdater(
            entity='offers',
            config=config_stub(self.yt_stuff),
            yt_client=yt,
            yql_executor=YqlExecutorStub(yt_client=yt)
        )._load_missing_days_shows(date_to_process=date_to_process)
        # проверяем таблицы за 5 суток, начиная с 2017-06-19
        # таблица была выгружена из mstat
        self.assertEqual(yt.get('//shows_dst/offers/2017-06-19/@yql_stub_table'), True)
        # таблиц нет в mstat
        self.assertFalse(yt.exists('//shows_dst/offers/2017-06-18'))
        self.assertFalse(yt.exists('//shows_dst/offers/2017-06-17'))
        # таблица была выгружена из mstat
        self.assertEqual(yt.get('//shows_dst/offers/2017-06-16/@yql_stub_table'), True)
        # таблицы не была выгружена, т.к. кончились попытки
        self.assertFalse(yt.exists('//shows_dst/offers/2017-06-15'))

    def create_shows_table(self, tablename, yt):
        yt.create(
            'table',
            tablename,
            attributes=dict(schema=[
                dict(name='product_id', type='string'),
                dict(name='city_id', type='int64'),
                dict(name='shows_count', type='uint64'),
                dict(name='timestamp', type='string'),
                dict(name='vendor_id', type='uint64'),
                dict(name='category_id', type='uint64'),
            ],),
            ignore_existing=True,
            recursive=True
        )

    def check_shows_aggregate_data(self, actual, expected):
        def normalize_data(values):
            return [
                (value['vendor_id'], value['category_id'], value['region_id'], value['shows_count'])
                for value in values
            ]
        actual_data = normalize_data(actual)
        expected_data = normalize_data(expected)
        # Проверяем значения без учета их порядка
        self.assertEqual(frozenset(actual_data), frozenset(expected_data))
        # Проверяем отсутствие дубликатов
        self.assertEqual(len(actual_data), len(expected_data))

    def prepare_shows_tables_for_aggregating(self, entity):
        '''Начальные условия для тестов суммирования показов
        Имеем таблички с показами за 5 дней, с 1.07.17 по 27.06.17
        Представленные в тестах регионы учитываются для следующих 'высокоуровневых' регионов:
        213 - 213, 3, 225, 0
        1 - 3, 225, 0
        2 - 2, 17, 225, 0
        11202 - 52, 225, 0
        43 - 40, 225, 0
        157 - 149, 0
        21175 - 0
        '''
        yt = self.yt_stuff.get_yt_client()
        # 01.07.17
        self.create_shows_table('//shows_dst/{entity}/2017-07-01'.format(entity=entity), yt)
        # Правильный timestamp, относящийся к тем суткам, какие хранятся в таблице (сутки нарезаны по МСК)
        good_timestamp = int(time.mktime(utc.localize(datetime.datetime(2017, 7, 1, 00, 00)).utctimetuple()))
        data = []
        data.append(dict(product_id='1000024', city_id=213, shows_count=5, timestamp=str(good_timestamp), vendor_id=1, category_id=100))
        data.append(dict(product_id='1000023', city_id=2, shows_count=10, timestamp=str(good_timestamp), vendor_id=2, category_id=100))
        data.append(dict(product_id='1000024', city_id=11202, shows_count=1, timestamp=str(good_timestamp), vendor_id=1, category_id=100))
        data.append(dict(product_id='1000021', city_id=43, shows_count=2, timestamp=str(good_timestamp), vendor_id=2, category_id=300))
        data.append(dict(product_id='1000025', city_id=2, shows_count=50, timestamp=str(good_timestamp), vendor_id=1, category_id=100))
        # Несуществующие регионы пропускаются, джоба на них не падает
        data.append(dict(product_id='1000021', city_id=33333, shows_count=1000, timestamp=str(good_timestamp), vendor_id=2, category_id=300))
        # 0-й регион сам по себе не учитывается, хоть в него и суммируются показы по всем регионам
        data.append(dict(product_id='1000022', city_id=0, shows_count=2000, timestamp=str(good_timestamp), vendor_id=3, category_id=100))
        yt.write_table('//shows_dst/{entity}/2017-07-01'.format(entity=entity), data)

        # 30.06.17
        self.create_shows_table('//shows_dst/{entity}/2017-06-30'.format(entity=entity), yt)
        good_timestamp = int(time.mktime(utc.localize(datetime.datetime(2017, 6, 30, 00, 00)).utctimetuple()))
        data = []
        data.append(dict(product_id='1000023', city_id=225, shows_count=8, timestamp=str(good_timestamp), vendor_id=2, category_id=100))
        data.append(dict(product_id='1000021', city_id=43, shows_count=11, timestamp=str(good_timestamp), vendor_id=2, category_id=300))
        data.append(dict(product_id='1000025', city_id=225, shows_count=100, timestamp=str(good_timestamp), vendor_id=1, category_id=100))
        yt.write_table('//shows_dst/{entity}/2017-06-30'.format(entity=entity), data)

        # 29.06.17
        self.create_shows_table('//shows_dst/{entity}/2017-06-29'.format(entity=entity), yt)
        good_timestamp = int(time.mktime(utc.localize(datetime.datetime(2017, 6, 29, 00, 00)).utctimetuple()))
        data = []
        data.append(dict(product_id='1000021', city_id=21175, shows_count=3, timestamp=str(good_timestamp), vendor_id=2, category_id=300))
        data.append(dict(product_id='1000022', city_id=213, shows_count=9, timestamp=str(good_timestamp), vendor_id=3, category_id=100))
        yt.write_table('//shows_dst/{entity}/2017-06-29'.format(entity=entity), data)

        # 28.06.17
        self.create_shows_table('//shows_dst/{entity}/2017-06-28'.format(entity=entity), yt)
        good_timestamp = int(time.mktime(utc.localize(datetime.datetime(2017, 6, 28, 00, 00)).utctimetuple()))
        data = []
        data.append(dict(product_id='1000024', city_id=157, shows_count=7, timestamp=str(good_timestamp), vendor_id=1, category_id=100))
        data.append(dict(product_id='1000022', city_id=1, shows_count=10, timestamp=str(good_timestamp), vendor_id=3, category_id=100))
        yt.write_table('//shows_dst/{entity}/2017-06-28'.format(entity=entity), data)

        # 27.06.17
        self.create_shows_table('//shows_dst/{entity}/2017-06-27'.format(entity=entity), yt)
        good_timestamp = int(time.mktime(utc.localize(datetime.datetime(2017, 6, 27, 00, 00)).utctimetuple()))
        data = []
        data.append(dict(product_id='1000021', city_id=2, shows_count=8, timestamp=str(good_timestamp), vendor_id=2, category_id=300))
        data.append(dict(product_id='1000023', city_id=17, shows_count=3, timestamp=str(good_timestamp), vendor_id=2, category_id=100))
        # записи без vendor_id и category_id пропускаем, не падаем на них (для обратной совместимости)
        data.append(dict(product_id='1000026', city_id=2, shows_count=8, timestamp=str(good_timestamp)))
        data.append(dict(product_id='1000026', city_id=17, shows_count=3, timestamp=str(good_timestamp)))
        yt.write_table('//shows_dst/{entity}/2017-06-27'.format(entity=entity), data)

    def test_shows_aggregator_geo(self):
        '''Тестируем логику подсчета показов по 'верхнеуровневым' регионам
        см. market/library/vcs/top_level_region_logic.h
        Показы суммируются для заданых регионов, куда сейчас входят МСК, СПБ, Фед. округа России,
        Россия, Украина, Белоруссия, Казахстан, и 0 регион (в него попадают все показы)
        '''
        self.prepare_shows_tables_for_aggregating(entity='products')
        date_to_process = datetime.datetime(2017, 7, 1, 00, 00)
        yt = self.yt_stuff.get_yt_client()

        VendorCategoryShows(
            config=config_stub(self.yt_stuff),
            yt_client=yt,
        )._do_calc_vendor_category_shows(date_to_process=date_to_process)

        actual_data = yt.read_table('//shows_dst/vendor_category_shows/20170701_000000')
        expected_data = []
        expected_data.append(dict(vendor_id=1, category_id=100, region_id=213, shows_count=5))
        expected_data.append(dict(vendor_id=3, category_id=100, region_id=213, shows_count=9))
        expected_data.append(dict(vendor_id=1, category_id=100, region_id=3, shows_count=5))
        expected_data.append(dict(vendor_id=3, category_id=100, region_id=3, shows_count=19))

        expected_data.append(dict(vendor_id=2, category_id=100, region_id=2, shows_count=10))
        expected_data.append(dict(vendor_id=2, category_id=300, region_id=2, shows_count=8))
        expected_data.append(dict(vendor_id=2, category_id=100, region_id=17, shows_count=13))
        expected_data.append(dict(vendor_id=2, category_id=300, region_id=17, shows_count=8))
        expected_data.append(dict(vendor_id=1, category_id=100, region_id=2, shows_count=50))
        expected_data.append(dict(vendor_id=1, category_id=100, region_id=17, shows_count=50))

        expected_data.append(dict(vendor_id=1, category_id=100, region_id=52, shows_count=1))

        expected_data.append(dict(vendor_id=2, category_id=300, region_id=40, shows_count=13))

        expected_data.append(dict(vendor_id=2, category_id=300, region_id=225, shows_count=21))
        expected_data.append(dict(vendor_id=3, category_id=100, region_id=225, shows_count=19))
        expected_data.append(dict(vendor_id=2, category_id=100, region_id=225, shows_count=21))
        expected_data.append(dict(vendor_id=1, category_id=100, region_id=225, shows_count=156))

        expected_data.append(dict(vendor_id=1, category_id=100, region_id=149, shows_count=7))

        expected_data.append(dict(vendor_id=2, category_id=300, region_id=0, shows_count=24))
        expected_data.append(dict(vendor_id=3, category_id=100, region_id=0, shows_count=19))
        expected_data.append(dict(vendor_id=2, category_id=100, region_id=0, shows_count=21))
        expected_data.append(dict(vendor_id=1, category_id=100, region_id=0, shows_count=163))
        self.check_shows_aggregate_data(actual_data, expected_data)

    def test_shows_aggregator_no_data(self):
        '''Тестируем: исключение в случае, если на входе аггрегатора нет таблиц
        '''
        date_to_process = datetime.datetime(2017, 7, 1, 00, 00)
        yt = self.yt_stuff.get_yt_client()

        with self.assertRaises(CalledProcessError):
            VendorCategoryShows(
                config=config_stub(self.yt_stuff),
                yt_client=yt,
            )._do_calc_vendor_category_shows(date_to_process=date_to_process)

    def test_shows_aggregator_result_schema(self):
        '''Тестируем схему полученной таблицы
        '''
        date_to_process = datetime.datetime(2017, 7, 1, 00, 00)
        yt = self.yt_stuff.get_yt_client()

        self.create_shows_table('//shows_dst/products/2017-06-30', yt)
        good_timestamp = int(time.mktime(utc.localize(datetime.datetime(2017, 6, 30, 00, 00)).utctimetuple()))
        data = []
        data.append(dict(product_id='1000023', city_id=225, shows_count=8, timestamp=str(good_timestamp)))
        yt.write_table('//shows_dst/products/2017-06-30', data)

        VendorCategoryShows(
            config=config_stub(self.yt_stuff),
            yt_client=yt,
        )._do_calc_vendor_category_shows(date_to_process=date_to_process)

        actual_schema = yt.get('//shows_dst/vendor_category_shows/20170701_000000/@schema')
        expected_schema = [
            dict(name='vendor_id', type='uint64'),
            dict(name='category_id', type='uint64'),
            dict(name='region_id', type='uint64'),
            dict(name='shows_count', type='uint64'),
        ]

        def normalize_schema(values):
            return [
                (value['type'], value['name'])
                for value in values
            ]
        actual = normalize_schema(actual_schema)
        expected = normalize_schema(expected_schema)
        self.assertEqual(frozenset(actual), frozenset(expected))
        self.assertEqual(len(actual), len(expected))

    def test_shows_aggregator_too_new_timestamp(self):
        '''Тестируем: исключение в случае, если среди записей в таблицах с показами за сутки
        попадается timestamp новее, чем targetDate 23:59:59 или старее, чем targetDate - daysToAggregateNum 00:00:00 (MSK)
        '''
        date_to_process = datetime.datetime(2017, 7, 1, 00, 00)
        self.prepare_shows_tables_for_aggregating(entity='offers')
        yt = self.yt_stuff.get_yt_client()

        self.create_shows_table('//shows_dst/offers/2017-07-01', yt)
        bad_datetime = datetime.datetime(2017, 7, 2, 00, 00, 00)
        bad_timestamp = int(time.mktime(utc.localize(bad_datetime).utctimetuple()))
        data = []
        data.append(dict(product_id='1000', city_id=225, shows_count=1000, timestamp=str(bad_timestamp), vendor_id=1, category_id=100))
        yt.write_table('//shows_dst/offers/2017-07-01', data)

        with self.assertRaises(Exception):
            VendorCategoryShows(
                config=config_stub(self.yt_stuff),
                yt_client=yt,
            )._do_calc_vendor_category_shows(date_to_process=date_to_process)

    def test_shows_aggregator_too_old_timestamp(self):
        '''Тестируем: исключение в случае, если среди записей в таблицах с показами за сутки
        попадается timestamp старше, чем targetDate - daysToAggregateNum 00:00:00 (MSK)
        '''
        date_to_process = datetime.datetime(2017, 7, 1, 00, 00)
        yt = self.yt_stuff.get_yt_client()
        self.create_shows_table('//shows_dst/offers/2017-07-01', yt)

        msk = timezone('Europe/Moscow')
        bad_datetime = datetime.datetime(2017, 6, 26, 23, 59, 59)
        bad_timestamp = int(time.mktime(msk.localize(bad_datetime).utctimetuple()))

        data = []
        data.append(dict(product_id='1000', city_id=225, shows_count=1000, timestamp=str(bad_timestamp), vendor_id=1, category_id=100))
        yt.write_table('//shows_dst/offers/2017-07-01', data)

        with self.assertRaises(Exception):
            VendorCategoryShows(
                config=config_stub(self.yt_stuff),
                yt_client=yt,
            )._do_calc_vendor_category_shows(date_to_process=date_to_process)

    def prepare_monitoring(self, tables_date, product_tables_cnt, offer_tables_cnt, result_datetime):
        '''Подготавливаем данные для положительного теста мониторинга
        '''
        yt = self.yt_stuff.get_yt_client()
        for delta_days in range(0, product_tables_cnt):
            d = tables_date - datetime.timedelta(days=delta_days)
            yt.create('table', '//shows_dst/products/{table}'.format(table=d.strftime('%Y-%m-%d')), ignore_existing=True, recursive=True)

        for delta_days in range(0, offer_tables_cnt):
            d = tables_date - datetime.timedelta(days=delta_days)
            yt.create('table', '//shows_dst/offers/{table}'.format(table=d.strftime('%Y-%m-%d')), ignore_existing=True, recursive=True)

        result_table = result_datetime.strftime('%Y%m%d_%H%M%S')
        yt.create('table', '//shows_dst/vendor_category_shows/{table}'.format(table=result_table), ignore_existing=True, recursive=True)
        yt.link('//shows_dst/vendor_category_shows/{table}'.format(table=result_table), '//shows_dst/vendor_category_shows/recent', force=True)

    def test_monitor_ok(self):
        '''Тестируем: мониторнинг yt_check_shows_data - успешный исход
        '''
        now = datetime.datetime.now(tz=timezone('Europe/Moscow'))
        # 2 дня - задержка обновления таблиц из MSTAT + еще день, т.к. результаты,
        # которые должны появиться (а может, уже появились) сегодня мы не проверяем
        # (т.е. проверяется shows_days_to_aggregate - 1 суток)
        # ex: 05.08  в течение дня у нас появятся результаты за 03.08, но монитор загорится при отсутсвии рез-тов за 02.08 и ранее
        expected_result_date = now - datetime.timedelta(days=3)
        # в тестовом конфиге shows_days_to_aggregate = 5, но самый последний день не проверяем
        self.prepare_monitoring(expected_result_date, 4, 4, expected_result_date)

        config = config_stub(self.yt_stuff)
        # Для упрощения в качестве запасного кластера проверим еще раз основной
        config.yt_proxy_reserve = config.yt_proxy_primary
        config.yt_tokenpath = None

        yt = self.yt_stuff.get_yt_client()
        monitor_msg = ShowsDataYtChecker(config=config, yt_client=yt).do_check_shows_data()
        code = monitor_msg.split(';')[0]
        # проверим код ошибки мониторинга
        self.assertEqual(code, '0')
        # немного проверим сообщение об ошибках
        # состояние резервного кластера идентично основному
        self.assertEqual(monitor_msg.count('0 missed tables'), 2)    # про суточные таблицы
        self.assertEqual(monitor_msg.count('Норм'), 1)    # про свежесть recent-результата

    def test_monitor_warn_tables_cnt(self):
        '''Тестируем: мониторнинг yt_check_shows_data - WARN из-за отсуствия четырех и одной суточной таблицы
        '''
        now = datetime.datetime.now(tz=timezone('Europe/Moscow'))
        expected_result_date = now - datetime.timedelta(days=3)
        self.prepare_monitoring(expected_result_date, 1, 5, expected_result_date)

        yt = self.yt_stuff.get_yt_client()
        config = config_stub(self.yt_stuff)
        config.yt_proxy_reserve = config.yt_proxy_primary
        config.yt_tokenpath = None
        # чтобы добиться CRIT нужно минимум 5 отсутсвующих таблиц, а одну из shows_days_to_aggregate не проверяем
        # так будет нагляднее, что у нас не CRIT
        config.shows_days_to_aggregate = 6

        monitor_msg = ShowsDataYtChecker(config=config, yt_client=yt).do_check_shows_data()
        code = monitor_msg.split(';')[0]
        self.assertEqual(code, '1')
        self.assertEqual(monitor_msg.count('4 missed tables'), 1)
        self.assertEqual(monitor_msg.count('0 missed tables'), 1)
        self.assertEqual(monitor_msg.count('Норм'), 1)

        self.prepare_monitoring(expected_result_date, 4, 5, expected_result_date)
        monitor_msg = ShowsDataYtChecker(config=config, yt_client=yt).do_check_shows_data()
        code = monitor_msg.split(';')[0]
        self.assertEqual(code, '1')
        self.assertEqual(monitor_msg.count('1 missed tables'), 1)
        self.assertEqual(monitor_msg.count('0 missed tables'), 1)
        self.assertEqual(monitor_msg.count('Норм'), 1)

    def test_monitor_crit_tables_cnt(self):
        '''Тестируем: мониторнинг yt_check_shows_data - CRIT из-за отсуствия 5 суточных таблиц
        '''
        now = datetime.datetime.now(tz=timezone('Europe/Moscow'))
        expected_result_date = now - datetime.timedelta(days=3)
        self.prepare_monitoring(expected_result_date, 5, 0, expected_result_date)

        config = config_stub(self.yt_stuff)
        config.yt_proxy_reserve = config.yt_proxy_primary
        config.yt_tokenpath = None
        # чтобы добиться CRIT нужно минимум 5 отсутсвующих таблиц, а одну из shows_days_to_aggregate не проверяем
        config.shows_days_to_aggregate = 6

        yt = self.yt_stuff.get_yt_client()
        monitor_msg = ShowsDataYtChecker(config=config, yt_client=yt).do_check_shows_data()
        code = monitor_msg.split(';')[0]
        self.assertEqual(code, '2')
        self.assertEqual(monitor_msg.count('5 missed tables'), 1)
        self.assertEqual(monitor_msg.count('0 missed tables'), 1)
        self.assertEqual(monitor_msg.count('Норм'), 1)

    def test_monitor_warn_old_recent(self):
        '''Тестируем: мониторнинг yt_check_shows_data - WARN из-за просроченных результатов суммирования
        '''
        now = datetime.datetime.now(tz=timezone('Europe/Moscow'))
        expected_result_date = now - datetime.timedelta(days=3)
        old_recent_datetime = now - datetime.timedelta(days=4)
        self.prepare_monitoring(expected_result_date, 4, 4, old_recent_datetime)

        config = config_stub(self.yt_stuff)
        config.yt_proxy_reserve = config.yt_proxy_primary
        config.yt_tokenpath = None

        yt = self.yt_stuff.get_yt_client()
        monitor_msg = ShowsDataYtChecker(config=config, yt_client=yt).do_check_shows_data()
        code = monitor_msg.split(';')[0]
        self.assertEqual(code, '1')
        self.assertEqual(monitor_msg.count('0 missed tables'), 2)
        self.assertEqual(monitor_msg.count('Не очень-то свежее поколение'), 1)

    def test_monitor_crit_no_recent(self):
        '''Тестируем: мониторнинг yt_check_shows_data - CRIT, тк нет recent-ссылки рез-тов суммирования
        '''
        now = datetime.datetime.now(tz=timezone('Europe/Moscow'))
        expected_result_date = now - datetime.timedelta(days=3)
        self.prepare_monitoring(expected_result_date, 4, 4, expected_result_date)

        config = config_stub(self.yt_stuff)
        config.yt_proxy_reserve = config.yt_proxy_primary
        config.yt_tokenpath = None

        yt = self.yt_stuff.get_yt_client()
        yt.remove('//shows_dst/vendor_category_shows/recent', force=True)

        monitor_msg = ShowsDataYtChecker(config=config, yt_client=yt).do_check_shows_data()
        code = monitor_msg.split(';')[0]
        self.assertEqual(code, '2')
        self.assertEqual(monitor_msg.count('0 missed tables'), 2)
        self.assertEqual(monitor_msg.count('Не найдено'), 1)

    def test_monitor_tables_cnt_no_crit_logic(self):
        '''Тестируем: мониторинг yt_check_shows_data в части количества таблиц: CRIT->WARN для запасного кластера
        '''
        yt = self.yt_stuff.get_yt_client()

        config = config_stub(self.yt_stuff)
        config.yt_proxy_reserve = config.yt_proxy_primary
        config.yt_tokenpath = None
        config.shows_days_to_aggregate = 6

        monitor_result = ShowsDataYtChecker(config=config, yt_client=yt, is_master=False).check_days_tables('products')
        self.assertEqual(monitor_result, (1, '[{proxy}:products] 5 missed tables'.format(proxy=self.yt_stuff.get_server())))

        monitor_result = ShowsDataYtChecker(config=config, yt_client=yt).check_days_tables('products')
        self.assertEqual(monitor_result, (2, '[{proxy}:products] 5 missed tables'.format(proxy=self.yt_stuff.get_server())))

    def test_monitor_recent_no_crit_logic(self):
        '''Тестируем: мониторинг yt_check_shows_data в части суммы показов: CRIT->WARN для запасного кластера
        '''
        yt = self.yt_stuff.get_yt_client()

        config = config_stub(self.yt_stuff)
        config.yt_proxy_reserve = config.yt_proxy_primary
        config.yt_tokenpath = None

        monitor_result = ShowsDataYtChecker(config=config, yt_client=yt, is_master=False).check_recent_result()
        self.assertEqual(
            monitor_result,
            (1, '[{proxy}:result] Не найдено //shows_dst/vendor_category_shows/recent'.format(proxy=self.yt_stuff.get_server()))
        )

        monitor_result = ShowsDataYtChecker(config=config, yt_client=yt).check_recent_result()
        self.assertEqual(
            monitor_result,
            (2, '[{proxy}:result] Не найдено //shows_dst/vendor_category_shows/recent'.format(proxy=self.yt_stuff.get_server()))
        )

    def prepare_recent(self, recent_datetime):
        '''Подготавливаем данные для теста VendorCategoryShows::get_recent_table
        '''
        yt = self.yt_stuff.get_yt_client()
        result_table = recent_datetime.strftime('%Y%m%d_%H%M%S')
        yt.create('table', '//shows_dst/vendor_category_shows/{table}'.format(table=result_table), ignore_existing=True, recursive=True)
        yt.link('//shows_dst/vendor_category_shows/{table}'.format(table=result_table), '//shows_dst/vendor_category_shows/recent', force=True)

    def test_get_recent_table(self):
        '''Тестируем VendorCategoryShows::get_recent_table
        '''
        recent_table_datetime = datetime.datetime(2019, 5, 21, 00, 00)
        self.prepare_recent(recent_table_datetime)
        self.assertEqual(
            VendorCategoryShows(config=config_stub(self.yt_stuff), yt_client=self.yt_stuff.get_yt_client()).get_recent_table(),
            recent_table_datetime.strftime('%Y%m%d_%H%M')
        )


if __name__ == '__main__':
    unittest.main()
