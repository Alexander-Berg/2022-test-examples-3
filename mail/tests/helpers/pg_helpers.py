import difflib

from mail.devpack.lib.components.postgres import Postgres
from mail.devpack.tests.helpers.fixtures import coordinator_factory


def template_migration_check(db_comp_cls, partition_tables=None):
    with coordinator_factory(db_comp_cls) as coord:
        return check_migrations_simple(coord.components[db_comp_cls], partition_tables=partition_tables)


def check_migrations_simple(db, partition_tables=None):
    """
    :type db: Postgres
    :type partition_tables: list[str]
    """
    return check_migrations(
        db.pg,
        users=db.users,
        partition_tables=partition_tables,
        before_all_prefixes=db.before_all_prefixes,
        after_all_prefixes=db.after_all_prefixes,
        snapshot_sql_files=db.snapshot_sql_files,
        migration_prefixes=db.migration_prefixes,
    )


def check_migrations(db, users, partition_tables,
                     before_all_prefixes, after_all_prefixes, snapshot_sql_files, migration_prefixes):
    db_snapshot = Postgres(
        dbname=db.dbname + '_from_snapshot',
        port=db.port + 1,
        users=users,
        ddl_prefixes=before_all_prefixes + snapshot_sql_files + after_all_prefixes,
        root=db.root,
    )
    db_migrations = Postgres(
        dbname=db.dbname + '_from_migrations',
        port=db.port + 2,
        users=users,
        ddl_prefixes=before_all_prefixes + migration_prefixes + after_all_prefixes,
        root=db.root,
    )

    with db_snapshot.standalone(), db_migrations.standalone():
        delta = compare_db_dumps(
            db_snapshot.pg,
            db_migrations.pg,
            partition_tables=partition_tables
        )
        assert not delta, '\n' + delta


def compare_db_dumps(db1, db2, partition_tables=None):
    """
    :type db1: Postgresql
    :type db2: Postgresql
    :type partition_tables: list
    :return: dumped databases diff
    """
    if partition_tables is None:
        partition_tables = []
    dump1 = db1.dump(partition_tables=partition_tables).split('\n')
    dump2 = db2.dump(partition_tables=partition_tables).split('\n')
    return '\n'.join(difflib.context_diff(dump1, dump2, db1.dbname, db2.dbname, n=5, lineterm=''))
