import pytest

import extsearch.ymusic.lib.python.saas_dm.relev_conf as rc

RELEV_CONF = {
    "static_factors": {
        "sf0": {"index": 0}
    },
    "dynamic_factors": {
        "df0": {"index": 20, "default": 0}
    },
    "zone_factors": {
        "zf0": 30
    },
    "user_factors": {
        "uf0": 40
    }
}


@pytest.fixture
def relev_conf():
    relev_conf = rc.RelevConf.parse_conf(RELEV_CONF)
    yield relev_conf


def test__parse_conf(relev_conf):
    assert relev_conf.max_idx == 40
    assert_factor_name_and_index(relev_conf.sorted_items[0], 'sf0', 0)
    assert_factor_name_and_index(relev_conf.sorted_items[20], 'df0', 20)
    assert_factor_name_and_index(relev_conf.sorted_items[30], 'zf0', 30)
    assert_factor_name_and_index(relev_conf.sorted_items[40], 'uf0', 40)
    for i in range(41):
        if i not in {0, 20, 30, 40}:
            assert relev_conf.sorted_items[i][0].startswith('unused')
            assert relev_conf.sorted_items[i][1].index == i


def assert_factor_name_and_index(item, factor_name, index):
    assert item[0] == factor_name
    assert item[1].index == index


def test__convert_to_dict(relev_conf):
    factors_dict = relev_conf.convert_to_dict([1, 2, 3, 4])
    assert len(factors_dict) == 41
    assert factors_dict.pop('sf0') == 1
    assert factors_dict.pop('df0') == 2
    assert factors_dict.pop('zf0') == 3
    assert factors_dict.pop('uf0') == 4
    for v in factors_dict.values():
        assert v == 0.0


def test__convert_to_dict__old_data(relev_conf):
    factors_dict = relev_conf.convert_to_dict([1, 2, 3])
    assert len(factors_dict) == 41
    assert factors_dict.pop('sf0') == 1
    assert factors_dict.pop('df0') == 2
    assert factors_dict.pop('zf0') == 3
    assert factors_dict.pop('uf0') == 0.0
    for v in factors_dict.values():
        assert v == 0.0


def test__convert_to_list(relev_conf):
    factors_list = relev_conf.convert_to_list({
        'sf0': 1,
        'df0': 2,
        'zf0': 3,
        'uf0': 4,
    })
    assert len(factors_list) == 41
    assert factors_list[0] == 1
    assert factors_list[20] == 2
    assert factors_list[30] == 3
    assert factors_list[40] == 4


def test__convert_to_list__user_factor():
    relev_conf = rc.RelevConf.parse_conf({
        'user_factors': {
            'DssmMusicLogDwellTimeBigramsMainTitle': {'index': 1, 'default_value': 0.0},
        },
    })
    factors_list = relev_conf.convert_to_list({'DssmMusicLogDwellTimeBigramsMainTitle': 0.4444})
    assert factors_list[1] == 0.4444


def test__convert_to_list__with_extra_unused(relev_conf):
    factors_list = relev_conf.convert_to_list({
        'sf0': 1,
        'df0': 2,
        'zf0': 3,
        'uf0': 4,
        'unused100500': 42,
    })
    assert len(factors_list) == 41
    for f in factors_list:
        assert f != 42


def test__add_static_factors__replace_unused():
    relev_conf = rc.RelevConf.parse_conf({
        'static_factors': {
            'f1': 1
        }
    })
    print(relev_conf.factors_info)
    assert relev_conf.factors_info['unused1'].index == 0
    relev_conf.add_static_factors(['f0'])
    assert relev_conf.factors_info['f0'].index == 0
    assert 'unused1' not in relev_conf.factors_info
    static_factors = relev_conf.get_section('static_factors')
    assert 'f0' in static_factors


def test__add_zone(relev_conf):
    relev_conf = rc.RelevConf.parse_conf({})
    relev_conf.add_zones(['z_some_zone'])

    zone_factors = relev_conf.get_section('zone_factors')
    assert 16 == len(zone_factors)
    for factor in zone_factors:
        assert 'z_some_zone' in factor
