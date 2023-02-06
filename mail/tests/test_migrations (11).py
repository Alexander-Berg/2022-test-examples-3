from mail.pg.spanieldb.devpack.components.spanieldb import SpanielDb
from mail.devpack.tests.helpers.pg_helpers import template_migration_check


def test_migrations():
    template_migration_check(SpanielDb)
