# -*- coding: utf-8 -*-
import pytest
import os

import market.pylibrary.yenv as yenv
import yatest
import yt.wrapper as yt

from freezegun import freeze_time

from suggests_storage import SuggestsStorage
from generator import suggests_dumper
import logging

log = logging.getLogger('conftest')


def make_config():
    with open(yatest.common.source_path('market/guru-models-dumper/py_test/etc/config.tpl')) as tpl_file:
        tpl = tpl_file.read()

    path = yatest.common.build_path('suggests_generator.cfg')

    with open(path, 'w') as f:
        f.write(tpl.format(data=yatest.common.source_path('market/guru-models-dumper/py_test/data')))

    return path


def prepare_yt_data():
    """
    Подготовка тестовой табоицы популярности, если вносите сюда изменения стоит так же отредактировать аналогичную таблицу
    в ../generator/suggest_generator.py, чтобы найти место в коде можно погрепать по одному из query.
    Это нужно для консистентности новых тестов и старого скрипта для генерации тестовой выгрузки, который не использует
    pytest
    """
    log.info('YT_PROXY: {}'.format(os.environ['YT_PROXY']))
    yt_client = yt.YtClient(os.environ["YT_PROXY"])
    yt_client.create('table', '//home/current_result_table')

    yt.write_table('//home/current_result_table', [
        {
            'query': u'"angry birds", 52х50 см',
            'market_suggest_total': '0',
            'refuses_count_after_inner': '0',
            'type_in_searches_total': '0',
            'weight': 1234
        },
        {
            'query': u'1 toy angry birds (т59159)',
            'market_suggest_total': '0',
            'refuses_count_after_inner': '0',
            'type_in_searches_total': '0',
            'weight': 4321
        },
        {
            'query': u'поисковая подсказка',
            'market_suggest_total': '0',
            'refuses_count_after_inner': '0',
            'type_in_searches_total': '0',
            'weight': 100
        },
        {
            'query': u'я поисковая подсказка',
            'market_suggest_total': '0',
            'refuses_count_after_inner': '0',
            'type_in_searches_total': '0',
            'weight': 123
        },
        {
            'query': u'21-le fou',
            'market_suggest_total': '0',
            'refuses_count_after_inner': '0',
            'type_in_searches_total': '0',
            'weight': 121
        },
        {
            'query': u'performa',
            'market_suggest_total': '0',
            'refuses_count_after_inner': '0',
            'type_in_searches_total': '0',
            'weight': 120
        },
    ])


@pytest.fixture(scope='session')
@freeze_time('2016-12-31')
def gendir():
    yenv.set_environment_type(yenv.DEVELOPMENT)

    path_to_config = make_config()
    prepare_yt_data()

    dumper = suggests_dumper.Dumper(path_to_config)
    generation_dir = dumper.run()
    dumper.remove_old_generations(2)
    return generation_dir


@pytest.fixture(scope='session')
def suggest_storage(gendir):
    return SuggestsStorage(gendir)
