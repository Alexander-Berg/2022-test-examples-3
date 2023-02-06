# -*- coding: utf-8 -*-

import logging

from data import OrderKey, Snapshot


class Order(object):
    partner_name = 'test_partner'

    def __init__(self, session_context, test_name):
        self.session_context = session_context
        self.test_name = test_name
        self.order_id = 'order_id_{}_{}'.format(test_name, session_context.get_order_id())
        self.order_key = OrderKey(partner_name=self.partner_name, partner_order_id=self.order_id)
        self.snapshots = list()
        self.not_saved_snapshots = set()
        self.last_saved_index = -1
        self.has_copies_to_save = False
        self.current_updated_at = 0

    def add_snapshot(self, copy_previous=False, copy_key=False, last_saved=False, skip_save=False):
        if not self.session_context.preparing:
            return
        if last_saved:
            if self.last_saved_index >= 0:
                raise Exception('Saved snapshots already finished at index {}'.format(self.last_saved_index))
            if self.has_copies_to_save:
                raise Exception('Saving snapshots with copies is prohibited')
            if skip_save:
                raise Exception('"last_saved" may not be "not_saved"')
            self.last_saved_index = len(self.snapshots)
        if copy_previous or copy_key:
            if not self.snapshots:
                raise Exception('Nothing to copy')
            previous_snapshot = self.snapshots[-1]
            if copy_previous:
                snapshot = previous_snapshot.replace(updated_at=self.current_updated_at)
            elif copy_key:
                snapshot = previous_snapshot.replace(hash='hash_{}'.format(len(self.snapshots)))
            else:
                raise Exception('One of "copy_previous" or "copy_key" expected to be True')
            if not skip_save and previous_snapshot not in self.not_saved_snapshots:
                self.has_copies_to_save = True
        else:
            snapshot = Snapshot(
                partner_name=self.partner_name,
                partner_order_id=self.order_id,
                hash='hash_{}'.format(len(self.snapshots)),
                updated_at=self.current_updated_at,
            )
        if skip_save:
            self.not_saved_snapshots.add(snapshot)
        self.current_updated_at += 1
        logging.info('Adding %s', snapshot)
        self.snapshots.append(snapshot)

    def get_processed_snapshots(self):
        last_saved_index = self.last_saved_index
        processed_snapshots = list()
        if last_saved_index >= 0:
            processed_snapshots.append(self.snapshots[last_saved_index])
        return processed_snapshots

    def get_saved_snapshots(self):
        return [s for s in self.snapshots[:self.last_saved_index + 1] if s not in self.not_saved_snapshots]

    def get_snapshots_to_send(self):
        return self.snapshots[:]

    def check(self):
        if self.session_context.preparing:
            return
        logging.info('Checking %s', self.test_name)
        deduplicated_snapshots = self._get_deduplicated_snapshots()
        self._check('processed_snapshots', deduplicated_snapshots[-1:])
        self._check('snapshots', deduplicated_snapshots)
        self._check('snapshot_errors', self._get_not_unique_key_snapshots())
        order_queue_expected = list()
        if deduplicated_snapshots != self.get_saved_snapshots():
            order_queue_expected.append(self.snapshots[-1])
        self._check('order_queue', order_queue_expected)

    def _get_deduplicated_snapshots(self):
        deduplicated_snapshots = list()
        previous_updated_at = None
        previous_hash = None
        for snapshot in self.snapshots:
            if snapshot.hash == previous_hash:
                continue
            if snapshot.updated_at == previous_updated_at:
                deduplicated_snapshots = deduplicated_snapshots[:-1]
            deduplicated_snapshots.append(snapshot)
            previous_updated_at = snapshot.updated_at
            previous_hash = snapshot.hash
        return deduplicated_snapshots

    def _get_not_unique_key_snapshots(self):
        not_unique_key_snapshots = list()
        previous_updated_at = None
        previous_hash = None
        for index, snapshot in enumerate(self.snapshots):
            need_check = index >= self.last_saved_index
            if need_check and snapshot.updated_at == previous_updated_at and snapshot.hash != previous_hash:
                not_unique_key_snapshots.append(snapshot)
            previous_updated_at = snapshot.updated_at
            previous_hash = snapshot.hash
        return not_unique_key_snapshots

    def _check(self, table_name, expected):
        actual = self.session_context.results.data[table_name].get(self.order_key, list())
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
            self._compare_pair(table_name, index, expected_item.as_dict(add_data=True), actual_item)

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
