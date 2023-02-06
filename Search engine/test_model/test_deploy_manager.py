# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from search.martylib.db_utils import to_model

from search.priemka.yappy.proto.structures.beta_component_pb2 import BetaComponent
from search.priemka.yappy.sqla.yappy import model

from search.priemka.yappy.src.model.model_service.workers.deploy_manager import DeployManager
from search.priemka.yappy.src.yappy_lib.config_utils import get_test_config

from search.priemka.yappy.tests.utils.test_cases import TestCase


class DeployManagerTestCase(TestCase):

    @classmethod
    def setUpClass(cls):
        try:
            super(DeployManagerTestCase, cls).setUpClass()
            cls.deploy_client_patch = mock.patch('search.priemka.yappy.src.yappy_lib.clients.ClientsMock.deploy')
            cls.session_scope_patch = mock.patch('search.priemka.yappy.src.model.model_service.workers.deploy_manager.session_scope')
            cls.deploy_client = cls.deploy_client_patch.start()
            cls.deploy_manager = DeployManager(get_test_config())
            cls.session_scope = cls.session_scope_patch.start()
        except Exception:  # in Py3, use `addClassCleanup` instead of try/except
            cls._doTearDownClass()

    @classmethod
    def tearDownClass(cls):
        cls._doTearDownClass()

    @classmethod
    def _doTearDownClass(cls):
        cls.deploy_client_patch.stop()
        cls.session_scope_patch.stop()

    def setUp(self):
        self.deploy_client.reset_mock()

    def test_create_slot_dont_translate_secrets(self):
        expected = False
        component = BetaComponent()
        component.type.deploy.stage_template_id = 'dummy_stage'
        component.type.deploy.deploy_unit = 'dummy_unit'

        self.deploy_manager.create_slot(to_model(component))
        passed_flag = self.deploy_client.copy_stage.call_args.kwargs['translate_secrets']

        self.assertEqual(passed_flag, expected)

    def test_create_slot_translate_secrets(self):
        expected = True
        component = BetaComponent(translate_secrets=True)
        component.type.deploy.stage_template_id = 'dummy_stage'
        component.type.deploy.deploy_unit = 'dummy_unit'

        self.deploy_manager.create_slot(to_model(component))
        passed_flag = self.deploy_client.copy_stage.call_args.kwargs['translate_secrets']

        self.assertEqual(passed_flag, expected)

    def test_create_slot_saved(self):
        component = BetaComponent()
        component.type.deploy.stage_template_id = 'dummy_stage'
        component.type.deploy.deploy_unit = 'dummy_unit'

        self.deploy_manager.create_slot(to_model(component))
        session = self.session_scope().__enter__()
        saved = session.add.call_args.args[0]

        self.assertIsInstance(saved, model.Slot)
