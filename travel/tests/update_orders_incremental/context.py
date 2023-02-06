# -*- coding: utf-8 -*-

import sys

from order_checker import Order, OrderChecker
from results import Results
from table_mapping_checker import TableMappingChecker


class SessionContext(object):

    def __init__(self):
        self.preparing = True
        self.results = Results()
        self.order_checker = OrderChecker(self.results)
        self.table_mapping_checker = TableMappingChecker(self.results)

    def finish_preparing(self):
        self.preparing = False

    def get_order(self, has_label: bool = False) -> Order:
        return self.order_checker.get_order(sys._getframe().f_back.f_code.co_name, self.preparing, has_label)

    def check_table_mapping(self):
        if not self.preparing:
            self.table_mapping_checker.check()
