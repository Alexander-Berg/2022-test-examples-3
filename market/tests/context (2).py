# -*- coding: utf-8 -*-

import getpass
import os
import logging

from market.pylibrary.yatestwrap.yatestwrap import source_path

ETC_DATA_DIR = source_path('market/backctld/etc')
ETC_DATA_PLUGINS_DIR = os.path.join(ETC_DATA_DIR, 'plugins')
MARKETSEARCH_DATA_DIR = source_path('market/backctld/tests/marketsearch_data')

TMP_DIR = 'tmp'
LOG_NAME = 'log'
LOGGER_INITED = None


def get_plugins_dir_path():
    return ETC_DATA_PLUGINS_DIR


def get_plugin_path(name):
    return os.path.join(ETC_DATA_PLUGINS_DIR, name)


def setup_log():
    logging.addLevelName(logging.WARN, 'WARN')
    logging.basicConfig(level=logging.DEBUG,
                        format='%(asctime)s %(levelname)-5s %(process)-5d b2.%(name)s:  %(message)s')
    global LOGGER_INITED
    LOGGER_INITED = True


def user_name():
    return getpass.getuser()


if not LOGGER_INITED:
    setup_log()
