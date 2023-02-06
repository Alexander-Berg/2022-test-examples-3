# -*- coding: utf-8 -*-


from __future__ import unicode_literals


from django.apps import apps
from django.conf import settings
from django.db import connection


qn = connection.ops.quote_name


CONFIG = {
    'copy_models': [],
    'auto_create_objects': [],
    'after_init_tables': lambda: None
}


def setup_config():
    CONFIG['copy_models'].extend([])
    CONFIG['auto_create_objects'].extend([])


def setup_django():
    import django
    from django.test.utils import setup_test_environment
    django.setup()
    setup_test_environment()
    print('Setup django')


def pytest_configure():
    # нужно делать django.setup тут, чтобы coverage покрыл модули до импортов
    setup_django()
    setup_config()


def init_db():
    """ Create empty test db using db scheme from real db. """

    assert settings.REAL_DB_NAME != settings.TEST_DB_NAME

    try:
        # Два варинта, если кому—то не захочется пользоваться mysql_switcher
        if hasattr(connection, 'sync_db'):
            connection.db_names[connection.alias] = settings.REAL_DB_NAME
        else:
            settings.DATABASES['default']['NAME'] = settings.REAL_DB_NAME
        connection.close()

        try:
            with connection.cursor() as cursor:
                cursor.execute('DROP DATABASE IF EXISTS {}'.format(qn(settings.TEST_DB_NAME)))
                cursor.execute('CREATE DATABASE {db_name} DEFAULT CHARSET utf8;'.format(db_name=qn(settings.TEST_DB_NAME)))

        finally:
            cursor.close()

    finally:
        if hasattr(connection, 'sync_db'):
            connection.db_names[connection.alias] = settings.TEST_DB_NAME
        else:
            settings.DATABASES['default']['NAME'] = settings.TEST_DB_NAME
        connection.close()

    # Need to register real_db in our wrapper to be able to get cursor.
    cursor = connection.cursor()

    cursor.execute('SHOW TABLES FROM {};'.format(qn(settings.REAL_DB_NAME)))
    table_names = [row[0] for row in cursor.fetchall()]

    cursor.execute(
        u';\n'.join(
            "CREATE TABLE {table_name} LIKE {real_db_name}.{table_name}".format(
                table_name=qn(table_name),
                real_db_name=qn(settings.REAL_DB_NAME)
            ) for table_name in table_names
        ) + u';'
    )

    # Нужно закрыть курсор, чтобы этот запрос прошел
    cursor.close()
    connection.close()


def init_tables():
    """ Fill test db with some default data. """
    test_db_cursor = connection.cursor()

    def copy_table(table):
        query = 'INSERT {test_db}.{table} SELECT * FROM {real_db}.{table}'.format(
            real_db=qn(settings.REAL_DB_NAME),
            test_db=qn(settings.TEST_DB_NAME),
            table=table._meta.db_table
        )
        test_db_cursor.execute(query)

    for model in CONFIG['copy_models']:
        if isinstance(model, (tuple, list)):
            model = apps.get_model(*model)
        copy_table(model)

    for obj in CONFIG['auto_create_objects']:
        obj.save()

    connection.connection.commit()
    test_db_cursor.close()

    CONFIG['after_init_tables']()


def pytest_sessionstart(session):
    """ Initialization before all tests. """

    if session.config.option.rasp_reuse_db:
        print('Reuse Initialized Database')
        return

    print('Initialize database')

    reinit_db()

    print('Database initialization done')


def reinit_db():
    init_db()
    init_tables()


def pytest_addoption(parser):
    group = parser.getgroup('rasp')
    group.addoption('--rasp-reuse-db', action='store_true',
                    dest='rasp_reuse_db', default=False,
                    help=u'Не пересоздавать базу')
