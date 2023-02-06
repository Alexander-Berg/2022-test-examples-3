# -*- coding: utf-8 -*-

import flask
import pytest
import six

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage
from market.idx.api.backend.blueprints.msku import (
    buybox_won_method,
    buybox_reject_reason,
    UNHANDLED_KEY_TEXT
)

from mock import patch


WON_METHOD = 'WON_BY_AUCTION'
REJECT_REASON = 'TOO_HIGH_PRICE'
MOCK_RESPONSE = {
    'Offers': [
        {
            'Id': 7,
            'WareMd5': 'WARE1',
        },
        {
            'Id': 8,
            'WareMd5': 'WARE2',
        },
    ],
    'RejectedOffers': [
        {
            'RejectReason': REJECT_REASON,
            'Offer':
            {
                'WareMd5': 'WARE3',
            },
        },
    ],
    'Won': 7,
    'WonMethod': WON_METHOD
}


@pytest.fixture(scope='module')
def test_app():
    return create_flask_app(Storage())


def check_won_reason(response, offer_index, expected_reason):
    assert response.status_code == 200
    data = flask.json.loads(response.data)

    assert data[six.ensure_text('Все офферы с msku 1111')][offer_index]['Status'] == six.ensure_text(expected_reason)


def test_valid_request(test_app):
    with patch('market.idx.api.backend.blueprints.msku.get_buybox_trace_from_report', autospec=True, return_value=MOCK_RESPONSE),\
            test_app.test_client() as client:
        response = client.get('/v1/msku?msku=1111')
        check_won_reason(response, 0, 'Выбранный оффер')
        check_won_reason(response, 1, 'Оффер не выбран: {}'.format(buybox_won_method[WON_METHOD]))
        check_won_reason(response, 2, 'Оффер не выбран: {}'.format(buybox_reject_reason[REJECT_REASON]))


def test_unhandled_won_method(test_app):
    mock_response = MOCK_RESPONSE.copy()
    unhandled_key = 'Defenetly_Unhandled_Key'
    mock_response['WonMethod'] = unhandled_key
    mock_response['RejectedOffers'][0]['RejectReason'] = unhandled_key
    with patch('market.idx.api.backend.blueprints.msku.get_buybox_trace_from_report', autospec=True, return_value=mock_response),\
            test_app.test_client() as client:
        response = client.get('/v1/msku?msku=1111')
        check_won_reason(response, 0, 'Выбранный оффер')
        check_won_reason(response, 1, 'Оффер не выбран: {}'.format(UNHANDLED_KEY_TEXT.format(unhandled_key)))
        check_won_reason(response, 2, 'Оффер не выбран: {}'.format(UNHANDLED_KEY_TEXT.format(unhandled_key)))
