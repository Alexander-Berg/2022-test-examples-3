# -*- coding: utf-8 -*-
import os

import django

os.environ['DJANGO_SETTINGS_MODULE'] = 'travel.avia.ticket_daemon_api.tests.tests_settings'
django.setup()

from travel.avia.library.python.tester import hacks
hacks.apply_format_explanation()

pytest_plugins = [
    'travel.avia.library.python.tester.initializer',
    'travel.avia.library.python.tester.plugins.transaction',
]
