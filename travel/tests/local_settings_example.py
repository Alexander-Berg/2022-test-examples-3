# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.info_center.settings import *  # noqa
from common.settings.configuration import Configuration

YANDEX_DATA_CENTER = 'dev'

Configuration().apply(globals())

DATABASES['default']['HOST'] = '127.0.0.1'
DATABASES['default']['NAME'] = 'rasp'
DATABASES['default']['USER'] = 'root'
DATABASES['default']['PASSWORD'] = ''

TEST_DB_NAME = 'rasp_test'

GEOBASE_DATA_PATH = "/Users/martuginp/Data/geodata4.bin"

DB_MACHINE_HOST = 'martuginp-1.sas.yp-c.yandex.net'
ALLOWED_TEST_DB_HOSTS = ['127.0.0.1', 'localhost', DB_MACHINE_HOST]
ALLOWED_HOSTS = ['*']

# влкючаем ручки админки
ENABLE_INTERNAL_HANDLES = True

DEBUG = True
BASE_LOG_LEVEL = 'DEBUG'
DISABLE_GROUP_CACHES = True

LOG_PATH = '/Users/martuginp/arc/log/info_center'

# переопределяем настройки логирования, чтобы применить новый LOG_PATH
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
        'qloud_warnings': {
            '()': 'travel.rasp.library.python.common23.logging.qloud.QloudJsonFormatter',
            'tag': 'warnings'
        },
        'qloud_application': {
            '()': 'travel.rasp.library.python.common23.logging.qloud.QloudJsonFormatter',
            'tag': 'application'
        },
        'qloud_errors': {
            '()': 'travel.rasp.library.python.common23.logging.qloud.QloudExceptionFormatter',
            'add_request_info': False,
            'tag': 'errors'
        },
        'qloud_errors_request': {
            '()': 'travel.rasp.library.python.common23.logging.qloud.QloudExceptionFormatter',
            'add_request_info': True,
            'tag': 'errors'
        },
    },
    'filters': {
        'unique_warnings': {
            '()': 'travel.rasp.library.python.common23.logging.WarningFilterOnce'
        },
        'add_context': {
            '()': 'travel.rasp.library.python.common23.logging.AddContextFilter',
        },
        'add_context_dict': {
            '()': 'travel.rasp.library.python.common23.logging.AddContextFilter',
            'as_dict': True
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
        'stdout_application': {
            'filters': ['add_context_dict'],
            'class': 'logging.StreamHandler',
            'stream': 'ext://sys.stdout',
            'formatter': 'qloud_application'
        },
        'stdout_warnings': {
            'class': 'logging.StreamHandler',
            'stream': 'ext://sys.stdout',
            'formatter': 'qloud_warnings',
        },
        'stderr': {
            'filters': ['add_context_dict'],
            'class': 'logging.StreamHandler',
            'stream': 'ext://sys.stderr',
            'formatter': 'qloud_errors',
            'level': 'ERROR',
        },
        'stderr_request': {
            'filters': ['add_context_dict'],
            'class': 'logging.StreamHandler',
            'stream': 'ext://sys.stderr',
            'formatter': 'qloud_errors_request',
            'level': 'ERROR',
        },
    },
    'loggers': {
        '': {
            'handlers': ['application', 'stdout_application'],
            'level': BASE_LOG_LEVEL,
        },
        'py.warnings': {
            'filters': ['unique_warnings'],
            'propagate': False,
            'handlers': ['warnings', 'stdout_warnings'],
            'level': BASE_LOG_LEVEL,
        },
        'django.request': {
            'handlers': ['exception', 'stderr_request'],
            'level': 'ERROR',
        },
    }
}


def stdoutlog():
    LOGGING['handlers']['stdout'] = {
        'class': 'logging.StreamHandler',
        'level': 'DEBUG',
    }

    LOGGING['loggers']['']['handlers'].append('stdout')
    LOGGING['loggers']['']['handlers'].remove('stdout_application')


stdoutlog()

