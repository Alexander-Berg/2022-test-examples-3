# -*- coding: utf-8 -*-

import pytest

from hamcrest import (
    assert_that,
    all_of,
    has_key
)

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage


class StorageMock(Storage):
    def get_feeds(self):
        return [1069, 9997, 10002]


@pytest.fixture(scope="module")
def test_app():
    return create_flask_app(StorageMock())


def test_response_has_mitype_and_env_type(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds')
        assert_that(
            resp.headers,
            all_of(has_key('X-Market-ENVTYPE'), has_key('X-Market-MITYPE'))
        )
