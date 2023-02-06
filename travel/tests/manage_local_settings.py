# coding: utf8
import os

from settings import *  # noqa
from settings import PROJECT_PATH, DATABASES, MONGO_DATABASES, CACHES
from common.db.mongo.mongo import DEFAULT_MONGO_PORT
from common.settings.configuration import Configuration

LOG_PATH = os.path.join(PROJECT_PATH, 'log')

YANDEX_DATA_CENTER = 'dev'  # чтоб не ломиться в кондуктор

print('Calling Configuration().apply')
Configuration().apply(globals())
CACHES['default'] = {
    'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    'LONG_TIMEOUT': 10,
    'TIMEOUT': 5,
}

# For test db
DATABASES['default']['HOST'] = os.getenv('TRAVEL_MYSQL_RECIPE_HOST', '127.0.0.1')
DATABASES['default']['PORT'] = int(os.getenv('TRAVEL_MYSQL_RECIPE_PORT', 3306))
DATABASES['default']['NAME'] = os.getenv('TRAVEL_MYSQL_RECIPE_TESTS_DB', 'rasp_tests')
DATABASES['default']['USER'] = 'root'
DATABASES['default']['PASSWORD'] = os.getenv('TRAVEL_MYSQL_RECIPE_PASSWORD', '')

MONGO_PORT = os.getenv('RASP_MONGO_RECIPE_PORT', DEFAULT_MONGO_PORT)
MONGO_DATABASES['default']['port'] = MONGO_PORT
MONGO_DATABASES['default_no_timeout']['port'] = MONGO_PORT
MONGO_DATABASES['train_purchase']['port'] = MONGO_PORT

_format = '%(levelname)s %(asctime)s %(process)d %(name)s %(message)s'
_format_with_context = '%(levelname)s %(asctime)s %(context)s %(process)d %(name)s %(message)s'
LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'simple': {'format': _format},
        'context': {'format': _format_with_context},
        'exception': {
            '()': 'ylog.ExceptionFormatter',
            'format': _format,
            'full': True,
            'show_locals': True
        },
    },
    'filters': {
        'add_context': {
            '()': 'travel.rasp.library.python.common23.logging.AddContextFilter',
        },
    },
    'handlers': {
        'application': {
            'filters': ['add_context'],
            'class': 'travel.rasp.library.python.common23.logging.WatchedFileHandler',
            'filename': os.path.join(LOG_PATH, 'application.log'),
            'formatter': 'context',
        },
        'warnings': {
            'class': 'travel.rasp.library.python.common23.logging.WatchedFileHandler',
            'filename': os.path.join(LOG_PATH, 'warnings.log'),
            'formatter': 'simple',
        },
        'exception': {
            'filters': ['add_context'],
            'class': 'travel.rasp.library.python.common23.logging.WatchedFileHandler',
            'filename': os.path.join(LOG_PATH, 'exception.log'),
            'formatter': 'exception',
            'level': 'ERROR',
        },
    },
}

print('manage_local_settings loaded')
