from mock import patch
import pytest
import tempfile
import time

from market.idx.pylibrary.mindexer_core.genlog_integrity.genlog_integrity import check_integrity, get_integrity_failures
from namedlist import namedlist

from hamcrest import assert_that, close_to, contains_string


GENERATION = '20200528_1300'
HOST = 'host.domain'
COLOR = 'white'
CH_CONFIG = None


@pytest.yield_fixture
def normal_integrity_stats_tsv_file():
    data = [
        ('field1', '10', '90', '0.1', '0.9'),
        ('field2', '20', '80', '0.2', '0.8'),
    ]
    with tempfile.NamedTemporaryFile(mode='w+') as f:
        f.write('\n'.join('\t'.join(row) for row in data))
        f.flush()
        yield f.name


@pytest.yield_fixture
def big_integrity_stats_tsv_file():
    data = [
        ('field1', '16', '84', '0.16', '0.84'),
        ('field2', '20', '80', '0.2', '0.8'),
    ]
    with tempfile.NamedTemporaryFile(mode='w+') as f:
        f.write('\n'.join('\t'.join(row) for row in data))
        f.flush()
        yield f.name


@pytest.yield_fixture
def small_integrity_stats_tsv_file():
    data = [
        ('field1', '4', '96', '0.04', '0.96'),
        ('field2', '20', '80', '0.2', '0.8'),
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
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'field1', '0.1'),
        (ts(2020, 5, 28, 10, 00), '20200528_1000', 'field2', '0.2'),

        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'field1', '0.11'),
        (ts(2020, 5, 28, 11, 00), '20200528_1100', 'field2', '0.2'),

        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'field1', '0.09'),
        (ts(2020, 5, 28, 12, 00), '20200528_1200', 'field2', '0.2'),
    ]
    with patch('market.idx.pylibrary.clickhouse.clickhouse_tools.get_clickhouse_data', return_value=data):
        yield


@pytest.fixture
def clickhouse_empty_data():
    with patch('market.idx.pylibrary.clickhouse.clickhouse_tools.get_clickhouse_data', return_value=[]):
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


def test_normal(config, normal_integrity_stats_tsv_file, clickhouse_data, sql_generations):
    """ Нормальные данные не дают разладку """
    ok = check_integrity(config, normal_integrity_stats_tsv_file, GENERATION, HOST, COLOR, CH_CONFIG)
    assert ok


def test_too_small(config, small_integrity_stats_tsv_file, clickhouse_data, sql_generations):
    """ Маленькие значения дают разладку """
    ok = check_integrity(config, small_integrity_stats_tsv_file, GENERATION, HOST, COLOR, CH_CONFIG)
    assert not ok


def test_too_big(config, big_integrity_stats_tsv_file, clickhouse_data, sql_generations):
    """ Большие значения дают разладку """
    ok = check_integrity(config, big_integrity_stats_tsv_file, GENERATION, HOST, COLOR, CH_CONFIG)
    assert not ok


def test_custom_confidence(config, big_integrity_stats_tsv_file, clickhouse_data, sql_generations):
    """ Кастомное отклонение от среднего """
    ok = check_integrity(config, big_integrity_stats_tsv_file, GENERATION, HOST, COLOR, CH_CONFIG, confidences={'field1': 20})
    assert ok


def test_integrity_failures(config, big_integrity_stats_tsv_file, clickhouse_data, sql_generations):
    """ Проверяем список разладок """
    failures = list(get_integrity_failures(config, big_integrity_stats_tsv_file, GENERATION, HOST, COLOR, CH_CONFIG))
    assert len(failures) == 1
    assert_that(failures[0].moving_avg, close_to(0.1025, 0.0001))
    assert_that(failures[0].moving_std, close_to(0.0035, 0.0001))
    assert_that(failures[0].median, close_to(0.1, 0.01))
    assert_that(failures[0].last, close_to(0.16, 0.01))
    assert_that(failures[0].message, contains_string('Razladka on white field1:'))


def test_get_failures_one_published(config, big_integrity_stats_tsv_file, clickhouse_data, sql_generations_one_published):
    """ Проверяем что игнорируются not-for-publish поколения """
    failures = list(get_integrity_failures(config, big_integrity_stats_tsv_file, GENERATION, HOST, COLOR, CH_CONFIG))
    assert len(failures) == 1
    assert_that(failures[0].moving_avg, close_to(0.1, 0.01))
    assert_that(failures[0].moving_std, close_to(0.0, 0.01))
    assert_that(failures[0].median, close_to(0.1, 0.01))
    assert_that(failures[0].last, close_to(0.16, 0.01))


def test_no_historical_data(config, big_integrity_stats_tsv_file, clickhouse_empty_data, sql_empty_generations):
    """ Нет исторических данных """
    ok = check_integrity(config, big_integrity_stats_tsv_file, GENERATION, HOST, COLOR, CH_CONFIG)
    assert ok


def test_blue_shard(config_for_blue_shard, big_integrity_stats_tsv_file, clickhouse_data, sql_generations):
    """ Проверяем разладку для синего шарда """
    failures = list(get_integrity_failures(config_for_blue_shard, big_integrity_stats_tsv_file, GENERATION, HOST, COLOR, CH_CONFIG))
    assert len(failures) == 1
    assert_that(failures[0].moving_avg, close_to(0.1025, 0.0001))
    assert_that(failures[0].moving_std, close_to(0.0035, 0.0001))
    assert_that(failures[0].median, close_to(0.1, 0.01))
    assert_that(failures[0].last, close_to(0.16, 0.01))
    assert_that(failures[0].message, contains_string('Razladka on blue field1:'))


def test_debug(config, normal_integrity_stats_tsv_file, clickhouse_data, sql_generations):
    """ Отладочный режим """
    ok = check_integrity(config, normal_integrity_stats_tsv_file, GENERATION, HOST, COLOR, CH_CONFIG, debug=True)
    assert ok
