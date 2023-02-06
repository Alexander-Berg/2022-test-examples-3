# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import os

import django
from django.test.utils import setup_test_environment


def pytest_configure():
    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'travel.rasp.library.python.sitemap.tests.settings')

    django.setup()
    setup_test_environment()
