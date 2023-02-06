import unittest

from events import TestEvent
from utils.helpers import *


def _set_up(client, client_helper=None):
    client_initial_state(client)
    if client_helper:
        client_initial_state(client_helper)


def _tear_down(client, client_helper=None):
    client_initial_state(client)
    if client_helper:
        client_initial_state(client_helper)


class TestBase(unittest.TestCase):

    def get_suite_name(self):
        return self.__class__.__name__

    @classmethod
    def setUpClass(cls):
        logging.info(f'* starting suite {cls.__name__} *')
        TestEvent(cls.__name__, "suite started").report_json()

    @classmethod
    def tearDownClass(cls):
        TestEvent(cls.__name__, "suite finished").report_json()
        logging.info(f'* leaving suite {cls.__name__} *')

    def setUp(self):
        logging.info("* setup *")
        _set_up(self.client)
        self.startTime = time.time()
        logging.info("* setup finish *")

    def tearDown(self):
        logging.info("* tearDown *")
        t = time.time() - self.startTime
        logging.info('%s: %.3f' % (self.id(), t))
        _tear_down(self.client)
        logging.info("* tearDown finish *")


class TestBaseTestResult(unittest.TextTestResult):
    def addFailure(self, test, err):
        super().addFailure(test, err)
        exctype, value, tb = err
        suite_name, test_name = self.get_test_inf(test)
        logging.error(value)
        TestEvent(suite_name + " " + test_name, type="failure", message=f'{value}').report_json()

    def addError(self, test, err):
        super().addError(test, err)
        exctype, value, tb = err
        suite_name, test_name = self.get_test_inf(test)
        logging.error(value)
        TestEvent(suite_name + " " + test_name, type="failure", message=f'{value}').report_json()

    def addSuccess(self, test):
        super().addSuccess(test)
        suite_name, test_name = self.get_test_inf(test)
        TestEvent(suite_name + " " + test_name, type="pass", message="test finished").report_json()

    def addSkip(self, test, reason):
        super().addSkip(test, reason)
        suite_name, test_name = self.get_test_inf(test)
        TestEvent(suite_name + " " + test_name, type="skip", message="test skipped").report_json()

    @staticmethod
    def get_test_inf(test):
        suite_name = None
        test_name = None
        test_id = test.id()
        test_splitted = test_id.split('.') or None
        if test_splitted:
            suite_name = test_splitted[-2]
            test_name = test_splitted[-1]
        return suite_name, test_name
