# -*- coding: utf-8 -*-

import unittest
import itertools
from mpfs.common.util.slice_tools import SequenceChain, offset_limit_to_slice, slice_to_offset_limit


class SequenceChainTestCase(unittest.TestCase):
    test_sequences = [
        [],
        [[]],
        [range(5)],
        [[], []],
        [range(5), []],
        [[], range(5)],
        [range(5), range(5)],
    ]

    def _make_single(self, *sequences):
        return list(itertools.chain(*sequences))

    def _generate_chained_and_normal_sequences(self):
        for s in self.test_sequences:
            chained_seq = SequenceChain(*s)
            normal_seq = self._make_single(*s)
            yield chained_seq, normal_seq

    def test_len(self):
        for chained_seq, normal_seq in self._generate_chained_and_normal_sequences():
            assert len(chained_seq) == len(normal_seq)

    def test_slice(self):
        for chained_seq, normal_seq in self._generate_chained_and_normal_sequences():
            assert chained_seq[2:8] == normal_seq[2:8]

    def test_int_index(self):
        for chained_seq, normal_seq in self._generate_chained_and_normal_sequences():
            try:
                result = normal_seq[2]
            except IndexError:
                with self.assertRaises(IndexError):
                    chained_seq[2]
            else:
                assert result == chained_seq[2]

    def test_raise_on_negative_indices_or_step(self):
        chained_sequences = SequenceChain([])
        self.assertRaises(ValueError, lambda: chained_sequences[-1:])
        self.assertRaises(ValueError, lambda: chained_sequences[:-1])
        self.assertRaises(ValueError, lambda: chained_sequences[-1:-1])
        self.assertRaises(ValueError, lambda: chained_sequences[::2])
        self.assertRaises(ValueError, lambda: chained_sequences[-1])

    def _product_sequences(self, num, size):
        num += 1
        size += 1
        yield []
        for cur_num in xrange(num):
            items = [range(size) for _ in xrange(cur_num)]
            for sequences_conf in itertools.product(*items):
                yield [range(c) for c in sequences_conf]

    def test_all_slices(self):
        """Генерим все возможные варианты рядов/срезов и сравниваем со стандартным механизмом"""
        max_seq_size = 3
        max_seq_num = 3
        max_len = max_seq_size * max_seq_num
        for offset, limit in itertools.product([None] + range(max_len), [None] + range(max_len)):
            slice_obj = offset_limit_to_slice(offset, limit)
            for s in self._product_sequences(max_seq_size, max_seq_size):
                normal_list = self._make_single(*s)
                chained_sequences = SequenceChain(*s)
                assert chained_sequences[slice_obj] == normal_list[slice_obj], "Sequences: %s, %s" % (s, slice_obj)


class SliceToolsTestCase(unittest.TestCase):
    def test_slice_converters(self):
        for offset, limit in itertools.product(range(5), range(5)):
            assert (offset, limit) == slice_to_offset_limit(offset_limit_to_slice(offset, limit))
        assert (0, None) == slice_to_offset_limit(offset_limit_to_slice(None, None))
