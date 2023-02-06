# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os

import yatest
from library.python.testing.recipe import declare_recipe

import travel.rasp.library.common_recipe.main
from travel.library.recipe_utils.utils import log, set_environ, untar, Timer


def add_path_to_env(env_key, path):
    value = os.getenv(env_key, '')
    new_value = ':'.join([value, path])
    set_environ(env_key, new_value)


def setup_ibm_db2_lib(base_dir='.'):
    lib_archive_path = yatest.common.build_path('travel/rasp/suburban_tasks/bin/tests_recipe/package/clidriver.tar.gz')
    untar(lib_archive_path, base_dir)

    lib_path = os.path.join(base_dir, 'clidriver/lib')
    log('ibm db2 clidriver path', lib_path)
    add_path_to_env('DYLD_LIBRARY_PATH', lib_path)
    add_path_to_env('LD_LIBRARY_PATH', lib_path)


def start(argv):
    travel.rasp.library.common_recipe.main.start(argv)

    with Timer('setup_ibm_db2_lib'):
        setup_ibm_db2_lib()


def stop(argv):
    travel.rasp.library.common_recipe.main.stop(argv)


def main():
    declare_recipe(start, stop)
