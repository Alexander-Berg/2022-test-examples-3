# coding: utf-8

from settings import *


import os
from datetime import datetime

from travel.rasp.library.python.common23.settings.utils import apply_switch_workflow
from travel.rasp.library.python.common23.settings.configuration import Configuration
from travel.rasp.library.python.common23.settings.configuration.base import BaseConfigurator


DEBUG = False
DJANGO_DEBUG = False

SEND_MAIL_TO_PARTNERS = False
EMAIL_HOST = 'localhost'

ENVIRONMENT_NOW = datetime(2017, 2, 1)

YANDEX_DATA_CENTER = 'xxx'

SCRIPTS_SOLOMON_PUSH_ENABLED = False


class IntegrationConfigurator(BaseConfigurator):
    def apply_development(self, settings):
        self._mysql_settings(settings)
        self._mongo_settings(settings)

    def _mysql_settings(self, settings):
        def create_db_settings(db_type):
            db_settings = {
                'ENGINE': 'common.db.backends.mysql',
                'NAME': os.environ['RASP_TESTS_MYSQL_NAME_{}'.format(db_type)],
                'HOST': os.environ['RASP_TESTS_MYSQL_HOST_{}'.format(db_type)],
                'PORT': int(os.environ['RASP_TESTS_MYSQL_PORT_{}'.format(db_type)]),
                'USER': os.environ['RASP_TESTS_MYSQL_USER_{}'.format(db_type)],
                'PASSWORD': os.environ['RASP_TESTS_MYSQL_PASSWORD_{}'.format(db_type)],
                'CLUSTER': {
                    'USE_MASTER': True,
                    'USE_REPLICAS': False,
                }
            }

            return db_settings

        apply_switch_workflow(
            settings,
            maint_conf=create_db_settings('MAINTENANCE'),
            main0_conf=create_db_settings('DB1'),
            main1_conf=create_db_settings('DB2'),
        )

    def _mongo_settings(self, settings):
        default_mongo = settings['MONGO_DATABASES']['default']
        default_mongo['host'] = os.getenv('RASP_TESTS_MONGO_HOST', '127.0.0.1')
        default_mongo['port'] = int(os.getenv('RASP_TESTS_MONGO_PORT', '27017'))
        default_mongo['db'] = os.environ['RASP_TESTS_MONGO_DB']


Configuration(ADMIN_CONFIGURATION_CLASSES + [IntegrationConfigurator]).apply(globals())

WORK_EXPORT_PATH = os.sep.join([PROJECT_PATH, 'www', 'db', 'scripts', 'export'])

LOG_PATH = os.path.join(PROJECT_PATH, 'log')
LOG_LEVEL = logging.DEBUG
TABLO_LOG_LEVEL = logging.DEBUG

import configure_logs
configure_logs.configure_logs(globals(), LOG_PATH, LOG_LEVEL_NAME)
