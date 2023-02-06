# -*- coding: utf-8 -*-

import unittest
import os
import shutil
from cStringIO import StringIO

from guruindexer import marketcatalog

DATA = '''\
<?xml version="1.0" encoding="utf-8"?>
<item name="Все товары" id="90401" parent="0" model_list_id="0" uniq_name="">
  <item name="Телефоны" id="91461" parent="90401" model_list_id="0" uniq_name="Телефоны и аксессуары к ним" >
    <item name="Мобильные телефоны" id="91491" parent="91461" visual="false" model_list_id="160043" uniq_name="Мобильные телефоны2" />
  </item>
</item>
'''


class Test(unittest.TestCase):
    dirpath = 'tmp.tests'
    filepath = os.path.join(dirpath, 'catalog.xml')

    def setUp(self):
        os.makedirs(self.dirpath)
        with open(self.filepath, 'w') as f:
            f.write(DATA)

    def tearDown(self):
        shutil.rmtree(self.dirpath, ignore_errors=True)

    def test1(self):
        tree = marketcatalog.load(StringIO(DATA))
        self._check_tree(tree)

    def _check_tree(self, tree):
        root = tree.root()
        self.assertEqual(90401, root.id)
        self.assertEqual(None, root.parentid)
        self.assertEqual([tree.node(91461)], root.childs)

        mobile = tree.node(91491)
        self.assertEqual('Мобильные телефоны', mobile.name)
        self.assertEqual('Мобильные телефоны2', mobile.uniq_name)
        self.assertEqual(160043, mobile.guruid)
