# coding: utf-8

import six

from components_app.api.nanny.api import NannyApi
from components_app.tests.base import BaseApiTestCase


def get_attr_values(obj):
    return set((v for k, v in obj.__dict__.items() if not k.startswith('__') and not k.endswith('__')))


class TestNannyApi(BaseApiTestCase):
    def __init__(self, methodName='runTest'):
        super(TestNannyApi, self).__init__(methodName)
        self.api = NannyApi()
        self.api.load_config(config={
            'url': 'https://dev-nanny.yandex-team.ru/v2',
            'http_client': {
                'headers': {
                    'Authorization': 'OAuth AQAD-qJSJqgbAAAAjOMRUoKY50yOhRUskotJllk'
                }
            }
        })

    def test_list(self):
        response = self.api.l7.heavy.list()
        self.assertNotEmptyList(response)

    def test_get(self):
        response = self.api.l7.heavy.get(balancer_id='production')
        self.assertNotEmptyDict(response)

    def test_version(self):
        response = self.api.l7.heavy.weights.version(balancer_id='production')
        self.assertTrue(isinstance(response, six.string_types))

    def test_its_version(self):
        response = self.api.l7.heavy.weights.its_version(balancer_id='production')
        self.assertTrue(isinstance(response, six.string_types))

    def test_values(self):
        response = self.api.l7.heavy.weights.values(balancer_id='production')
        self.assertNotEmptyDict(response)

    def test_sections(self):
        response = self.api.l7.heavy.weights.sections(balancer_id='production')
        self.assertNotEmptyList(response)

    def test_section(self):
        response = self.api.l7.heavy.weights.section(balancer_id='production', section_id='images')
        self.assertNotEmptyDict(response)

    def test_section_values(self):
        response = self.api.l7.heavy.weights.section_values(balancer_id='production', section_id='images')
        self.assertNotEmptyDict(response)

    def test_snapshots_get(self):
        response = self.api.l7.heavy.weights.snapshots.get(balancer_id='production',
                                                           snapshot_id='30093242439cc906ccd5e66db6164562b3ff828e')
        self.assertNotEmptyDict(response)

    def test_snapshots_list(self):
        response = self.api.l7.heavy.weights.snapshots.list(balancer_id='production', limit=10, skip=5)
        self.assertNotEmptyList(response)

    def test_active_snapshot(self):
        response = self.api.l7.heavy.weights.active_snapshot(balancer_id='production')
        self.assertNotEmptyDict(response)
