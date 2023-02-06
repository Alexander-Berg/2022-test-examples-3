# -*- coding: utf-8 -*-

import logging
from unittest import TestCase

import mock
from search.pumpkin.yalite_service.libyalite.actions import main_actions
from search.pumpkin.yalite_service.libyalite.core import YaLiteCore

import utils_for_tests as utils


class TestMainActions(TestCase):

    @classmethod
    def setUpClass(cls):
        print "Testing 'actions.main_actions' module:"
        logging.root.setLevel("CRITICAL")

    @classmethod
    def tearDownClass(cls):
        print ""

    def action_with_subactions_test(self, action_object):
        subaction_1 = mock.MagicMock()
        subaction_2 = mock.MagicMock()
        subaction_3 = mock.MagicMock()
        subaction_4 = mock.MagicMock()

        subaction_1.command = "subact1"
        subaction_2.command = "subact2"
        subaction_3.command = "subact3"
        subaction_4.command = "subact4"

        subaction_2.run_action.return_value = "subaction_return_value"

        actions = [subaction_1, subaction_2, subaction_3, subaction_4]

        action_object.actions = actions

        self.assertEqual(action_object.run_action("subact2", "additional_arg", additional_kwarg="val"),
                         "subaction_return_value",
                         "Action '{0}' result differs from subaction's one.".format(action_object.command))
        self.assertEqual(subaction_2.run_action.call_args,
                         (("additional_arg",), {"additional_kwarg": "val"}))

    def test_GetData(self):
        core = mock.MagicMock()
        service = mock.MagicMock()
        service.get_data = mock.MagicMock()

        core.get_service.return_value = service

        service.get_data.return_value = 123

        get_data = main_actions.GetData(core=core)

        self.assertEqual(get_data.run_action("test_yalite_service", "additional_arg", additional_kwarg="val"), 123)

        self.assertEqual(core.get_service.call_args,
                         ((), {"service_name": "test_yalite_service"}))

        self.assertEqual(service.get_data.call_args,
                         (("additional_arg",), {"additional_kwarg": "val"}))

    def test_TestService(self):
        core = mock.MagicMock()
        service = mock.MagicMock()
        service.test_service = mock.MagicMock()

        core.get_service.return_value = service

        service.test_service.return_value = 123

        test_service = main_actions.TestService(core=core)

        self.assertEqual(test_service.run_action("test_yalite_service", "additional_arg", additional_kwarg="val"), 123)

        self.assertEqual(core.get_service.call_args,
                         ((), {"service_name": "test_yalite_service"}))

        self.assertEqual(service.test_service.call_args,
                         (("additional_arg",), {"additional_kwarg": "val"}))

    def test_SwitchService(self):
        core = mock.MagicMock()
        service = mock.MagicMock()
        service.switch_service = mock.MagicMock()

        core.get_service.return_value = service

        service.switch_service.return_value = 123

        switch_service = main_actions.SwitchService(core=core)

        self.assertEqual(switch_service.run_action("test_yalite_service", "additional_arg", additional_kwarg="val"),
                         123)

        self.assertEqual(core.get_service.call_args,
                         ((), {"service_name": "test_yalite_service"}))

        self.assertEqual(service.switch_service.call_args,
                         (("additional_arg",), {"additional_kwarg": "val"}))

    def test_Check(self):
        core_mock = mock.MagicMock()
        core_mock.config.domains = ["yandex.ru", "pumpkin.yandex.ru"]

        self.action_with_subactions_test(action_object=main_actions.Check(core_mock))

    @mock.patch("search.pumpkin.yalite_service.libyalite.actions.main_actions.SwitchService")
    @mock.patch("search.pumpkin.yalite_service.libyalite.actions.main_actions.GetData")
    def test_Update(self, get_data, switch):
        new_data = mock.MagicMock()
        new_data.resource_id = 123

        get_data_action = mock.MagicMock()
        get_data_action.command = "get-data"
        get_data_action.run_action.return_value = new_data

        switch_action = mock.MagicMock()
        switch_action.command = "switch"

        get_data.return_value = get_data_action
        switch.return_value = switch_action

        core = YaLiteCore(utils.test_config_path)

        update_action = main_actions.Update(core=core)

        update_action.run_action("test_service", restart="yes", additional_kwarg="val")

        self.assertEqual(get_data_action.run_action.call_args,
                         (("test_service",), {"additional_kwarg": "val"}))

        self.assertEqual(switch_action.run_action.call_args,
                         (("test_service", 123), {"restart": "yes", "additional_kwarg": "val"}))

    def test_Status(self):
        self.action_with_subactions_test(action_object=main_actions.Status(None))

    def test_Enable(self):
        self.action_with_subactions_test(action_object=main_actions.Enable(None))

    def test_Disable(self):
        self.action_with_subactions_test(action_object=main_actions.Enable(None))

    def test_Generate(self):
        self.action_with_subactions_test(action_object=main_actions.Generate(None))
