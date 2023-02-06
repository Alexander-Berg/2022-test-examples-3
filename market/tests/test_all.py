# -*- coding: utf-8 -*-

import logging
import os
import shutil
import unittest
from mock import patch

import market.pylibrary.yenv as yenv

from getter import core
from getter import util
from getter import validator


logging.basicConfig(format='%(asctime)s [%(levelname)s] %(message)s', level=logging.DEBUG)

ROOTDIR = os.path.join(os.getcwd(), 'tmp')
GOOD_PATH = os.path.join(ROOTDIR, 'data/good.csv')
BAD_PATH = os.path.join(ROOTDIR, 'data/not_exists')
GOOD_URL = 'file://' + GOOD_PATH
BAD_URL = 'file://' + BAD_PATH


def touch(filepath, content=None):
    dirname = os.path.dirname(filepath)
    if not os.path.exists(dirname):
        os.makedirs(dirname)
    with open(filepath, 'w') as fobj:
        if content:
            fobj.write(content)


class Test(unittest.TestCase):

    def setUp(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)
        util.makedirs(ROOTDIR)
        touch(GOOD_PATH, '1:11\n2:22\n')

    def tearDown(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)

    def test_good(self):

        def checker(x):
            validator.validate_csv(x, delimiter=':', types=[int, int])

        resource = core.Resource(os.path.basename(GOOD_PATH), GOOD_URL, checker)
        service = core.Service([resource])
        root = core.Root(ROOTDIR, create_lazy=False)
        root.register('good', lambda: service)

        service.update_service()

        # Test monitoring state
        monstate = service.monitoring.get()
        self.assertEqual(service.monitoring.OK, monstate.code)
        self.assertEqual("Ok", monstate.message)

        # Test copier
        copydir = os.path.join(ROOTDIR, 'good-data-copy')
        service.copy(copydir)
        self.assertTrue(util.cmptree(service.generations.recent_path, copydir))

        # Test revisioning
        service.update_service()
        service.update_service()
        self.assertTrue(len(service.generations.names) == 1)
        recent_file_path = service.generations.get_path('recent-generation')
        self.assertTrue(os.path.exists(recent_file_path))
        with open(recent_file_path, 'r') as fd:
            self.assertTrue(fd.read() == service.generations.recent)

    def test_bad(self):
        resource = core.Resource('not_exists', BAD_URL)
        service = core.Service([resource])
        root = core.Root(ROOTDIR, create_lazy=False)
        root.register('bad', lambda: service)

        # Test service update failure
        self.assertRaises(core.Error, service.update_service)

        # Make sure the monitoring shows the failure state
        monstate = service.monitoring.get()
        self.assertTrue(monstate.code > 0)
        self.assertTrue(monstate.message.startswith('Error'), monstate.message)

        # Make sure service.copy fails
        copydir = os.path.join(ROOTDIR, 'bad-data-copy')
        self.assertRaises(core.Error, service.copy, copydir)
        self.assertFalse(os.path.exists(copydir))

    def _test_elliptics(self):
        root = core.create_root(self.rootdir)
        for service_name in ['abo', 'pers']:
            root[service_name].update_service()

    def test_monitoring_error_message_changed(self):
        resource = core.Resource('not_exists', BAD_URL)
        service = core.Service([resource])
        service_name = 'bad'
        # set up old error message to monitoring
        monitoring = core.create_monitoring(ROOTDIR, service_name)
        monitoring.set(monitoring.ERROR, 'old not actual message')

        root = core.Root(ROOTDIR, create_lazy=False)
        root.register(service_name, lambda: service)

        # Test service update failure
        self.assertRaises(core.Error, service.update_service)

        # make sure that monitoring message is changed
        monstate = monitoring.get()
        self.assertTrue(monstate.code > 0)
        self.assertTrue(monstate.message.startswith('Error'), monstate.message)


class TestMbi(unittest.TestCase):

    def setUp(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)
        util.makedirs(ROOTDIR)
        touch(GOOD_PATH, '1:11\n2:22\n')

        URL = 'file://' + GOOD_PATH

        class Meta(core.Resource):

            def __init__(self):
                super(Meta, self).__init__('meta', URL)
                self.finished = None

            def finish(self, filepath, result, recent_result):
                self.finished = True
                parent.assertTrue(os.path.exists(filepath))

        class Shopsdat(core.Resource):

            def __init__(self, name, meta):
                super(Shopsdat, self).__init__(name, url=None, depends=[meta])
                self.meta = meta

            @property
            def urls(self):
                parent.assertTrue(self.meta.finished)
                return [URL]

        parent = self
        meta = Meta()
        shopsdat = Shopsdat('shops-utf8.dat', meta)
        self.service = core.Service(shopsdat, ROOTDIR)

    def tearDown(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)

    def test_update(self):
        self.service.update_service(names=None, lazy=False)

    def test_update_lazy(self):
        self.service.update_service(names=['shops-utf8.dat'], lazy=True)


class TestRoot(unittest.TestCase):

    def test(self):
        skiplist = [
            'corba',
            'mbo_clusters',
            'mbo_stuff',
            'mbo_fast',
            'mbo_cms',
            'cms',
        ]
        with patch('getter.mds.get_latest_marketdynamic', return_value='fake'):
            for envtype in [yenv.DEVELOPMENT, yenv.TESTING, yenv.PRODUCTION]:
                root = core.create_root(ROOTDIR)
                for sname in root:
                    if sname not in skiplist:
                        root.get(sname)


if __name__ == '__main__':
    import doctest
    doctest.testmod()

    unittest.main()
