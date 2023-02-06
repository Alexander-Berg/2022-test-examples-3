#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import os
import pytest
import yatest.common

from market.pylibrary.mindexerlib import util
from market.idx.marketindexer.marketindexer import promo_secondary_offers_converter
from market.idx.marketindexer.yatf.test_env import MarketIndexer
from market.idx.marketindexer.yatf.resources.common_ini import CommonIni
from mock import patch


def report_mock_data():
    return json.dumps({'result': [
        {
            "feed": 321,
            "offers": [
                "offer_1",
                "offer_2"
            ]
        },
        {
            "feed": 123,
            "offers": [
                "abc",
                "def"
            ]
        },
    ]})


@pytest.fixture(scope='module', name='report_mock_data')
def report_mock_data_fixture():
    return report_mock_data()


def promo_secondary_offers_converter_dir():
    return os.path.join(yatest.common.work_path(), 'promo_secondary_offers_converter_dir')


@pytest.fixture(scope='module', name='promo_secondary_offers_converter_dir')
def promo_secondary_offers_converter_dir_fixture():
    return promo_secondary_offers_converter_dir()


@pytest.yield_fixture(scope='module')
def absolute_converter_bin_path():
    relative_bin_path = os.path.join(
        'market',
        'tools',
        'promo_secondary_offers_converter',
        'bin',
    )
    return yatest.common.binary_path(relative_bin_path)


@pytest.yield_fixture(scope='function')
def workflow(
        yt_server,
        absolute_converter_bin_path,
        promo_secondary_offers_converter_dir
):
    resources = {
        'common_ini': CommonIni(
            os.path.join(yatest.common.work_path(), 'common.ini'),
            yatest.common.work_path(),
            misc={
                'promo_secondary_offers_converter_enabled': 'true',
                'promo_secondary_offers_converter_dir': promo_secondary_offers_converter_dir,
            },
            bin={
                'promo_secondary_offers_converter': absolute_converter_bin_path,
            },
        ),
    }

    with MarketIndexer(yt_server, **resources) as env:
        yield env


def __mocked_requests_get(*args, **kwargs):
    class MockResponse:
        def __init__(self, content):
            self.content = content

        def content(self):
            return self.content

        def raise_for_status(self):
            pass

    return MockResponse(report_mock_data())


class MockConfig(object):
    def __init__(self):
        self.promo_secondary_offers_converter = yatest.common.binary_path("market/tools/promo_secondary_offers_converter/bin/promo-secondary-offers-converter")
        self.promo_secondary_offers_converter_dir = promo_secondary_offers_converter_dir()
        self.promo_secondary_offers_converter_report_proxy = 'mock'


def test_query_report(workflow, report_mock_data, promo_secondary_offers_converter_dir):
    log = util.get_func_log()
    with patch('requests.get', side_effect=__mocked_requests_get):
        # из-за подмены тестового ответа код надо выдывать напрямую, не запуском mindexer (он стартует новый процесс, патчинг теряется)
        # workflow.execute(clt_command_args_list=['promo_secondary_offers_converter'])
        config = MockConfig()
        promo_secondary_offers_converter.__query_report(config, log)

    file_path = os.path.join(promo_secondary_offers_converter_dir, promo_secondary_offers_converter.JSON_FILE_NAME)
    assert json.dumps(json.load(open(file_path))) == report_mock_data

    promo_secondary_offers_converter.__convert_file(config, log)
