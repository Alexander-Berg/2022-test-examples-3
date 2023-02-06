# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa
from common.settings.configuration import Configuration
from travel.rasp.wizards.train_wizard_api.settings import *

# чтобы тесты не лезли за сертификатами
# есть проблемы с httpretty. Он как-то ломается при попытке достать сертификаты
os.environ.pop('REQUESTS_CA_BUNDLE')
REQUESTS_CA_BUNDLE = None

Configuration().apply(globals())

DATABASES['default']['HOST'] = '127.0.0.1'
DATABASES['default']['NAME'] = os.environ.get('actual_db', 'rasp')

DATABASES['default']['USER'] = 'root'
DATABASES['default']['PASSWORD'] = ''

os.environ.setdefault('DBAAS_TRAIN_WIZARD_API_DB_NAME', 'rasp_postgres_test')
os.environ.setdefault('DBAAS_TRAIN_WIZARD_API_DB_USER', 'rasp')
os.environ.setdefault('DBAAS_TRAIN_WIZARD_API_DB_PASSWORD', '')

DBAAS_TRAIN_WIZARD_API_DB_HOST = 'localhost'
DBAAS_TRAIN_WIZARD_API_SELECT_TIMEOUT = 2

TEST_DB_NAME = os.environ.get('TRAVEL_MYSQL_RECIPE_TESTS_DB', 'rasp_tests')
ALLOWED_TEST_DB_HOSTS = ['127.0.0.1', 'localhost']

# user settings
try:
    from local_settings import *
except ImportError:
    pass

DBAAS_TRAIN_WIZARD_API_SSL_MODE = 'disable'

MAINTENANCE_DB_NAME = None
CELERY_TASK_ALWAYS_EAGER = True
MAINTENANCE_DB_CRITICAL = False

if 'LOG_PATH' not in globals():
    globals()['LOG_PATH'] = os.path.join(PROJECT_PATH, 'log')

LOGGING = {}

CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
        'LONG_TIMEOUT': 86400,
        'TIMEOUT': 60,
    }
}

YASMS_DONT_SEND_ANYTHING = False
TRY_HARD_NEVER_SLEEP = True
REQUEST_LIMITER_NEVER_SLEEP = True
BACKEND_URL = None
MDS_URL = None
SANDBOX_OWNER = 'FAKE_OWNER'
MORDA_HOST_BY_TLD = {'ru': 'ru-rasp-host'}
TOUCH_HOST_BY_TLD = {'ru': 'ru-touch-rasp-host'}
TRAINS_HOST_BY_TLD = {'ru': 'ru-train-host'}

try:
    from tests_settings import *
# Специфичных для тестов настроек может не быть
except ImportError:
    pass


REST_FRAMEWORK = {
    'DEFAULT_RENDERER_CLASSES': ('drf_ujson.renderers.UJSONRenderer',)
}
