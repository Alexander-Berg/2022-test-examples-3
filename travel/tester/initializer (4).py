# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os
import subprocess
from tempfile import NamedTemporaryFile

import six
from django.apps import apps
from django.conf import settings
from django.contrib.auth.management import create_permissions
from django.core.management import call_command
from django.db import connection

from travel.rasp.library.python.common23.tester import hacks


CONFIG = {
    'after_init_tables': lambda: None
}


def setup_django():
    import os
    import django
    from django.test.utils import setup_test_environment
    os.environ['RASP_MIGRATION_ALLOWED'] = 'true'
    django.setup()
    setup_test_environment()
    print('Setup django')


def prepare_mysql_settings():
    for key in list(settings.DATABASES.keys()):
        if key != 'default':
            del settings.DATABASES[key]

    assert not getattr(settings, 'REAL_DB_NAME', None)
    assert settings.TEST_DB_NAME
    assert settings.DATABASES['default']['HOST'] in settings.ALLOWED_TEST_DB_HOSTS
    assert settings.DATABASES['default']['NAME']
    settings.REAL_DB_NAME = settings.DATABASES['default']['NAME']
    assert settings.TEST_DB_NAME != settings.REAL_DB_NAME
    settings.DATABASES['default']['NAME'] = settings.TEST_DB_NAME

    # тесты могут делать более долгие запросы
    settings.MYSQL_READ_TIMEOUT = settings.MYSQL_WRITE_TIMEOUT = 600


def prepare_mongo_settings():
    for db_conf in settings.MONGO_DATABASES.values():
        assert db_conf.get('host', 'localhost') in settings.ALLOWED_TEST_DB_HOSTS

        db_name = db_conf['db'] + os.getenv('RASP_MONGO_RECIPE_DB_NAME_SUFFIX', '_test')
        db_conf.setdefault('TEST', {}).setdefault('db', db_name)
        port = os.getenv('RASP_MONGO_RECIPE_PORT')
        if port:
            db_conf['TEST'].setdefault('port', int(port))

        # в тестах не нужны таймауты к базе
        if 'socketTimeoutMS' not in db_conf['TEST'].get('options', {}):
            db_conf['TEST'].setdefault('options', {})['socketTimeoutMS'] = None

        db_conf.update(db_conf['TEST'])


def prepare_geobase_settings():
    geodata_path = '{}/geodata/geodata4-tree+ling.bin'.format(os.getcwd())  # common_recipe/recipe_lib.inc => geodata
    if 'RASP_GEOBASE_DATA_PATH' not in os.environ and os.path.exists(geodata_path):
        os.environ['RASP_GEOBASE_DATA_PATH'] = geodata_path


def prepare_settings():
    prepare_mysql_settings()
    prepare_mongo_settings()
    prepare_geobase_settings()


def pytest_configure():
    # нужно делать django.setup тут, чтобы coverage покрыл модули до импортов
    prepare_settings()
    setup_django()


def clone_db_schema():
    """ Create empty test db using db scheme from real db. """
    try:
        with NamedTemporaryFile() as temp_file:
            connection.ensure_connection()
            db_user = connection.connection.conn_params['user']
            db_host = connection.connection.conn_params['host']
            db_password = connection.connection.conn_params['passwd']

            options = '--create-options --no-data --column-statistics=0'

            subprocess.check_call(
                'mysqldump -u {user} -h {host} --password={password} {options} {db_name} > {output_file}'.format(
                    user=db_user,
                    host=db_host,
                    password=db_password,
                    db_name=settings.REAL_DB_NAME,
                    output_file=temp_file.name,
                    options=options
                ),
                shell=True
            )

            qn = connection.ops.quote_name
            with connection.cursor() as cursor:
                cursor.execute('DROP DATABASE IF EXISTS {}'.format(qn(settings.TEST_DB_NAME)))
                cursor.execute('CREATE DATABASE {} DEFAULT CHARSET utf8;'.format(qn(settings.TEST_DB_NAME)))

            subprocess.check_call(
                'cat {output_file} |'
                ' mysql -u {user} -h {host} --password={password} {db_name}'.format(
                    user=db_user,
                    host=db_host,
                    password=db_password,
                    db_name=settings.TEST_DB_NAME,
                    output_file=temp_file.name
                ),
                shell=True
            )

    finally:
        connection.close()


def init_tables():
    """ Fill test db with some default data. """

    call_command('loadraspinitialdata', verbosity=1)

    try:
        from django.contrib.contenttypes.management import update_contenttypes as create_contenttypes
    except ImportError:
        # Django 1.11
        from django.contrib.contenttypes.management import create_contenttypes

    for app_config in apps.get_app_configs():
        create_contenttypes(app_config, verbosity=0, interactive=False)
        create_permissions(app_config, verbosity=0, interactive=False)

    CONFIG['after_init_tables']()


def debug_print(msg):
    import os
    from datetime import datetime

    with open('/tmp/pytest_sessionstart.txt', 'a') as f:
        f.writelines(["{} {} {} {}\n".format(
            os.getppid(),
            os.getpid(),
            datetime.now(),
            msg,
        )])


def pytest_sessionstart(session):
    """ Initialization before all tests. """
    debug_print('')

    if six.PY3:
        hacks.disable_httpretty_for_db_connection()

    if session.config.option.rasp_reuse_db:
        print('Reuse Initialized Database')
        return

    print('Initialize database')
    if getattr(session.config.option, 'rasp_reuse_db_schema', False):
        print('Reuse db schema')
    else:
        clone_db_schema()
    init_tables()
    print('Database initialization done')


def pytest_sessionfinish(session, exitstatus):
    debug_print('END')


def reinit_db():
    call_command('flush', verbosity=1, interactive=False, load_initial_data=True)


def pytest_addoption(parser):
    group = parser.getgroup('rasp')
    group.addoption('--rasp-reuse-db', action='store_true',
                    dest='rasp_reuse_db', default=False,
                    help=u'Не пересоздавать базу')
