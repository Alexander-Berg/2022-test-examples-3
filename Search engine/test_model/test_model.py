# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.db_utils import to_model

from search.priemka.yappy.proto.structures.quota_pb2 import Quota
from search.priemka.yappy.proto.structures.slot_pb2 import Slot
from search.priemka.yappy.src.model.model_service.service import Model

from search.priemka.yappy.tests.utils.test_cases import TestCase


class ModelTestCase(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.model = Model()

    def test_convert_to_shallow_quota_slot_hashring(self):
        expected = 'hashring-id'

        quota = Quota()
        slot = Slot()
        slot.hashring_id = expected

        quota.slots.add()
        quota.slots[-1].CopyFrom(slot)
        shallow_quota = self.model.convert_to_shallow_quota(to_model(quota))

        self.assertEqual(shallow_quota.slots[0].hashring_id, expected)
