# -*- coding: utf-8 -*-

import os

if not os.environ.get('DJANGO_SETTINGS_MODULE'):
    os.environ['DJANGO_SETTINGS_MODULE'] = 'tests_settings'

# Нельзя определять pytest_plugins в подмодулях
# pytest_plugins = [
#     'travel.avia.library.python.tester.initializer',
#     'travel.avia.library.python.tester.plugins.transaction',
#     'travel.avia.library.python.tester.plugins.rasp_test_paths'
# ]

# Доп настройки наших плагинов
# from travel.avia.library.python.tester.plugins import rasp_test_paths
#
# rasp_test_paths.testpaths.extend([...])
# rasp_test_paths.testpaths = [...]
#
# from travel.avia.library.python.tester.initializer import CONFIG as INITIALIZER_CONFIG
#
# INITIALIZER_CONFIG['copy_models'] = [...]
# INITIALIZER_CONFIG['auto_create_objects'] = [...]
