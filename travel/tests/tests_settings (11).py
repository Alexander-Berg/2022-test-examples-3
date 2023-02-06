# -*- coding: utf-8 -*-
# flake8: noqa
from __future__ import absolute_import

import os

cwd = os.getcwd()
os.environ.setdefault('YANDEX_ENVIRONMENT_TYPE', 'development')
os.environ.setdefault('AVIA_LOG_PATH', os.path.join(cwd, 'log'))

from travel.avia.ticket_daemon_processing.local_settings import *

DEBUG = True

MAINTENANCE_DB_NAME = None

DATABASES['default']['HOST'] = os.getenv('AVIA_MYSQL_HOST', '127.0.0.1')
DATABASES['default']['PORT'] = os.getenv('AVIA_MYSQL_PORT')
DATABASES['default']['NAME'] = os.getenv('AVIA_MYSQL_DATABASE')
DATABASES['default']['USER'] = os.getenv('AVIA_MYSQL_USER', 'root')
DATABASES['default']['PASSWORD'] = os.getenv('AVIA_MYSQL_PASSWORD', '')

assert DATABASES['default']['HOST'] in ('localhost', '127.0.0.1')
assert DATABASES['default']['NAME']

for key in list(DATABASES.keys()):
    if key != 'default':
        del DATABASES[key]

REAL_DB_NAME = DATABASES['default']['NAME']
TEST_DB_NAME = os.getenv('AVIA_MYSQL_TEST_DATABASE')
DATABASES['default']['NAME'] = TEST_DB_NAME
