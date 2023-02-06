# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

try:
    from library.python.django import patch
    patch()
except ImportError:
    pass

import os

from travel.rasp.library.python.common23.tester import hacks


hacks.apply_format_explanation()
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'travel.rasp.library.python.common23.tester.full_settings.settings')
os.environ.setdefault('RASP_VAULT_STUB_SECRETS', '1')

DISABLED_PLUGINS_TESTS_NAMES = [
    'sandbox/projects/tests/bin',
    'sandbox/tasks/tests'
]

if not any(n in os.getcwd() for n in DISABLED_PLUGINS_TESTS_NAMES):
    pytest_plugins = [
        'travel.rasp.library.python.common23.tester.initializer',
        'travel.rasp.library.python.common23.tester.plugins.transaction',
        'travel.rasp.library.python.common23.tester.plugins.translation',
        'travel.rasp.library.python.common23.tester.utils.language_activator',
        'travel.rasp.library.python.common23.tester.plugins.http',
    ]

    try:
        from travel.rasp.library.python.common23.tester import plugins_models
        pytest_plugins += plugins_models.plugins
    except ImportError:
        pass

    try:
        from travel.rasp.library.python.common23.db.mongo.tester import plugins  # noqa
        pytest_plugins += ['travel.rasp.library.python.common23.db.mongo.tester.plugins']
    except ImportError:
        pass


def pytest_addoption(parser):
    try:
        group = parser.getgroup('rasp')

        group.addoption('--rasp-reuse-db-schema', action='store_true',
                        dest='rasp_reuse_db_schema', default=True,
                        help=u'Не пересоздавать базу, только заполнить initial data')

        group.addoption('--no-rasp-reuse-db-schema', action='store_false',
                        dest='rasp_reuse_db_schema', default=False,
                        help=u'Пересоздавать базу')
    except ValueError:
        print('Attempt to add --rasp-reuse-db-schema more then once')
