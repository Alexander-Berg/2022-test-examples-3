# -*- coding: utf-8 -*-

import unittest
from cStringIO import StringIO

from guruindexer import marketeditions
from guruindexer import mbodata


class TestEditions(unittest.TestCase):
    data = '''\
<books>
  <book>
    <hyper_category_id>91000</hyper_category_id>
    <hyper_id>1</hyper_id>
    <addition_date>30.04.2009</addition_date>
  </book>
  <book>
    <hyper_category_id>91000</hyper_category_id>
    <hyper_id>2</hyper_id>
    <addition_date>02.05.2009</addition_date>
  </book>
</books>
'''

    def test(self):
        editions = list(marketeditions.iter_editions(StringIO(self.data)))
        self.assertEquals(len(editions), 2)
        self.assertEquals(editions[0].id, 1)
        self.assertEquals(editions[1].id, 2)


class TestSearchQueries(unittest.TestCase):
    def test(self):
        data = '''\
<search-queries>
<query>hello world</query>
<query>привет мир</query>
</search-queries>
'''
        queries = list(mbodata.iter_search_queries(StringIO(data)))
        self.assertEquals(2, len(queries))
        self.assertEquals('hello world', queries[0])
        self.assertEquals('привет мир', queries[1])
