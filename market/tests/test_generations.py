# -*- coding: utf-8 -*-

import os
import datetime

from yatest.common import source_path
from mock import patch

import market.idx.api.backend.generations as generations
import market.idx.api.backend.tsum as tsum


class DictWrapper(object):
    def __init__(self, key_values):
        self.__dict__.update(key_values)


FULL_GENERATIONS = [
    # поколение строилось 1 час
    generations.Generation(DictWrapper({
        'name': '20171127_1518',
        'mi_ver': '2017.1.1931|4661684|18_1914',
        'mitype': 'stratocaster',
        'half_mode': False,
        'start_date': '2017-11-27 15:18:00',
        'end_date': '2017-11-27 16:18:00',
        'release_date': None,
        'type': 'FULL',
        'state': None,
        'fail_reason': '',
        'ok': 1,
        'released': 1,
        'mbo_stuff': '20171127_1407',
        'num_offers': 1000 * 1000,
        'num_blue_offers': 1000 * 1000,
        'num_red_offers': 500 * 1000,
    })),
    # поколение строилось 2 часа
    generations.Generation(DictWrapper({
        'name': '20171127_1302',
        'mitype': 'stratocaster',
        'half_mode': False,
        'start_date': '2017-11-27 13:02:00',
        'end_date': '2017-11-27 15:02:00',
        'release_date': None,
        'state': None,
        'type': 'FULL',
        'fail_reason': '',
        'ok': 1,
        'released': 1,
        'mbo_stuff': '20171127_1407',
        'num_offers': 500 * 1000,
        'num_red_offers': 1000 * 1000,
    })),
]

LAST_GENERATION = generations.Generation(DictWrapper({
    'name': '20171127_1715',
    'mitype': 'stratocaster',
    'half_mode': False,
    'start_date': '2017-11-27 17:21:07',
    'end_date': None,
    'release_date': None,
    'type': 'FULL',
    'state': None,
    'fail_reason': '',
    'ok': 0,
    'released': 0,
    'mbo_stuff': '20171127_1407',
}))

EXPECTED_GAINS = {
    'num_offers': 2.,
    'num_blue_offers': 1.,  # old generation has none
    'num_red_offers': 0.5,  # new generation has half
    'num_offers_with_cluster_good': 0,
    'num_offers_with_model': 0,
    'num_offers_with_thumbs': 0,
    'build_time': 0.5,
}


'''
os.getenv('DS_CONFIG_PATH', '/etc/yandex/market-datasources/datasources.conf')
os.getenv('DS_CONFIG_PATH', '/etc/yandex/market-datasources/indexer/{}.conf'.format(mitype)
'''
os.environ['DS_CONFIG_PATH'] = os.path.abspath(source_path('market/idx/api/tests/datasources.conf'))


def test_get_generations():
    """
    Тест проверяет корректность забора законченных полных поколений
    """
    with patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=FULL_GENERATIONS):
        gens = generations.get_generations(only_successful=True)
        assert len(gens) == 2


def test_get_last_generation():
    """
    Тест проверяет корректность забора текущего полного поколения
    """
    with patch('market.idx.api.backend.generations.get_last_generation', auto_spec=True, return_value=LAST_GENERATION):
        last_gen = generations.get_last_generation()
        assert last_gen.name == '20171127_1715'
        assert not last_gen.end_date


def test_get_average_indexer_time_seconds():
    """
    Тест проверят правильность вычисления среднего времени построения поколения
    """
    with patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=FULL_GENERATIONS):
        gens = generations.get_generations(only_successful=True)
        average_time = generations.get_average_indexer_time_seconds(gens)
        assert average_time == 1.5 * 3600  # срденее арифметическое времени построения двух тестовых поколений


def test_estimated_indexer_time():
    """
    Тест проверяет корректность вычесления оставшегося до конца времени индексации текущего поколения
    """
    patch_gen = patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=FULL_GENERATIONS)
    patch_last_gen = patch('market.idx.api.backend.generations.get_last_generation', auto_spec=True, return_value=LAST_GENERATION)
    with patch_gen, patch_last_gen:
        cdt = datetime.datetime(2017, 11, 27, 18, 40, 10)  # время относительного которого высчитывается остаток
        assert generations.get_estimated_indexer_time(current_datetime=cdt) == (4743, 657)


def test_weighted_average():
    assert generations.weighted_average([], 0.5) is None
    assert 2 == generations.weighted_average([2, 2, 2], 0.5)
    assert 5 / 1.75 == generations.weighted_average([1, 4, 8], 0.5)


def test_get_recent_generation():
    with patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=FULL_GENERATIONS):
        assert FULL_GENERATIONS[0] == generations.get_recent_generation(mitype='stratocaster')


def test_get_full_recent_generation():
    with patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=FULL_GENERATIONS):
        assert FULL_GENERATIONS[0] == generations.get_recent_generation(mitype='stratocaster', generation_type='FULL')


def test_get_generation_by_name_positive():
    with patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=FULL_GENERATIONS):
        assert FULL_GENERATIONS[0] == generations.get_generation_by_name(generation='20171127_1518', mitype='stratocaster')


def test_get_generation_by_name_negative():
    with patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=[]):
        assert not generations.get_generation_by_name(generation='20171127_1500', mitype='stratocaster')


def test_get_offers_gains():
    with patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=FULL_GENERATIONS):
        actual_gains = generations.get_offers_gains(FULL_GENERATIONS[0], 10, 0.5)['gains']
        assert EXPECTED_GAINS == actual_gains


def test_make_recent_generation_info():
    packages = {'yandex-market-models': '2022.1.2662.0'}
    expected_recent_generation_info = {
        'name': '20171127_1518',
        'mi_ver': '2017.1.1931|4661684|18_1914',
        'half_mode': False,
        'successful': True,
        'build_time': 3600.0,
        'pub_time': None,
        'num_offers': 1000 * 1000,
        'num_blue_offers': 1000 * 1000,
        'num_red_offers': 500 * 1000,
        'num_offers_with_cluster_good': 0,
        'num_offers_with_model': 0,
        'num_offers_with_thumbs': 0,
        'gains': EXPECTED_GAINS,
        'cancelled': False
    }
    expected_recent_generation_info.update(packages)
    patch_recent = patch('market.idx.api.backend.tsum.get_recent_generation', auto_spec=True, return_value=FULL_GENERATIONS[0])
    patch_gains = patch('market.idx.api.backend.tsum.get_offers_gains', auto_spec=True, return_value={'gains': EXPECTED_GAINS})
    clickhouse_result = patch(
        'market.idx.api.backend.tsum.get_package_versions',
        auto_spec=True,
        return_value=packages
    )
    with patch_recent, patch_gains, clickhouse_result:
        actual_recent_generation_info = tsum.make_recent_generation_info('stratocaster', 10, 0.5)
        assert expected_recent_generation_info == actual_recent_generation_info


GENERATIONS_FOR_STATS = [
    # комбинируем варианты присутствия статистик в текущем поколении и истории
    generations.Generation(DictWrapper({
        'mitype': 'stratocaster',
        'half_mode': False,
        'release_date': None,
        'type': 'FULL',
        'state': None,
        'fail_reason': '',
        'ok': 1,
        'released': 1,
        'mbo_stuff': '20171127_1407',

        'name': '20171127_1518',
        'start_date': '2017-11-27 15:18:00',
        'end_date': '2017-11-27 16:18:00',
        'num_offers': 100,
        'num_blue_offers': 50,
        'num_red_offers': 50,
    })),
    generations.Generation(DictWrapper({
        'mitype': 'stratocaster',
        'half_mode': False,
        'release_date': None,
        'type': 'FULL',
        'state': None,
        'fail_reason': '',
        'ok': 1,
        'released': 1,
        'mbo_stuff': '20171127_1407',

        'name': '20171127_1302',
        'start_date': '2017-11-27 13:02:00',
        'end_date': '2017-11-27 15:02:00',
        'num_offers': 200,
        'num_blue_offers': 50,
        'num_offers_with_cluster_good': 50,
    })),
    generations.Generation(DictWrapper({
        'mitype': 'stratocaster',
        'half_mode': False,
        'release_date': None,
        'type': 'FULL',
        'state': None,
        'fail_reason': '',
        'ok': 1,
        'released': 1,
        'mbo_stuff': '20171127_1407',

        'name': '20171127_1130',
        'start_date': '2017-11-27 11:30:00',
        'end_date': '2017-11-27 13:00:00',
        'num_offers': 150,
        'num_offers_with_cluster_good': 100,
    }))
]

DEFAULT_MIN_RATIO = 0.8
DEFAULT_MAX_RATIO = 1.3


def test_get_generation_stats():
    expected_stats = {
        "build_time": {  # есть и в предыдущих, и в текущем
            "avg_value": 6300.0,
            "max_ratio": DEFAULT_MAX_RATIO,
            "min_ratio": DEFAULT_MIN_RATIO,
            "ratio": 0.5714285714285714,
            "value": 3600.0
        },
        "num_blue_offers": {  # есть в текущем, но не во всех предыдущих
            "avg_value": 50.0,
            "max_ratio": DEFAULT_MAX_RATIO,
            "min_ratio": DEFAULT_MIN_RATIO,
            "ratio": 1.0,
            "value": 50
        },
        "num_offers": {  # есть и в предыдущих, и в текущем
            "avg_value": 175.0,
            "max_ratio": DEFAULT_MAX_RATIO,
            "min_ratio": DEFAULT_MIN_RATIO,
            "ratio": 0.5714285714285714,
            "value": 100,
        },
        "num_offers_with_cluster_good": {  # было в предыдущих, нет в текущем
            "avg_value": 75.0,
            "max_ratio": DEFAULT_MAX_RATIO,
            "min_ratio": DEFAULT_MIN_RATIO,
            "ratio": 0,
            "value": 0,
        },
        "num_offers_with_model": {  # не было в предыдущих, нет в текущем
            "avg_value": 0,
            "max_ratio": DEFAULT_MAX_RATIO,
            "min_ratio": DEFAULT_MIN_RATIO,
            "ratio": 0,
            "value": 0,
        },
        "num_offers_with_thumbs": {  # не было в предыдущих, нет в текущем
            "avg_value": 0,
            "max_ratio": DEFAULT_MAX_RATIO,
            "min_ratio": DEFAULT_MIN_RATIO,
            "ratio": 0,
            "value": 0,
        },
        "num_red_offers": {  # не было в предыдущих, есть в текущем
            "avg_value": 1,
            "max_ratio": DEFAULT_MAX_RATIO,
            "min_ratio": DEFAULT_MIN_RATIO,
            "ratio": 1,
            "value": 50,
        },

    }
    with patch('market.idx.api.backend.generations.get_generations', auto_spec=True, return_value=GENERATIONS_FOR_STATS):
        actual_stats = generations.get_offers_gains(GENERATIONS_FOR_STATS[0], 10, 1)['stats']
        assert expected_stats == actual_stats
