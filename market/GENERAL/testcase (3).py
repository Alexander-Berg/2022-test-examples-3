# -*- coding: utf-8 -*-
import argparse
import inspect
import os
import socket
import sys
import unittest
import shutil
import json

from color import Color, draw, colorstr
from frozen import Frozen
import rand
from server import ShinyServer
from client import ShinyClient
from resources import Resources
from report import Report
from response import NullResponse, ustr
from core.logs import EventLog
from core.offline import OfflineService
import paths
import config
import testenv

class TestCase(unittest.TestCase, Frozen):
    __name__ = None
    __dump = None
    __wait = None
    __port = None
    __breakpoint = None
    __debug = None
    __name = None
    __paths = paths.Paths()

    __resources = None
    __server = None
    __report = None
    __event_log = None
    __on_break = []

    _UnitTestCase__pytest_class_setup = None
    _UnitTestCase__pytest_method_setup = None

    @classmethod
    def set_args(cls, args):
        cls.__wait = args.wait
        cls.enable_dump(args.dump, args.seed)
        cls.__port = args.port
        cls.__breakpoint = args.breakpoint
        cls.__debug = args.debug
        if cls.__port is None and (args.server or args.breakpoint or args.wait):
            cls.__port = rand.get_stable_port()
        if args.dump and os.path.exists(args.dump):
            shutil.rmtree(args.dump)
        if args.root is not None:
            cls.__paths.forced_root = os.path.realpath(os.path.expanduser(args.root))

    @classmethod
    def set_testname(cls, name):
        cls.__paths.test_name = name

    @classmethod
    def enable_dump(cls, path, seed=None):
        cls.__dump = path
        if path is not None and seed is None:
            rand.set_seed(0)

    # called after __init__ of object
    @classmethod
    def setUpClass(cls):
        try:
            cls._setup_class()
        except Exception as err:
            import traceback
            tb = traceback.format_exc()
            raise RuntimeError(str(err) + '\nOriginal traceback = {}\nSeed = {}'.format(tb, rand.get_seed()))

    @classmethod
    def _setup_class(cls):
        if not cls.__paths.test_name:
            cls.__paths.test_name = os.path.splitext(os.path.basename(inspect.getfile(cls)))[0]
        cls.__paths.setup()

        testenv.setup(cls.__paths)

        cls.__resources = Resources(paths=cls.__paths)
        cls.__server = ShinyServer(paths=cls.__paths, port=cls.__port)
        cls.__report = Report(paths=cls.__paths)

        cls.prepare()

        cls.__report.start()
        config.generate(cls.__paths, report_host_and_port=cls.__report.host_and_port)

        cls.breakpoint('before start')
        try:
            cls.__server.start(debug=cls.__debug)
            cls.__event_log = EventLog(paths=cls.__paths)
        except Exception as e:
            cls.__server.stop(e)
            cls.__report.stop()
            raise

        if cls.__wait or cls.__breakpoint:
            cls._print_info()
            sys.stdout.write('Report process pid:      ')
            draw(str(cls.__server.pid), Color.GREEN)
            if cls.__wait:
                raw_input('Press ENTER to run tests...')

    @classmethod
    def tearDownClass(cls):
        cls.__server.stop()
        cls.__report.stop()
        cls.__event_log.close()
        testenv.cleanup()

    @classmethod
    def prepare(cls):
        for name in dir(cls):
            if name.startswith('prepare_'):
                prepare_method = getattr(cls, name)
                prepare_method(resources=cls.__resources, report=cls.__report)
        cls.__resources.create_all()

    @classmethod
    def wait(cls):
        cls._print_info()
        if cls.__debug:
            cls.__server.wait()
        else:
            raw_input('Press ENTER to exit...')

    @classmethod
    def _print_info(cls):
        sys.stdout.write('Data has been stored:    ')
        draw(str(cls.__paths.test_root), Color.GREEN)
        sys.stdout.write('Server has been started: ')
        request_str = 'http://{host}:{port}'.format(
            host=socket.getfqdn(), port=cls.__server.port)
        draw(request_str, Color.GREEN)

    def __init__(self, name='runTest'):
        unittest.TestCase.__init__(self, name)
        def alive_stub():
            pass

        self.service = ShinyClient(self.__paths, alive_stub)  # intellisence only, will be overridden on setUp

    def fail(self, msg=None):
        unittest.TestCase.fail(self, msg)

    def __breakpoint_observer(self, tail):
        self.breakpoint('before request "{}"'.format(tail))

    def setUp(self):
        self.__event_log.reopen()
        self.service = ShinyClient(self.__paths, self.__server.alive)
        self.service.connect(port=self.__server.port, debug=self.__debug)
        self.service.check_alive()
        self.service.observe_call(self.__breakpoint_observer)

    @classmethod
    def breakpoint(cls, hint=None):
        if cls.__breakpoint:
            for on_break in cls.__on_break:
                on_break()
            text = '{} [ENTER]'.format(hint) if hint else ' [ENTER] '
            draw(text, Color.YELLOW, newline=False)
            raw_input()

    def tearDown(self):
        self.breakpoint('before tearDown')
        self.service.observe_call(self.__breakpoint_observer, False)
        all_requests = self.service.all_requests
        self.service.check_alive()
        self.__on_break = []

        class Validator:
            def __init__(self, testcase):
                self._testcase = testcase

            def fail_verbose(self, message):
                self._testcase.fail_verbose(message, ["Event log check failed"], all_requests)

        self.__event_log.rename(suffix=self.id())
        self.assertFragmentIn(self.service.request_text('reopen_log', store_request=False), '0;OK')
        self.__event_log.check(Validator(self))
        self.service.stop()

    def __log_response(self, response):
        fr_path = os.path.join(self.__paths.logs, 'failed_response.log')
        with open(fr_path, 'w+') as fr:
            fr.write(str(response))
        return fr_path

    def resourcesVersion(self):
        return self.__resources.version

    @property
    def resources(self):
        return self.__resources

    def eventLog(self):
        return self.__event_log

    def runOfflineService(self, request_args=[]):
        service = OfflineService(paths=self.__paths)
        return service.run(request_args=request_args)

    def assertFragmentIn(self, response, fragment, __barrier=0xdeadbeaf,
                         preserve_order=False, allow_different_len=True,
                         use_regex=False):
        # need to prevent fragment leakage to assert arguments due to wrong json formatting
        if __barrier != 0xdeadbeaf:
            print(__barrier)
            raise RuntimeError('Fragment formatting error')

        if isinstance(response, NullResponse):
            self.fail_verbose('Report has crashed', [], response.request)
        success, reasons = response.contains(fragment, preserve_order,
                                             allow_different_len, use_regex)
        if success is False:
            fr_path = self.__log_response(response)
            if isinstance(fragment, list) or isinstance(fragment, dict):
                fragment = json.dumps(
                    fragment,
                    indent=2,
                    ensure_ascii=False,
                    default=str)
            self.fail_verbose('Response does not contain {}\nStrict order: {}\nOriginal response: {}'.format(
                ustr(fragment), preserve_order, fr_path), reasons, response.request)

    def assertFragmentNotIn(self, response, fragment, __barrier=0xdeadbeaf, preserve_order=False):
        # need to prevent fragment leakage to assert arguments due to wrong json formatting
        if __barrier != 0xdeadbeaf:
            raise RuntimeError('Fragment formatting error')

        if isinstance(response, NullResponse):
            self.fail_verbose('Report has crashed', [], response.request)
        exist, _ = response.contains(fragment, preserve_order)
        if exist:
            if isinstance(fragment, list) or isinstance(fragment, dict):
                fragment = json.dumps(
                    fragment,
                    indent=2,
                    ensure_ascii=False,
                    default=ustr)
            fr_path = self.__log_response(response)
            self.fail_verbose(
                'Response contains unexpected {}\nStrict order: {}\nOriginal response: {}'.format(
                    ustr(fragment), preserve_order, fr_path), [], response.request)

    def assertEqual(self, first, second):
        msg = '{} != {}'.format(first, second)
        unittest.TestCase.assertEqual(
            self, first, second, format_assert(msg, self.service.last_request, self.__paths.test_root))

    def assertNotEqual(self, first, second):
        msg = '{} == {}'.format(first, second)
        unittest.TestCase.assertNotEqual(
            self, first, second, format_assert(msg, self.service.last_request, self.__paths.test_root))

    def assertTrue(self, expr):
        msg = '{} is not True'.format(expr)
        unittest.TestCase.assertTrue(self, expr, format_assert(msg, self.service.last_request, self.__paths.test_root))

    def assertFalse(self, expr):
        msg = '{} is not False'.format(expr)
        unittest.TestCase.assertFalse(self, expr, format_assert(msg, self.service.last_request, self.__paths.test_root))

    def assertIn(self, elem, lst):
        msg = '{} not in {}'.format(elem, lst)
        unittest.TestCase.assertIn(
            self, elem, lst, format_assert(msg, self.service.last_request, self.__paths.test_root))

    def assertNotIn(self, elem, lst):
        msg = '{} in {}'.format(elem, lst)
        unittest.TestCase.assertNotIn(
            self, elem, lst, format_assert(msg, self.service.last_request, self.__paths.test_root))

    def __assertEqualResponses(self, get_response_func, offer_template, request1, request2, count_offers):
        response1 = get_response_func(request1)
        response2 = get_response_func(request2)
        is_equal, reasons = response1.equal_to(response2)
        if not is_equal:
            self.fail_verbose('Response on request\n{}\nis not equal to response on request\n{}'.format(
                response1.request, response2.request), reasons, response1.request)
        if count_offers is not None:
            self.assertEqual(response1.count(offer_template), count_offers)

    def assertEqualJsonResponses(self, request1, request2, count_offers=None):
        return self.__assertEqualResponses(self.service.request_json, {"entity": "offer"},
                                           request1, request2, count_offers)

    def fail_verbose(self, msg, reasons, requests):
        reasons = set(reasons)
        reasons_fmt = '\n'.join(reasons)
        error = colorstr(msg.strip(), Color.RED)
        why = colorstr(reasons_fmt, Color.YELLOW)
        where = colorstr(self.__paths.test_root, Color.GREEN)
        request = requests if type(requests) is str else '\n'.join(requests)
        how = colorstr(request, Color.GREEN)
        seed = colorstr('Seed: {}'.format(rand.get_seed()), Color.GREEN)
        text = '\n{}\n\n{}\n\n{}\n{}\n{}'.format(error, why, how, where, seed) if reasons \
            else '\n{}\n\n{}\n{}\n{}'.format(error, how, where, seed)
        self.fail(text)

    def shortDescription(self):
        return None


def format_assert(msg, request, test_root):
    error = colorstr(msg, Color.RED)
    where = colorstr(test_root, Color.GREEN)
    how = colorstr(request, Color.GREEN)
    seed = colorstr('Seed: {}'.format(rand.get_seed()), Color.GREEN)
    return '\n{}\n\n{}\n{}\n{}'.format(error, how, where, seed)


def reflect_testcase():
    module = __import__('__main__')
    hierarchy = []
    for name in dir(module):
        obj = getattr(module, name)
        if isinstance(obj, type) and issubclass(obj, TestCase) and obj is not TestCase:
            hierarchy.append(obj)

    if len(hierarchy) == 0:
        return None

    def class_cmp(x, y):
        xy = issubclass(x, y)
        yx = issubclass(y, x)
        if xy == yx:
            return 0
        return -1 if xy else 1

    hierarchy = sorted(hierarchy, cmp=class_cmp)
    return hierarchy[0]


def server_mode(name=None, wait=True):
    testcase = reflect_testcase()
    if testcase is None:
        raise RuntimeError('Cannot find test case to run.')

    if name is not None:
        testcase.set_testname(name)
    testcase.setUpClass()
    if wait:
        testcase.wait()
    testcase.tearDownClass()


def make_common_parser():
    """
    Arguments that are suitable for both lite with tests and lite binary
    """
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('-p', '--port', help="port to run report on", metavar='NUM')
    parser.add_argument('-r', '--root', help="test root directory", metavar='PATH')
    parser.add_argument('-d', '--debug', action="store_true", help="run tests under debugger")
    parser.add_argument('--seed', help="seed for randomizer", metavar='NUM')
    parser.add_argument('--no-color', help="disable colorization of output", action="store_true")
    return parser


def parse_args():
    parser = make_common_parser()
    parser.add_argument('-s', '--server', action="store_true", help="prepare data, run server and wait")
    parser.add_argument('-t', '--test', help="name of test to execute", metavar='NAME')
    parser.add_argument('-w', '--wait', action="store_true", help="keypress wait after server starts")
    parser.add_argument('-v', '--verbose', action="store_true", help="verbose execution")
    parser.add_argument('-f', '--failfast', action="store_true", help="stop after first fail")
    parser.add_argument('-b', '--breakpoint', action="store_true", help="pause before each test run")
    parser.add_argument('-l', '--list', action="store_true", help="list all tests in suite")
    parser.add_argument('-e', '--env', help="path to custom environment config", metavar='PATH')

    parser.add_argument('--dump', help="dumps all output and logs got from server to passed directory", metavar='PATH')

    args = parser.parse_args()
    return args


def list_tests():
    testcase = reflect_testcase()
    if testcase is None:
        print 'Testcase has not been found'
        return

    for name in dir(testcase):
        if name.startswith('test_'):
            print name


def main():
    args = parse_args()
    if args.list:
        return list_tests()

    if args.no_color:
        Color.enabled = False

    if args.seed is not None:
        rand.set_seed(int(args.seed))

    TestCase.set_args(args)

    if args.server is True:
        return server_mode()

    verbosity = 2 if args.verbose or args.breakpoint else 1
    default_test = 'T.{0}'.format(args.test) if args.test else None
    unittest.TestProgram(argv=sys.argv[:1], defaultTest=default_test, verbosity=verbosity, failfast=args.failfast)
