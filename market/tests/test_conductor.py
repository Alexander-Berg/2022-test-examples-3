#!/usr/bin/env python
# -*- coding: utf-8 -*-

import mock
from mock import patch
from library.python import resource

from market.tools.resource_monitor.lib.client import Client
from market.tools.resource_monitor.lib.conductor import ConductorClient


def conductor_response(request_url, data=None):
    if request_url.find('api/projects') != -1:
        data_str = resource.find('/data/conductor_projects_response.xml')
    elif request_url.find('projects2hosts') != -1:
        data_str = resource.find('/data/conductor_hosts_response.tsv')
    else:
        return None
    return data_str


@patch.object(Client, '_get')
def test_projects(method):
    method.side_effect = conductor_response
    x = ConductorClient()
    projects = x.get_projects()
    method.assert_called_once()
    assert len(projects) == 5
    assert filter(lambda p: p.name == 'pricelabs', projects)

    hosts = x.get_hosts(project='project1')
    assert(len(hosts) == 4)

