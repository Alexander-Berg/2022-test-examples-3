# -*- coding: utf-8 -*-

from itertools import groupby

from data import OrderKey


class Results(object):

    def __init__(self, tables_data):
        data = dict()
        for table_name, table_data in tables_data.items():
            if table_name == 'labels':
                data[table_name] = table_data
                continue
            data[table_name] = self._grouped_data(table_data)

        self.data = data

    def _grouped_data(self, data):
        grouped_data = dict()
        sorted_data = sorted(data, key=self._snapshot_grouper)
        for order_key, order_snapshots in groupby(sorted_data, self._snapshot_grouper):
            grouped_data[order_key] = list(order_snapshots)
        return grouped_data

    @staticmethod
    def _snapshot_grouper(snapshot):
        return OrderKey(partner_name=snapshot['partner_name'], partner_order_id=snapshot['partner_order_id'])
