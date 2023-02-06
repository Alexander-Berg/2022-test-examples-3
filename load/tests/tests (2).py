import job_page.artifacts.tests.mocks

from common.tests import CommonTestCase
from job_page.artifacts.tank import TankAPICommunicator
from unittest.mock import patch


class ArtifactsTest(CommonTestCase):
    @patch('job_page.artifacts.tank.TankAPICommunicator', spec=TankAPICommunicator)
    def simple_get_artifact(self, ):
        self.client.get('/artifacts/')
