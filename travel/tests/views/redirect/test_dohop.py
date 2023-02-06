# -*- encoding: utf-8 -*-
import json

import pytest
from mock import patch
from requests import RequestException

from travel.avia.ticket_daemon.tests.views.redirect import utils


SECRET_GETTER = 'travel.avia.ticket_daemon.ticket_daemon.lib.partner_secret_storage.partner_secret_storage.get'
EXAMPLE_SITE = 'http://example.com'


def cook_redirect():
    return utils.api_cook_redirect({
        'order_data': {
            'partner': utils.TEST_PARTNER,
            'end_point_url': EXAMPLE_SITE,
        }
    })


@pytest.mark.dbuser
def test__endpoint_error__400():
    with patch('requests.get', side_effect=RequestException()):
        with patch(SECRET_GETTER, return_value='secret'):
            utils.create_test_partner_with_module(module='dohop')
            response = cook_redirect()

            assert response.status_code == 400, response.data


@pytest.mark.dbuser
def test__bad_request__404():
    with patch('requests.get', return_value=utils.get_response(status=400)):
        with patch(SECRET_GETTER, return_value='secret'):
            utils.create_test_partner_with_module(module='dohop')
            response = cook_redirect()

            assert response.status_code == 404, response.data


@pytest.mark.dbuser
def test__redirect_error__because_no_deeplink__400():
    with patch('requests.get', return_value=utils.get_response(status=200, json_content={})):
        with patch(SECRET_GETTER, return_value='secret'):
            utils.create_test_partner_with_module(module='dohop')
            response = cook_redirect()

            assert response.status_code == 400, response.data


@pytest.mark.dbuser
def test__successful__200():
    response = utils.get_response(
        status=200,
        json_content={
            'deeplink-url': {
                'url': EXAMPLE_SITE
            }
        },
    )

    with patch('requests.get', return_value=response):
        with patch(SECRET_GETTER, return_value='secret'):
            utils.create_test_partner_with_module(module='dohop')
            response = cook_redirect()

            assert response.status_code == 200, response.data

            order_data = json.loads(response.data)
            assert EXAMPLE_SITE in order_data['url']
