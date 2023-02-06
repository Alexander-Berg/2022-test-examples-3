# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

try:
    # TODO: #py23remove
    # Метод с TOPLEVEL settings.py использовать не следует.
    # Нужно переводить все проекты на абсолютные пути к тестовым настройкам.
    from settings import *  # noqa
except ImportError:
    from travel.rasp.library.python.common23.settings import *  # noqa

import os
from travel.rasp.library.python.common23.settings.configuration import Configuration

if not os.environ.get('RASP_TEST_APPLIED_CONFIG'):
    Configuration().apply(globals())

DATABASES['default']['HOST'] = '127.0.0.1'  # noqa
DATABASES['default']['NAME'] = os.environ.get('actual_db', 'rasp')  # noqa

DATABASES['default']['USER'] = 'root'  # noqa
DATABASES['default']['PASSWORD'] = ''  # noqa

TEST_DB_NAME = os.environ.get('TRAVEL_MYSQL_RECIPE_TESTS_DB', 'rasp_tests')
ALLOWED_TEST_DB_HOSTS = ['localhost', '127.0.0.1']

# user settings
try:
    from local_settings import *  # noqa
except ImportError:
    pass

MAINTENANCE_DB_NAME = None
CELERY_TASK_ALWAYS_EAGER = True
MAINTENANCE_DB_CRITICAL = False
INTERNAL_ROOT_CERT = None

if 'LOG_PATH' not in globals():
    globals()['LOG_PATH'] = os.path.join(PROJECT_PATH, 'log')  # noqa

LOGGING = {}

CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
        'LONG_TIMEOUT': 86400,
        'TIMEOUT': 60,
    }
}

SANDBOX_TOKEN = 'FAKE_TOKEN'
SANDBOX_OWNER = 'FAKE_OWNER'
SANDBOX_API_URL = 'FAKE_URL'

TVM_FAKE = True
TVM_SERVICE_ID = 42

YASMS_DONT_SEND_ANYTHING = False
TRY_HARD_NEVER_SLEEP = True
REQUEST_LIMITER_NEVER_SLEEP = True
BACKEND_URL = None
MDS_URL = None
MDS_ENABLE_WRITING = False

# чтобы тесты не лезли за сертификатами
# есть проблемы с httpretty. Он как-то ломается при попытке достать сертификаты
os.environ.pop('REQUESTS_CA_BUNDLE', None)
REQUESTS_CA_BUNDLE = None

DBAAS_OAUTH_TOKEN = 'FAKE_DBAAS_TOKEN'

try:
    # TODO: #py23remove
    # Метод с TOPLEVEL settings.py использовать не следует.
    # Нужно переводить все проекты на абсолютные пути к тестовым настройкам.
    from tests_settings import *  # noqa
# Специфичных для тестов настроек может не быть
except ImportError:
    pass
