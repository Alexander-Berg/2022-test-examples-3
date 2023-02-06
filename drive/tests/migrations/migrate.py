import logging
import pgmigrate
import yatest.common

from dataclasses import dataclass
from drive.devops.cli.migrations.migrations import create_migration_directory
from maps.pylibs.local_postgres import Database

logger = logging.getLogger("test_logger")


@dataclass
class MigrateArgs:
    conn: str
    target: str
    base_dir: str


def _get_config(connection_string, migration_directory):
    return pgmigrate.get_config(migration_directory, MigrateArgs(
        conn=connection_string,
        target='latest',
        base_dir=migration_directory,
    ))


def create_database():
    database = Database.create_instance()
    database.execute_sql('CREATE EXTENSION IF NOT EXISTS pg_trgm')
    database.execute_sql('CREATE EXTENSION IF NOT EXISTS "uuid-ossp"')

    dump = yatest.common.binary_path('drive/tests/resources/extmaps-carsharing-testing-pg/extmaps-carsharing-testing.sql')
    with open(dump) as sql_dump:
        sql = sql_dump.read()
        database.execute_sql(sql)

    return database


def run_migration(database, migration_directory):
    def get_config():
        return _get_config(database.connection_string, migration_directory)

    pgmigrate.migrate(get_config())
    pgmigrate.info(get_config())


def test_migration():
    database = create_database()
    migration_directory = yatest.common.source_path('drive/backend')
    run_migration(database, migration_directory)


def test_cli_migration():
    database = create_database()
    migration_directory = create_migration_directory(yatest.common.work_path())
    run_migration(database, migration_directory)
