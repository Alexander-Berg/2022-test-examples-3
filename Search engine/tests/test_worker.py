"""
    Test for worker class
"""

import pytest
import logging

from search.mon.wabbajack.libs import workers
from search.mon.wabbajack.libs.workers import sync
from search.mon.wabbajack.libs.plugins import Plugins

GOOD_PACKAGE = 'search.mon.wabbajack.libs.modlib.modules'

log = logging.getLogger(__name__)


class TestWorkerFunctions:

    def setup_class(self):
        self.modules = Plugins(namespace=GOOD_PACKAGE)

    def test_search_module(self):
        assert workers.search_module('sys', self.modules)

    def test_missing_module_error(self):
        with pytest.raises(workers.MissingModuleError):
            workers.search_module('test', self.modules)

    def test_search_function(self):
        m = workers.search_module('sys', self.modules)
        assert workers.search_function('test', m)

    def test_missing_function_error(self):
        m = workers.search_module('sys', self.modules)
        with pytest.raises(workers.MissingFunctionError):
            workers.search_function('some', m)

    def test_try_function(self):
        r = workers.try_function(
            func=workers.search_function(
                function_name='test',
                module=workers.search_module('sys', self.modules)
            ),
            args={}
        )
        assert isinstance(r, str)


class TestWorker:
    def setup_class(self):
        self.modules = Plugins(namespace=GOOD_PACKAGE)

    def test_worker(self):
        w = sync.SyncWorker(modules=self.modules, logger=log)
        w_processing_result = w.processing('sys', 'test', {})
        assert isinstance(w_processing_result, dict)
        assert w_processing_result['error'] is None
        assert w_processing_result['result'] == 'all cheese and pie with brains'
