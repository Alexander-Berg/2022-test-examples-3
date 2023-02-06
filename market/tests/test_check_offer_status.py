# -*- coding: utf-8 -*-

import pytest
import six
from six.moves.urllib.parse import urlencode

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage


@pytest.fixture(scope="module")
def app():
    return create_flask_app(Storage())


def test_check_white_offer_status_is_not_susceptible_to_xss(app):
    params = {
        'rgb': 'white',
        'shop_id': 0,
        'business_id': 0,
        'feed_id': '',
        'offer_id': '<script>alert(`XSS`)</script>',
        'whid': ''
    }
    url = '/v1/admin/dukalis/check_offer_status/offer?' + urlencode(params)
    with app.test_client() as client:
        resp = client.get(url)
        assert resp.status_code == 200
        assert '<script>' not in six.ensure_str(resp.data)


def test_check_blue_offer_status_is_not_susceptible_to_xss(app):
    params = {
        'rgb': 'blue',
        'business_id': 1,
        'shop_id': 1,
        'feed_id': 1,
        'offer_id': '<script>alert(`XSS`)</script>',
        'whid': ''
    }
    url = '/v1/admin/dukalis/check_offer_status/offer?' + urlencode(params)
    with app.test_client() as client:
        resp = client.get(url)
        assert resp.status_code == 200
        assert '<script>' not in six.ensure_str(resp.data)
