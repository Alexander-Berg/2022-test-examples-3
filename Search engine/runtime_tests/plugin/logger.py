# -*- coding: utf-8 -*-
import pytest
import logging


pytest_plugins = [
    'runtime_tests.plugin.fs',
]


MAIN_LOG = 'tests.log'
LOG_FORMAT = '%(asctime)s %(levelname)-6s (%(module)s) %(message)s'


@pytest.fixture(scope='session')
def logger(session_fs_manager):
    log_file = session_fs_manager.create_file(MAIN_LOG)
    formatter = logging.Formatter(LOG_FORMAT)
    file_handler = logging.FileHandler(log_file)
    file_handler.setFormatter(formatter)
    session_logger = logging.getLogger()
    session_logger.addHandler(file_handler)
    session_logger.setLevel(logging.INFO)
    return session_logger
