# coding: utf-8


import unittest
from mak.utils.utils import get_nanny_secret


class TestUtilsUtils(unittest.TestCase):

    def test_get_nanny_secret(self):
        self.assertEqual(39, len(get_nanny_secret))
