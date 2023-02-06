# coding: utf-8

import json
import yatest.common


FORMAT_SPEC_URL = 'https://firebase.google.com/docs/reference/remote-config/rest/v1/RemoteConfig?hl=ru'
CONDITION_COLORS = [
    'CONDITION_DISPLAY_COLOR_UNSPECIFIED',
    'BLUE',
    'BROWN',
    'CYAN',
    'DEEP_ORANGE',
    'GREEN',
    'INDIGO',
    'LIME',
    'ORANGE',
    'PINK',
    'PURPLE',
    'TEAL'
]


def test_not_empty():
    config = _read_config()
    assert len(config) > 0


def test_no_free_parameters():
    config = _read_config()
    assert 'parameters' not in config.keys(), "Config should not contain toggles outside of 'parameterGroups' key"


def test_contains_required_keys():
    config = _read_config()
    msg = "Config should satisfy format {}".format(FORMAT_SPEC_URL)
    assert 'conditions' in config.keys(), "Key 'conditions' is missing. " + msg
    assert 'version' in config.keys(), "Key 'version' is missing. " + msg
    assert 'parameterGroups' in config.keys(), "Key 'parameterGroups' is missing. " + msg


def test_keys_count():
    config = _read_config()
    assert len(config.keys()) == 3, """
        Redundant keys in config. Expected: ['conditions', 'version', 'parameterGroups']. Got {}
    """.format(config.keys())


def test_conditions():
    config = _read_config()
    for condition in config['conditions']:
        assert len(condition['name']) > 0
        assert len(condition['expression']) > 0
        if 'tagColor' in condition.keys():
            assert condition['tagColor'] in CONDITION_COLORS
            assert len(condition.keys()) == 3, "Redundant keys in condition '{}'".format(condition['name'])
        else:
            assert len(condition.keys()) == 2, "Redundant keys in condition '{}'".format(condition['name'])


def test_parameters():
    config = _read_config()
    groups = dict(config['parameterGroups'])
    assert len(groups.keys()) > 0
    for group_key in groups.keys():
        group = groups[group_key]
        assert type(group) is dict, "Parameter group '{}' is not a JSON".format(group_key)
        if 'description' in group.keys():
            assert len(group['description']) > 0
            assert len(group.keys()) == 2
        else:
            assert len(group.keys()) == 1
        parameters = group['parameters']
        assert type(parameters) is dict, "Parameters object in group '{}' is not a JSON".format(group_key)
        assert len(parameters.keys()) > 0
        for key in parameters.keys():
            parameter = parameters[key]
            _test_parameter(config, key, parameter)


def _test_parameter(config, key, parameter):
    assert 'defaultValue' in parameter.keys()
    default_value = dict(parameter['defaultValue'])
    _test_value(key, default_value)

    if 'conditionalValues' in parameter.keys():
        possible_conditions = map(lambda c: c['name'], config['conditions'])
        conditional_values = parameter['conditionalValues']
        for condition_key in conditional_values.keys():
            assert condition_key in possible_conditions
            _test_value(key, conditional_values[condition_key])

    assert 'description' in parameter.keys(), "Missing description for toggle '{}'".format(key)
    description = parameter['description']
    assert ('https://st.yandex-team.ru/BLUEMARKETAPPS-' in description) or \
           ('https://st.yandex-team.ru/BMP-' in description) or \
           ('https://st.yandex-team.ru/MINCATPROJECT-' in description) or \
           ('https://st.yandex-team.ru/MADV-' in description) or \
           ('https://st.yandex-team.ru/MARKETPROJECT-' in description) \
            , "Missing ST ticket link in description for toggle '{}'".format(key)
    assert len(description) <= 256, "Description should not exceed 256 characters"

    assert len(parameter.keys()) >= 2


def _test_value(key, value):
    assert 'value' in value.keys()
    value = json.loads(value['value'])

    assert 'name' in value.keys()
    assert value['name'] == key

    assert 'ios' in value.keys()
    _test_platform(value['ios'])
    _test_platform_ios(value['ios'])

    assert 'android' in value.keys()
    _test_platform(value['android'])

    assert len(value.keys()) == 3, "Redundant keys in value '{}'".format(value['name'])


def _test_platform(platform):
    assert 'enabled' in platform.keys()
    assert str(platform['enabled']) in ['True', 'False']

    if 'info' in platform.keys():
        _test_info(platform['info'])

    if 'fromVersion' in platform.keys():
        assert len(platform['fromVersion']) > 0


def _test_platform_ios(platform):
    assert set(platform.keys()) <= set(['enabled', 'info', 'fromVersion']), "Redundant keys in platform specs"


def _test_info(info):
    assert type(info) is dict, "Info value is not a JSON"
    assert info.keys() > 0


def _read_config():
    path = yatest.common.source_path('market/mobile/beru/remote_configurator/remoteConfig.json')
    with open(path, 'r') as f:
        config = f.read()
    return dict(json.loads(config))
