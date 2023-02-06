# coding: utf-8

from behave import given, then, when
from contextlib import closing
import subprocess
import os.path
import json
import re

import logging

log = logging.getLogger(__name__)


@given(u'DB name is "{schema_name:w}"')
def step_set_dbname(context, schema_name):
    context.schema_name = schema_name
    context.DB = context.schema_name + '_tests'
    context.DSN = 'host={host} dbname={dbname}'.format(
            host=context.PG_HOST,
            dbname=context.DB
    )
    log.info('test db dsn: "{0}"'.format(context.DSN))
    context.DB_DIR = os.path.join(context.BASE_DIR, context.schema_name)
    log.info('test db directory: "{0}"'.format(context.DB_DIR))


def make_new_db(context):
    assert hasattr(context, 'DB'), 'context.DB field required'
    with closing(context.make_connect('postgres')) as pg_conn:
        pg_conn.autocommit = True
        cur = pg_conn.cursor()
        cur.execute(
            'SELECT EXISTS (SELECT 1 FROM pg_database WHERE datname=%s)',
            (context.DB,)
        )
        if cur.fetchone()[0]:
            cur.execute('DROP DATABASE %s' % context.DB)
        cur.execute('CREATE DATABASE %s' % context.DB)


@given(u'new DB')
def step_new_db(context):
    make_new_db(context)


def migrate_to(context, target, apply_callbacks=False):
    cmd = [
        'python',
        '/usr/local/yandex/pgmigrate/pgmigrate.py',
        '-t',
        str(target),
        '-d',
        context.DB_DIR,
        '-c',
        context.DSN
    ]
    if apply_callbacks:
        cmd += [
            '-a',
            'afterAll:{dir}/code'.format(dir=context.DB_DIR)
        ]
    cmd.append('migrate')

    exitcode = subprocess.call(cmd)
    assert exitcode == 0, 'Migrate failed with error: {0}'.format(exitcode)


def check_migrate(dsn, target):
    args = [
        'python',
        '/usr/local/yandex/pgmigrate/pgmigrate.py',
        '-c',
        dsn,
        'info',
    ]
    log.info('Execute %r', subprocess.list2cmdline(args))
    cmd = subprocess.Popen(args, stdout=subprocess.PIPE)
    out, err = cmd.communicate()

    assert not err, 'Expected empty stderr, but found: "{0}"'.format(err)
    assert cmd.returncode == 0, 'Migration version check failed with error: ' \
                                '{0}'.format(cmd.returncode)

    output = json.loads(out)
    last_migration = str(max(int(x) for x in output.iterkeys()))
    assert output[last_migration]["version"] == target, \
        'Expected schema version: "{exp}", but was: "{act}"'.format(
            exp=target,
            act=output["1"]["version"],
        )


def get_last_migration(context):
    migration_files = os.listdir(
        os.path.join(
            context.DB_DIR,
            'migrations',
        )
    )
    pattern = re.compile("^V(?P<version>\d+)")
    versions = [int(pattern.match(file).group('version'))
                for file in migration_files]
    version = max(versions)
    log.info('Last migration file has version: "{version}"'.format(
        version=version
    ))
    return version


@given(u'DB version is {version:d}')
def step_init(context, version):
    make_new_db(context)
    migrate_to(context, version)


@given(u'migration version is {target:d}')
def step_set_target(context, target):
    context.target = target


@when(u'we migrate')
def step_migrate(context):
    migrate_to(context, context.target, apply_callbacks=True)


@when(u'we migrate to newest version')
def step_migrate(context):
    context.target = get_last_migration(context)
    migrate_to(context, context.target, apply_callbacks=True)


@then(u'no error is produced')
def step_no_error(context):
    check_migrate(context.DSN, context.target)

