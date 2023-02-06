from mail.devpack.tests.helpers.pg_helpers import check_migrations


def test_migrations(mdb):
    check_migrations(
        db=mdb.shard.master,
        users=mdb.users,
        partition_tables=['mail.doberman_jobs_change_log', 'mail.change_log', 'contacts.change_log',
                          'contacts.equivalent_revisions'],
        before_all_prefixes=mdb.before_all_prefixes,
        after_all_prefixes=mdb.after_all_prefixes,
        snapshot_sql_files=mdb.snapshot_sql_files,
        migration_prefixes=mdb.migration_prefixes,
    )
