# -*- coding: utf-8 -*-

import os
from unittest import TestCase

import mock
import requests
from search.pumpkin.yalite_service.libyalite.actions import statuses
from search.pumpkin.yalite_service.libyalite.common.config import YaLiteConfiguration

import utils_for_tests as utils
from search.pumpkin.yalite_service.libyalite.common import exceptions


class TestStatusHTTP(TestCase):

    @classmethod
    def setUpClass(cls):
        print "Testing 'actions.statuses.StatusHTTP' class:"

        cls.config = YaLiteConfiguration(utils.test_config_path)

        cls.check_file = cls.config.http_check_file
        cls.check_dir = os.path.dirname(cls.check_file)

        cls.core = mock.MagicMock()
        cls.core.config = cls.config

        if not os.path.isdir(cls.check_dir):
            os.makedirs(cls.check_dir)

    @classmethod
    def tearDownClass(cls):
        print ""

    @mock.patch("search.pumpkin.yalite_service.libyalite.actions.statuses.requests.get")
    def test_run_action(self, mock_requests_get):
        status = statuses.StatusHTTP(core=self.core,
                                     port=8383)

        mock_requests_get.side_effect = requests.exceptions.ConnectionError

        self.assertRaises(exceptions.YaLiteCheckFailed, status.run_action)

        mock_requests_get.side_effect = None

        result = mock.MagicMock()
        result.status_code = 404
        mock_requests_get.return_value = result

        self.assertRaises(exceptions.YaLiteCheckFailed, status.run_action)

        result.status_code = 200

        self.assertTrue(status.run_action())

    def test_complete(self):
        status = statuses.StatusHTTP(core=self.core,
                                     port=8383)

        self.assertEqual(status.complete(), [])
