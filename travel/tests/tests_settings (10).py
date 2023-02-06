# -*- coding: utf-8 -*-
# flake8: noqa
from __future__ import absolute_import

import os

cwd = os.getcwd()
os.environ.setdefault('AVIA_TICKET_HTTP_API_ENVIRONMENT_TYPE', 'development')
os.environ.setdefault('AVIA_TICKET_HTTP_API_LOG_PATH', os.path.join(cwd, 'log'))
os.environ.setdefault('AVIA_TICKET_HTTP_API_YT_LOG_PATH', os.path.join(cwd, 'log'))
os.environ.setdefault('AVIA_TICKET_HTTP_API_SECRET_KEY', 'Some')
os.environ.setdefault('AVIA_TICKET_HTTP_API_MCR_HOST', 'example.com:11211')  # TODO recipe ???
os.environ.setdefault('AVIA_TICKET_HTTP_API_REDIS_CLUSTER_ID', '')
os.environ.setdefault('AVIA_TICKET_HTTP_API_REDIS_SENTINEL_SERVICE_NAME', 'mymaster')
os.environ.setdefault('AVIA_TICKET_HTTP_API_TRAVEL_HOST_FOR_RU', "travel-test.yandex.ru")
os.environ.setdefault('AVIA_TICKET_HTTP_API_FRONT_HOST_FOR_RU', 'avia-frontend.some-login.avia.dev.yandex.ru')
os.environ.setdefault('AVIA_TICKET_HTTP_API_FRONT_HOST_FOR_COM', 'avia-frontend.some-login.avia.dev.yandex.com')
os.environ.setdefault('AVIA_TICKET_HTTP_API_FRONT_HOST_FOR_TR', 'avia-frontend.some-login.avia.dev.yandex.com.tr')
os.environ.setdefault('AVIA_TICKET_HTTP_API_FRONT_HOST_FOR_UA', 'avia-frontend.some-login.avia.dev.yandex.ua')
os.environ.setdefault('AVIA_TICKET_HTTP_API_FRONT_HOST_FOR_KZ', 'avia-frontend.some-login.avia.dev.yandex.kz')
os.environ.setdefault('AVIA_TICKET_HTTP_API_CURRENCY_RATES_URL', 'http://example.com/converter-rasp')

os.environ.setdefault('AVIA_TICKET_HTTP_API_MYSQL_HOST', os.getenv('AVIA_MYSQL_HOST', '127.0.0.1'))
os.environ.setdefault('AVIA_TICKET_HTTP_API_MYSQL_PORT', os.getenv('AVIA_MYSQL_PORT'))
os.environ.setdefault('AVIA_TICKET_HTTP_API_MYSQL_DATABASE_NAME', os.getenv('AVIA_MYSQL_DATABASE'))
os.environ.setdefault('AVIA_TICKET_HTTP_API_MYSQL_TEST_DATABASE_NAME', os.getenv('AVIA_MYSQL_TEST_DATABASE'))
os.environ.setdefault('AVIA_TICKET_HTTP_API_MYSQL_PASSWORD', os.getenv('AVIA_MYSQL_PASSWORD', ''))
os.environ.setdefault('AVIA_TICKET_HTTP_API_MYSQL_USER', os.getenv('AVIA_MYSQL_USER', 'root'))

os.environ.setdefault('AVIA_TICKET_HTTP_API_TICKET_DAEMON_URL', 'http://ticket-daemon-internal.some-login.avia.dev.yandex.net')
os.environ.setdefault('AVIA_TICKET_HTTP_API_PARTNER_QUERY_TIMEOUT', '1')
os.environ.setdefault('AVIA_TICKET_HTTP_API_FEATURE_FLAG_HOST', '')

os.environ.setdefault('AVIA_TICKET_HTTP_API_SENTRY_DSN', 'http://PLEASE:USE@sentry.testing.avia.yandex.net/YOUR_SENTRY')
os.environ.setdefault('AVIA_TICKET_HTTP_API_YDB_DATABASE', '/ru/home/some-login/mydb')
os.environ.setdefault('AVIA_TICKET_HTTP_API_YDB_ENDPOINT', 'ydb-ru-sas-1025.search.yandex.net:31071')
os.environ.setdefault('AVIA_TICKET_HTTP_API_WIZARD_YDB_DATABASE', '/ru/home/some-login/mydb')
os.environ.setdefault('AVIA_TICKET_HTTP_API_WIZARD_YDB_ENDPOINT', 'ydb-ru-sas-1025.search.yandex.net:31071')

from travel.avia.library.python.tester.arcadia_tests_settings import *  # noqa
from travel.avia.ticket_daemon_api.api_settings import *

ALLOW_GEOBASE = True  # Без этого падают 2 теста в common-e
DEBUG = True

assert DATABASES['default']['HOST'] in ('localhost', '127.0.0.1')
assert DATABASES['default']['NAME']

for key in list(DATABASES.keys()):
    if key != 'default':
        del DATABASES[key]

REAL_DB_NAME = DATABASES['default']['NAME']
DATABASES['default']['NAME'] = TEST_DB_NAME

# disable all caches for tests
default_cache_settings = {
    'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    'LONG_TIMEOUT': 86400,
    'TIMEOUT': 60,
}
MCR_DAEMON_CACHES = CACHES = {
    alias: default_cache_settings.copy()
    for alias in ['default', 'shared_cache']
}

# чтобы не ходить вовне
CURRENCY_RATES_URL = None

TICKET_DAEMON_URL ='http://internal-daemon.mock/'
TRAVEL_HOST_BY_NATIONAL_VERSION = {
    'ru': 'mock-travel.yandex.ru',
}
AVIA_HOST_BY_NATIONAL_VERSION = {
    'ru': 'mock-avia.yandex.ru',
    'kz': 'mock-avia.yandex.kz',
    'ua': 'mock-avia.yandex.ua',
    'tr': 'mock-avia.yandex.tr',
    'com': 'mock-avia.yandex.com',
}

ENABLE_TVM = False

LOGGING['root']['handlers'].remove('sentry')
