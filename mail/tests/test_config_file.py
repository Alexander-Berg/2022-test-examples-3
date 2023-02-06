import pytest

import ora2pg.app.config_file as CF


@pytest.mark.parametrize('bad_key', [
    'oh-no',
    'and',
    'or',
    '42',
    '42_x',
    'x y z',
    list('abc'),
    None,
])
def test_bad_config_keys(bad_key):
    assert CF.why_bad_config_key(bad_key)


def choice_keys(from_keys):
    if isinstance(from_keys, str):
        return {from_keys: 'leaf-%s' % from_keys}
    res = {}
    for rk in from_keys:
        if isinstance(rk, str):
            res[rk] = rk
        else:
            if isinstance(rk, CF.OneFrom):
                res.update(choice_keys(rk[0]))
            elif isinstance(rk, CF.AllFrom):
                for sub_rk in rk:
                    res.update(choice_keys(sub_rk))
    return res


def mk_config():
    return choice_keys(CF.REQUIRED_CONFIG_ITEMS)


def test_full_config_is_good():
    config = mk_config()
    assert not CF.why_bad_config(config)
    config['new_parameter_with_stange_long_name'] = '//_-)'
    assert not CF.why_bad_config(config)


def test_config_without_required():
    config = mk_config()
    del config[list(config.keys())[0]]
    assert CF.why_bad_config(config)


def test_config_with_bad_argument():
    config = mk_config()
    config['orly?'] = 'bad'
    assert CF.why_bad_config(config)


@pytest.mark.parametrize('bad_config', [
    None,
    [],
    ['zoom zoom'],
    tuple(),
    lambda a: a,
])
def test_strange_objects_as_config(bad_config):
    assert CF.why_bad_config(bad_config)
