# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six
import uuid

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from search.martylib.core.exceptions import Locked, MaxRetriesReached
from search.martylib.db_utils import to_model

from search.priemka.yappy.src.model.model_service.workers.allocator import BaseSlotManager
from search.priemka.yappy.src.model.model_service.workers.exceptions import BrokenSlotsLimitExceeded

from search.priemka.yappy.proto.structures.beta_pb2 import Beta
from search.priemka.yappy.proto.structures.beta_component_pb2 import BetaComponent
from search.priemka.yappy.sqla.yappy import model

from search.priemka.yappy.tests.utils.test_cases import TestCase

SLOT_MANAGER_IMPORT_PATH = 'search.priemka.yappy.src.model.model_service.workers.allocator.BaseSlotManager'


class SlotManagerTestCase(TestCase):

    session = mock.MagicMock()

    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'start_betas']))
    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'get_can_be_allocated']))
    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'run_delete_slots']))
    def test_run_once_delete_expired_slots(self, run_delete_slots, *mocked):
        slot_manager = BaseSlotManager()
        slot_manager.run_once(self.session)
        run_delete_slots.assert_called()

    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'delete_slots']))
    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'get_expired_slots']))
    def test_delete_expired_slots(self, get_expired, delete_slots):
        expired_slots = [model.Slot(id='slot-1'), model.Slot(id='slot-2')]
        expected = [slot.to_protobuf() for slot in expired_slots]
        get_expired.return_value = list(expired_slots)
        slot_manager = BaseSlotManager()
        slot_manager.run_delete_slots(self.session)
        delete_slots.assert_called_with(expected)

    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'delete_slots']))
    def test_delete_expired_locked_allocator(self, delete_slots):
        slot_manager = BaseSlotManager()
        with mock.patch.object(slot_manager, 'allocator_lock') as mocked_lock:
            mocked_lock.__enter__.side_effect = Locked()
            slot_manager.run_once(self.session)
            delete_slots.assert_not_called()

    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'run_delete_slots']))
    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'start_betas']))
    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'get_can_be_allocated']))
    def test_run_once_start_betas(self, can_be_allocated, start_betas, *mocked):
        allocatable_betas = [Beta(name='beta-1'), Beta(name='beta-2')]
        can_be_allocated.return_value = list(allocatable_betas)
        slot_manager = BaseSlotManager()
        slot_manager.run_once(self.session)
        start_betas.assert_called_with(allocatable_betas, self.session)

    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'run_delete_slots']))
    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'start_betas']))
    @mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'get_can_be_allocated']))
    def test_run_once_start_betas_locked_allocator(self, can_be_allocated, start_betas, *mocked):
        allocatable_betas = [Beta(name='beta-1'), Beta(name='beta-2')]
        can_be_allocated.return_value = list(allocatable_betas)
        slot_manager = BaseSlotManager()
        with mock.patch.object(slot_manager, 'allocator_lock') as mocked_lock:
            mocked_lock.__enter__.side_effect = Locked()
            slot_manager.run_once(self.session)
            start_betas.assert_called_with(allocatable_betas, self.session)


class StartBetasTestCase(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.create_slot_patch = mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'create_slot']))
        cls.update_history_patch = mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, 'update_history']))
        cls.component_filter_patch = mock.patch('.'.join([SLOT_MANAGER_IMPORT_PATH, '_component_filter']))
        cls.slot_manager = BaseSlotManager()

    def setUp(self):
        self.create_slot = self.create_slot_patch.start()
        self.update_history = self.update_history_patch.start()
        self.component_filter = self.component_filter_patch.start()
        self.component_filter.return_value = True

    def tearDown(self):
        self.create_slot_patch.stop()
        self.update_history_patch.stop()
        self.component_filter_patch.stop()

    @property
    def beta(self):
        return to_model(
            Beta(
                name='beta',
                components=[BetaComponent(id=str(uuid.uuid4()))],
            )
        )

    def exception_with__str__(self, text):
        class MyException(Exception):
            def __str__(self):
                return text
        return MyException()

    def _get_history_error(self, c):
        try:
            return c.kwargs.get('error') or c.args[2]
        except IndexError:
            pass

    def get_history_errors(self):
        return [
            self._get_history_error(c)
            for c in self.update_history.call_args_list
        ]

    def test_start_betas_error_message_in_history(self):
        expected_error = 'some error'
        self.create_slot.side_effect = Exception(expected_error)
        self.slot_manager.start_betas([self.beta], session=None)
        error_passed = self.get_history_errors()[-1]
        self.assertRegexpMatches(error_passed, expected_error)

    def test_start_betas_max_retries_reached_last_error__str__in_history(self):
        """ Check that exceptions like those from ``martylib.http.exceptions`` will be properly mentioned in the history"""
        expected_error = 'some error'
        self.create_slot.side_effect = MaxRetriesReached(
            'retry error',
            [
                self.exception_with__str__('err1'),
                self.exception_with__str__('err2'),
                self.exception_with__str__(expected_error),
            ]
        )
        self.slot_manager.start_betas([self.beta], session=None)
        error_passed = self.get_history_errors()[-1]
        self.assertRegexpMatches(error_passed, expected_error)

    def test_broken_slots_limit_exceeded_not_produce_history(self):
        self.create_slot.side_effect = BrokenSlotsLimitExceeded()
        self.slot_manager.start_betas([self.beta], session=None)
        self.update_history.assert_not_called()

    def test_no_broken_slots_limit_exceeded_metric(self):
        self.create_slot.side_effect = BrokenSlotsLimitExceeded()
        self.slot_manager.start_betas([self.beta], session=None)
        metric_name = self.slot_manager.metrics.get_metric_name('traced-exceptions-BrokenSlotsLimitExceeded_summ')[1]
        self.assertNotIn(metric_name, self.slot_manager.metrics)
