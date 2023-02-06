#!/usr/bin/env python
# -*- coding: utf-8 -*-
import struct
import unittest

import numpy as np

from crypta.profile.lib import vector_helpers


class TestUtils(unittest.TestCase):
    def test_binary_to_numpy(self):
        self.assertTrue(np.array_equal(
            vector_helpers.binary_to_numpy(b''),
            np.empty(0, dtype=np.float32)
        ))

        self.assertTrue(np.array_equal(
            vector_helpers.binary_to_numpy(b'\xdb\x0fI@T\xf8-@'),
            np.array([np.pi, np.exp(1)], dtype='float32')
        ))

    def test_binary_to_numpy_struct_compatibility(self):
        array = [-1.3, 42, -100, 1e21]
        self.assertTrue(np.array_equal(
            vector_helpers.binary_to_numpy(struct.pack('f' * len(array), *array)),
            np.array(array, dtype='float32')
        ))

    def test_normalize_function(self):
        self.assertTrue(np.allclose(
            vector_helpers.normalize(np.arange(-2, 2)),
            np.array([-0.81649658, -0.40824829, 0., 0.40824829])
        ))

    def test_vector_to_features_conversion(self):
        vector = b'\x00\x00\x00?\x00\x00\x00?'  # [0.5, 0.5]

        self.assertTrue(np.allclose(
            vector_helpers.vector_to_features(vector),
            np.array([0.70710677, 0.70710677], dtype=np.float32)
        ))
