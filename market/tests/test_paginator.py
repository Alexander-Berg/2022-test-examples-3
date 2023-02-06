# -*- coding: utf-8 -*-

import unittest

from context import daas
from daas.button_report.data_model import *
from daas.button_report.index_processor import *
from daas.button_report.paginator import Paginator
import os
import init_data


class Test(unittest.TestCase):

    def _generate_inital_data(self):
        self._options = init_data.gen_init_data()
        init_connection(self._options)
        create_tables(self._options)

    def test_paging(self):
        ''' Тестируем работу пейджинга в репорте по кнопке
        '''
        self._generate_inital_data()
        hd = MiniCluster.get(name='hd')
        for x in xrange(0, 20):
            index = IndexState.create(generation='101010_20%02d' % (x,), cluster=hd, state=INDEX_STATE_QUEUED_UPLOAD)
            index.save()

        query = IndexState.select(IndexState).order_by(IndexState.id)
        paginator = Paginator(query, {'page': 2, 'on-page': 10})
        elem = paginator.get_tasks().next()
        self.assertEqual(elem.generation, '101010_2010')
