#!/usr/bin/python
# -*- coding: utf-8 -*-
import logging
import os
import subprocess
import sys

from tempfile import mkdtemp
from shutil import rmtree

import yatest.common


log = logging.getLogger(__name__)

OUTPUT_DIR = None
COUNTRIES = {
    'ru': 'https://market.yandex.ru',
    'ru_blue': 'https://pokupki.market.yandex.ru',
    'ru_touch': 'https://m.market.yandex.ru',
    'ru_blue_touch': 'https://m.pokupki.market.yandex.ru',
}


def setup_module(module):
    global OUTPUT_DIR

    OUTPUT_DIR = mkdtemp()
    log.info('Generating sitemap files into %s directory', OUTPUT_DIR)

    params = [
        '--out-dir', OUTPUT_DIR,
        '--stats-log', OUTPUT_DIR + 'sitemap_generator_source_stats.log',
        '--geobase-info',  yatest.common.source_path('market/sitemap/test/data/geobase.xml'),
        '--geobase-tree', yatest.common.source_path('market/sitemap/test/data/geo.c2p'),
        '--models-path', yatest.common.source_path('market/sitemap/test/data/models/'),
        '--catalog-dump', yatest.common.source_path('market/sitemap/test/data/cataloger.catalog_dump.xml'),
        '--mbo-navigation', yatest.common.source_path('market/sitemap/test/data/cataloger.navigation.xml'),
        '--shops-data', yatest.common.source_path('market/sitemap/test/data/shops.dat'),
        '--models-for-koldunshik', yatest.common.source_path('market/sitemap/test/data/models_for_koldunshik.txt'),
        '--predefined', yatest.common.source_path('market/sitemap/etc/predefined_urls.txt'),
        '--recipes', yatest.common.source_path('market/sitemap/test/data/recipes.xml'),
        '--promo-recipes', yatest.common.source_path('market/sitemap/test/data/recipes.pb'),
        '--models-questions', yatest.common.source_path('market/sitemap/test/data/models-questions.json'),
        '--models-versus', yatest.common.source_path('market/sitemap/test/data/models-versus.tsv'),
        '--keep-xml', '1',
        '--result-ttl-hours', '0',
        '--new-search-pages', yatest.common.source_path('market/sitemap/test/data/requests_for_sitemap_new_search_pages.tsv'),
        # Провоцируем креш генератора тут
        '--offers-sns-main-pages',  'market/some_incorrect_path.xml',
        '--search-pages',  'market/some_incorrect_path.xml',
    ]
    for country in COUNTRIES:
        params.extend(['--url-{}'.format(country), COUNTRIES[country]])

    generator_path = yatest.common.binary_path('market/sitemap/generator/generator')
    subprocess.call([generator_path] + params, stdout=sys.stdout, stderr=sys.stderr)


def teardown_module(module):
    log.info('Removing %s directory', OUTPUT_DIR)
    rmtree(OUTPUT_DIR)


def test_sitemap_file():
    """Check for missing sitemaps.tar"""
    assert not os.path.exists(os.path.join(OUTPUT_DIR, 'sitemaps.tar'))


def test_errorfile():
    """Checking for error file"""
    assert os.path.exists(os.path.join(OUTPUT_DIR, 'generator.error.lock'))
