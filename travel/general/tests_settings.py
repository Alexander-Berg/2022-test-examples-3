# -*- coding: utf-8 -*-
from __future__ import absolute_import

import copy

from local_settings import *

DEBUG = True

assert DATABASES['default']['HOST'] == 'localhost'
assert DATABASES['default']['NAME']

DATABASES['real_db'] = copy.deepcopy(DATABASES['default'])
DATABASES['default']['NAME'] = TEST_DB_NAME

# disable all caches for tests
CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
        'LONG_TIMEOUT': 86400,
        'TIMEOUT': 60,
    }
}

# чтобы не ходить вовне
CURRENCY_RATES_URL = None
PATHFINDER_URL = None
