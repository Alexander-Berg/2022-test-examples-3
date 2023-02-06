from mail.devpack.lib.components.sharddb import ShardDb
from mail.devpack.tests.helpers.pg_helpers import template_migration_check


def test_migrations():
    template_migration_check(ShardDb)
