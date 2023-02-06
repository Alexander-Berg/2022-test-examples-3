# -*- coding: utf-8 -*-

import unittest
import os
import shutil
from cStringIO import StringIO

from guruindexer import marketvendors

DATA = '''\
<?xml version="1.0" encoding="utf-8"?>
<global-vendors>
  <vendor id="1" name="a">
    <site>http://a.ru</site>
    <picture>http://data.yandex.ru/</picture>
    <description>bla</description>
  </vendor>
  <vendor id="2" name="б">
  </vendor>
</global-vendors>
'''


class Test(unittest.TestCase):
    dirpath = 'tmp.tests'
    filepath = os.path.join(dirpath, 'global.vendors.xml')

    def setUp(self):
        os.makedirs(self.dirpath)
        with open(self.filepath, 'w') as f:
            f.write(DATA)

    def tearDown(self):
        shutil.rmtree(self.dirpath, ignore_errors=True)

    def test1(self):
        vendors = marketvendors.load(StringIO(DATA))
        self._check_vendors(vendors)

    def test2(self):
        vendors = marketvendors.read(self.filepath)
        self._check_vendors(vendors)

    def _check_vendors(self, vendors):
        vendor = vendors[0]
        self.assertEqual(1, vendor.id)
        self.assertEqual('a', vendor.name)
        self.assertEqual('a.ru', vendor.site)
        self.assertEqual('http://mdata.yandex.ru/', vendor.picture)
        self.assertEqual('bla', vendor.description)

        vendor = vendors[1]
        self.assertEqual(2, vendor.id)
        self.assertEqual('б', vendor.name)
        self.assertEqual(None, vendor.site)
        self.assertEqual(None, vendor.picture)
        self.assertEqual(None, vendor.description)
