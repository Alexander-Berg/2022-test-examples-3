# coding: utf-8

from market.idx.pylibrary.murmur.hash2 import hasher


class TestHasher(object):
    def test_english(self):
        test_string = 'best_test_string'
        test_hasher = hasher()
        result = test_hasher(test_string)

        assert result == 13698503399341767646

    def test_other(self):
        test_string = 'строка'
        test_hasher = hasher()
        result = test_hasher(test_string)

        assert result == 17067593983527298194
