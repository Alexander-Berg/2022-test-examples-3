# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function


# TODO: #py23remove
# Эти настройки используются в common, route_search и подобных старых библиотеках
# Все они зависят от common и от INSTALLED_APPS

from common.settings import *  # noqa
from travel.rasp.library.python.common23.tester.settings import *  # noqa

INSTALLED_APPS = COMMON_CONTRIB_APPS + COMMON_INSTALLED_APPS
ALLOWED_HOSTS = '*'
INTERNAL_ROOT_CERT = None
