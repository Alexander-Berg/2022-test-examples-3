# -*- coding: utf-8 -*-
from travel.avia.avia_api.local_settings import *
from travel.avia.library.python.tester.arcadia_tests_settings import *

TIME_ZONE = 'Europe/Moscow'

# disable cache for tests
CACHES['default'] = {
    'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    'LONG_TIMEOUT': 86400,
    'TIMEOUT': 60,
}
CACHES['shared_cache'] = CACHES['default']

DATABASES['writable'] = DATABASES['default']

FAKE_API_KEYS = True
ENABLE_TVM = False
