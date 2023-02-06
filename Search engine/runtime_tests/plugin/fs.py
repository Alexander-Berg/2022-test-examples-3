# -*- coding: utf-8 -*-
import os
import pytest

from runtime_tests.util.settings import Configurator
from runtime_tests.util.fs import FileSystemManager


pytest_plugins = [
    'runtime_tests.plugin.settings',
]


_SLAVE_ID_KEY = 'slaveid'
_LOG_DIR_KEY = 'log_fs_manager'


def pytest_addoption(parser):
    parser.addoption('-L', '--log_dir', dest='log_dir', default='./logs', help='path to logs directory')

    debug = parser.getgroup('debug')
    debug.addoption('--clean_logs', dest='clean_logs', action='store_true', default=False, help='clean logs of passed tests')


class ConfigureNodePlugin(object):
    @staticmethod
    def pytest_configure_node(node):
        node.slaveinput[_LOG_DIR_KEY] = node.config.log_fs_manager.root_dir


class FileSystemConfigurator(Configurator):
    def configure_master(self):
        log_dir = os.path.abspath(self._config.getoption('log_dir'))
        if os.path.exists(log_dir) and not os.access(log_dir, os.W_OK):
            raise OSError('[Errno 13] log_dir %s directory exists, but it\'s not writeable (check permissions)' % log_dir)
        parent_dir, log_dir_name = os.path.split(log_dir)
        parent_fs_manager = FileSystemManager(parent_dir)
        parent_fs_manager.remove(log_dir)
        new_log_dir = parent_fs_manager.create_dir(log_dir_name)
        assert new_log_dir == log_dir, 'unable to create log_dir'
        self._config.log_fs_manager = FileSystemManager(new_log_dir)
        if self._xdist_run:
            self._config.pluginmanager.register(ConfigureNodePlugin)  # TODO: encapsulate it in Configurator class


@pytest.mark.tryfirst
def pytest_configure(config):
    FileSystemConfigurator(config).configure()


@pytest.mark.tryfirst
def pytest_runtest_makereport(item, call, __multicall__):  # pylint: disable=unused-argument
    report = __multicall__.execute()
    setattr(item, 'report', report)
    return report


@pytest.fixture(scope='session')
def session_fs_manager(request, settings):
    if settings.yatest:
        import yatest.common
        return FileSystemManager(yatest.common.output_path())
    else:
        if settings.xdist:
            session_id = request.config.slaveinput[_SLAVE_ID_KEY]
            log_fs_manager = FileSystemManager(request.config.slaveinput[_LOG_DIR_KEY])
        else:
            session_id = 0
            log_fs_manager = request.config.log_fs_manager
        path = log_fs_manager.create_dir('session.{}'.format(session_id))
        session_fs_manager = FileSystemManager(path)
        return session_fs_manager


@pytest.fixture(scope='module')
def module_fs_manager(session_fs_manager, request):
    path = session_fs_manager.create_dir(request.module.__name__)
    return FileSystemManager(path)


@pytest.fixture(scope='class')
def class_fs_manager(module_fs_manager, request):
    if request.cls is not None:
        path = module_fs_manager.create_dir(request.cls.__name__)
        return FileSystemManager(path)
    else:
        return None


@pytest.fixture(scope='function')
def function_fs_manager(module_fs_manager, class_fs_manager, request):
    fs_mgr = class_fs_manager if class_fs_manager is not None else module_fs_manager
    path = fs_mgr.create_dir(request.node.name)

    def fin():
        fs_mgr.chmod_default_recursive(path)
        if request.config.option.clean_logs and request.node.report.passed:
            fs_mgr.remove(path)

    request.addfinalizer(fin)
    return FileSystemManager(path)


@pytest.fixture(scope='function')
def fs_manager(function_fs_manager):
    return function_fs_manager
