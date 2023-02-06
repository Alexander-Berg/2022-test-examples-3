# coding=utf-8
import unittest

from vw_throw_rare_tokens_from_input import load_dict
from vw_throw_rare_tokens_from_input import throw_rare_tokens


class VWThrowRareTokens(unittest.TestCase):
    def test_load_dict(self):
        dict = load_dict("test_data/dict.txt")
        self.assertTrue("фараон" in dict)
        self.assertTrue("скад" in dict)
        self.assertTrue("jack" in dict)
        self.assertTrue("c." in dict)

    def test_throw_rare_tokens(self):
        dict = load_dict("test_data/dict.txt")

        feature_line = "1 1 offer_key_77686845c52f9b10b989aaf41cd0dc9f;15561854|offer jack c. bold |category_id 15561854"
        new_features_line = throw_rare_tokens(feature_line, dict)
        self.assertEqual("1 1 offer_key_77686845c52f9b10b989aaf41cd0dc9f;15561854|offer jack c. |category_id 15561854",
                         new_features_line)
