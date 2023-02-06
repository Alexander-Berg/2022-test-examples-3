# -*- coding: utf-8 -*-
from settings import *

DATABASES = {
    'default': {
        'NAME': 'testmldatabase',
        'ENGINE': 'django.db.backends.sqlite3',
    }
}

SOUTH_TESTS_MIGRATE = False
INSTALLED_APPS.extend([
    'django_nose',
])

TEST_RUNNER = 'django_nose.NoseTestSuiteRunner'
NOSE_ARGS = ['--failed', '--pdb']

# If this is True, all tasks will be executed locally by blocking until the task returns.
CELERY_ALWAYS_EAGER = False

import logging

LOGGING['root']['level'] = 'ERROR'

LOG_LEVEL = logging.ERROR

DATABASE_ROUTERS = []
