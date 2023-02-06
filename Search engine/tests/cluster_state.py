# coding: utf-8

from components_app.api.cluster_state import ClusterStateApi
from components_app.configs.base import cluster_state as cluster_state_config
from components_app.tests.base import BaseApiTestCase


class TestClusterStateApi(BaseApiTestCase):
    def __init__(self, methodName='runTest'):
        super(TestClusterStateApi, self).__init__(methodName)
        self.api = ClusterStateApi()
        self.api.load_config(config=cluster_state_config)

    def test_pnetwork(self):
        # result = self.api.pnetwork()
        # self.assertNotEmptyDict(result)
        pass
