# coding: utf-8

import unittest

from avtomatika.tests.actions.flapodav import TestFlapodav, TestFlapodavParallels, TestUpValveTime, TestUpValvePart


def get_suite():
    tests = [
        TestFlapodav,
        TestFlapodavParallels,
        TestUpValveTime,
        TestUpValvePart,
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
