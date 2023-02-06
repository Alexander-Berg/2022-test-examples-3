import pytest

from mail.ipa.ipa.utils.helpers import merge_lists


class TestMergeLists:
    @pytest.mark.parametrize('list1,list2,less,expected', (
        pytest.param(
            [],
            [],
            lambda x, y: 1 / 0,
            [],
            id='empty_lists'
        ),
        pytest.param(
            [],
            [1, 2, 3],
            lambda x, y: 1 / 0,
            [1, 2, 3],
            id='empty_list1'
        ),
        pytest.param(
            [1, 2, 3],
            [],
            lambda x, y: 1 / 0,
            [1, 2, 3],
            id='empty_list2'
        ),
        pytest.param(
            [1, 2, 5, 6, 7],
            [3, 4, 8, 9, 10, 11],
            lambda x, y: x < y,
            [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11],
            id='same_types'
        ),
        pytest.param(
            [0, 1, 2, 3, 4, 5],
            ['a', 'b', 'c', 'd', 'e'],
            lambda x, y: x < (ord(y) - ord('a')),
            ['a', 0, 'b', 1, 'c', 2, 'd', 3, 'e', 4, 5],
            id='different_types'
        ),
    ))
    def test_merges(self, list1, list2, less, expected):
        assert list(merge_lists(list1, list2, less)) == expected
