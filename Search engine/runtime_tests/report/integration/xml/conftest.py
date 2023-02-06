# -*- coding: utf-8 -*-

import os
import pytest

from report.functional.conftest import *

@pytest.fixture(scope='function')
def static_data_dir(fs_manager, request):
    old_static = os.path.abspath(os.path.join(os.path.dirname(__file__), 'data', request.node.name))
    return fs_manager.copy(old_static, 'static_data')

@pytest.fixture(scope='class')
def class_static_data_dir(class_fs_manager, request):
    old_static = os.path.abspath(os.path.join(os.path.dirname(__file__), 'data', request.node.name))
    return class_fs_manager.copy(old_static, 'static_data')
