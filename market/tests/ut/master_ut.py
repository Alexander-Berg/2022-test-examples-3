# coding: utf-8

from ConfigParser import ConfigParser
import glob

import market.idx.mif.configurator.configurator as configurator
import yatest
import market.idx.marketindexer.miconfig as miconfig


def readconfig(path):
    cp = ConfigParser()
    cp.optionxform = str  # prevents lowercasing keys
    cp.read(path)
    return cp


def validate_master_config(cfgpath):
    configurator.make_master_config(cfgpath, 'result.ini', dry_run=True)
    cp1 = readconfig(cfgpath)
    cp2 = readconfig('result.ini')
    # [model_vclusters]
    section = 'model_vclusters'
    assert cp1.options(section) == cp2.options(section)
    # [offers]
    section = 'offers'
    for option in ['UseNewIndexer', 'UseWadForC2N', 'NeedBuildAnnIndex']:
        assert not cp1.has_option(section, option) or cp1.get(section, option) == cp2.get(section, option)


def test_master_configs():
    dirname = yatest.common.source_path('market/idx/miconfigs/etc/master/conf-available/')
    for filepath in glob.glob(dirname + '/*.ini'):
        validate_master_config(filepath)


# hvost239: Закомитил до лучших времен. Либо переписывапм весь конфиг на плоский либо этот тест всегда будет падать
# def test_feature_configs():
#    dirname = yatest.common.source_path('market/idx/miconfigs/etc/feature/')
#    for filepath in glob.glob(dirname + '/*.ini'):
#        cp = readconfig(filepath)
#        assert cp.sections() == ['general'], u"В конфиге должна быть только 1 секция general!"


def test_generated_plus_feature_configs():
    ds_config_path = yatest.common.source_path('market/idx/marketindexer/tests/datasources.conf')
    for cpath in glob.iglob(yatest.common.build_path('market/idx/marketindexer/etc/generated') + '/*.ini'):
        for lpath in glob.glob(yatest.common.source_path('market/idx/miconfigs/etc/feature/') + '/*.ini'):
            miconfig.MiConfig([cpath, lpath], ds_config_path)
