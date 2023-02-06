# -*- coding: utf8 -*-
import logging

from django.test import override_settings

from travel.avia.admin.lib.script_helpers.script_run_logger_factory import script_run_logger_factory


def test_create_log_and_write_some_messages(tmpdir):
    with override_settings(LOG_PATH=str(tmpdir)):
        path_to_log_file = script_run_logger_factory.create('some_script_code')
        log = logging.getLogger('some_logger_name')
        log.info('some_funny_message')

        with open(path_to_log_file) as path_to_log_file:
            assert 'some_funny_message' in path_to_log_file.read()
