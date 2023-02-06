# -*- coding: utf-8 -*-

import unittest
import market.pylibrary.shopsdat as shopsdat


class TestExtractingAuthorization(unittest.TestCase):
    def test_none(self):
        self.assertEqual(shopsdat.extract_authorization(None), None)

    def test_empty(self):
        self.assertEqual(shopsdat.extract_authorization(''), None)

    def test_something(self):
        self.assertEqual(shopsdat.extract_authorization('bv:zx:xc:cv'), None)

    def test_1(self):
        user, password = shopsdat.extract_authorization("HTTP_AUTH='basic:*:user:password'")
        self.assertEqual('user', user)
        self.assertEqual('password', password)

    def test_2(self):
        user, password = shopsdat.extract_authorization("HTTP_AUTH='basic:*:biletskii87@mail.ru:110859'")
        self.assertEqual('biletskii87@mail.ru', user)
        self.assertEqual('110859', password)


if '__main__' == __name__:
    unittest.main()
