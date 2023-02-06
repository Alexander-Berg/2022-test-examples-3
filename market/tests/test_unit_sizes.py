from mock import patch
import pytest
import tempfile
import time

import market.idx.pylibrary.mindexer_core.razladki.unit_sizes as unit_sizes
from namedlist import namedlist

from hamcrest import assert_that, close_to, contains_string


GENERATION = '20200528_1300'
PREV_GENERATION = '20200528_1200'
HOST = 'host.domain'
CH_CONFIG = None


@pytest.yield_fixture
def normal_dist_stats_tsv_file():
    data = [
        ('worker_1:workindex/indexaa', '1000'),
        ('worker_2:workindex/indexaa', '1010'),
        ('master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        ('num_offers', '100'),
        ('num_models', '10'),
    ]
    with tempfile.NamedTemporaryFile(mode='w+') as f:
        f.write('\n'.join('\t'.join(row) for row in data))
        f.flush()
        yield f.name


@pytest.yield_fixture
def blue_shard_dist_stats_tsv_file():
    data = [
        ('worker_1:workindex/indexaa', '1000'),
        ('worker_2:workindex/indexaa', '1010'),
        ('worker_blue-0:workindex/indexaa', '2000'),
        ('master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        ('num_offers', '100'),
        ('num_offers_in_blue_shard', '5'),
        ('num_models', '10'),
    ]
    with tempfile.NamedTemporaryFile(mode='w+') as f:
        f.write('\n'.join('\t'.join(row) for row in data))
        f.flush()
        yield f.name


@pytest.yield_fixture
def small_dist_stats_tsv_file():
    data = [
        ('worker_1:workindex/indexaa', '1000'),
        ('worker_2:workindex/indexaa', '500'),
        ('master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        ('num_offers', '100'),
        ('num_models', '10'),
    ]
    with tempfile.NamedTemporaryFile(mode='w+') as f:
        f.write('\n'.join('\t'.join(row) for row in data))
        f.flush()
        yield f.name


@pytest.yield_fixture
def big_dist_stats_tsv_file():
    data = [
        ('worker_1:workindex/indexaa', '1000'),
        ('worker_2:workindex/indexaa', '2000'),
        ('master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        ('num_offers', '100'),
        ('num_models', '10'),
    ]
    with tempfile.NamedTemporaryFile(mode='w+') as f:
        f.write('\n'.join('\t'.join(row) for row in data))
        f.flush()
        yield f.name


@pytest.yield_fixture
def prev_dist_stats_tsv_file():
    data = [
        ('worker_1:workindex/indexaa', '1000'),
        ('worker_2:workindex/indexaa', '1020'),
        ('master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        ('num_offers', '100'),
        ('num_models', '10'),
    ]
    with tempfile.NamedTemporaryFile(mode='w+') as f:
        f.write('\n'.join('\t'.join(row) for row in data))
        f.flush()
        yield f.name


@pytest.fixture
def clickhouse_data():
    def ts(year, month, day, hour, minute):
        return str(int(time.mktime((year, month, day, hour, minute, 0, 0, 0, 0))))

    data = [
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'worker_1:workindex/indexaa', '1000'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'worker_2:workindex/indexaa', '1020'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'worker_blue-0:workindex/indexaa', '1000'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'num_offers', '100'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'num_offers_in_blue_shard', '5'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'num_models', '10'),

        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'worker_1:workindex/indexaa', '1000'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'worker_2:workindex/indexaa', '1010'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'worker_blue-0:workindex/indexaa', '1000'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'num_offers', '100'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'num_offers_in_blue_shard', '5'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'num_models', '10'),

        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'worker_1:workindex/indexaa', '1000'),
        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'worker_2:workindex/indexaa', '1020'),
        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'worker_blue-0:workindex/indexaa', '1000'),
        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'num_offers', '100'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'num_offers_in_blue_shard', '5'),
        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'num_models', '10'),
    ]
    with patch('market.idx.pylibrary.clickhouse.clickhouse_tools.get_clickhouse_data', return_value=data):
        yield


@pytest.fixture
def clickhouse_empty_data():
    with patch('market.idx.pylibrary.clickhouse.clickhouse_tools.get_clickhouse_data', return_value=[]):
        yield


@pytest.fixture
def clickhouse_data_with_dups():
    def ts(year, month, day, hour, minute):
        return str(int(time.mktime((year, month, day, hour, minute, 0, 0, 0, 0))))

    data = [
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'worker_1:workindex/indexaa', '1000'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'worker_1:workindex/indexaa', '1000'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'worker_2:workindex/indexaa', '1020'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'worker_2:workindex/indexaa', '1020'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'num_offers', '100'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'num_models', '10'),

        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'worker_1:workindex/indexaa', '1000'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'worker_1:workindex/indexaa', '1000'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'worker_2:workindex/indexaa', '1010'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'worker_2:workindex/indexaa', '1010'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'num_offers', '100'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'num_models', '10'),

        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'worker_1:workindex/indexaa', '1000'),
        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'worker_2:workindex/indexaa', '1020'),
        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'master:search-stats-mmap/model_geo_stats.mmap', '9000'),
        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'num_offers', '100'),
        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'num_models', '10'),
    ]
    with patch('market.idx.pylibrary.clickhouse.clickhouse_tools.get_clickhouse_data', return_value=data):
        yield


@pytest.fixture
def config():
    MiConfig = namedlist('MiConfig', [('for_blue_shard', False), ('is_blue', False)])
    return MiConfig()


@pytest.fixture
def config_for_blue_shard():
    MiConfig = namedlist('MiConfig', [('for_blue_shard', True), ('is_blue', False)])
    return MiConfig()


@pytest.yield_fixture
def sql_generations():
    data = [
        {'name': '20200528_1000', 'hostname': 'host'},
        {'name': '20200528_1100', 'hostname': 'host'},
        {'name': '20200528_1200', 'hostname': 'host'},
    ]
    with patch('market.pylibrary.mindexerlib.sql.get_generations', return_value=data):
        yield


@pytest.yield_fixture
def sql_empty_generations():
    with patch('market.pylibrary.mindexerlib.sql.get_generations', return_value=[]):
        yield


@pytest.yield_fixture
def sql_generations_one_published():
    data = [
        {'name': '20200528_1000', 'hostname': 'host'},
    ]
    with patch('market.pylibrary.mindexerlib.sql.get_generations', return_value=data):
        yield


def test_normal(config, normal_dist_stats_tsv_file, clickhouse_data, sql_generations):
    """ Нормальные данные не дают разладку """
    ok = unit_sizes.check(config, normal_dist_stats_tsv_file, GENERATION, HOST, CH_CONFIG)
    assert ok


def test_too_small(config, small_dist_stats_tsv_file, clickhouse_data, sql_generations):
    """ Маленькие значения дают разладку """
    ok = unit_sizes.check(config, small_dist_stats_tsv_file, GENERATION, HOST, CH_CONFIG)
    assert not ok


def test_too_big(config, big_dist_stats_tsv_file, clickhouse_data, sql_generations):
    """ Большие значения дают разладку """
    ok = unit_sizes.check(config, big_dist_stats_tsv_file, GENERATION, HOST, CH_CONFIG)
    assert not ok


def test_existing_stats(config, prev_dist_stats_tsv_file, clickhouse_data, sql_generations):
    """ Если данные есть и в tsv-файле, и в кликхаусе, должна учитываться только одна копия """
    ok = unit_sizes.check(config, prev_dist_stats_tsv_file, PREV_GENERATION, HOST, CH_CONFIG)
    assert ok


def test_get_failures(config, big_dist_stats_tsv_file, clickhouse_data, sql_generations):
    """ Проверяем список разладок """
    failures = list(unit_sizes.get_failures(config, big_dist_stats_tsv_file, GENERATION, HOST, CH_CONFIG))
    assert len(failures) == 1
    assert_that(failures[0].moving_avg, close_to(20.175, 0.001))
    assert_that(failures[0].moving_std, close_to(0.0353, 0.0001))
    assert_that(failures[0].median, close_to(20.2, 0.01))
    assert_that(failures[0].last, close_to(30.0, 0.01))
    assert_that(failures[0].message, contains_string('Razladka on white indexaa_per_offer:'))


def test_get_failures_one_published(config, big_dist_stats_tsv_file, clickhouse_data, sql_generations_one_published):
    """ Проверяем что игнорируются not-for-publish поколения """
    failures = list(unit_sizes.get_failures(config, big_dist_stats_tsv_file, GENERATION, HOST, CH_CONFIG))
    assert len(failures) == 1
    assert_that(failures[0].moving_avg, close_to(20.2, 0.01))
    assert_that(failures[0].moving_std, close_to(0.0, 0.01))
    assert_that(failures[0].median, close_to(20.2, 0.01))
    assert_that(failures[0].last, close_to(30.0, 0.01))


def test_custom_confidence(config, big_dist_stats_tsv_file, clickhouse_data, sql_generations):
    """ Кастомный доверительный интервал """
    ok = unit_sizes.check(config, big_dist_stats_tsv_file, GENERATION, HOST, CH_CONFIG, confidences={'indexaa_per_offer': 500})
    assert ok


def test_no_historical_data(config, big_dist_stats_tsv_file, clickhouse_empty_data, sql_empty_generations):
    """ Нет исторических данных """
    ok = unit_sizes.check(config, big_dist_stats_tsv_file, GENERATION, HOST, CH_CONFIG)
    assert ok


def test_history_with_duplicates(config, normal_dist_stats_tsv_file, clickhouse_data_with_dups, sql_generations):
    """ Дубликаты должны игнорироваться """
    ok = unit_sizes.check(config, normal_dist_stats_tsv_file, GENERATION, HOST, CH_CONFIG, debug=True)
    assert ok


def test_blue_shard(config_for_blue_shard, blue_shard_dist_stats_tsv_file, clickhouse_data, sql_generations):
    """ Разладки по синему шарду считаются по синему воркеру """
    failures = list(unit_sizes.get_failures(config_for_blue_shard, blue_shard_dist_stats_tsv_file, GENERATION, HOST, CH_CONFIG))
    assert len(failures) == 1
    assert_that(failures[0].moving_avg, close_to(200, 0.01))
    assert_that(failures[0].moving_std, close_to(0.0, 0.01))
    assert_that(failures[0].median, close_to(200, 0.01))
    assert_that(failures[0].last, close_to(400.0, 0.01))
    assert_that(failures[0].message, contains_string('Razladka on blue indexaa_per_offer:'))


def test_debug(config, normal_dist_stats_tsv_file, clickhouse_data, sql_generations):
    """ Отладочный режим """
    ok = unit_sizes.check(config, normal_dist_stats_tsv_file, GENERATION, HOST, CH_CONFIG, debug=True)
    assert ok
