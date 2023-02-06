import pytest

from pytest_factor.factortypes.storages import MegaDict


@pytest.fixture
def megadict():
    return MegaDict({'a': 'cdef',
                     'b': 'abcdefghijk',
                     'c': 5,
                     'd': 0,
                     'e': [1,
                           2,
                           {'a': 'a',
                            'b': 2,
                            'c': (1, 2, '3', '45', 6)},
                           [11, 12, 13, 14]]})


def test_access_existed_1(megadict):
    assert megadict['a'] == 'cdef'


def test_access_existed_2(megadict):
    assert megadict['a'][0] == 'c'


def test_access_not_existed_1(megadict):
    assert megadict['f'] == 0


def test_access_not_existed_2(megadict):
    assert megadict['a']['b'] == 0


def test_access_not_existed_3(megadict):
    assert megadict['a'][0]['c'] == 0


def test_str_in_str(megadict):
    assert megadict['a'] in megadict['b']


def test_int_in_int(megadict):
    with pytest.raises(TypeError):
        assert megadict['c'] in megadict['d']


def test_str_in_int(megadict):
    with pytest.raises(TypeError):
        assert megadict['a'] in megadict['d']


def test_int_in_str(megadict):
    with pytest.raises(TypeError):
        assert megadict['d'] in megadict['a']


def test_int_in_tuple(megadict):
    assert megadict['e'][1] in megadict['e'][2]['c']


def test_builtins_gt(megadict):
    assert megadict['c'] > megadict['e'][2]['b']


def test_builtins_lt(megadict):
    assert megadict['e'][3]['b'] < megadict['c']


def test_builtins_str(megadict):
    assert str(megadict['c']) == '5'


def test_builtins_eq(megadict):
    assert megadict['e'][2]['c'][2] == '3'


def test_builtins_int(megadict):
    assert int(megadict['e'][2]['c'][2]) == 3


def test_builtins_add(megadict):
    assert megadict['c'] == megadict['e'][2]['c'][1] + 3


def test_builtins_len_str(megadict):
    assert len(megadict['a']) == 4


def test_builtins_len_int(megadict):
    with pytest.raises(TypeError):
        assert len(megadict['c']) == 4


def test_builtins_len_list(megadict):
    assert len(megadict['e'][3]) == 4


def test_builtins_len_tuple(megadict):
    assert len(megadict['e'][2]['c']) == 5


def test_builtins_len_not_existed(megadict):
    with pytest.raises(TypeError):
        assert len(megadict['g']['c']) == 4


def test_builtins_isinstance(megadict):
    assert isinstance(megadict['d'], int)


def test_builtins_keys(megadict):
    assert sorted(megadict.keys()) == ['a', 'b', 'c', 'd', 'e']


def test_list_comprehension(megadict):
    assert sorted([i for i in megadict.values() if isinstance(i, int)]) == [0, 5]


def test_list_slice_1(megadict):
    assert megadict['b'][2:6] == megadict['a']


def test_list_slice_2(megadict):
    assert sorted(megadict['e'][3][:-2]) == [11, 12]


@pytest.mark.xfail
def test_none_create():
    assert MegaDict(None) is None


@pytest.mark.xfail
def test_none_access():
    assert MegaDict(None)[0] == 0

