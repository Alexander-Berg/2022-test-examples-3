# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import copy

from django.core.management import call_command
from django.conf import settings
from django.db import connection
from dump_databases import recreate
from contextlib import closing

import management.commands.loadinitialdata as loadinitialdata


def pytest_configure():
    # нужно делать django.setup тут, чтобы coverage покрыл модули до импортов
    import django
    from django.test.utils import setup_test_environment
    django.setup()
    setup_test_environment()
    print('Setup django')


def pytest_sessionstart(session):
    """ Initialization before all tests. """

    if session.config.option.rasp_reuse_db:
        print('Reuse Initialized Database')
        return

    print('Initialize database')
    reinit_db()
    init_tables()
    print('Database initialization done')


def init_tables():
    """ Fill test db with some default data. """
    call_command(loadinitialdata.Command(), verbosity=1)


def reinit_db():
    with closing(connection):
        assert settings.REAL_DB_NAME != settings.TEST_DB_NAME

        test_settings = copy.deepcopy(settings.DATABASES['default'])
        test_settings['NAME'] = settings.TEST_DB_NAME
        source_settings = copy.deepcopy(settings.DATABASES['default'])
        source_settings['NAME'] = settings.REAL_DB_NAME
        recreate(test_settings, source_settings)


def pytest_addoption(parser):
    group = parser.getgroup('rasp')
    group.addoption('--rasp-reuse-db', action='store_true',
                    dest='rasp_reuse_db', default=False,
                    help=u'Не пересоздавать базу')
