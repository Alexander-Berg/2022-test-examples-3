from mail.pg.mopsdb.devpack.components.mopsdb import MopsDb
from mail.devpack.tests.helpers.pg_helpers import template_migration_check


def test_migrations():
    template_migration_check(MopsDb)
