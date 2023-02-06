from mail.pg.auditlogdb.devpack.components.auditlogdb import AuditlogDb
from mail.devpack.tests.helpers.pg_helpers import template_migration_check


def test_migrations():
    template_migration_check(AuditlogDb)
