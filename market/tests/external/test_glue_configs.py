#!/usr/bin/env python
# coding: utf-8
import os
import subprocess

import yatest.common
import pytest


def get_paths():
    conf_available_path = yatest.common.source_path('market/idx/miconfigs/etc/glue/')
    files = list(map(lambda name: os.path.join(conf_available_path, name),
                 list(filter(lambda name: (name.endswith(".json")),
                             os.listdir(conf_available_path)))))
    return files


@pytest.mark.parametrize("config_file_path", get_paths(), ids=list(map(lambda path: path.split("/")[-1], get_paths())))
def test_glue_configs(config_file_path):
    configure_binary_path = yatest.common.binary_path('market/idx/offers/bin/glue-config-validator/glue-config-validator')

    assert subprocess.call([configure_binary_path, config_file_path]) == 0
