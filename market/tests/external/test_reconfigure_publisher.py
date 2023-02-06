#!/usr/bin/env python
# coding: utf-8
import os
import subprocess

import yatest.common
import pytest


def get_paths():
    conf_available_path = yatest.common.build_path('market/idx/miconfigs/etc/reductor') + '/' + 'conf-available/'
    files = list(map(lambda name: conf_available_path + name,
                 list(filter(lambda name: (name.endswith(".ini")),
                             os.listdir(conf_available_path)))))
    return files


@pytest.mark.parametrize("ini_file_path", get_paths(), ids=list(map(lambda path: path.split("/")[-1], get_paths())))
def test_reconfigure_publisher(ini_file_path):
    configure_binary_path = yatest.common.binary_path('market/reductor/configure/configure')
    test_directory_path = yatest.common.work_path()
    assert subprocess.call([configure_binary_path,
                             '-j',
                             test_directory_path + '/' + 'tmp/config.json',
                             '-b',
                             test_directory_path + '/' + 'tmp/backends',
                             ini_file_path]) == 0
