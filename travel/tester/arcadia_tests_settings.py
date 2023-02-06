# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import os

from travel.avia.library.python.common.settings import *  # noqa
from travel.avia.library.python.common.settings import DATABASES, PROJECT_PATH

INSTALLED_APPS = (
    'travel.avia.library.python.avia_data',

    'travel.avia.library.python.common',
    'travel.avia.library.python.common.app_stubs.currency',
    'travel.avia.library.python.common.app_stubs.www',
    'travel.avia.library.python.common.app_stubs.order',
    'travel.avia.library.python.common.importinfo',
    'travel.avia.library.python.common.geotargeting',
    'travel.avia.library.python.common.xgettext',

    'simple_history',

    'django.contrib.auth',
    'django.contrib.admin',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
)


ALLOWED_HOSTS = '*'
INTERNAL_ROOT_CERT = None
AVIA_NATIONAL_VERSIONS = ['ru', 'ua', 'tr', 'com', 'kz']
AVIA_NATIONAL_VERSIONS_CHOICES = tuple([
    (v, v) for v in AVIA_NATIONAL_VERSIONS]
)
AVIA_NATIONAL_VERSIONS_CHOICES_MAX_LEN = 5


DEBUG = True
MAINTENANCE_DB_NAME = None

for key in list(DATABASES.keys()):
    if key != 'default':
        del DATABASES[key]

DATABASES['default']['HOST'] = os.getenv('AVIA_MYSQL_HOST', '127.0.0.1')
DATABASES['default']['USER'] = os.getenv('AVIA_MYSQL_USER', 'root')
DATABASES['default']['PASSWORD'] = os.getenv('AVIA_MYSQL_PASSWORD', '')
DATABASES['default']['PORT'] = os.getenv('AVIA_MYSQL_PORT', 3306)
DATABASES['default']['REPLICAS'] = [DATABASES['default']['HOST']]
REAL_DB_NAME = os.getenv('AVIA_MYSQL_DATABASE')
TEST_DB_NAME = os.getenv('AVIA_MYSQL_TEST_DATABASE')
DATABASES['default']['NAME'] = TEST_DB_NAME

# disable all caches for tests
default_cache_settings = {
    'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    'LONG_TIMEOUT': 86400,
    'TIMEOUT': 60,
}
CACHES = {
    alias: default_cache_settings.copy()
    for alias in ['default', 'logging_memcached_cache', 'redis_cache']
}

# чтобы не ходить вовне
CURRENCY_RATES_URL = None
PATHFINDER_URL = None
QUERY_TICKET_DAEMON = False
# ALLOW_GEOBASE = True
LOG_PATH = os.path.join(PROJECT_PATH, 'logs')
