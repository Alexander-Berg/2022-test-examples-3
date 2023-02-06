import pytest

from lib.blueprints.metabase import prepare_data, FIELD_DEFENITION_FIELD_NAME, FIELD_DEFENITION_ORDER, \
    get_many_from_dict


def test_prepare_data():
    assert prepare_data(
        [
            {'a': 1, 'b': 2, "c": None},
            {'b': 4, 'a': 3, "c": None},
        ],
        [
            {FIELD_DEFENITION_FIELD_NAME: 'a', FIELD_DEFENITION_ORDER: 0},
            {FIELD_DEFENITION_FIELD_NAME: 'b', FIELD_DEFENITION_ORDER: 1},
        ]
    ) == [
               [1, 2],
               [3, 4]
           ]


@pytest.mark.parametrize(
    "d,keys,expected",
    [
        (
                # d
                {'a': 1},
                # keys
                ['a'],
                # expected
                (1,)
        ),
        (
                # d
                {'a': 1, 'b': 2},
                # keys
                ['a', 'b'],
                # expected
                (1, 2)
        ),
        (
                # d
                {'a': 1, 'b': 2, 'c': 3},
                # keys
                ['a', 'b'],
                # expected
                (1, 2)
        ),
    ]
)
def test_get_many_from_dict(d, keys, expected):
    assert expected == get_many_from_dict(d, keys)
