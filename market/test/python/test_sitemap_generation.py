#!/usr/bin/python
# -*- coding: utf-8 -*-
import logging
import os
import re
import subprocess
import sys

from tempfile import mkdtemp
from shutil import rmtree
import xml.etree.ElementTree as ET

from util import s3_config
from util import s3_uploader
import yatest.common


log = logging.getLogger(__name__)

OUTPUT_DIR = None
TEMPORARY_DIR = "tmp"
COUNTRIES = {
    'ru': 'https://market.yandex.ru',
    'ru_blue': 'https://pokupki.market.yandex.ru',
    'ru_touch': 'https://m.market.yandex.ru',
    'ru_blue_touch': 'https://m.pokupki.market.yandex.ru',
}


class FakeS3Client(object):
    def list(self, bucket, path):
        return ['sitemap-dev/100', 'sitemap-dev/200', 'sitemap-dev/300', 'sitemap-dev/400']

    def delete(self, bucket, path):
        return

    def get_or_create(self, bucket):
        return None

    def upload_file(self, bucket, upload_src, file):
        return 'test-bucket/{}'.format(upload_src)

    def make_symlink(self, bucket, upload_src, upload_dst):
        return 'test-bucket/recent'


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
        '--offers-sns-main-pages',  yatest.common.source_path('market/sitemap/test/data/offers_sns_main_pages.tsv'),
        '--search-pages', yatest.common.source_path('market/sitemap/test/data/requests_for_sitemap_search_pages.tsv'),
        '--new-search-pages', yatest.common.source_path('market/sitemap/test/data/requests_for_sitemap_new_search_pages.tsv'),
        '--keep-xml', '1',
        '--result-ttl-hours', '0',
    ]
    for country in COUNTRIES:
        params.extend(['--url-{}'.format(country), COUNTRIES[country]])

    generator_path = yatest.common.binary_path('market/sitemap/generator/generator')
    subprocess.check_call([generator_path] + params, stdout=sys.stdout, stderr=sys.stderr)


def teardown_module(module):
    log.info('Removing %s directory', OUTPUT_DIR)
    rmtree(OUTPUT_DIR)


def test_s3_uploader():
    uploader = s3_uploader.S3Uploader(s3_config.Config(yatest.common.source_path('market/sitemap/test/data/test.args')), None, FakeS3Client())
    uploader.run()


def _make_test(filepath, regex):
    tree = ET.parse(filepath)
    root = tree.getroot()
    for child in root.findall("./{http://www.sitemaps.org/schemas/sitemap/0.9}url/{http://www.sitemaps.org/schemas/sitemap/0.9}loc"):
        assert re.match(regex, child.text), 'Invalid url {}'.format(child.text)


def test_sns_offers():
    """Checking format of sns-offers urls"""
    for country in COUNTRIES:
        # SnS sitemap should be generated only for White Market in Russia region
        if country not in ('ru', 'ru_touch'):
            assert not os.path.exists(os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-offers-sns-000.xml'))
        else:
            assert os.path.exists(os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-offers-sns-000.xml'))

            _make_test(
                # https://market.yandex.ru/business--f5it/862596
                os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-offers-sns-000.xml'),
                r'^{}(/business--.*/\d+)$'
                    .format(COUNTRIES[country]),
            )


def test_search_pages():
    """Checking format of search pages urls"""
    for country in COUNTRIES:
        if country not in ('ru', 'ru_touch'):
            assert not os.path.exists(os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-search-pages-000.xml'))
        else:
            assert os.path.exists(os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-search-pages-000.xml'))

            _make_test(
                os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-search-pages-000.xml'),
                r'^{}/search\?text=.*'
                    .format(COUNTRIES[country]),
            )


def test_new_search_pages():
    """Checking format of new search pages urls"""
    for country in COUNTRIES:
        if country not in ('ru', 'ru_touch'):
            assert not os.path.exists(os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-new-search-pages-000.xml'))
        else:
            assert os.path.exists(os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-new-search-pages-000.xml'))

            _make_test(
                os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-new-search-pages-000.xml'),
                r'^{}/search\?text=.*'
                    .format(COUNTRIES[country]),
            )


def test_reviews():
    """Checking format of reviews urls in categories"""
    for country in COUNTRIES:
        if country in ('ru_blue', 'ru_blue_touch'):
            # Reviews sitemap should be generated only for white market
            assert not os.path.exists(os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-review-000.xml'))
        else:
            _make_test(
                os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-review-000.xml'),
                # https://market.yandex.ru/catalog--muzhskie-krossovki-i-kedy/57444/list?hid=7815007&show-reviews=1&glfilter=7893318:8339653
                r'^{}/catalog--[\w-]+/\d+/list\?hid=\d+&show-reviews=1'.format(COUNTRIES[country]),
            )


def test_recipes():
    """Checking format of recipe urls"""
    _make_test(
        os.path.join(OUTPUT_DIR, TEMPORARY_DIR, "ru", 'sitemap-recipes-000.xml'),
        r'(^{}/catalog--[\w-]+/\d+/list\?hid=\d+)|(^{}/catalog--[\w-]+/\d+/list\?recipe-id=\d+&hid=\d+)'.format(COUNTRIES["ru"], COUNTRIES["ru"]),
    )


def test_nav():
    """Checking format of catalog urls"""
    for country in COUNTRIES:
        regex = None
        if country in ('ru_blue', 'ru_blue_touch'):
            # https://pokupki.market.yandex.ru/catalog/telefony-i-aksessuary-k-nim/54437?hid=91461
            regex = r'.*/catalog/[\w-]+/\d+(/list)?\?hid=\d+'
        else:
            # https://market.yandex.ru/catalog--telefony-i-aksessuary-k-nim/54437?hid=91461
            regex = r'.*/catalog--[\w-]+/\d+(/list)?\?hid=\d+'

        _make_test(os.path.join(OUTPUT_DIR, TEMPORARY_DIR, country, 'sitemap-nav-000.xml'), regex)


def test_errorfile():
    """Check for missing error file"""
    assert not os.path.exists(os.path.join(OUTPUT_DIR, 'generator.error.lock'))
