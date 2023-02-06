# -*- coding: utf-8 -*-

import unittest

from context import daas
from daas.button_report.data_model import *
from daas.button_report.index_processor import *
import os
import init_data


class Test(unittest.TestCase):

    def _generate_inital_data(self):
        self._options = init_data.gen_init_data()
        init_connection(self._options)
        create_tables(self._options)

    def test_update_mi_states(self):
        ''' Тестируем обновление списка поколений на мастерах индексации
        '''
        res = update_mi_states()
        self.assertEqual(res, True)
        query = MiState.select(MiState).where(MiState.is_testing)
        elem = query.get()
