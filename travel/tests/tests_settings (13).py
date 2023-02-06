# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.library.python.common23.tester.settings import *  # noqa
from common.settings import COMMON_CONTRIB_APPS, COMMON_INSTALLED_APPS

INSTALLED_APPS = COMMON_CONTRIB_APPS + COMMON_INSTALLED_APPS + [
    'django.contrib.sites',
    'route_search',
]
