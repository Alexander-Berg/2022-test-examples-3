__author__ = 'aokhotin'
import requests

import pytest


def test_simple_response_200(pytestconfig):
    response = requests.get(pytestconfig.option.beta, verify=False)
    try:
        response.raise_for_status()
    except Exception, e:
        pytest.fail('Bad response status: {}'.format(response.status_code))
