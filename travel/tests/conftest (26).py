# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa

os.environ['DJANGO_SETTINGS_MODULE'] = 'common.tester.settings_for_old_libs'
