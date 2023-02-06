# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from django.conf import settings

from travel.rasp.info_center.settings import *  # noqa
from travel.rasp.library.python.common23.settings.configuration import Configuration

Configuration().apply(globals())
import os
os.environ.setdefault('RASP_TEST_APPLIED_CONFIG', globals()['APPLIED_CONFIG'])

from travel.rasp.library.python.common23.tester.settings import *  # noqa

from travel.rasp.library.python.common23.settings.db import MONGO_DATABASES
MONGO_DATABASES[settings.SUBURBAN_NOTIFICATION_DATABASE_NAME] = MONGO_DATABASES['default']

ENABLE_INTERNAL_HANDLES = True
TVM_SERVICE_ID = 100
ROOT_URLCONF = 'travel.rasp.info_center.info_center.urls'
TEMPLATES = [
    {
        'BACKEND': 'library.python.django.template.backends.arcadia.ArcadiaTemplates',
        'OPTIONS': {
            'debug': False,
            'loaders': [
                'library.python.django.template.loaders.resource.Loader',
                'library.python.django.template.loaders.app_resource.Loader',
            ],
            'context_processors': [
                'django.contrib.auth.context_processors.auth',
                'django.template.context_processors.debug',
                'django.template.context_processors.media',
                'django.template.context_processors.request',
                'django.template.context_processors.static',
                'django.contrib.messages.context_processors.messages',
            ]
        },
    },
]
