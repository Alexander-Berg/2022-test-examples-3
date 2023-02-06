# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from search.priemka.yappy.proto.structures.check_pb2 import Check
from search.priemka.yappy.proto.structures.bolver_pb2 import BolverConfiguration
from search.priemka.yappy.proto.structures.beta_pb2 import Beta

from search.priemka.yappy.src.processor.modules.verificator.checks.abc import RunResult
from search.priemka.yappy.src.processor.modules.verificator.checks import stoker
from search.priemka.yappy.src.yappy_lib.config_utils import get_test_config

from search.priemka.yappy.tests.utils.test_cases import TestCase


class StokerCheckTest(TestCase):
    @classmethod
    def setUpClass(cls):
        cls.config = get_test_config()

    def setUp(self):
        self.beta = Beta(name='beta-name', checks=[Check(check_class='stoker.CheckBolverConfig')])

    def test_not_yet_pushed(self):
        target_cfg = BolverConfiguration(params=['some_param'])
        self.beta.target_state.bolver_configuration.CopyFrom(target_cfg)
        check = stoker.CheckBolverConfig(self.config, self.beta.checks[0])
        with mock.patch.object(check.clients.stoker, 'get_bolver_configuration') as mocked:
            mocked.return_value = BolverConfiguration()
            result = check._run(self.beta)
        expected = RunResult(False, stoker.CheckBolverConfig.ResultMessage.NOT_PUSHED.value)
        self.assertEqual(result, expected)

    def test_should_not_be_pushed(self):
        check = stoker.CheckBolverConfig(self.config, self.beta.checks[0])
        with mock.patch.object(check.clients.stoker, 'get_bolver_configuration') as mocked:
            mocked.return_value = BolverConfiguration(params=['some_param'])
            result = check._run(self.beta)
        expected = RunResult(False, stoker.CheckBolverConfig.ResultMessage.UNEXPECTEDLY_PUSHED.value)
        self.assertEqual(result, expected)

    def test_outdated(self):
        target_cfg = BolverConfiguration(params=['some_param', 'another_param'])
        self.beta.target_state.bolver_configuration.CopyFrom(target_cfg)
        check = stoker.CheckBolverConfig(self.config, self.beta.checks[0])
        with mock.patch.object(check.clients.stoker, 'get_bolver_configuration') as mocked:
            mocked.return_value = BolverConfiguration(params=['some_param'])
            result = check._run(self.beta)
        expected = RunResult(False, stoker.CheckBolverConfig.ResultMessage.OUTDATED.value)
        self.assertEqual(result, expected)

    def test_ok(self):
        target_cfg = BolverConfiguration(params=['some_param'])
        self.beta.target_state.bolver_configuration.CopyFrom(target_cfg)
        check = stoker.CheckBolverConfig(self.config, self.beta.checks[0])
        with mock.patch.object(check.clients.stoker, 'get_bolver_configuration') as mocked:
            mocked.return_value = BolverConfiguration(params=['some_param'])
            result = check._run(self.beta)
        expected = RunResult(True, stoker.CheckBolverConfig.ResultMessage.OK.value)
        self.assertEqual(result, expected)

    def test_ok_empty(self):
        check = stoker.CheckBolverConfig(self.config, self.beta.checks[0])
        with mock.patch.object(check.clients.stoker, 'get_bolver_configuration') as mocked:
            mocked.return_value = BolverConfiguration()
            result = check._run(self.beta)
        expected = RunResult(True, stoker.CheckBolverConfig.ResultMessage.OK.value)
        self.assertEqual(result, expected)

    def test_run(self):
        check = stoker.CheckBolverConfig(self.config, self.beta.checks[0])
        with mock.patch.object(check.clients.stoker, 'get_bolver_configuration') as mocked:
            mocked.return_value = BolverConfiguration()
            result = check.run(self.beta)
            result = {'success': result.success, 'message': result.message, 'error': result.error}
        expected = {'success': True, 'message': stoker.CheckBolverConfig.ResultMessage.OK.value, 'error': ''}
        self.assertEqual(result, expected)
