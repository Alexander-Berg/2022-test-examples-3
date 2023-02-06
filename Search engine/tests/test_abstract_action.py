# -*- coding: utf-8 -*-

__author__ = 'denkoren'

from unittest import TestCase

import mock
from search.pumpkin.yalite_service.libyalite.actions import abstract_action
from search.pumpkin.yalite_service.libyalite.core import YaLiteCore

import utils_for_tests as utils
from search.pumpkin.yalite_service.libyalite.common import exceptions


class TestYaLiteAction(TestCase):

    @classmethod
    def setUpClass(cls):
        print "Testing 'actions.abstract_action.YaLiteAction' class:"

    @classmethod
    def tearDownClass(cls):
        print ""

    def setUp(self):
        subaction_mock_1 = mock.MagicMock()
        subaction_mock_1.command = "test-command"
        subaction_mock_2 = mock.MagicMock()
        subaction_mock_2.command = "test-command_2"

        self.subaction_mock_1 = subaction_mock_1
        self.subaction_mock_2 = subaction_mock_2

        action = abstract_action.YaLiteAction(core=None,
                                              command="",
                                              description="descr",
                                              actions=[subaction_mock_1, subaction_mock_2])

        self.action = action

    def test_run_action(self):
        self.subaction_mock_1.run_action.return_value = "test return value"

        result = self.action.run_action("test-command", "additional_arg", additional_kwarg="val")

        self.assertEqual(self.subaction_mock_1.run_action.call_args,
                         (("additional_arg",), {"additional_kwarg": "val"}))

        self.assertEqual(result, "test return value", "Action returned incorrect result.")

        self.assertRaises(exceptions.YaLiteActionNameError, self.action.run_action, "incorrect-command-name")

    def test_complete(self):
        self.subaction_mock_1.complete.return_value = ["complete variant 1", "complete variant 2"]

        result = self.action.complete("test-command", "additional_arg", additional_kwarg="val")

        self.assertEqual(self.subaction_mock_1.complete.call_args,
                         (("additional_arg",), {"additional_kwarg": "val"}))

        self.assertEqual(result, ["complete variant 1", "complete variant 2"],
                         "Action returned incorrect completion list from subaction.")

        result = self.action.complete("incorrect-test-command", "additional_arg", additional_kwarg="val")

        self.assertEqual(result, ["test-command", "test-command_2"],
                         "Action returned incorrect completion list of subactions.")

    def test_get_action(self):
        result = self.action.get_action("test-command")

        self.assertEqual(result, self.subaction_mock_1)

        self.assertRaises(exceptions.YaLiteActionNameError, self.action.get_action, "incorrect-test-command")


class TestYaLiteActionComplex(TestCase):

    @classmethod
    def setUpClass(cls):
        print "Testing 'actions.abstract_action.YaLiteActionComplex' class:"

    @classmethod
    def tearDownClass(cls):
        print ""

    def setUp(self):
        subaction_mock_1 = mock.MagicMock()
        subaction_mock_1.command = "test-command"
        subaction_mock_2 = mock.MagicMock()
        subaction_mock_2.command = "test-command_2"

        self.subaction_mock_1 = subaction_mock_1
        self.subaction_mock_2 = subaction_mock_2

        action = abstract_action.YaLiteActionComplex(core=None,
                                                     command="",
                                                     description="descr",
                                                     actions=[subaction_mock_1, subaction_mock_2])

        self.action = action

    def test_run_actions_list(self):
        self.subaction_mock_1.run_action.return_value = "test 1 return value"
        self.subaction_mock_2.run_action.return_value = "test 2 return value"

        result = self.action.run_actions_list("additional_arg_1", "additional_arg_2", additional_kwarg="val")

        self.assertEqual(self.subaction_mock_1.run_action.call_args,
                         (("additional_arg_1", "additional_arg_2",), {"additional_kwarg": "val"}))
        self.assertEqual(self.subaction_mock_2.run_action.call_args,
                         (("additional_arg_1", "additional_arg_2",), {"additional_kwarg": "val"}))

        self.assertEqual(result, ("test 1 return value", "test 2 return value"),
                         "Action returned incorrect result.")

    def test_run_action(self):
        self.subaction_mock_1.run_action.return_value = "test 1 return value"
        self.subaction_mock_2.run_action.return_value = "test 2 return value"

        result = self.action.run_action("additional_arg_1", "additional_arg_2", additional_kwarg="val")

        self.assertEqual(self.subaction_mock_1.run_action.call_args,
                         (("additional_arg_1", "additional_arg_2",), {"additional_kwarg": "val"}))
        self.assertEqual(self.subaction_mock_2.run_action.call_args,
                         (("additional_arg_1", "additional_arg_2",), {"additional_kwarg": "val"}))

        self.assertEqual(result, ("test 1 return value", "test 2 return value"),
                         "Action returned incorrect result.")

    def test_complete(self):
        result = self.action.complete("additional_arg_1", "additional_arg_2", additional_kwarg="val")

        self.assertEqual(result, [])


class TestYaLiteServiceAction(TestCase):

    @classmethod
    def setUpClass(cls):
        print "Testing 'actions.abstract_action.YaLiteServiceAction' class:"

    @classmethod
    def tearDownClass(cls):
        print ""

    def setUp(self):
        service_mock_1 = mock.MagicMock()
        service_mock_1.NAME = "test-service_1"
        service_mock_2 = mock.MagicMock()
        service_mock_2.NAME = "test-service"

        service_mock_1.test_service_action = mock.MagicMock()
        service_mock_1.test_service_action.return_value = "service action result 1"
        service_mock_2.test_service_action = mock.MagicMock()
        service_mock_2.test_service_action.return_value = "service action result 2"

        core = YaLiteCore(utils.test_config_path)
        core.services = [service_mock_1, service_mock_2]

        self.service_mock_1 = service_mock_1
        self.service_mock_2 = service_mock_2

        action = abstract_action.YaLiteServiceAction(core=core,
                                                     command="",
                                                     description="descr",
                                                     service_action="test_service_action")

        self.action = action

    def test_run_action(self):
        result = self.action.run_action("test-service", "additional_arg", additional_kwarg="val")

        self.assertEqual(result, "service action result 2")

    def test_complete(self):
        result = self.action.complete("incorrect-test-service")
        self.assertEqual(result, ["test-service_1", "test-service"])

        result = self.action.complete("test-service_1")
        self.assertEqual(result, ["test-service_1"])
