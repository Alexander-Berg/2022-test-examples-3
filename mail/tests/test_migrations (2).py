from mail.pg.cachedb.devpack.cachedb import CacheDb
from mail.devpack.tests.helpers.pg_helpers import template_migration_check


def test_migrations():
    template_migration_check(CacheDb)
