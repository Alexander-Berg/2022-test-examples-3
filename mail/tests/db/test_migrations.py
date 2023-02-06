from mail.callmeback.devpack.components.db import CallmebackDb

from mail.devpack.tests.helpers.pg_helpers import template_migration_check


def test_migrations():
    template_migration_check(CallmebackDb)
