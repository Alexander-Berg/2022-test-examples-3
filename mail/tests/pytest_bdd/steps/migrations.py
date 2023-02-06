# coding: utf-8

from pymdb.operations import Migration
from tests_common.pytest_bdd import when


@when(u'we apply "{migration:w}" migration')
def step_apply_migration(context, migration):
    OurMigration = Migration.get_migration_by_name(migration)
    context.apply_op(OurMigration)
