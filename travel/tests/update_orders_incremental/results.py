# -*- coding: utf-8 -*-

from itertools import groupby

from travel.cpa.data_processing.lib.protobuf_utils import proto_from_base64_str
from travel.proto.cpa import order_info_pb2 as order_info_pb2

from data import OrderKey


class Results(object):

    def __init__(self):
        self.tables_data = dict()
        self.lb_data = set()

    def update(self, tables_data, lb_data):
        for table_name, table_data in tables_data.items():
            self.tables_data[table_name] = self._grouped_data(table_data)
        for item in lb_data:
            message = proto_from_base64_str(order_info_pb2.TGenericOrderInfo, item['proto'])
            self.lb_data.add((message.PartnerName, message.PartnerOrderId))

    def _grouped_data(self, data):
        grouped_data = dict()
        sorted_data = sorted(data, key=self._grouper)
        for order_key, order_snapshots in groupby(sorted_data, self._grouper):
            grouped_data[order_key] = list(order_snapshots)
        return grouped_data

    @staticmethod
    def _grouper(row):
        return OrderKey(partner_name=row['partner_name'], partner_order_id=row['partner_order_id'])
