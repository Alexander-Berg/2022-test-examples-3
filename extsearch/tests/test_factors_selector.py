import pytest

import extsearch.ymusic.lib.python.saas_dm.relev_conf as rconf
import extsearch.ymusic.quality.learning.lib.factors_selector as factors_selector


_relev_conf = {
    'static_factors': {
        'f1': {
            'index': 1,
        },
        'f2': {
            'index': 2,
            'tags': ['deprecated'],
        },
        'f3': {
            'index': 3,
            'tags': ['unimplemented'],
        },
    },
    'dynamic_factors': {
        'f4': {
            'index': 4,
            'tags': ['foo'],
        },
    },
}


@pytest.fixture(scope='function')
def relev_conf():
    yield rconf.RelevConf.parse_conf(_relev_conf)


@pytest.mark.parametrize("indexes,expected_range", [
    ([], ''),
    ([1, 2, 3, 4, 5], '1-5'),
    ([1, 2, 3, 5, 8], '1-3,5,8'),
    ([1, 5, 6, 7, 10, 11, 12], '1,5-7,10-12'),
])
def test__create_factors_range__parametrized(indexes, expected_range):
    actual_range = factors_selector.create_factors_range(indexes)
    assert expected_range == actual_range, f'Ranges differ for indexes: {indexes}'


def test__select_factors__none_tags(relev_conf):
    assert '' == factors_selector.select_factors(relev_conf, include_tags=None, exclude_tags=None, inverted=False)


def test__select_factors__empty_tags(relev_conf):
    assert '' == factors_selector.select_factors(relev_conf, include_tags=[], exclude_tags=[], inverted=False)


@pytest.mark.parametrize('include_tags,exclude_tags,expected_range', [
    (['static_factors'], [], '1-3'),
    (['dynamic_factors'], [], '4'),
    (['all'], [], '0-4'),
    (['static_factors', 'foo'], [], '1-4'),
    (['dynamic_factors', 'deprecated'], [], '2,4'),
    (['static_factors'], ['deprecated'], '1,3'),
    (['static_factors', 'dynamic_factors'], ['deprecated', 'unimplemented'], '1,4'),
    (['all'], ['foo', 'deprecated'], '0-1,3')
])
def test__select_factors__non_empty_tags(relev_conf, include_tags, exclude_tags, expected_range):
    actual_range = factors_selector.select_factors(
        relev_conf,
        include_tags=include_tags,
        exclude_tags=exclude_tags,
        inverted=False)
    assert expected_range == actual_range, f'Ranges differ. Include tags: {include_tags}. Exclude tags: {exclude_tags}.'


def test__select_factors__inverted(relev_conf):
    actual_range = factors_selector.select_factors(
        relev_conf,
        include_tags=['static_factors'],
        exclude_tags=['unimplemented'],
        inverted=True)
    assert '0,3-4' == actual_range
