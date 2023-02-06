# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from mock import MagicMock

import requests

from travel.rasp.bus.api.connectors.client import MetaClient
from travel.rasp.bus.api.tests.factories import EndpointFactory


def test_endpoints():
    endpoints = EndpointFactory.create_batch(size=123)
    resp = requests.Response()
    resp.status_code = 200
    resp.json = MagicMock(return_value=endpoints)
    requests.get = MagicMock(return_value=resp)
    endpoints_get = list(MetaClient.endpoints(supplier_code=''))
    assert endpoints == endpoints_get
