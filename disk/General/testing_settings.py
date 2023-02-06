# -*- coding: utf-8 -*-

from __future__ import unicode_literals

from settings import *  # noqa
from django_idm_api.settings import *

import yaml

DEBUG = True

ALLOWED_HOSTS = ['.yandex-team.ru']


with open('/etc/yandex/disk-secret-keys.yaml') as f:
    content = f.read()

data = yaml.load(content)
api_admin_section = data['api_admin']


database_section = api_admin_section['database']
DATABASE_PASSWORD = (
    'fSlK9zzh1IUONQ61ahzBpEJsSU1WVdHbYMBXsHUxMAiIiwxjUMMvjKKDgOkKBOU6'
)
DATABASE_USER = 'disk_api_admin_testing'
DATABASE_PORT = database_section['default']['port']
DATABASE_HOST = database_section['default']['host']
DATABASE_ENGINE = 'django.db.backends.postgresql'



DATABASES['default'] = {
    # основная база
    'ENGINE': DATABASE_ENGINE,
    'NAME': 'disk_api_admin_db_pgaas_testing',
    'USER': DATABASE_USER,
    'PASSWORD': DATABASE_PASSWORD,
    'HOST': DATABASE_HOST,
    'PORT': DATABASE_PORT,
}
DATABASES['ro_sync'] = {
    # синхронная реплика
    'ENGINE': DATABASE_ENGINE,
    'NAME': 'disk_api_admin_db_pgaas_testing_ro_sync',
    'USER': DATABASE_USER,
    'PASSWORD': DATABASE_PASSWORD,
    'HOST': DATABASE_HOST,
    'PORT': DATABASE_PORT,
}
DATABASES['ro_local'] = {
    # ближайшая база
    'ENGINE': DATABASE_ENGINE,
    'NAME': 'disk_api_admin_db_pgaas_testing_ro_local',
    'USER': DATABASE_USER,
    'PASSWORD': DATABASE_PASSWORD,
    'HOST': DATABASE_HOST,
    'PORT': DATABASE_PORT,
}


IDM_URL_PREFIX = 'testing/cloud-api/idm/'

tvm_section = api_admin_section['tvm']

IDM_API_TVM_SETTINGS = {
    'client_id': tvm_section['client_id'],
    'secret': tvm_section['secret_key'],
}

MIDDLEWARE_CLASSES.append('django_idm_api.middleware.TVMMiddleware')


ZOOKEEPER_ENVIRONMENT = 'testing'
ZOOKEEPER_PASSWORD = 'mGJcyi8Bd02nKqRb'

PROJECT_ROOT = '/usr/lib/python2.7/dist-packages/api_admin'

STATIC_ROOT = os.path.join(PROJECT_ROOT, 'collected_static')

FORCE_SCRIPT_NAME = '/testing/cloud-api/'

STATIC_URL = os.path.join(FORCE_SCRIPT_NAME, 'static') + '/'

USE_X_FORWARDED_HOST = True
