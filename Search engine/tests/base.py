# coding: utf-8

import unittest


class BaseTestCase(unittest.TestCase):
    def assertNotEmptyDict(self, _dict):
        self.assertIsInstance(_dict, dict)
        self.assertNotEqual(_dict, {})

    def assertNotEmptyList(self, _list):
        self.assertIsInstance(_list, list)
        self.assertNotEqual(_list, [])


class BaseApiTestCase(BaseTestCase):
    def setUp(self):
        super(BaseApiTestCase, self).setUp()
        self.api.start()

    def tearDown(self):
        super(BaseApiTestCase, self).tearDown()
        self.api.stop()
