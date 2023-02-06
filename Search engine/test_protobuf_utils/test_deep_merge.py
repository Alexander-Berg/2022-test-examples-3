# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import collections

from google.protobuf import wrappers_pb2

from search.martylib.proto.structures import test_pb2

from search.martylib.protobuf_utils.deep_merge import deep_merge
from search.martylib.test_utils import TestCase


TestInput = collections.namedtuple('TestInput', ('left', 'right', 'merge_from_result', 'deep_merge_result'))


class TestDeepMerge(TestCase):
    @classmethod
    def setUpClass(cls):
        cls.A = test_pb2.TestDeepMerge()
        cls.B = test_pb2.TestDeepMerge()
        cls.C = test_pb2.TestDeepMerge()
        cls.D = test_pb2.TestDeepMerge()
        cls.E = test_pb2.TestDeepMerge()
        cls.F = test_pb2.TestDeepMerge()
        cls.G = test_pb2.TestDeepMerge()
        cls.H = test_pb2.TestDeepMerge()
        cls.I = test_pb2.TestDeepMerge()
        cls.J = test_pb2.TestDeepMerge()
        cls.K = test_pb2.TestDeepMerge()
        cls.M = test_pb2.TestDeepMerge()
        cls.L = test_pb2.TestDeepMerge()
        cls.N = test_pb2.TestDeepMerge()

        cls.A.s_wrapper.value = ''
        cls.A.b_wrapper.value = False
        cls.A.s_plain = 'a'

        cls.B.s_wrapper.value = 'b'
        cls.B.b_wrapper.value = True
        cls.B.s_plain = 'b'

        # Nested merge.
        cls.D.b_wrapper.value = True
        cls.D.nested.b_wrapper.value = True
        cls.D.nested.nested.b_wrapper.value = True
        cls.D.nested.nested.nested.b_wrapper.value = True
        cls.D.nested.m_nested['foo'].CopyFrom(test_pb2.TestDeepMerge())
        cls.D.nested.m_nested['foo'].s_wrapper.value = 'foo_d'
        cls.D.nested.m_nested['foo'].b_wrapper.value = True
        cls.D.nested.m_nested['foo'].rw.extend((wrappers_pb2.StringValue(value='foo_d'), ))
        cls.D.nested.m_nested['foo'].rm.extend((test_pb2.TestDeepMerge(), ))
        cls.D.nested.m_nested['foo'].rm[0].s_wrapper.value = 'foo_d'

        cls.E.b_wrapper.value = False
        cls.E.nested.b_wrapper.value = False
        cls.E.nested.nested.b_wrapper.value = False
        cls.E.nested.nested.nested.b_wrapper.value = False
        cls.E.nested.m_nested['foo'].CopyFrom(test_pb2.TestDeepMerge())
        cls.E.nested.m_nested['foo'].s_wrapper.value = 'foo_e'
        cls.E.nested.m_nested['foo'].b_wrapper.value = False
        cls.E.nested.m_nested['foo'].rw.extend((wrappers_pb2.StringValue(value='foo_e'), ))
        cls.E.nested.m_nested['foo'].rm.extend((test_pb2.TestDeepMerge(),))
        cls.E.nested.m_nested['foo'].rm[0].s_wrapper.value = 'foo_e'

        # Repeated plain.
        cls.F.rs.append('f')
        cls.K.rs.append('k')
        cls.F.rw.extend((wrappers_pb2.StringValue(value='fw'), ))
        cls.K.rw.extend((wrappers_pb2.StringValue(value='kw'), ))

        # Repeated message.
        cls.G.rm.extend((test_pb2.TestDeepMerge(), ))
        cls.G.rm[0].CopyFrom(cls.F)
        cls.H.rm.extend((test_pb2.TestDeepMerge(), ))
        cls.H.rm[0].CopyFrom(cls.K)

        # Maps.
        cls.I.m_nested['foo'].CopyFrom(cls.D.nested.m_nested['foo'])
        cls.I.m_nested['bar'].CopyFrom(cls.D.nested.m_nested['foo'])
        cls.I.m_nested['bar'].s_wrapper.value = 'foo_i'
        cls.I.m_nested['bar'].rw[0].value = 'foo_i'
        cls.I.m_nested['bar'].rm[0].s_wrapper.value = 'foo_i'

        cls.I.m_wrapper['foo_w'].CopyFrom(wrappers_pb2.StringValue(value='foo_wi'))
        cls.I.m_wrapper['bar_w'].CopyFrom(wrappers_pb2.StringValue(value='bar_wi'))

        cls.J.m_nested['foo'].CopyFrom(cls.E.nested.m_nested['foo'])
        cls.J.m_nested['baz'].CopyFrom(cls.E.nested.m_nested['foo'])
        cls.J.m_nested['baz'].s_wrapper.value = 'foo_j'
        cls.J.m_nested['baz'].rw[0].value = 'foo_j'
        cls.J.m_nested['baz'].rm[0].s_wrapper.value = 'foo_j'

        cls.J.m_wrapper['foo_w'].CopyFrom(wrappers_pb2.StringValue(value='foo_wj'))
        cls.J.m_wrapper['baz_w'].CopyFrom(wrappers_pb2.StringValue(value='baz_wj'))

        cls.M.enum_field = test_pb2.TestDeepMergeEnum.DEFAULT
        cls.L.enum_field = test_pb2.TestDeepMergeEnum.NOT_DEFAULT

    def test_plain(self):
        inputs = (
            TestInput(left=self.A, right=self.B, merge_from_result=self.B, deep_merge_result=self.B),
            TestInput(left=self.B, right=self.A, merge_from_result=self.B, deep_merge_result=self.A),
            TestInput(left=self.A, right=self.C, merge_from_result=self.A, deep_merge_result=self.A),
            TestInput(left=self.C, right=self.A, merge_from_result=self.A, deep_merge_result=self.A),
            TestInput(left=self.B, right=self.C, merge_from_result=self.B, deep_merge_result=self.B),
            TestInput(left=self.C, right=self.B, merge_from_result=self.B, deep_merge_result=self.B),
        )

        for index, i in enumerate(inputs):
            merge_from_result = test_pb2.TestDeepMerge()
            merge_from_result.CopyFrom(i.left)
            merge_from_result.MergeFrom(i.right)

            deep_merge_result = deep_merge(i.left, i.right)

            self.assertEqual(merge_from_result, i.merge_from_result, msg='MergeFrom yielded unexpected result in case #{}'.format(index))
            self.assertEqual(deep_merge_result, i.deep_merge_result, msg='deep_merge yielded unexpected result in case #{}:\n{}'.format(index, deep_merge_result))

    def test_nested(self):
        inputs = (
            TestInput(left=self.D, right=self.E, deep_merge_result=self.E, merge_from_result=None),
            TestInput(left=self.E, right=self.D, deep_merge_result=self.D, merge_from_result=None),
        )
        for index, i in enumerate(inputs):
            deep_merge_result = deep_merge(i.left, i.right)
            self.assertEqual(deep_merge_result, i.deep_merge_result, msg='deep_merge yielded unexpected result in case #{}'.format(index))

    def test_repeated(self):
        inputs = (
            TestInput(left=self.F, right=self.K, deep_merge_result=self.K, merge_from_result=None),
            TestInput(left=self.K, right=self.F, deep_merge_result=self.F, merge_from_result=None),
        )
        for index, i in enumerate(inputs):
            deep_merge_result = deep_merge(i.left, i.right)
            self.assertEqual(deep_merge_result, i.deep_merge_result, msg='deep_merge yielded unexpected result in case #{}'.format(index))

    def test_repeated_message(self):
        inputs = (
            TestInput(left=self.G, right=self.H, deep_merge_result=self.H, merge_from_result=None),
            TestInput(left=self.H, right=self.G, deep_merge_result=self.G, merge_from_result=None),
        )
        for index, i in enumerate(inputs):
            deep_merge_result = deep_merge(i.left, i.right)
            self.assertEqual(deep_merge_result, i.deep_merge_result, msg='deep_merge yielded unexpected result in case #{}'.format(index))

    def test_map(self):
        result = deep_merge(self.I, self.J, inplace=False)

        self.assertEqual(len(result.m_nested), 3)

        self.assertEqual(result.m_nested['foo'].s_wrapper.value, 'foo_e')
        self.assertEqual(result.m_nested['bar'].s_wrapper.value, 'foo_i')
        self.assertEqual(result.m_nested['bar'].rw[0].value, 'foo_i')
        self.assertEqual(result.m_nested['bar'].rm[0].s_wrapper.value, 'foo_i')
        self.assertEqual(result.m_nested['baz'].s_wrapper.value, 'foo_j')
        self.assertEqual(result.m_nested['baz'].rw[0].value, 'foo_j')
        self.assertEqual(result.m_nested['baz'].rm[0].s_wrapper.value, 'foo_j')

        self.assertEqual(len(result.m_wrapper), 3)

        self.assertEqual(result.m_wrapper['foo_w'].value, 'foo_wj')
        self.assertEqual(result.m_wrapper['bar_w'].value, 'bar_wi')
        self.assertEqual(result.m_wrapper['baz_w'].value, 'baz_wj')

        result = deep_merge(self.J, self.I, inplace=False)

        self.assertEqual(len(result.m_nested), 3)

        self.assertEqual(result.m_nested['foo'].s_wrapper.value, 'foo_d')
        self.assertEqual(result.m_nested['bar'].s_wrapper.value, 'foo_i')
        self.assertEqual(result.m_nested['bar'].rw[0].value, 'foo_i')
        self.assertEqual(result.m_nested['bar'].rm[0].s_wrapper.value, 'foo_i')
        self.assertEqual(result.m_nested['baz'].s_wrapper.value, 'foo_j')
        self.assertEqual(result.m_nested['baz'].rw[0].value, 'foo_j')
        self.assertEqual(result.m_nested['baz'].rm[0].s_wrapper.value, 'foo_j')

        self.assertEqual(len(result.m_wrapper), 3)

        self.assertEqual(result.m_wrapper['foo_w'].value, 'foo_wi')
        self.assertEqual(result.m_wrapper['bar_w'].value, 'bar_wi')
        self.assertEqual(result.m_wrapper['baz_w'].value, 'baz_wj')

    def test_enum(self):
        inputs = (
            TestInput(left=self.M, right=self.L, deep_merge_result=self.L, merge_from_result=None),
            TestInput(left=self.L, right=self.M, deep_merge_result=self.L, merge_from_result=None),
            TestInput(left=self.M, right=self.N, deep_merge_result=self.M, merge_from_result=None),
            TestInput(left=self.L, right=self.N, deep_merge_result=self.L, merge_from_result=None),
        )
        for index, i in enumerate(inputs):
            deep_merge_result = deep_merge(i.left, i.right)
            self.assertEqual(deep_merge_result, i.deep_merge_result, msg='deep_merge yielded unexpected result in case #{}'.format(index))
