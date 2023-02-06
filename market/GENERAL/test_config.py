import pytest
from config_manager import Boost, BoostConfig
import main as config_main
import ujson


def deepcopy(jsonf):
    return ujson.loads(ujson.dumps(jsonf))


class TestBoost:
    @pytest.mark.parametrize('json,', [
        {'name': 'B1', 'type': 'brand_boost', 'hids': [123]},
        {'name': 'B1', 'type': 'brand_boost', 'hids': [123], 'args': {}},
    ])
    def test_manager_boost_init(self, json):
        Boost(json, is_manager=True)

    @pytest.mark.parametrize('json,', [
        {'type': 'brand_boost', 'hids': [123]},
        {'name': 'B1', 'hids': [123]},
        {'name': 'B1', 'type': 'brand_boost', 'hids': [123], 'args': {'@type': 'someprotobuf'}},
    ])
    def test_manager_boost_init_fail(self, json):
        with pytest.raises(AssertionError):
            Boost(json, is_manager=True)


@pytest.fixture
def type_map():
    return {
        "brand_boost": "type.googleapis.com/MarketSearch.Booster.TBrandBoostArgs",
        "business_boost": "type.googleapis.com/MarketSearch.Booster.TBusinessBoostArgs",
        "shop_boost": "type.googleapis.com/MarketSearch.Booster.TShopBoostArgs",
        "gl_filter_boost": "type.googleapis.com/MarketSearch.Booster.TGlFiltersBoostArgs",
        "hardcoded_gl_filter_boost": "type.googleapis.com/MarketSearch.Booster.THardcodedGlFilterArgs",
        "personal_gl_filter_boost": "type.googleapis.com/MarketSearch.Booster.TMultiBoostArgs",
        "personal_brand_boost": "type.googleapis.com/MarketSearch.Booster.TMultiBoostArgs"
    }


class JsonFactory:
    def __init__(self, json):
        self._data = json

    def __call__(self):
        return deepcopy(self._data)


@pytest.fixture
def brand_boost_1_json():
    return JsonFactory({'name': 'B1', 'type': 'brand_boost', 'hids': [123], 'args': {}})


@pytest.fixture
def brand_boost_1(brand_boost_1_json):
    return lambda: Boost(brand_boost_1_json())


@pytest.fixture
def brand_boost_2_json():
    return JsonFactory({'name': 'B2', 'type': 'brand_boost', 'hids': [345]})


@pytest.fixture
def brand_boost_2(brand_boost_2_json):
    return lambda: Boost(brand_boost_2_json())


def make_config(*boost_jsons):
    return {'boosts': list(boost_jsons)}


@pytest.fixture()
def manager_boost_config(brand_boost_1_json, brand_boost_2_json):
    data = make_config(brand_boost_1_json(), brand_boost_2_json())
    return lambda: BoostConfig(deepcopy(data), is_manager=True)


@pytest.fixture
def production_boost_config(brand_boost_1_json, brand_boost_2_json, type_map):
    data = make_config()
    for boost_json in (brand_boost_1_json(), brand_boost_2_json()):
        boost_json = boost_json
        args = boost_json.setdefault(Boost.Field.ARGS, {})
        args[Boost.Field.REPORT_TYPE] = type_map[boost_json[Boost.Field.TYPE]]
        base_coeffs = boost_json.setdefault(Boost.Field.BASE_COEFFS, {})
        base_coeffs['text'] = 1.1
        base_coeffs['textless'] = 1.2
        data['boosts'].append(boost_json)

    return lambda: BoostConfig(deepcopy(data), is_manager=False)


@pytest.fixture
def business_boost_1():
    return lambda: Boost({'name': 'business1', 'type': 'business_boost', 'hids': [345]}, is_manager=True)


class TestBoostConfig:
    def test_config_contains(self, manager_boost_config, brand_boost_1):
        assert brand_boost_1() in manager_boost_config()

    def test_config_not_contains(self, manager_boost_config, business_boost_1):
        assert business_boost_1() not in manager_boost_config()

    def test_main_get_new_boosts(self, monkeypatch, production_boost_config, manager_boost_config, business_boost_1,
                                 type_map):
        production_boost_config = production_boost_config()
        manager_boost_config = manager_boost_config()
        business_boost_1 = business_boost_1()

        def mock_open_json(filepath):
            if filepath == 'booster.json':
                return production_boost_config.get_json()
            elif filepath == 'man.json':
                manager_boost_config.add_boost(business_boost_1)
                return manager_boost_config.get_json()
            elif filepath == 'type_map.json':
                return type_map

        monkeypatch.setattr(config_main, 'open_json', mock_open_json)

        class Args:
            manager_file = 'man.json'
            production_file = 'booster.json'
            type_map_file = 'type_map.json'

        _, new_boosts = config_main.look_for_updates(Args)
        import sys
        print(new_boosts, file=sys.stderr)
        assert len(new_boosts) == 1
        assert new_boosts[0][Boost.Field.NAME] == business_boost_1.name

    @pytest.mark.parametrize('manager_boost_config, production_boost_config, expected', [
        (
            {'name': 'B1', 'type': 'brand_boost', 'hids': [123, 345]},
            {'name': 'B1', 'type': 'brand_boost', 'hids': [123],
             'base_coeffs': {'text': 1.2, 'textless': 1.1},
             'args': {'@type': 'type.googleapis.com/MarketSearch.Booster.TBrandBoostArgs'}},
            {'name': 'B1', 'type': 'brand_boost', 'hids': [123, 345],
             'base_coeffs': {'text': 1., 'textless': 1.},
             'analytics': {'importance': 1.0, 'is_new': True},
             'args': {'@type': 'type.googleapis.com/MarketSearch.Booster.TBrandBoostArgs'}}
        ), (
            {'name': 'B1', 'type': 'brand_boost', 'hids': [123], 'args': {'x': 1}},
            {'name': 'B1', 'type': 'brand_boost', 'hids': [123],
             'base_coeffs': {'text': 1.2, 'textless': 1.1},
             'args': {'@type': 'type.googleapis.com/MarketSearch.Booster.TBrandBoostArgs'}},
            {'name': 'B1', 'type': 'brand_boost', 'hids': [123],
             'base_coeffs': {'text': 1., 'textless': 1.},
             'analytics': {'importance': 1.0, 'is_new': True},
             'args': {'x': 1, '@type': 'type.googleapis.com/MarketSearch.Booster.TBrandBoostArgs'}}
        ),
        (
            {'args': {'param_id': 77777777, 'value_int': 2}, 'hids': [0],
             'name': 'boost_hype_offers',
             'importance': 1.1,
             'type': 'hardcoded_gl_filter_boost'},
            {'args': {'param_id': 77777777, 'value_int': 1,
                      '@type': "type.googleapis.com/MarketSearch.Booster.THardcodedGlFilterArgs"},
             'hids': [0], 'name': 'boost_hype_offers',
             'analytics': {'importance': 1.2},
             'type': 'hardcoded_gl_filter_boost',
             'base_coeffs': {'text': 1.2, 'textless': 1.2},
             },
            {'args': {'param_id': 77777777, 'value_int': 2,
                      '@type': "type.googleapis.com/MarketSearch.Booster.THardcodedGlFilterArgs"},
             'hids': [0], 'name': 'boost_hype_offers',
             'analytics': {'importance': 1.1, 'is_new': True},
             'type': 'hardcoded_gl_filter_boost',
             'base_coeffs': {'text': 1., 'textless': 1.},
             },
        )
    ])
    def test_update_existing_boost(self, monkeypatch, manager_boost_config, production_boost_config, expected,
                                   type_map):
        def mock_open_json(filepath):
            if filepath == 'booster.json':
                return {'boosts': [production_boost_config]}
            elif filepath == 'man.json':
                manager_boost_config
                return {'boosts': [manager_boost_config]}
            elif filepath == 'type_map.json':
                return type_map

        monkeypatch.setattr(config_main, 'open_json', mock_open_json)

        class Args:
            manager_file = 'man.json'
            production_file = 'booster.json'
            type_map_file = 'type_map.json'

        new_production_config, new_boosts = config_main.look_for_updates(Args)
        assert new_production_config == {'boosts': [expected]}
        assert new_boosts == [expected]
