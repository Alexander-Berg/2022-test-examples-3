# -*- coding: utf-8 -*-
import os

import django

os.environ['DJANGO_SETTINGS_MODULE'] = 'travel.avia.ticket_daemon_processing.tests.tests_settings'
django.setup()

pytest_plugins = [
    'travel.avia.library.python.tester.initializer',
    'travel.avia.library.python.tester.plugins.transaction',
]
