from mail.devpack.tests.helpers.pg_helpers import template_migration_check
from mail.pg.huskydb.devpack.components.huskydb import HuskyDb


def test_migrations():
    template_migration_check(HuskyDb)
