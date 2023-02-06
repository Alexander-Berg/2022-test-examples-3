# coding=utf-8
import unittest

from vw_build_dict import get_tokens
from vw_build_dict import load_tokens_dict


class VWBuildDict(unittest.TestCase):
    def test_get_tokens(self):
        feature_line = "1 1 offer_key_77686845c52f9b10b989aaf41cd0dc9f;15561854|offer jack c. |category_id 15561854"
        tokens = get_tokens(feature_line)
        self.assertEqual(2, len(tokens))
        self.assertEqual("jack", tokens[0])
        self.assertEqual("c.", tokens[1])

    def test_load_tokens_dict(self):
        tokens_dict = load_tokens_dict("test_data/dummy_vw_features.txt")
        self.assertEqual(5, len(tokens_dict))
        self.assertTrue("jack" in tokens_dict)
        self.assertTrue("c." in tokens_dict)
        self.assertTrue("скад" in tokens_dict)
        self.assertTrue("фараон" in tokens_dict)
        self.assertTrue("bold" in tokens_dict)
        self.assertTrue(2, tokens_dict["jack"])
        self.assertTrue(1, tokens_dict["фараон"])
