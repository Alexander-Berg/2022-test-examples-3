# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.protobuf_utils.patch import patch_enums
from search.martylib.test_utils import TestCase


class TestPatch(TestCase):
    def test_patch_enums(self):
        from search.martylib.proto.structures import test_pb2

        patch_enums()

        self.assertEqual(
            getattr(test_pb2.TopLevelEnum, 'NULL'),
            0,
        )
        self.assertEqual(
            getattr(test_pb2.Alpha.AlphaNestedEnum, 'N_NULL'),
            0,
        )

        # Make sure values from different enums aren't mixed.
        self.assertFalse(
            hasattr(test_pb2.TopLevelEnum, 'N_NULL')
        )
        self.assertFalse(
            hasattr(test_pb2.Alpha.AlphaNestedEnum, 'NULL')
        )
