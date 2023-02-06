import os

import django

os.environ['DJANGO_SETTINGS_MODULE'] = 'travel.avia.library.python.avia_data.tests.tests_settings'

pytest_plugins = [
    b'travel.avia.library.python.tester.plugins.transaction',
    b'travel.avia.library.python.tester.utils.language_activator',
]

django.setup()
