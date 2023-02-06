#!/usr/bin/env python
# -*- coding: utf-8 -*-

from market.tools.resource_monitor.lib.gencfg import GenCfgClient

import mock
from mock import patch
import json
from library.python import resource


@patch.object(GenCfgClient, '_get')
def test_tags(method):
    data_str = resource.find('/data/gencfg_tags_response.json')
    method.return_value = json.loads(data_str)
    x = GenCfgClient()
    tag = x.get_latest_tag()
    method.assert_called_once()
    assert tag == 'stable-142-r72'


def gencfg_response(request_url, data=None):
    if request_url.find('searcherlookup/groups') != -1:
        data_str = resource.find('/data/gencfg_group_info_response.json')
    elif request_url.find('card') != -1:
        data_str = resource.find('/data/gencfg_card_response.json')
    else:
        return {}
    return json.loads(data_str)


@patch.object(GenCfgClient, '_get')
def test_group_info(method):
    method.side_effect = gencfg_response
    x = GenCfgClient(tag='stable-142-r72')
    g = x.get_group_info('some group')
    method.assert_called()
    assert g.instances[0].ncpu == 32
    assert g.instances[0].memory == 64
