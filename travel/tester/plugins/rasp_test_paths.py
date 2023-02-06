# -*- coding: utf-8 -*-

import os.path
import glob


PROJECT_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../'))

prev_path = os.getcwd()
try:
    os.chdir(PROJECT_PATH)

    SUBMODULES_TESTS = glob.glob('*/tests') + glob.glob('tests')
    PROJECT_ROOT_TESTS = glob.glob('tests')
finally:
    os.chdir(prev_path)


testpaths = SUBMODULES_TESTS + PROJECT_ROOT_TESTS


def pytest_configure(config):
    if len(config.args) == 1 and os.path.abspath(config.args[0]) == PROJECT_PATH:
        config.args = testpaths
