# -*- coding: utf-8 -*-

import unittest

from context import daas
from daas.button_report.data_model import *
import os
import init_data

class Test(unittest.TestCase):
    def _generate_inital_data(self):
        self._options = init_data.gen_init_data()
        init_connection(self._options)
        create_tables(self._options)

    def test(self):
        self._generate_inital_data()
        hd = MiniCluster.get(name='hd')
        index = IndexState.create(generation='101010_2020', cluster=hd, state='Running')
        index.save()

        indicies  = (IndexState.select().join(MiniCluster))
        for ind in indicies:
            print ind.cluster.name


