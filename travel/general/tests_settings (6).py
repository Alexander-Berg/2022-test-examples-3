# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os

from travel.rasp.morda_backend.settings import *  # noqa
from travel.rasp.library.python.common23.settings.configuration import Configuration

Configuration().apply(globals())
os.environ.setdefault('RASP_TEST_APPLIED_CONFIG', globals()['APPLIED_CONFIG'])

from travel.rasp.library.python.common23.tester.settings import *  # noqa

ROOT_URLCONF = 'travel.rasp.morda_backend.morda_backend.urls'
DEBUG_TRAIN_ORDER_SKIP_TRUST = False
BYPASS_BACKOFFICE_AUTH = False
DEBUG_TOOLBAR_PANELS = []
ENABLE_HOTEL_BANNER = False
