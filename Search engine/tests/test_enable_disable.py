# -*- coding: utf-8 -*-

import os
from unittest import TestCase

from search.pumpkin.yalite_service.libyalite.actions import enable_disable
from search.pumpkin.yalite_service.libyalite.common.config import YaLiteConfiguration
from search.pumpkin.yalite_service.libyalite.core import YaLiteCore

import utils_for_tests as utils


class TestChecks(TestCase):

    @classmethod
    def setUpClass(cls):
        print "Testing 'actions.enable_disable' module:"

        cls.config = YaLiteConfiguration(utils.test_config_path)
        cls.http_check_file = cls.config.http_check_file
        cls.force_http_disable = cls.config.force_http_disable

        if not os.path.isdir(cls.config.cache_dir):
            os.makedirs(cls.config.cache_dir)

        check_dir = os.path.dirname(cls.config.http_check_file)
        if not os.path.isdir(check_dir):
            os.makedirs(check_dir)

    @classmethod
    def tearDownClass(cls):
        print ""

        if os.path.isfile(cls.http_check_file):
            os.remove(cls.http_check_file)

        if os.path.isfile(cls.force_http_disable):
            os.remove(cls.force_http_disable)

    def setUp(self):
        self.core = YaLiteCore(utils.test_config_path)

        self.http_check_file = self.core.config.http_check_file
        self.force_http_disable = self.core.config.force_http_disable

    def set_state(self, http_check_state, forced_disable_state):
        utils.switch_flag_file(self.http_check_file, http_check_state)
        utils.switch_flag_file(self.force_http_disable, forced_disable_state)

    def test_EnableHTTP(self):

        enable_http = enable_disable.EnableHTTP(self.core)

        # No forced disable, HTTP check file not exist
        self.set_state(http_check_state=False, forced_disable_state=False)

        self.assertTrue(enable_http.run_action(), "HTTP check file enable failed.")
        self.assertTrue(os.path.isfile(self.http_check_file), "HTTP check file had not been created.")

        # No forced disable, HTTP check file exists
        self.set_state(http_check_state=True, forced_disable_state=False)
        check_mtime = os.stat(self.http_check_file).st_mtime

        self.assertTrue(enable_http.run_action(), "HTTP check file enable failed.")
        self.assertTrue(os.path.isfile(self.http_check_file), "HTTP check file had not been created.")
        self.assertEqual(os.stat(self.http_check_file).st_mtime, check_mtime, "HTTP check file had been updated.")

        # Forced check disable. HTTP check file exists
        self.set_state(http_check_state=True, forced_disable_state=True)

        self.assertFalse(enable_http.run_action(), "HTTP check file enable succeed in spite of forced disable.")
        self.assertTrue(os.path.isfile(self.force_http_disable), "HTTP check forced disable had been canceled.")
        self.assertFalse(os.path.isfile(self.http_check_file),
                         "HTTP check disable forced, but file had not been removed.")

        # Forced check disable. HTTP check file does not exist.
        self.set_state(http_check_state=False, forced_disable_state=True)

        self.assertFalse(enable_http.run_action(), "HTTP check file enable succeed in spite of forced disable.")
        self.assertTrue(os.path.isfile(self.force_http_disable), "HTTP check forced disable had been canceled.")
        self.assertFalse(os.path.isfile(self.http_check_file),
                         "HTTP check disable forced, check file is still exists.")

        # Forced check disable. Forced enable.
        self.set_state(http_check_state=False, forced_disable_state=True)

        self.assertTrue(enable_http.run_action("force"), "HTTP check file enable failed in spite of forced enable.")
        self.assertFalse(os.path.isfile(self.force_http_disable), "HTTP check forced disable had not been canceled.")
        self.assertTrue(os.path.isfile(self.http_check_file),
                        "HTTP check enable forced, but check file does not exist.")

    def test_DisableHTTP(self):

        disable_http = enable_disable.DisableHTTP(self.core)

        # No forced disable, HTTP check file not exist
        self.set_state(http_check_state=False, forced_disable_state=False)

        self.assertTrue(disable_http.run_action(), "HTTP check file disable failed.")
        self.assertFalse(os.path.isfile(self.http_check_file), "HTTP check file had not been removed.")

        # No forced disable, HTTP check file exists
        self.set_state(http_check_state=True, forced_disable_state=False)

        self.assertTrue(disable_http.run_action(), "HTTP check file disable failed.")
        self.assertFalse(os.path.isfile(self.http_check_file), "HTTP check file had not been removed.")

        # Force check disable. HTTP check file exists
        self.set_state(http_check_state=True, forced_disable_state=False)

        self.assertTrue(disable_http.run_action("forced"), "Forced HTTP check disable failed.")
        self.assertFalse(os.path.isfile(self.http_check_file), "HTTP check file had not been removed.")
        self.assertTrue(os.path.isfile(self.force_http_disable), "Forced HTTP check disable had not been switched on.")
