from mail.pg.furitadb.devpack.components.furitadb import FuritaDb
from mail.devpack.tests.helpers.pg_helpers import template_migration_check


def test_migrations():
    template_migration_check(FuritaDb)
