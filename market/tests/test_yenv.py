# -*- coding: utf-8 -*-

import unittest

import market.pylibrary.yenv as yenv


class Test(unittest.TestCase):
    def test(self):
        envtype = yenv.TESTING
        mitype = yenv.GIBSON

        self.assertFalse(yenv.is_development(envtype))
        self.assertTrue(yenv.is_testing(envtype))
        self.assertFalse(yenv.is_production(envtype))

        self.assertFalse(yenv.is_stratocaster(mitype))
        self.assertTrue(yenv.is_gibson(mitype))
        self.assertFalse(yenv.is_planeshift(mitype))


if __name__ == '__main__':
    unittest.main()
