from common.tests import CommonTestCase
from .views import _prepare_config
import json

from unittest import skip


class JobRepeatTest(CommonTestCase):

    # @skip('Empty monitoring config')
    def test_prepare_yaml(self):
        aa = _prepare_config(self.job1, 'lunapark')
        self.assertEqual(aa, 0)

    def test_project_ammo(self):
        ammo = json.loads(self.client.get(path='/firestarter/project_ammo?task={}'.format(self.job1.task)).content)
        self.assertEqual(len(ammo), 2)
