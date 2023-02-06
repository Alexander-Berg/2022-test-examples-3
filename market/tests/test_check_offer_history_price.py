# -*- coding: utf-8 -*-

import pytest
import six
from six.moves.urllib.parse import urlencode

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage


@pytest.fixture(scope="module")
def app():
    return create_flask_app(Storage())


def test_check_offer_history_price_is_not_susceptible_to_xss(app):
    params = {
        'shop_id': '',
        'offer_id': '<script>alert(`XSS`)</script>',
        'whids': '',
        'date_for_check': ''
    }
    url = '/v1/admin/dukalis/check_offer_history_price/offer?' + urlencode(params)
    with app.test_client() as client:
        resp = client.get(url)
        assert resp.status_code == 200
        assert '<script>' not in six.ensure_str(resp.data)
