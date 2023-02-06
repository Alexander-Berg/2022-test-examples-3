# -*- coding: utf-8 -*-

from collections import namedtuple
from itertools import chain
import logging

from google.protobuf.descriptor import FieldDescriptor

from data import LABEL_FIELD_MAPPING, LabelAvia,  LabelHotels, LabelTrain, LabelSuburban, LabelBuses, LabelTours
import travel.proto.avia.cpa.label_pb2 as label_pb2


TYPE_MAP = {
    FieldDescriptor.TYPE_UINT32: int,
    FieldDescriptor.TYPE_UINT64: int,
    FieldDescriptor.TYPE_INT32: int,
    FieldDescriptor.TYPE_INT64: int,
    FieldDescriptor.TYPE_DOUBLE: float,
}


LabelKey = namedtuple('LabelKey', ['category', 'label'])


class LabelChecker(object):
    partner_name = 'test_partner_labels'
    partner_order_id = 'partner_order_id_labels'

    def __init__(self, session_context):
        self.session_context = session_context
        self.field_map = {v: k for k, v in LABEL_FIELD_MAPPING.items()}
        self.labels_avia = list()
        self.labels_hotels = list()
        self.labels_train = list()
        self.labels_suburban = list()
        self.labels_buses = list()
        self.labels_tours = list()
        self.purgatory_items = list()
        self.current_label_index = 0
        self.current_updated_at = 0

    def add_label(self, awaited=False, stick=False):
        if not self.session_context.preparing:
            return
        label_id = self._get_current_label_id()
        data = 'some_data_{}'.format(self.current_label_index)

        label_avia = self._get_label_avia()
        logging.info('Adding %s', label_avia)
        self._add_label(label_avia, self.labels_avia, awaited, stick)

        label_hotels = LabelHotels(unixtime=self.current_updated_at, Label=label_id, Proto=data)
        logging.info('Adding %s', label_hotels)
        self._add_label(label_hotels, self.labels_hotels, awaited, stick)

        label_train = LabelTrain(unixtime=self.current_updated_at, LabelHash=label_id, Proto=data)
        logging.info('Adding %s', label_train)
        self._add_label(label_train, self.labels_train, awaited, stick)

        label_suburban = LabelSuburban(unixtime=self.current_updated_at, LabelHash=label_id, Proto=data)
        logging.info('Adding %s', label_suburban)
        self._add_label(label_suburban, self.labels_suburban, awaited, stick)

        label_buses = LabelBuses(unixtime=self.current_updated_at, LabelHash=label_id, Proto=data)
        logging.info('Adding %s', label_buses)
        self._add_label(label_buses, self.labels_buses, awaited, stick)

        label_tours = LabelTours(unixtime=self.current_updated_at, LabelHash=label_id, Proto=data)
        logging.info('Adding %s', label_tours)
        self._add_label(label_tours, self.labels_tours, awaited, stick)

    def check(self):
        if self.session_context.preparing:
            return
        table_name = 'labels'
        logging.info('Checking %s', table_name)
        actual = self.session_context.results.data[table_name]
        self._check(table_name, actual, self._get_expected_labels())

        table_name = 'order_queue'
        logging.info('Checking %s', table_name)
        table_content = self.session_context.results.data[table_name]
        actual = table_content.get((self.partner_name, self.partner_order_id), list())
        self._check(table_name, actual, self._get_expected_order_queue())

    def _add_label(self, label, container, awaited, stick):
        if awaited:
            purgatory_item = dict(
                label=self._get_current_label_id(),
                partner_name=self.partner_name,
                partner_order_id=self.partner_order_id,
                updated_at=self.current_updated_at,
            )
            self.purgatory_items.append(purgatory_item)
        if stick:
            if not container:
                raise Exception('No batch to stick to')
            batch = container[-1]
        else:
            batch = list()
            container.append(batch)
        batch.append(label)
        self.current_label_index += 1
        self.current_updated_at += 1

    def _get_label_avia(self):
        label_index = self.current_label_index
        label_fields = dict()
        for field in label_pb2.TLabel.DESCRIPTOR.fields:
            field_name = self.field_map[field.name]
            converter = TYPE_MAP.get(field.type)
            if converter is None:
                field_value = '{}_{}'.format(field_name, label_index)
            else:
                field_value = converter(label_index)
            label_fields[field_name] = field_value
        return LabelAvia(unixtime=self.current_updated_at, marker=self._get_current_label_id(), **label_fields)

    def _get_current_label_id(self):
        return 'label_{}'.format(self.current_label_index)

    def _get_expected_labels(self):
        all_labels = self.labels_avia + self.labels_hotels + self.labels_train + self.labels_suburban + self.labels_buses + self.labels_tours
        return sorted(
            (label.get_expected_value() for label in chain.from_iterable(all_labels)),
            key=lambda x: (x['category'], x['label'])
        )

    def _get_expected_order_queue(self):
        return [
            dict(partner_name=item['partner_name'], partner_order_id=item['partner_order_id'])
            for item in self.purgatory_items
        ]

    def _check(self, table_name, actual, expected):
        if not expected:
            if actual:
                raise Exception('{}: nothing expected but got {}'.format(table_name, actual))
            return
        self._compare(expected, actual, table_name)

    def _compare(self, expected, actual, table_name):
        expected_size = len(expected)
        actual_size = len(actual)
        if expected_size > actual_size:
            extra_items = expected[actual_size:]
            raise Exception('{}, more expected than actual. Extra items started at index {}: {}'.format(
                table_name, actual_size, extra_items)
            )
        if actual_size > expected_size:
            extra_items = actual[expected_size:]
            raise Exception('{}, more actual than expected. Extra items started at index {}: {}'.format(
                table_name, expected_size, extra_items)
            )
        for index, (expected_item, actual_item) in enumerate(zip(expected, actual)):
            self._compare_pair(table_name, index, expected_item, actual_item)

    @staticmethod
    def _compare_pair(table_name, index, expected, actual):
        for field, actual_value in actual.items():
            # TODO: better way to ignore or check 'error' field
            if field == 'error':
                continue
            expected_value = expected[field]
            if expected_value != actual_value:
                raise Exception('{}[{}]>{}: expected {} but got {}'.format(
                    table_name, index, field, expected_value, actual_value)
                )
