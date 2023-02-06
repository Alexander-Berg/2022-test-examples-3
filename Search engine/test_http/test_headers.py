# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest

from search.martylib.http.headers import Headers


class TestHeaders(unittest.TestCase):
    # noinspection SpellCheckingInspection
    def test_cookies(self):
        input_alpha = (
            ('Cookie', 'yp=1891360668.udn.cDpyb2Jvc2xvbmU%3D; yandexuid=706532361572961187'),
        )
        input_bravo = (
            ('Cookie', 'yp=1891360668.udn.cDpyb2Jvc2xvbmU%3D'),
            ('Cookie', 'yandexuid=706532361572961187'),
        )

        self.assertEqual(Headers(*input_alpha).cookies, Headers(*input_bravo).cookies)
