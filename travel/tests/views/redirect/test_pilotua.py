# -*- encoding: utf-8 -*-
import pytest
from mock import patch

from travel.avia.ticket_daemon.tests.views.redirect import utils


def cook_redirect():
    return utils.api_cook_redirect({
        'order_data': {
            'partner': utils.TEST_PARTNER,
            'air_reservation': '',
        }
    })


@pytest.mark.dbuser
def test__request_error__400():
    with patch('requests.post', return_value=utils.get_response(status=400)):
        utils.create_test_partner_with_module(module='pilotua')
        response = cook_redirect()

        assert response.status_code == 400, response.data


@pytest.mark.dbuser
def test__successful__200():
    response = utils.get_response(
        status=200,
        json_content={'token': ''},
    )

    with patch('requests.post', return_value=response):
        utils.create_test_partner_with_module(module='pilotua')
        response = cook_redirect()

        assert response.status_code == 200, response.data
