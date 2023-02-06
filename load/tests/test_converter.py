import os
import yaml
import pytest

from tankapi.config_tweaker import PhantomToPandora as ptp
from yatest import common

PATH = common.source_path('load/projects/tankapi_server/tankapi/tests/')
options = []


@pytest.mark.parametrize('phantom_conf, pandora_conf', [
    ('phantom-1.yaml', 'pandora-1.yaml')
])
def test_convert_phantom_to_pandora(phantom_conf, pandora_conf):
    with open(os.path.join(PATH, phantom_conf), 'rb') as ph:
        with open(os.path.join(PATH, pandora_conf), 'rb') as pa:
            conf, opt = ptp().convert(ph, options)
            assert yaml.load(conf, Loader=yaml.FullLoader) == yaml.load(pa, Loader=yaml.FullLoader)


@pytest.mark.parametrize('phantom_conf, pandora_conf', [
    ('bad-1.yaml', 'bad-1.yaml'),
    ('bad-2.yaml', 'bad-2.yaml'),
    ('bad-3.yaml', 'bad-3.yaml')
])
def test__bad_convert_phantom_to_pandora(phantom_conf, pandora_conf):
    with open(os.path.join(PATH, phantom_conf), 'rb') as ph:
        with open(os.path.join(PATH, pandora_conf), 'rb') as pa:
            conf, opt = ptp().convert(ph.read(), options)
            assert conf == pa.read()


@pytest.mark.parametrize('schedule, expected', [
    ('line(1,100,1m)',
        [{
            'type': 'line',
            'from': 1,
            'to': 100,
            'duration': '1m'
        }]),
    ('const(100,1m)',
        [{
            'type': 'const',
            'ops': 100,
            'duration': '1m'
        }]),
    ('const( 200, 2m )',
        [{
            'type': 'const',
            'ops': 200,
            'duration': '2m'
        }]),
    ('step(10,100,10,1m)',
        [{
            'type': 'step',
            'from': 10,
            'to': 100,
            'step': 10,
            'duration': '1m'
        }]),
    ('step(10,100,10,1m) line(1,100,1m)',
        [{
            'type': 'step',
            'from': 10,
            'to': 100,
            'step': 10,
            'duration': '1m'
        },
        {
            'type': 'line',
            'from': 1,
            'to': 100,
            'duration': '1m'
        }])
])
def test_parse_load_profile(schedule, expected):
    assert ptp().parse_load_profile(schedule) == expected


@pytest.mark.parametrize('phantom, expected', [
    ({'instances': 10000}, [{'type': 'once', 'times': 10000}]),
    ({'phantom': {}}, [{'type': 'once', 'times': 1000}])
])
def test_parse_phantom_instances(phantom, expected):
    assert ptp().parse_phantom_instances(phantom) == expected


@pytest.mark.parametrize('ammo_type, expected', [
    ({'ammo_type': 'phantom'}, 'raw'),
    ({'ammo_type': 'uri'}, 'uri'),
    ({'ammo_type': 'uripost'}, None)
])
def test_get_ammo_type(ammo_type, expected):
    assert ptp().get_ammo_type(ammo_type) == expected
