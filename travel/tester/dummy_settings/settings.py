# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os

SECRET_KEY = 'test'

CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
        'LONG_TIMEOUT': 86400,
        'TIMEOUT': 60,
    }
}

CACHEROOT = 'dummyroot'
PKG_VERSION = 'dummyversion'

YANDEX_DATA_CENTER = 'dev'

PROJECT_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))
