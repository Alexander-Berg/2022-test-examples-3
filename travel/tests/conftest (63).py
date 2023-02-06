# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa

import os
os.environ['DJANGO_SETTINGS_MODULE'] = 'travel.rasp.trains.scripts.tests.tests_settings'
