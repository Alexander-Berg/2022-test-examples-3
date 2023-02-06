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
from search.priemka.yappy.proto.structures.beta_component_pb2 import BetaComponent
from search.priemka.yappy.proto.structures.slot_pb2 import Slot

from search.priemka.yappy.src.processor.modules.verificator.checks.abc import RunResult
from search.priemka.yappy.src.processor.modules.verificator.checks import nanny
from search.priemka.yappy.src.yappy_lib.config_utils import get_test_config

from search.priemka.yappy.tests.utils.test_cases import TestCase


class NannyCheckTest(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.config = get_test_config()

    def setUp(self):
        self.component = BetaComponent()
        self.component.slot.id = 'slot-id'
        self.component.slot.type = Slot.Type.NANNY

    def test_tickets_integration_disabled_fail(self):
        self.component.checks.add().CopyFrom(Check(check_class='nanny.CheckTicketsIntegrationDisabled'))
        check = nanny.CheckTicketsIntegrationDisabled(self.config, self.component.checks[0])
        with mock.patch.object(check.clients.nanny, 'get_info_attrs') as mocked:
            mocked.return_value = {'content': {'tickets_integration': {'service_release_tickets_enabled': True}}}
            result = check._run(self.component)
        expected = RunResult(False, nanny.CheckTicketsIntegrationDisabled.RESULT_MSG.format('not disabled'))
        self.assertEqual(result, expected)

    def test_tickets_integration_disabled_ok(self):
        self.component.checks.add().CopyFrom(Check(check_class='nanny.CheckTicketsIntegrationDisabled'))
        check = nanny.CheckTicketsIntegrationDisabled(self.config, self.component.checks[0])
        with mock.patch.object(check.clients.nanny, 'get_info_attrs') as mocked:
            mocked.return_value = {'content': {'tickets_integration': {'service_release_tickets_enabled': False}}}
            result = check._run(self.component)
        expected = RunResult(True, nanny.CheckTicketsIntegrationDisabled.RESULT_MSG.format('disabled'))
        self.assertEqual(result, expected)
