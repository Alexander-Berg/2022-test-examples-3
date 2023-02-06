import pytest
# import typing

import extsearch.ymusic.quality.learning.lib.extensions as extensions


@pytest.fixture()
def factors_merger():
    merger = extensions.FactorsMerger()
    yield merger


@pytest.fixture()
def factors_extender():
    extender = extensions.FactorsExtender()
    yield extender


def test__factors_merger__simple(factors_merger):
    current_factors = {
        'a': 1,
        'b': 1,
        'c': 0,
        'd': 1,
    }
    new_factors = {
        'a': 0,
        'e': 1,
        'b': 0.5,
    }

    updated_factors = factors_merger.merge_factors(current_factors, new_factors)
    assert set(updated_factors.keys()) == {'a', 'b', 'c', 'd', 'e'}
    assert updated_factors['a'] == 0
    assert updated_factors['b'] == 0.5
    assert updated_factors['c'] == 0
    assert updated_factors['d'] == 1
    assert updated_factors['e'] == 1


def test__factors_merger__with_unused(factors_merger):
    current_factors = {
        'a': 0,
        'b': 1,
        'unused1': 2,
    }
    new_factors = {
        'b': 0,
        'c': 1,
    }

    updated_factors = factors_merger.merge_factors(current_factors, new_factors)
    assert set(updated_factors.keys()) == {'a', 'b', 'c'}
    assert updated_factors['a'] == 0
    assert updated_factors['b'] == 0
    assert updated_factors['c'] == 1


def test__factors_extender__base(factors_extender):
    key, recs = _construct_extender_input(
        {'query': 'q', 'url': 'foo'},
        [
            {'@table_index': 0, 'f1': 5, 'f2': 2},
            {'@table_index': 1, 'factors': {'f0': 3, 'f3': 7, 'f4': 8}},
        ])
    res = list(factors_extender(key, recs))
    assert len(res) == 1
    assert res[0]['factors'] == {
        'f0': 3,
        'f1': 5,
        'f2': 2,
        'f3': 7,
        'f4': 8,
    }


def test__factors_extender__multiple_sources(factors_extender):
    key, recs = _construct_extender_input(
        {'query': 'q', 'url': 'foo'},
        [
            {'@table_index': 0, 'f1': 5, 'f2': 2},
            {'@table_index': 0, 'f5': 0},
            {'@table_index': 1, 'factors': {'f0': 3, 'f3': 7, 'f4': 8}},
        ])
    res = list(factors_extender(key, recs))
    assert len(res) == 1
    assert res[0]['factors'] == {
        'f0': 3,
        'f1': 5,
        'f2': 2,
        'f3': 7,
        'f4': 8,
        'f5': 0,
    }


def test__factors_extender__multiple_destinations(factors_extender):
    key, recs = _construct_extender_input(
        {'query': 'q', 'url': 'foo'},
        [
            {'@table_index': 0, 'f1': 5, 'f2': 2},
            {'@table_index': 1, 'factors': {'f0': 3, 'f3': 7, 'f4': 8}},
            {'@table_index': 1, 'factors': {'f0': 1, 'f3': 6, 'f4': 5}},
        ])
    res = list(factors_extender(key, recs))
    assert len(res) == 2
    assert res[0]['factors'] == {
        'f0': 3,
        'f1': 5,
        'f2': 2,
        'f3': 7,
        'f4': 8,
    }
    assert res[1]['factors'] == {
        'f0': 1,
        'f1': 5,
        'f2': 2,
        'f3': 6,
        'f4': 5,
    }


def test__factors_extender__dict_sources(factors_extender):
    key, recs = _construct_extender_input(
        {'query': 'q', 'url': 'foo'},
        [
            {'@table_index': 0, 'dict_column': {'f1': 5, 'f2': 2}},
            {'@table_index': 1, 'factors': {'f0': 3, 'f3': 7, 'f4': 8}},
        ])
    res = list(factors_extender(key, recs))
    assert len(res) == 1
    assert res[0]['factors'] == {
        'f0': 3,
        'f1': 5,
        'f2': 2,
        'f3': 7,
        'f4': 8,
    }


def test__factors_extender__junk_columns(factors_extender):
    key, recs = _construct_extender_input(
        {'query': 'q', 'url': 'foo'},
        [
            {'@table_index': 0, 'junk': 'non-expected value'},
            {'@table_index': 1, 'factors': {'f0': 3, 'f3': 7, 'f4': 8}}
        ]
    )
    with pytest.raises(extensions.FactorsExtenderError):
        list(factors_extender(key, recs))  # list is required because extender returns a generator


def _construct_extender_input(key: dict, recs_without_key: list[dict]):
    return key, (rec | key for rec in recs_without_key)
