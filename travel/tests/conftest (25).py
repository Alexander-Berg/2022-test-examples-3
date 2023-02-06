# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import os

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa
os.environ['DJANGO_SETTINGS_MODULE'] = 'travel.rasp.info_center.tests_settings'

from common.tester.admin.django_auth import *  # noqa
