# -*- coding: utf-8 -*-
import argparse
import inspect
import json
import os
import sys
import unittest

import cataloger
from types.color import Color, draw, colorstr
from index import Index
from core import config
from types.paths import Paths
from response import NullResponse, ustr
from market.shade.lite.shade import Shade
from .combinator import CombinatorExpress
from .content_storage import ContentStorage
from yatest.common.network import PortManager

_DEBUG = False
_WAIT = False
_BREAKPOINT = False


def safe_lambda_exec(exceptions, func):
    try:
        func()
    except Exception as e:
        exceptions.append(e)


class TestCase(unittest.TestCase):
    __server = None
    __cataloger_config = None
    __testcase_name = None
    index = None
    combinator_express = None
    content_storage = None
    __shade = None

    @classmethod
    def setUpClass(cls):
        try:
            cls._setup_class()
        except Exception as err:
            import traceback
            tb = traceback.format_exc()
            raise RuntimeError(str(err) + '\nOriginal traceback = {}\n'.format(tb))

    @classmethod
    def _setup_class(cls):
        cls.__testcase_name = os.path.splitext(os.path.basename(inspect.getfile(cls)))[0]
        Paths.setup(cls.__testcase_name)
        cls.__server = cataloger.Server()
        cls.combinator_express = CombinatorExpress()
        cls.content_storage = ContentStorage()
        cls.__cataloger_config = config.CatalogerConfig(Paths.CATALOGER_CFG, cls.__server.port)
        cls.index = Index(config=cls.__cataloger_config)
        cls.prepare()

        cls.external_services = [
            cls.combinator_express,
            cls.content_storage
        ]

        enable_shade_grpc = (
            cls.combinator_express.has_data()
            or cls.content_storage.has_data()
        )
        cls.__shade = Shade(
            str(Paths.TESTROOT),
            Paths.BUILDROOT,
            Paths.SRCROOT,
            Paths.SHADE_BIN,
            PortManager(),
            enable_shade_grpc,
        )
        # if service shade_support
        for service in cls.external_services:
            cls.__shade.register(service)

        # после запуска шейда, доступны хост/порт комбинатора
        try:
            cls.__shade.start()
        except Exception as e:
            cls.__shade.stop()
            raise

        cls.__cataloger_config.add_fake_combinator(
            cls.combinator_express.get_config(),
        )
        cls.__cataloger_config.add_mock_content_storage(
            use_cs=int(cls.index.use_content_storage_data),
            cs_host_and_port='{}'.format(cls.content_storage.host_and_port_grpc()),
            timeout=20000,
            retry=2
        )

        cls.index.commit()  # до config.save()
        cls.__cataloger_config.save()

        cls.breakpoint('before start')

        try:
            cls.__server.start(debug=_DEBUG)
        except Exception as e:
            cls.__server.stop(e)
            cls.__shade.stop()
            raise

        if _WAIT or _BREAKPOINT:
            sys.stdout.write('cataloger process pid:      ')
            draw(str(cls.__server.pid), Color.GREEN)
            if _WAIT:
                raw_input('Press ENTER to run tests...')

    @classmethod
    def _stop_binaries(cls):
        exceptions = []
        if cls.__shade:
            safe_lambda_exec(exceptions, lambda: cls.__shade.stop())

        for e in exceptions:
            if e:
                raise e

    @classmethod
    def tearDownClass(cls):
        cls.__server.stop()
        cls._stop_binaries()
        # Paths.cleanup()

    @classmethod
    def prepare(cls):
        """Hook method for setting up class fixture before running tests in the class."""

    @classmethod
    def wait(cls):
        if _DEBUG:
            cls.__server.wait()
        else:
            raw_input('Press ENTER to exit...')

    @classmethod
    def stop_cataloger(cls):
        cls.__server.stop_cataloger_binary()

    @classmethod
    def restart_cataloger(cls):
        cls.__server.restart(debug=_DEBUG)

    def __init__(self, name='runTest'):
        unittest.TestCase.__init__(self, name)
        self.cataloger = cataloger.Client(self.__cataloger_config)

    def fail(self, msg=None):
        unittest.TestCase.fail(self, msg)

    @property
    def server(self):
        return self.__server

    def setUp(self):
        self.cataloger.connect(port=self.__server.port)
        self.cataloger.check_alive()

    @classmethod
    def breakpoint(cls, hint=None):
        if _BREAKPOINT:
            for on_break in cls.__on_break:
                on_break()
            text = '{} [ENTER]'.format(hint) if hint else ' [ENTER] '
            draw(text, Color.YELLOW, newline=False)
            raw_input()

    def tearDown(self):
        self.breakpoint('before tearDown')
        self.cataloger.check_alive()
        self.cataloger.stop()

    def __log_response(self, response):
        fr_path = os.path.join(Paths.LOGS_PATH, 'failed_response.log')
        with open(fr_path, 'w+') as fr:
            fr.write(str(response))
        return fr_path

    def assertFragmentIn(self, response, fragment, __barrier=0xdeadbeaf,
                         preserve_order=False, allow_different_len=True,
                         use_regex=False):
        # need to prevent fragment leakage to assert arguments due to wrong json formatting
        if __barrier != 0xdeadbeaf:
            print(__barrier)
            raise RuntimeError('Fragment formatting error')

        request = self.cataloger.last_request
        if isinstance(response, NullResponse):
            self.fail_verbose('cataloger has crashed', [], request)
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
                ustr(fragment), preserve_order, fr_path), reasons, request)

    def assertFragmentNotIn(self, response, fragment, __barrier=0xdeadbeaf, preserve_order=False):
        # need to prevent fragment leakage to assert arguments due to wrong json formatting
        if __barrier != 0xdeadbeaf:
            raise RuntimeError('Fragment formatting error')

        request = self.cataloger.last_request
        if isinstance(response, NullResponse):
            self.fail_verbose('cataloger has crashed', [], request)
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
                    ustr(fragment), preserve_order, fr_path), [], request)

    def assertEqual(self, first, second):
        msg = '{} != {}'.format(first, second)
        unittest.TestCase.assertEqual(
            self, first, second, format_assert(
                msg, self.cataloger.last_request))

    def assertNotEqual(self, first, second):
        msg = '{} == {}'.format(first, second)
        unittest.TestCase.assertNotEqual(
            self, first, second, format_assert(
                msg, self.cataloger.last_request))

    def assertTrue(self, expr):
        msg = '{} is not True'.format(expr)
        unittest.TestCase.assertTrue(self, expr, format_assert(msg, self.cataloger.last_request))

    def assertFalse(self, expr):
        msg = '{} is not False'.format(expr)
        unittest.TestCase.assertFalse(self, expr, format_assert(msg, self.cataloger.last_request))

    def assertIn(self, elem, lst):
        msg = '{} not in {}'.format(elem, lst)
        unittest.TestCase.assertIn(
            self, elem, lst, format_assert(
                msg, self.cataloger.last_request))

    def assertNotIn(self, elem, lst):
        msg = '{} in {}'.format(elem, lst)
        unittest.TestCase.assertNotIn(
            self, elem, lst, format_assert(
                msg, self.cataloger.last_request))

    def __assertEqualResponses(self, get_response_func, request1, request2):
        response1 = get_response_func(request1)
        response2 = get_response_func(request2)
        is_equal, reasons = response1.equal_to(response2)
        if not is_equal:
            self.fail_verbose('Response on request\n{}\nis not equal to response on request\n{}'.format(
                response1.request, response2.request), reasons, response1.request)

    def assertEqualJsonResponses(self, request1, request2):
        return self.__assertEqualResponses(self.cataloger.request_json, request1, request2)

    def fail_verbose(self, msg, reasons, requests):
        reasons = set(reasons)
        reasons_fmt = '\n'.join(reasons)
        error = colorstr(msg.strip(), Color.RED)
        why = colorstr(reasons_fmt, Color.YELLOW)
        where = colorstr(str(Paths.TESTROOT), Color.GREEN)
        request = requests if type(requests) is str else '\n'.join(requests)
        how = colorstr(request, Color.GREEN)
        text = '\n{}\n\n{}\n\n{}\n{}'.format(error, why, how, where) if reasons \
            else '\n{}\n\n{}\n{}'.format(error, how, where)
        self.fail(text)


def format_assert(msg, request):
    error = colorstr(msg, Color.RED)
    where = colorstr(str(Paths.TESTROOT), Color.GREEN)
    how = colorstr(request, Color.GREEN)
    return '\n{}\n\n{}\n{}'.format(error, how, where)


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


def parse_args():
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('-d', '--debug', action="store_true", help="run tests under debugger")
    parser.add_argument('--no-color', help="disable colorization of output", action="store_true")
    parser.add_argument('-t', '--test', help="name of test to execute", metavar='NAME')
    parser.add_argument('-v', '--verbose', action="store_true", help="verbose execution")
    parser.add_argument('-l', '--list', action="store_true", help="list all tests in suite")
    args = parser.parse_args()
    return args


def setup_common_args(args):
    global _DEBUG
    _DEBUG = args.debug
    if args.no_color:
        Color.enabled = False


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
    setup_common_args(args)
    if args.list:
        list_tests()
        return

    verbosity = 2 if args.verbose else 1
    default_test = 'T.{0}'.format(args.test) if args.test else None
    unittest.TestProgram(argv=sys.argv[:1], defaultTest=default_test, verbosity=verbosity)
