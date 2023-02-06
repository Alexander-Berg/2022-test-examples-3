# -*- coding: utf-8 -*-

import gzip
import os
import shutil
import unittest

from getter import core
from getter import logadapter
from getter import util
from getter.service import mbo

from util import touch, calc_md5


logadapter.init_logger()

ROOTDIR = os.path.join(os.getcwd(), 'tmp')
REQUIRED_FILES = [
    'stable/models/models_1.pb',
    'stable/models/sku_1.pb',
    'stable/models/parameters_1.pb',
    'global.vendors.xml',
    'mbo_parameters.pb',
    'vendor-recommended-shops.xml',
    'tovar-tree.pb'
]


class FakeMboStuffService(mbo.MboStuffService):
    FILENAMES = ['a', 'b', 'c.gz'] + REQUIRED_FILES

    def __init__(self, config={}):
        self.md5sums = {}
        for filename in self.FILENAMES:
            filepath = self.getfilepath(filename)
            content = filename + '\n'
            if filename.find('models_') > -1 or filename.find('sku_') > -1:
                content = 'MBEM'
            touch(filepath, content=content)
            md5 = calc_md5(content)
            self.md5sums[md5] = filename
        with gzip.open(self.getfilepath(mbo.MD5SUMS_FILENAME), 'w') as fobj:
            for md5 in self.md5sums:
                fobj.write('%s  %s\n' % (md5, self.md5sums[md5]))

        super(FakeMboStuffService, self).__init__(
            config=config
        )

    def getfilepath(self, filename):
        return os.path.join(ROOTDIR, filename + '.gz')

    @classmethod
    def _get_recent_sid(cls):
        return cls.GENERATION

    def _calc_url(self, filename):
        filepath = os.path.join(ROOTDIR, filename)
        return 'file://' + filepath


class Test(unittest.TestCase):

    def setUp(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)
        util.makedirs(ROOTDIR)

    def tearDown(self):
        pass  # shutil.rmtree(ROOTDIR, ignore_errors=True)

    def _create_mbo(self, generation, config={}):
        FakeMboStuffService.GENERATION = generation
        service = FakeMboStuffService(config)
        root = core.Root(ROOTDIR, create_lazy=False)
        root.register('mbo_stuff', lambda: service)
        return service

    def test(self):
        service = self._create_mbo('1')
        service.update_service(names=['a'])
        service.update_service(names=['a', 'c.gz'], lazy=True)
        service.update_service(names=['a', 'c.gz', 'b'], lazy=True)
        service.update_service(names=['a', 'c.gz', 'b'])

        service = self._create_mbo('2')
        service.update_service(names=['a', 'c.gz', 'b'])

    def test_sku(self):
        checking_file_path = os.path.join(
            ROOTDIR,
            'mbo_stuff',
            'recent',
            'stable/models/sku_1.pb'
        )
        config_skip_sku = {'skip_sku': True}
        service = self._create_mbo('3', config=config_skip_sku)
        service.update_service(names=['stable/models/sku_1.pb'])
        self.assertFalse(os.path.exists(checking_file_path))

        config_not_skip_sku = {'skip_sku': False}
        service = self._create_mbo('4', config=config_not_skip_sku)
        service.update_service(names=['stable/models/sku_1.pb'])
        self.assertTrue(os.path.exists(checking_file_path))

        service = self._create_mbo('5')
        service.update_service(names=['stable/models/sku_1.pb'])
        self.assertTrue(os.path.exists(checking_file_path))


if __name__ == '__main__':
    unittest.main()
