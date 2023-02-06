# -*- coding: utf-8 -*-
import pytest
import os

@pytest.fixture(scope='session')
def static_root_dir(session_fs_manager, request):
    return os.path.abspath(os.path.join(os.path.dirname(__file__), 'data'))

@pytest.fixture(scope='class')
def class_data_dir(request, static_root_dir):
    return os.path.abspath(os.path.join(static_root_dir, request.node.name))

@pytest.fixture(scope='function')
def static_file_content(request, class_data_dir):
    def p(suffix):
        with open(os.path.join(class_data_dir, suffix or request.node.name)) as f:
            return f.read()
    return p

""" disable and see what breaks
@pytest.fixture(scope='function')
def static_data_dir(fs_manager, request):
    old_static = os.path.abspath(os.path.join(os.path.dirname(__file__), 'data', request.node.name))
    return fs_manager.copy(old_static, 'static_data')


@pytest.fixture(scope='class')
def class_static_data_dir(class_fs_manager, request):
    old_static = os.path.abspath(os.path.join(os.path.dirname(__file__), 'data', request.node.name))
    return class_fs_manager.copy(old_static, 'static_data')
"""
