# -*- coding: utf-8 -*-

import logging
import os
import shutil
import time
import unittest

import market.pylibrary.yenv as yenv

from getter import core
from getter import util
from getter.main import Settings


ROOT_DIR = os.path.abspath('tmp')
DATA_DIR = os.path.join(ROOT_DIR, 'data')
OVERRIDES_DIR = os.path.join(ROOT_DIR, 'overrides')
DOWNLOAD_DIR = os.path.join(ROOT_DIR, 'download')

FOO_DATA = 'foo_data'
FOO_OVERRIDE = 'foo_override'
BAR_DATA = 'bar_data'
BAR_OVERRIDE = 'bar_override'

SERVICE_NAME = 'fubar'


logging.basicConfig(format='%(asctime)s [%(levelname)s] %(message)s', level=logging.DEBUG)


core.SETTINGS = Settings(
    args=None,
    parser=None,
    data_dir=DATA_DIR,
    overrides_dir=OVERRIDES_DIR,
    log_file=None,
    confd_dir=None,
    mbo_confd_dir=None,
    corba_confd_dir=None,
    config=None,
    host_to_switch=None,
    nonblock=None,
    web_features_real=None,
    no_yt_debug_web_features=None,
    srv_environment_override_config_dir=None
)


def make_download_path(name):
    return os.path.join(DOWNLOAD_DIR, name)


def make_download_url(name):
    return 'file://' + make_download_path(name)


def make_download_contents(name, contents):
    open(make_download_path(name), 'w').write(contents)


def make_override_path(service, name):
    return os.path.join(OVERRIDES_DIR, service, name)


def make_override_contents(service, name, contents):
    open(make_override_path(service, name), 'w').write(contents)


def make_service(foo_checker, bar_checker):
    bar = core.Resource(
        'bar',
        make_download_url('bar'),
        checker=bar_checker)
    foo = core.Resource(
        'foo',
        make_download_url('foo'),
        checker=foo_checker,
        depends=[bar])
    return core.Service([foo, bar], rootdir=os.path.join(DATA_DIR, SERVICE_NAME))


class TestOverrides(unittest.TestCase):
    def setUp(self):
        yenv.set_environment_type(yenv.TESTING)
        shutil.rmtree(ROOT_DIR, ignore_errors=True)
        util.makedirs(DATA_DIR, DOWNLOAD_DIR, OVERRIDES_DIR)
        util.makedirs(os.path.join(OVERRIDES_DIR, SERVICE_NAME))

    def tearDown(self):
        shutil.rmtree(ROOT_DIR, ignore_errors=True)

    def test_no_overrides(self):
        """Old logic must still work.
        """
        self._prepare_service(FOO_DATA, None, BAR_DATA, None).update_service()

    def test_override_foo(self):
        """Overriding foo shouldn't break it's dependency on bar.
        """
        self._prepare_service(FOO_DATA, FOO_OVERRIDE, BAR_DATA, None).update_service()

    def test_override_bar(self):
        """Downloading bar as dependency shouldn't break overrides.
        """
        self._prepare_service(FOO_DATA, None, BAR_DATA, BAR_OVERRIDE).update_service()

    def test_override_foo_expired(self):
        """Expired overrides should not be applied.
        """
        # swap foo_data and foo_override so that validation still works
        self._prepare_service(FOO_OVERRIDE, FOO_DATA, BAR_DATA, None, foo_age=9).update_service()

    def test_override_bar_fail(self):
        """Neither overrides nor dependencies should break validation.
        """
        with self.assertRaises(Exception):
            self._prepare_service(FOO_DATA, None, BAR_DATA, 'bogus').update_service()

    def _contents_checker(self, contents):
        def checker(path):
            self.assertEquals(contents, open(path).read())
        return checker

    def _prepare_service(self, foo_data, foo_override, bar_data, bar_override, foo_age=0, bar_age=0):
        now = int(time.time())

        if foo_data:
            make_download_contents('foo', foo_data)
        if foo_override:
            make_override_contents(SERVICE_NAME, 'foo', foo_override)
            age = now - 60 * 60 * foo_age
            os.utime(make_override_path(SERVICE_NAME, 'foo'), (age, age))
        if bar_data:
            make_download_contents('bar', bar_data)
        if bar_override:
            make_override_contents(SERVICE_NAME, 'bar', bar_override)
            age = now - 60 * 60 * bar_age
            os.utime(make_override_path(SERVICE_NAME, 'bar'), (age, age))

        return make_service(
            self._contents_checker(FOO_OVERRIDE if foo_override else FOO_DATA),
            self._contents_checker(BAR_OVERRIDE if bar_override else BAR_DATA))


if __name__ == '__main__':
    unittest.main()
