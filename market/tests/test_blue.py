# -*- coding: utf-8 -*-

import pytest

from hamcrest import assert_that

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage
from market.idx.api.backend.blueprints.blue import BlueAssortmentScript
from market.idx.api.backend.config import get_config_paths

from utils import (
    is_success_response,
    is_bad_response,
    set_config_env_variables

)


class MockBlueScript(BlueAssortmentScript):
    def __init__(self, name, logger=None):
        super(MockBlueScript, self).__init__(name, logger)

    def call(self, query_args=None, shell=True):
        if query_args is None:
            query_args = {}
        args = self.make_args(query_args)
        return 0, str(args)

    def pipe(self, query_args=None):
        if query_args is None:
            query_args = {}
        args = self.make_args(query_args)
        yield str(args)
        yield '\n'
        yield 'X-RETURN-CODE: {0}\n'.format(0)


@pytest.fixture(scope="module")
def test_app():
    set_config_env_variables()
    app = create_flask_app(Storage())
    app.config['blue_market_suggest'] = MockBlueScript('suggest')
    app.config['blue_market_commit'] = MockBlueScript('commit')
    return app


@pytest.mark.parametrize('url', [
    '/v1/blue/commit',
    '/v1/blue/suggest',
])
def test_missing_url_param(url, test_app):
    with test_app.test_client() as client:
        resp = client.get(url)

        assert_that(resp, is_bad_response('400 Bad Request\nrequest must contain query parameter "url"'))


@pytest.mark.parametrize('method, expected_url', [
    ('/v1/blue/suggest', '/usr/lib/yandex/blue-market/suggest'),
    ('/v1/blue/commit', '/usr/lib/yandex/blue-market/commit')
])
def test_blue_suggest(method, expected_url, test_app):
    with test_app.test_client() as client:
        resp = client.get('{}?url=http%3A%2F%2Fyandex.ru'.format(method))

        expected_data = ("['{}', '--log-file=/var/log/yandex/feedparser/blue_assortment.log', "
                         "'--feedchecker-config=/etc/yandex/market-feedparser/checker-backend.cfg', "
                         "'--idxapi-common-config={}', "
                         "'--idxapi-local-config={}', "
                         "'--idxapi-local-override-config={}', "
                         "'http://yandex.ru']\nX-RETURN-CODE: 0\n".format(expected_url, *get_config_paths()))

        assert_that(resp, is_success_response(expected_data))
