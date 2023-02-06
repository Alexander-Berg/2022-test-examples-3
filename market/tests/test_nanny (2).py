#!/usr/bin/env python
# -*- coding: utf-8 -*-

from market.tools.resource_monitor.lib.nanny import NannyClient

import mock
from mock import patch
import json
from library.python import resource


def nanny_services_response(category, skip):
    if skip is None:
        data_str = resource.find('/data/nanny_services_response.json')
        return json.loads(data_str)
    assert isinstance(skip, int)
    return {'result': []}

@patch.object(NannyClient, '_request_services')
def test_services(method):
    method.side_effect = nanny_services_response
    x = NannyClient(token='token')
    services = x.get_market_services()
    method.assert_called()
    assert len(services) > 0
    assert len(services[0].groups) > 0
    assert services[0].service_id == 'adv-machine-market-robot'
    assert services[0].groups[0] == 'SAS_ADV_MACHINE_MARKET_ROBOT'

