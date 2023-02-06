# -*- coding: utf-8 -*-

import sys
import logging

from snapshot_checker import Order
from label_checker import LabelChecker


class SessionContext(object):

    def __init__(self):
        self.preparing = True
        self.orders = dict()
        self.current_order_id = 0
        self.results = None
        self.label_checker = LabelChecker(self)

    def finish_preparing(self):
        self.preparing = False

    def get_order(self):
        test_name = sys._getframe().f_back.f_code.co_name
        stage = 'preparing' if self.preparing else 'running'
        logging.info('get_order called from %s at %s stage', test_name, stage)
        if self.preparing:
            order = Order(self, test_name)
            self.orders[test_name] = order
        else:
            order = self.orders[test_name]
        return order

    def add_label(self, **kwargs):
        stage = 'preparing' if self.preparing else 'running'
        logging.info('add_label called at %s stage', stage)
        self.label_checker.add_label(**kwargs)

    def check_labels(self):
        self.label_checker.check()

    def get_order_id(self):
        self.current_order_id += 1
        return self.current_order_id

    def get_processed_snapshots(self):
        return self._get_snapshots(Order.get_processed_snapshots)

    def get_saved_snapshots(self):
        return self._get_snapshots(Order.get_saved_snapshots)

    def get_snapshots_to_send(self):
        return self._get_snapshots(Order.get_snapshots_to_send)

    def get_labels_to_send(self):
        return dict(
            avia=self.label_checker.labels_avia[:],
            hotels=self.label_checker.labels_hotels[:],
            train=self.label_checker.labels_train[:],
            suburban=self.label_checker.labels_suburban[:],
            buses=self.label_checker.labels_buses[:],
            tours=self.label_checker.labels_tours[:],
        )

    def get_purgatory_items(self):
        return self.label_checker.purgatory_items[:]

    def _get_snapshots(self, snapshot_getter):
        snapshots = list()
        for order in self.orders.values():
            snapshots.extend(snapshot_getter(order))
        return snapshots
