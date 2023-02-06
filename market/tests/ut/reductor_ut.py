# coding: utf-8

from ConfigParser import ConfigParser
import glob

import pytest
import yatest


def readconfig(path):
    cp = ConfigParser()
    cp.optionxform = str  # prevents lowercasing keys
    cp.read(path)
    return cp


def validate_reductor_config(cfgpath):
    cp = readconfig(cfgpath)
    for section in cp.sections():
        for name, value in cp.items(section):
            # Значение не должно содержать символа newline.
            value2 = ','.join([x.strip() for x in value.split(',')])
            assert '\n' not in value2, [cfgpath, section, name]


def check_directory(dirname):
    config_files = list(glob.glob(dirname + '/*.ini'))
    assert len(config_files) > 0, 'Has at least one config file'
    for filepath in config_files:
        validate_reductor_config(filepath)


def generated_conf_available():
    return yatest.common.build_path('market/idx/miconfigs/etc/reductor/conf-available/')


def generated_updater_conf_available():
    return yatest.common.build_path('market/idx/miconfigs/etc/reductor/updater/conf-available/')


def test_reductor_generated_configs():
    check_directory(generated_conf_available())


def test_reductor_updater_generated_configs():
    check_directory(generated_updater_conf_available())


def read_all_configs_from_dir(dirname):
    cfg = ConfigParser()
    cfg.optionxform = str
    for filepath in glob.glob(dirname + '/*.ini'):
        # при пересечении конфигов, старые перетираются
        cfg.read(filepath)
    return cfg


def read_reductor_config():
    return read_all_configs_from_dir(generated_conf_available())


@pytest.fixture()
def reductor_config():
    return read_reductor_config()


def read_updater_config():
    return read_all_configs_from_dir(generated_updater_conf_available())


@pytest.fixture()
def updater_config():
    return read_updater_config()


def test_same_groups_in_reductor_configs(reductor_config, updater_config, section_name):
    if reductor_config.has_option(section_name, 'groups') and updater_config.has_option(section_name, 'groups'):
        red_groups = reductor_config.get(section_name, 'groups')
        upd_groups = updater_config.get(section_name, 'groups')
        assert set(red_groups).issubset(upd_groups)


def pytest_generate_tests(metafunc):
    """
    Магия, которая параметризует тесты фикстурой section_name
    """
    if "section_name" in metafunc.fixturenames and "reductor_config" in metafunc.fixturenames and "updater_config" in metafunc.fixturenames:
        reductor_conf = read_reductor_config()
        updater_conf = read_updater_config()
        metafunc.parametrize("section_name", set(reductor_conf.sections()).intersection(updater_conf.sections()))
