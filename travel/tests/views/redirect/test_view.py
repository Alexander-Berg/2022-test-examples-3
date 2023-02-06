# -*- encoding: utf-8 -*-
import json
import mock
import pytest
from six.moves.urllib.parse import urlparse, parse_qsl

from django.test import override_settings

from travel.avia.ticket_daemon.tests.views.redirect import utils
from travel.avia.ticket_daemon.ticket_daemon.partners.__test_partner_module import Behavior


def redirect_with_behavior(behavior):
    return utils.api_cook_redirect({
        'order_data': {
            'partner': utils.TEST_PARTNER,
            'behavior': behavior,
        }
    })


@pytest.mark.dbuser
def test__no_partner__400():
    response = utils.api_cook_redirect({
        'order_data': {
            'partner': utils.TEST_PARTNER,
        }
    })
    assert response.status_code == 400, response.data


@pytest.mark.dbuser
@mock.patch('travel.avia.ticket_daemon.ticket_daemon.api.yaclid.YaClid.dumps', return_value='demo_yaclid')
def test__return_url__200(mock_yaclid):
    utils.create_test_partner_with_module()
    response = redirect_with_behavior(Behavior.URL)
    data = json.loads(response.get_data(as_text=True))

    assert response.status_code == 200, response.data
    assert data == {'url': 'http://example.com?yaclid=demo_yaclid'}


@pytest.mark.dbuser
def test__bad_request__400():
    utils.create_test_partner_with_module()
    response = redirect_with_behavior(Behavior.BAD_REQUEST)

    assert response.status_code == 400, response.data


@pytest.mark.dbuser
def test__error_redirect__500():
    utils.create_test_partner_with_module()
    response = redirect_with_behavior(Behavior.ERROR_VIEW)

    assert response.status_code == 500, response.data


@pytest.mark.dbuser
@mock.patch('travel.avia.ticket_daemon.ticket_daemon.api.redirect.generate_marker', return_value='testmarker')
@mock.patch('travel.avia.ticket_daemon.ticket_daemon.api.yaclid.YaClid.dumps', return_value='demo_yaclid')
@override_settings(USE_BACKEND_MARKER_GENERATION=True)
def test__get_request_marker(mock_yaclid, mock_marker):
    mock_marker.__name__ = 'trivial_marker_generator'

    utils.create_test_partner_with_module()
    response = redirect_with_behavior(Behavior.URL)
    data = json.loads(response.get_data(as_text=True))

    assert response.status_code == 200, response.data

    url = urlparse(data['url'])
    assert 'http' == url.scheme
    assert 'example.com' == url.netloc
    assert '' == url.path

    query = dict(parse_qsl(url.query, keep_blank_values=True))
    assert query == {
        'marker': 'testmarker',
        'yaclid': 'demo_yaclid',
    }
