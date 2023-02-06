import logging
import sys
import unittest

from base import TestBaseTestResult
from tests import SUITES
from utils.args import get_args


def main():
    args = get_args()
    test_method_name = None
    list_tests = {}
    failfast = args.stop_on_failure or False

    logger = logging.getLogger("main.py")
    logging_level = logging.DEBUG if args.verbose else logging.INFO
    logging.basicConfig(level=logging_level, format='%(asctime)s %(levelname)s %(message)s')

    if args.list_tests:
        test_loader = unittest.TestLoader()
        for suite in SUITES:
            test_cases_names = test_loader.getTestCaseNames(suite)
            for name in test_cases_names:
                print(f"{suite().get_suite_name()}::{name}")
        return

    if args.tests:
        for test in args.tests:
            tests_splitted = test.split("::")
            list_tests.setdefault(tests_splitted[0], list()).append(tests_splitted[1])

    suites = []
    for s in SUITES:
        suite = s()
        if args.suites and suite.get_suite_name() not in args.suites:
            continue

        if args.disable_suites and suite.get_suite_name() in args.disable_suites:
            logging.info(f'{suite.get_suite_name()} suite is disabled')
            continue

        if list_tests:
            test_method_name = None
            if suite.get_suite_name() not in list_tests.keys():
                continue
            for value in list_tests.get(suite.get_suite_name()):
                test_method_name = value

        suites.append(create_suite(s, test_method=test_method_name))
    combo_suite = unittest.TestSuite(suites)

    count_test_cases = combo_suite.countTestCases()
    if count_test_cases > 0:
        logging.info(f'start executing {count_test_cases} test cases')
    else:
        sys.exit("There is no test cases for executing")

    runner = unittest.TextTestRunner(verbosity=2, failfast=failfast, resultclass=TestBaseTestResult) \
        .run(unittest.TestSuite(combo_suite))

    if len(runner.skipped) > 0:
        print(f'Skipped tests:')
        for name, reason in runner.skipped:
            print(f'{name}')


def create_suite(test_case_class, test_method=None):
    test_loader = unittest.TestLoader()
    test_loader.sortTestMethodsUsing = None
    test_names = test_loader.getTestCaseNames(test_case_class)
    suite = unittest.TestSuite()
    for name in test_names:
        if test_method and test_method != name:
            continue
        suite.addTest(test_case_class(name))
    return suite
