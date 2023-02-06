from django_pgaas import HostManager

from .base import *

DEBUG = False
ENV_TYPE = 'testing'

ALLOWED_HOSTS.append('test-back.luna.yandex-team.ru')

MIDDLEWARE.append('django.middleware.csrf.CsrfViewMiddleware')

# Database
# https://github.yandex-team.ru/tools/django_pgaas#multiple-hosts
hosts = [
    ('sas-j2o8qc1fwfsr9h8y.db.yandex.net', 'sas'),
    ('vla-c3nkqq4yo6omiup6.db.yandex.net', 'vla'),
]
manager = HostManager(hosts)

DB_HOST = manager.host_string
DB_PORT = os.environ.get('DB_PORT')
DB_NAME = os.environ.get('DB_NAME', 'voltadb_test')
DB_PASS = os.environ.get('DB_PASS')
DB_USER = os.environ.get('DB_USER')

DATABASES = {
    'default': {
        'DISABLE_SERVER_SIDE_CURSORS': True,  # server-side cursors are incompatible with pgbouncer
        'ENGINE': 'django.db.backends.postgresql_psycopg2',  # 'pgaas_db_backend'
        'NAME': DB_NAME,
        'USER': DB_USER,
        'PASSWORD': DB_PASS,
        'HOST': DB_HOST,
        'PORT': DB_PORT,
        'TEST_USER': 'root',
        'TEST_CHARSET': 'utf8',
        'TEST_COLLATION': 'utf8_general_ci',
        'OPTIONS': {
            'sslmode': 'require',
            'target_session_attrs': 'read-write'
        },
        'CONN_MAX_AGE': 49,
    },
    'slave': {
        'DISABLE_SERVER_SIDE_CURSORS': True,
        'ENGINE': 'django.db.backends.postgresql_psycopg2',  # 'pgaas_db_backend'
        'NAME': DB_NAME,
        'USER': DB_USER,
        'PASSWORD': DB_PASS,
        'HOST': DB_HOST,
        'PORT': DB_PORT,
        'TEST_USER': 'root',
        'TEST_CHARSET': 'utf8',
        'TEST_COLLATION': 'utf8_general_ci',
        'OPTIONS': {
            'sslmode': 'require',
            'target_session_attrs': 'any'
        },
        'CONN_MAX_AGE': 49,
    },
}

LOGGING['handlers']['file'] = file_handler
LOGGING['loggers']['django']['handlers'] = ['file']
# LOGGING['loggers']['django']['handlers'] = ['console']


CLICKHOUSE_HOST = os.environ.get('CLICKHOUSE_HOST') or 'man-vnpeczxqwqiwkguq.db.yandex.net'  # 'vla-kvh4wvcc4lk0uj0s.db.yandex.net'
CLICKHOUSE_PORT = os.environ.get('CLICKHOUSE_PORT')
CLICKHOUSE_PASS = os.environ.get('CLICKHOUSE_PASS')
CLICKHOUSE_USER = os.environ.get('CLICKHOUSE_USER')
CLICKHOUSE_NAME = os.environ.get('CLICKHOUSE_NAME')
