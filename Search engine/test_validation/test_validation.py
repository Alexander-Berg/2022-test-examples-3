# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest
import uuid

from search.martylib.validation import check_uuid


class TestCheckUUID(unittest.TestCase):
    def test_check_id(self):
        t1 = str(uuid.uuid4())
        t2 = "456"
        check_uuid(t1)
        with self.assertRaises(ValueError):
            check_uuid(t2)
