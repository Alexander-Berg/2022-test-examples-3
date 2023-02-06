# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

"""
Минимальные настройки для тестов с django.

pytest ищет conftest.py рекурсивно, поэтому достаточно подключить эту либу в ya.make, чтобы настройки заработали
"""

import os
os.environ['DJANGO_SETTINGS_MODULE'] = 'travel.rasp.library.python.common23.tester.dummy_settings.settings'
os.environ['RASP_VAULT_STUB_SECRETS'] = '1'


def setup_django():
    import django
    from django.test.utils import setup_test_environment

    django.setup()
    setup_test_environment()


def pytest_configure():
    setup_django()
