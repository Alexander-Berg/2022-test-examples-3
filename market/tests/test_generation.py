# -*- coding: utf-8 -*-

import unittest

from market.idx.snippets.src.generation import id_to_name, name_to_id, key_to_name, name_to_key


class TestGeneration(unittest.TestCase):
    def test(self):
        assert(id_to_name(1) == '1')
        assert(id_to_name(12) == '21')
        assert(id_to_name(123) == '32/1')
        assert(id_to_name(1234) == '43/21')
        assert(id_to_name(12345) == '54/321')

        assert(name_to_id('1') == 1)
        assert(name_to_id('21') == 12)
        assert(name_to_id('32/1') == 123)
        assert(name_to_id('43/21') == 1234)
        assert(name_to_id('54/321') == 12345)

        assert(key_to_name(12345, 'MARKET') == 'market/54/321')
        assert(name_to_key('market/54/321') == (12345, 'MARKET'))
