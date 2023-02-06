# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.proto.structures import test_pb2
from search.martylib.protobuf_utils import mask
from search.martylib.test_utils import TestCase


class TestMask(TestCase):

    TEST_PATCH = test_pb2.TestDeepMerge(
        s_plain='lollol',
        b_plain=False,
        nested=test_pb2.TestDeepMerge(
            b_plain=True,
            s_plain='kekkek',
        )
    )

    TEST_RESULT = test_pb2.TestDeepMerge(
        s_plain='lollol',
        nested=test_pb2.TestDeepMerge(
            b_plain=True,
            s_plain='kekkek',
            nested=test_pb2.TestDeepMerge(
                s_plain='cheburek',
            )
        )
    )

    def setUp(self):
        self.TEST_OBJ = test_pb2.TestDeepMerge(
            s_plain='lol',
            b_plain=True,
            nested=test_pb2.TestDeepMerge(
                s_plain='kek',
                nested=test_pb2.TestDeepMerge(
                    s_plain='cheburek',
                )
            )
        )

    def test_get_field(self):
        self.assertEqual(mask.get_field(self.TEST_OBJ, 's_plain'), 'lol')
        self.assertEqual(mask.get_field(self.TEST_OBJ, 'nested.s_plain'), 'kek')
        self.assertFalse(mask.get_field(self.TEST_OBJ, 'nested.b_plain'))
        self.assertEqual(mask.get_field(self.TEST_OBJ, 'nested.nested.s_plain'), 'cheburek')

    def test_patch(self):
        field_mask = ['s_plain', 'b_plain', 'nested.b_plain', 'nested.s_plain']
        mask.patch(self.TEST_OBJ, self.TEST_PATCH, field_mask)

        self.assertEqual(self.TEST_OBJ, self.TEST_RESULT)

    def test_patch_repeated(self):
        # noinspection PyTypeChecker
        patch = test_pb2.TestDeepMerge(
            rm=[
                test_pb2.TestDeepMerge(s_plain='kek')
            ]
        )

        # noinspection PyTypeChecker
        result = test_pb2.TestDeepMerge(
            s_plain='lol',
            b_plain=True,
            nested=test_pb2.TestDeepMerge(
                s_plain='kek',
                nested=test_pb2.TestDeepMerge(
                    s_plain='cheburek',
                )
            ),
            rm=[
                test_pb2.TestDeepMerge(s_plain='kek')
            ]
        )

        mask.patch(self.TEST_OBJ, patch, ('rm',))
        self.assertEqual(self.TEST_OBJ, result)
