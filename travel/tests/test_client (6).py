# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
import httpretty
import mock
import os

from travel.rasp.library.python.api_clients.deploy_public_api.client import DeployPublicApiClient


RESPONSE = {
    'node_meta': {
        'dc': 'sas',
        'fqdn': 'sas3-5396.search.yandex.net'
    },
    'box_resource_requirements': {
        'box_42': {
            'cpu': {
                'cpu_limit_millicores': 2001.24,
                'cpu_guarantee_millicores': 1005.78
            },
            'memory': {
                'memory_guarantee_bytes': 33554432,
                'memory_limit_bytes': 67108864
            }
        }
    }
}


class TestDeployPublicClient(object):
    def test_box_id(self):
        with mock.patch.dict(os.environ, {'DEPLOY_BOX_ID': 'box_42'}):
            assert DeployPublicApiClient.get_current_box_id() == 'box_42'

    @httpretty.activate
    def test_box_requirements(self):
        httpretty.register_uri(
            httpretty.GET, 'http://localhost:1/pod_attributes',
            content_type='application/json',
            body=json.dumps(RESPONSE)
        )

        client = DeployPublicApiClient()
        assert client.get_pod_attributes() == RESPONSE

        with mock.patch.dict(os.environ, {'DEPLOY_BOX_ID': 'box_42'}):
            assert client.get_current_box_requirements() == RESPONSE['box_resource_requirements']['box_42']

        assert client.get_current_box_requirements() is None
