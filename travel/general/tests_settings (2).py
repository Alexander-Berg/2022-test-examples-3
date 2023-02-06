# -*- coding: utf-8 -*-
from __future__ import absolute_import

from local_settings import *

DEBUG = True

assert DATABASES['default']['HOST'] == 'localhost'
assert DATABASES['default']['NAME']

for key in list(DATABASES.keys()):
    if key != 'default':
        del DATABASES[key]

REAL_DB_NAME = DATABASES['default']['NAME']
DATABASES['default']['NAME'] = TEST_DB_NAME

# disable all caches for tests
CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
        'LONG_TIMEOUT': 86400,
        'TIMEOUT': 60,
    }
}
