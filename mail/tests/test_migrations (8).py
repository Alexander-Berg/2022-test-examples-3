from mail.pg.queuedb.devpack.components.queuedb import QueueDb
from mail.devpack.tests.helpers.pg_helpers import template_migration_check


def test_migrations():
    template_migration_check(QueueDb)
