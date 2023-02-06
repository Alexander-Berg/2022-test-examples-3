# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import os

from common.settings.configuration import Configuration
from travel.rasp.wizards.proxy_api.settings import *  # noqa: F403


# чтобы тесты не лезли за сертификатами
# есть проблемы с httpretty. Он как-то ломается при попытке достать сертификаты
os.environ.pop('REQUESTS_CA_BUNDLE', None)
REQUESTS_CA_BUNDLE = None

Configuration().apply(globals())

DATABASES['default'].update({  # noqa: F405 (из proxy_api.settings)
    'HOST': '127.0.0.1',
    'NAME': os.environ.get('actual_db', 'rasp'),
    'USER': 'root',
    'PASSWORD': ''
})

TEST_DB_NAME = os.environ.get('TRAVEL_MYSQL_RECIPE_TESTS_DB', 'rasp_tests')
ALLOWED_TEST_DB_HOSTS = ['127.0.0.1', 'localhost']

# user settings
try:
    from local_settings import *  # noqa: F403
except ImportError:
    pass

MAINTENANCE_DB_NAME = None
CELERY_TASK_ALWAYS_EAGER = True
MAINTENANCE_DB_CRITICAL = False

if 'LOG_PATH' not in globals():
    globals()['LOG_PATH'] = os.path.join(PROJECT_PATH, 'log')  # noqa: F405 (из proxy_api.settings)

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

try:
    from tests_settings import *  # noqa: F401, F403
except ImportError:
    # Специфичных для тестов настроек может не быть
    pass


REST_FRAMEWORK = {
    'DEFAULT_RENDERER_CLASSES': ('drf_ujson.renderers.UJSONRenderer',)
}
