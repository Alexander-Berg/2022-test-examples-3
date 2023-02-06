import unittest
from unittest.loader import _make_failed_load_tests     # noqa


class TestLoader(unittest.TestLoader):
    def loadTestsFromModule(self, module, use_load_tests=True, pattern=None):
        tests = []
        for name in dir(module):
            obj = getattr(module, name)
            if isinstance(obj, type) and issubclass(obj, unittest.TestCase) and obj.__module__ == module.__name__:
                tests.append(self.loadTestsFromTestCase(obj))
        load_tests = getattr(module, 'load_tests', None)
        tests = self.suiteClass(tests)
        if use_load_tests and load_tests is not None:
            try:
                return load_tests(self, tests, pattern)
            except Exception as e:
                return _make_failed_load_tests(module.__name__, e, self.suiteClass)
        return tests
