from mail.collie.devpack.components.colliedb import CollieDb
from mail.devpack.tests.helpers.pg_helpers import template_migration_check


def test_migrations():
    template_migration_check(CollieDb)
