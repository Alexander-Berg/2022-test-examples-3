# -*- coding: utf-8 -*-
import base64
from copy import deepcopy
from datetime import datetime
from typing import Dict, NamedTuple, Optional, Type
import json
import logging

from order_comparator import OrderComparator
from data import Fields, SnapshotProducer, TableData
from google.protobuf.descriptor import FieldDescriptor
from google.protobuf.message import Message
from results import Results

from travel.cpa.data_processing.lib.label_converter import LABEL_CONVERTERS, LabelConverter
from travel.cpa.data_processing.lib.label_mapper import LABEL_MAPPERS, LabelMapper
from travel.cpa.data_processing.lib.order_data_model import CATEGORY_CONFIGS
from travel.cpa.data_processing.lib.protobuf_utils import protobuf_to_dict


TYPE_MAP = {
    FieldDescriptor.TYPE_UINT32: int,
    FieldDescriptor.TYPE_UINT64: int,
    FieldDescriptor.TYPE_INT32: int,
    FieldDescriptor.TYPE_INT64: int,
    FieldDescriptor.TYPE_DOUBLE: float,
    FieldDescriptor.TYPE_ENUM: int,
    FieldDescriptor.TYPE_BOOL: bool,
}


class CategoryTestSettings(NamedTuple):
    partner_name: str
    currency_code: str
    label_cls: Optional[Type[Message]] = None
    label_converter: Optional[LabelConverter] = None
    label_mapper: Optional[LabelMapper] = None
    label_category: Optional[str] = None


class TLabelInfo(NamedTuple):
    category: str
    label_id: str
    label: Message


class Order(object):

    __test_settings__ = dict(
        avia=CategoryTestSettings(
            partner_name='agent',
            currency_code='USD',
            label_cls=LABEL_CONVERTERS['avia'].proto_cls,
            label_converter=LABEL_CONVERTERS['avia'],
        ),
        buses=CategoryTestSettings(
            partner_name='sks',
            currency_code='RUB',
            label_cls=LABEL_CONVERTERS['buses'].proto_cls,
            label_converter=LABEL_CONVERTERS['buses'],
        ),
        hotels=CategoryTestSettings(
            partner_name='booking',
            currency_code='EUR',
            label_cls=LABEL_CONVERTERS['hotels'].proto_cls,
            label_converter=LABEL_CONVERTERS['hotels'],
        ),
        train=CategoryTestSettings(
            partner_name='im_boy',
            currency_code='RUB',
            label_cls=LABEL_CONVERTERS['train'].proto_cls,
            label_converter=LABEL_CONVERTERS['train'],
        ),
        suburban=CategoryTestSettings(
            partner_name='suburban',
            currency_code='RUB',
            label_cls=LABEL_CONVERTERS['suburban'].proto_cls,
            label_converter=LABEL_CONVERTERS['suburban'],
        ),
        tours=CategoryTestSettings(
            partner_name='leveltravel_whitelabel',
            currency_code='RUB',
            label_cls=LABEL_CONVERTERS['tours'].proto_cls,
            label_converter=LABEL_CONVERTERS['tours'],
        ),
        generic=CategoryTestSettings(
            partner_name='boy',
            currency_code='RUB',
            label_cls=LABEL_CONVERTERS['train'].proto_cls,
            label_converter=LABEL_CONVERTERS['generic'],
            label_mapper=LABEL_MAPPERS[('train', 'generic')],
            label_category='train',
        ),
    )

    def __init__(self, test_name: str, order_id: int, results: Results, has_label: bool):
        self.test_name = test_name
        self.order_id = order_id
        self.results = results
        self.has_label = has_label
        self.preparing = True

        self.snapshot_producers = dict()
        self.snapshots = dict()
        self.labels = dict()
        for category, config in CATEGORY_CONFIGS.items():
            producer = SnapshotProducer(config.order_with_encoded_label_cls)
            self.snapshot_producers[category] = producer
            self.snapshots[category] = list()

            label_cls = self.__test_settings__[category].label_cls
            if label_cls is not None:
                label_info = TLabelInfo(
                    category=self.__test_settings__[category].label_category or category,
                    label_id=f'label_{category}_{order_id}',
                    label=self._get_label(label_cls),
                )
                self.labels[category] = label_info

        self.started_at = int(datetime.now().timestamp())

    def add_snapshot(self, fields_to_change: Optional[Fields] = None):
        if not self.preparing:
            return

        if fields_to_change is None:
            fields_to_change = dict()

        for category, snapshots in self.snapshots.items():
            if not snapshots:
                producer = self.snapshot_producers[category]
                config = self.__test_settings__[category]
                snapshot = producer.get_snapshot(
                    category=category,
                    partner_name=config.partner_name,
                    partner_order_id=self.order_id,
                    currency_code=config.currency_code,
                )
                snapshots.append(snapshot)
            else:
                snapshot = deepcopy(snapshots[-1])
                for field_name, value in fields_to_change.items():
                    setattr(snapshot, field_name, value)
            snapshot_id = len(snapshots)
            snapshot.updated_at = self.started_at + snapshot_id
            label_info = self.labels.get(category)
            if label_info is not None:
                snapshot.label = label_info.label_id
            snapshots.append(snapshot)

    def get_tables(self) -> Dict[str, TableData]:
        snapshots = list()
        order_queue = list()
        labels = list()
        order_purgatory = list()

        for category, category_snapshots in self.snapshots.items():
            for snapshot_id, snapshot in enumerate(category_snapshots):
                data = snapshot.as_dict()
                data['hash'] = f'hash_{self.order_id}_{snapshot_id}'
                snapshot_fields = dict(
                    partner_name=snapshot.partner_name,
                    partner_order_id=snapshot.partner_order_id,
                    updated_at=snapshot.updated_at,
                    data=json.dumps(data),
                )
                snapshots.append(snapshot_fields)
            last_snapshot = category_snapshots[-1]
            order_queue_fields = dict(
                partner_name=last_snapshot.partner_name,
                partner_order_id=last_snapshot.partner_order_id,
            )
            order_queue.append(order_queue_fields)

            label_info = self.labels.get(category)
            if self.has_label and label_info is not None:
                label = dict(
                    category=label_info.category,
                    label=label_info.label_id,
                    data=base64.urlsafe_b64encode(label_info.label.SerializeToString()).decode(),
                )
                labels.append(label)

                snapshot = snapshots[-1]
                order_purgatory.append(dict(
                    label=label_info.label_id,
                    partner_name=snapshot['partner_name'],
                    partner_order_id=snapshot['partner_order_id'],
                    updated_at=snapshot['updated_at'],
                ))

        return dict(
            snapshots=snapshots,
            order_queue=order_queue,
            labels=labels,
            order_purgatory=order_purgatory,
        )

    def check(self):
        if self.preparing:
            return
        logging.info('Checking %s', self.test_name)

        for category, snapshots in self.snapshots.items():
            table_name = f'{category}/orders_internal'
            partner_name = self.__test_settings__[category].partner_name
            partner_order_id = str(self.order_id)
            order_key = partner_name, partner_order_id
            actual = self.results.tables_data[table_name].get(order_key, list())
            expected = [self._get_expected_order(category, snapshots[-1])]
            self._check(table_name, actual, expected, OrderComparator.__order_fields_to_ignore__)

            expected = list()
            label_info = self.labels.get(category)
            if not self.has_label and label_info is not None:
                expected.append(dict(
                    label=label_info.label_id,
                    partner_name=partner_name,
                    partner_order_id=partner_order_id,
                    updated_at=snapshots[-1].updated_at,
                ))

            table_name = 'order_purgatory'
            actual = self.results.tables_data[table_name].get(order_key, list())
            self._check(table_name, actual, expected)

            if category not in ('hotels', 'avia'):
                continue
            for snapshot in snapshots:
                assert (snapshot.partner_name, snapshot.partner_order_id) in self.results.lb_data

    def _get_label(self, label_cls: Type[Message]) -> Message:
        label = label_cls()
        for field in label_cls.DESCRIPTOR.fields:
            field_name = field.name
            if field_name in ('TestBuckets',):
                continue
            converter = TYPE_MAP.get(field.type)
            if converter is None:
                field_value = '{}_{}'.format(field_name, self.order_id)
            else:
                field_value = converter(self.order_id)
            if field.label == FieldDescriptor.LABEL_REPEATED:
                getattr(label, field_name).append(field_value)
            else:
                setattr(label, field_name, field_value)
        return label

    def _get_expected_order(self, category: str, order) -> TableData:
        label_info = self.labels.get(category)
        if label_info is not None:
            order.label = label_info.label_id
        config = CATEGORY_CONFIGS[category]
        order_cls = config.order_with_decoded_label_cls or config.order_with_encoded_label_cls
        order_dict = order.as_dict()
        order_dict['has_label'] = False
        if self.has_label and label_info is not None:
            test_settings = self.__test_settings__[category]
            label_mapper = test_settings.label_mapper
            label = label_info.label
            if label_mapper is not None:
                label = label_mapper.get_mapped_proto(label)
            modifier = test_settings.label_converter.modified_key_func
            label = {modifier(k): v for k, v in protobuf_to_dict(label).items()}
            order_dict = {**order.as_dict(), **label, 'has_label': True}
        order = order_cls.from_dict(order_dict, ignore_unknown=True, convert_type=True)
        return order.as_dict()

    def _check(self, table_name, actual, expected, fields_to_ignore=None):
        if fields_to_ignore is None:
            fields_to_ignore = list()
        if not expected:
            if actual:
                raise Exception('{}: nothing expected but got {}'.format(table_name, actual))
            return
        self._compare(expected, actual, table_name, fields_to_ignore)

    @staticmethod
    def _compare(expected, actual, table_name, fields_to_ignore):
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
            OrderComparator.compare_pair(table_name, index, expected_item, actual_item, fields_to_ignore)


class OrderChecker:

    def __init__(self, results: Results):
        self.current_order_id = 0
        self.orders: Dict[str, Order] = dict()
        self.results = results

    def get_order(self, test_name: str, preparing: bool, has_label: bool) -> Order:
        stage = 'preparing' if preparing else 'running'
        logging.info('get_order called from %s at %s stage', test_name, stage)
        if preparing:
            order = Order(test_name, self._new_order_id(), self.results, has_label)
            self.orders[test_name] = order
        else:
            order = self.orders[test_name]
        order.preparing = preparing
        return order

    def get_tables(self) -> Dict[str, TableData]:
        tables = dict()
        for _, order in self.orders.items():
            for table_name, rows in order.get_tables().items():
                all_rows = tables.setdefault(table_name, list())
                all_rows.extend(rows)
        return tables

    @staticmethod
    def get_tables_to_read():
        return [
            'avia/orders_internal',
            'buses/orders_internal',
            'hotels/orders_internal',
            'train/orders_internal',
            'suburban/orders_internal',
            'generic/orders_internal',
            'tours/orders_internal',
            'order_queue',
            'order_purgatory',
        ]

    def _new_order_id(self):
        self.current_order_id += 1
        return self.current_order_id
