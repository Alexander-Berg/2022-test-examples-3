# -*- coding: utf-8 -*-

import os
import shutil

from market.pylibrary.yatestwrap.yatestwrap import source_path

from market.idx.marketindexer.marketindexer import miconfig

rootdir = 'tmp'

MI_CONFIG_PATH = source_path('market/idx/miconfigs/etc/feature/common.ini')
DS_CONFIG_PATH = source_path('market/idx/marketindexer/tests/datasources.conf')
ZK_CONFIG_PATH = source_path('market/idx/marketindexer/tests/zookeeper.conf')


def setup():
    cleanup()
    os.makedirs(rootdir)


def cleanup():
    shutil.rmtree(rootdir, ignore_errors=True)


def create_config():
    icpath = source_path('market/idx/miconfigs/etc/feature/common.ini')
    dspath = source_path('market/idx/marketindexer/tests/datasources.conf')
    # zkpath = source_path('market/idx/marketindexer/tests/zookeeper.conf')
    config = miconfig.MiConfig(icpath, dspath)
    return config
