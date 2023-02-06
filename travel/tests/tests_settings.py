# -*- coding: utf-8 -*-
from __future__ import absolute_import, unicode_literals
import os

cwd = os.getcwd()
os.environ.setdefault('LOG_PATH', os.path.join(cwd, 'log'))

os.environ.setdefault('AVIA_ADMIN_MYSQL_HOST', os.getenv('AVIA_MYSQL_HOST', '127.0.0.1'))
os.environ.setdefault('AVIA_ADMIN_MYSQL_PORT', os.getenv('AVIA_MYSQL_PORT', ''))
os.environ.setdefault('AVIA_ADMIN_MYSQL_DATABASE_NAME', os.getenv('AVIA_MYSQL_DATABASE', 'not used'))
os.environ.setdefault('AVIA_ADMIN_MYSQL_TEST_DATABASE_NAME', os.getenv('AVIA_MYSQL_TEST_DATABASE'))
os.environ.setdefault('AVIA_ADMIN_MYSQL_USER', os.getenv('AVIA_MYSQL_USER', 'root'))
os.environ.setdefault('AVIA_ADMIN_MYSQL_PASSWORD', os.getenv('AVIA_MYSQL_PASSWORD', ''))

from travel.avia.admin.settings import *  # noqa
from travel.avia.admin.settings import DATABASES, TEST_DB_NAME

DEBUG = True
APIKEYS_ENABLED = False

assert DATABASES['default']['HOST'] in ('localhost', '127.0.0.1')
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

AVIA_STATS_ADMIN_HOST = ''
AVIA_STATS_IMPORT_USER = ''
AVIA_STATS_IMPORT_PASSWORD = ''

# yt_stuff из рецепта устанавливает переменную окружения YT_PROXY
YT_PROXY = os.getenv('YT_PROXY')
assert YT_PROXY.split(':')[0] == 'localhost', 'YT_PROXY указывает не на локальный YT'
