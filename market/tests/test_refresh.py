# -*- coding: utf-8 -*-

import pytest

import flask

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage

from hamcrest import assert_that
from utils import is_success_response


class StorageMock(Storage):
    def get_feed_refresh_flag(self, feed_id=None):
        return {
            "feed_111": {
                "refresh_ts": "1518621721916043",
                "refresh": "1"
            },
            "feed_1069": {
                "refresh_ts": "1518716964711961",
                "refresh": "1"
            },
            "feed_317183": {
                "refresh": "1"
            }}


@pytest.fixture(scope="module")
def test_app():
    return create_flask_app(StorageMock())


def test_show_refresh_flags_for_all_feeds(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/refreshes')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert len(data) == 3
        assert data['feed_1069']
        assert len(data['feed_1069']) == 2
        assert data['feed_1069']['refresh'] == "1"
        assert data['feed_1069']['refresh_ts'] == "1518716964711961"

        assert data['feed_317183']
        assert len(data['feed_317183']) == 1
