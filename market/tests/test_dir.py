# -*- coding: utf-8 -*-

import os
import shutil
import unittest

from getter import core
from getter import logadapter
from getter import util

from market.pylibrary.filelock import FileLock
from mock import patch

logadapter.init_logger()

ROOTDIR = os.path.join(os.getcwd(), 'tmp')


class Resource(core.Resource):
    def download(self, dst, state, recent_result):
        os.makedirs(dst)
        with open(os.path.join(dst, 'bar'), 'w') as fobj:
            fobj.write('hello\n')
        return {'code': 200}


def create_service():
    service = core.Service(Resource('foo', url=None))
    root = core.Root(ROOTDIR, create_lazy=False)
    root.register('service', lambda: service)
    return service


class Test(unittest.TestCase):

    def setUp(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)
        util.makedirs(ROOTDIR)

    def tearDown(self):
        pass  # shutil.rmtree(ROOTDIR, ignore_errors=True)

    def test(self):
        service = create_service()
        for i in xrange(3):
            service.update_service()
            with open(os.path.join(ROOTDIR, 'service/recent/foo/bar')) as fobj:
                self.assertEqual('hello', fobj.read().strip())

    @patch.object(FileLock, 'lock')
    def test_lazy(self, method):
        service = create_service()
        method.__name__ = "Mock name"
        service.update_service(names=['foo'])  # foo is a dir
        method.assert_called_once()  # update gets a lock once

        service.update_service(names=['foo'], lazy=True)
        method.assert_called_once()  # no new lock calls on lazy update


if __name__ == '__main__':
    unittest.main()
