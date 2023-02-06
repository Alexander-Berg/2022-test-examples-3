# coding: utf-8

import unittest

from components_app.tests.base_component import TestComponent
from components_app.tests.cluster_state import TestClusterStateApi
from components_app.tests.db import TestDb
from components_app.tests.juggler import TestJugglerApi
from components_app.tests.nanny import TestNannyApi
from components_app.tests.netmon import TestNetmonApi
from components_app.tests.new_zk_storage import TestInitZkStorage
from components_app.tests.new_zk_storage import TestZkStorage
from components_app.tests.yasm import TestYasmApi
from components_app.tests.ydl import TestYdlApi
from components_app.tests.switter import TestSwitterApi


def get_suite():
    tests = [
        TestComponent,
        TestNetmonApi,
        TestYasmApi,
        TestYdlApi,
        TestClusterStateApi,
        TestDb,
        TestJugglerApi,
        TestNannyApi,
        TestInitZkStorage,
        TestZkStorage,
        TestSwitterApi,
    ]
    suite = unittest.TestSuite()
    loader = unittest.TestLoader()
    for test_class in tests:
        tests = loader.loadTestsFromTestCase(test_class)
        suite.addTests(tests)

    return suite

if __name__ == '__main__':
    runner = unittest.TextTestRunner()
    runner.run(get_suite())
