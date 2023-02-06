# -*- coding: utf-8 -*-
import os
import shutil
import logging
import unittest
import multiprocessing

import context
from market.pylibrary.database.profiler import profile
from market.pylibrary.mi_util import util

TMPROOT = 'tmp'
TMPLOGFILE = os.path.join(TMPROOT, 'tmpfile.log')


def rm_root_dir():
    shutil.rmtree(TMPROOT, ignore_errors=True, onerror=None)


def recreate_root_dir():
    rm_root_dir()
    util.makedirs(TMPROOT)


def rmtmpfile():
    if os.path.exists(TMPLOGFILE):
        os.unlink(TMPLOGFILE)


def get_logs_from_separate_process(separate_process):
    def redirect_testing_logging(tmplog_filepath):
        open(tmplog_filepath, 'w').close()

        ok = False
        root_logger = logging.getLogger()
        for handler in root_logger.handlers:
            if hasattr(handler, 'stream'):
                handler.stream = open(tmplog_filepath, 'a')
                ok = True

        if not ok:
            raise

    def separate_process_wrapper(separate_process):
        redirect_testing_logging(TMPLOGFILE)
        separate_process()

    proc = multiprocessing.Process(target=separate_process_wrapper,
                                   args=(separate_process,))
    proc.start()
    proc.join()

    output = open(TMPLOGFILE, 'r').read().strip()
    rmtmpfile()

    return output


class MyTestClass(object):
    def __init__(self):
        self.mytestvalue = 5

    @profile()
    def get_test_value(self):
        return self.mytestvalue

    def similar_name(self):
        return self.mytestvalue


@profile()
def get_test_value(mytestvalue):
    return mytestvalue


@profile()
def similar_name(testObj):
    return testObj.similar_name()


class TestProfiler(unittest.TestCase):
    def contains(self, result, substring):
        self.assertTrue(-1 != result.find(substring))

    def notContains(self, result, substring):
        self.assertTrue(-1 == result.find(substring))

    def setUp(self):
        recreate_root_dir()
        rmtmpfile()

    def tearDown(self):
        rm_root_dir()

    def test_profile_class_method(self):
        def separate_process():
            tc = MyTestClass()
            tc.get_test_value()

        result = get_logs_from_separate_process(separate_process)
        self.contains(result, 'MyTestClass::get_test_value')
        self.contains(result, 'takes')
        self.contains(result, 'args=()')
        self.contains(result, 'returns=5')

    def test_profile_function(self):
        def separate_process():
            get_test_value(5)

        result = get_logs_from_separate_process(separate_process)
        self.notContains(result, '::get_test_value')
        self.contains(result, 'get_test_value')
        self.contains(result, 'takes')
        self.contains(result, 'args=(5,)')
        self.contains(result, 'returns=5')

    def test_profile_function_complex(self):
        def separate_process():
            tc = MyTestClass()
            similar_name(tc)

        result = get_logs_from_separate_process(separate_process)
        self.notContains(result, '::similar_name')
        self.contains(result, 'similar_name')
        self.contains(result, 'args=(<')  # object pointer printed here
        self.contains(result, 'returns=5')


if '__main__' == __name__:
    context.main()
