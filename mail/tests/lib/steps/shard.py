from tests_common.pytest_bdd import when, then
from time import sleep
import json as jsn


@then(u'shiva responds ok')
def step_then_shiva_responds_ok(context):
    assert context.response.status_code == 200, \
        'Expected: status_code 200, but was: "{}"'.format(context.response.status_code)


@when('we make cleanup_doomed request')
def step_when_cleanup_doomed(context):
    context.response = context.shiva.client().shard().cleanup_doomed(shard_id=context.config["shard_id"])


@when('we make pg_partman_maintenance request for shard')
def step_when_pg_partman_maintenance_for_shard(context):
    context.response = context.shiva.client().shard().pg_partman_maintenance(shard_id=context.config["shard_id"])


@when('we make pg_partman_maintenance request for huskydb')
def step_when_pg_partman_maintenance_for_huskydb(context):
    context.response = context.shiva.client().huskydb().pg_partman_maintenance()


@when('we make pg_partman_maintenance request for queuedb')
def step_when_pg_partman_maintenance_for_queuedb(context):
    context.response = context.shiva.client().queuedb().pg_partman_maintenance()


@when('we make end_prepared_transaction request')
def step_when_end_prepared_transaction(context):
    context.response = context.shiva.client().shard().end_prepared_transaction(
        shard_id=context.config["shard_id"],
        ttl_sc=0)


@when('we make init_pop3_folder request')
def step_when_init_pop3_folders(context):
    context.response = context.shiva.client().shard().init_pop3_folder(shard_id=context.config["shard_id"])


@when('we make purge_chained_log request')
def step_when_purge_chained_log(context):
    context.response = context.shiva.client().shard().purge_chained_log(shard_id=context.config["shard_id"])


@when('we make update_mailbox_size request')
def step_when_update_mailbox_size(context):
    context.response = context.shiva.client().shard().update_mailbox_size(shard_id=context.config["shard_id"])


@when('we make purge_transferred_user request')
def step_when_purge_transferred_user(context):
    context.response = context.shiva.client().shard().purge_transferred_user(shard_id=context.config["shard_id"])


@when('we make purge_deleted_user request')
def step_when_purge_deleted_user(context):
    context.response = context.shiva.client().shard().purge_deleted_user(shard_id=context.config["shard_id"])


@when('we make purge_deleted_user request with force flag')
def step_when_purge_deleted_user_with_force(context):
    context.response = context.shiva.client().shard().purge_deleted_user(shard_id=context.config["shard_id"], force=True)


@when('we make purge_transferred_user request with ttl_days "{ttl_days}"')
def step_when_purge_transferred_user_with_ttl_days(context, ttl_days):
    context.response = context.shiva.client().shard().purge_transferred_user(
        shard_id=context.config["shard_id"],
        ttl_days=ttl_days)


@when('we make purge_deleted_user request with ttl_days "{ttl_days}"')
def step_when_purge_deleted_user_with_ttl_days(context, ttl_days):
    context.response = context.shiva.client().shard().purge_deleted_user(
        shard_id=context.config["shard_id"],
        ttl_days=ttl_days)


@when('we make purge_transferred_user request with jobs_count "{jobs_count}" and job_no "{job_no}"')
def step_when_purge_transferred_user_with_job_no(context, jobs_count, job_no):
    context.response = context.shiva.client().shard().purge_transferred_user(
        shard_id=context.config["shard_id"],
        jobs_count=jobs_count,
        job_no=job_no)


@when('we make purge_deleted_box request')
def step_when_purge_deleted_box(context):
    context.response = context.shiva.client().shard().purge_deleted_box(shard_id=context.config["shard_id"])


@when('we make purge_deleted_box request with ttl_days "{ttl_days}"')
def step_when_purge_deleted_box_with_ttl_days(context, ttl_days):
    context.response = context.shiva.client().shard().purge_deleted_box(
        shard_id=context.config["shard_id"],
        ttl_days=ttl_days)


@when('we make purge_deleted_box request for all shiva shards with ttl_days "{ttl_days}"')
def step_when_maildb_purge_deleted_box_with_ttl_days(context, ttl_days):
    context.response = context.shiva.client().maildb().purge_deleted_box(ttl_days=ttl_days)


@when('we make purge_synced_deleted_box request')
def step_when_purge_synced_deleted_box(context):
    context.response = context.shiva.client().shard().purge_synced_deleted_box(shard_id=context.config["shard_id"])


@when('we make folder_archivation request')
def step_when_folder_archivation(context):
    context.response = context.shiva.client().shard().folder_archivation(shard_id=context.config["shard_id"])


@when('we make purge_storage request')
def step_when_purge_storage(context):
    context.response = context.shiva.client().shard().purge_storage(shard_id=context.config["shard_id"], max_delay=0)


@when('we make purge_storage request with ttl_days "{ttl_days}"')
def step_when_purge_storage_with_ttl_days(context, ttl_days):
    context.response = context.shiva.client().shard().purge_storage(
        shard_id=context.config["shard_id"],
        max_delay=0,
        ttl_days=ttl_days)


@when('we make purge_storage request with jobs_count "{jobs_count}" and job_no "{job_no}"')
def step_when_purge_storage_with_job_no(context, jobs_count, job_no):
    context.response = context.shiva.client().shard().purge_storage(
        shard_id=context.config["shard_id"],
        max_delay=0,
        jobs_count=jobs_count,
        job_no=job_no)


@when('we make space_balancer request')
def step_when_space_balancer(context):
    context.response = context.shiva.client().shard().space_balancer(shard_id=context.config["shard_id"])


@when('we make space_balancer request with "{load_type}" load_type')
def step_when_space_balancer_with_load_type(context, load_type):
    context.response = context.shiva.client().shard().space_balancer(
        shard_id=context.config["shard_id"],
        load_type=load_type,
    )


@when('we make space_balancer request for all shiva shards')
def step_when_maildb_space_balancer(context):
    context.response = context.shiva.client().maildb().space_balancer()


@when('we make space_balancer request for all shiva shards with task args {task_args}')
def step_when_maildb_space_balancer_with_task_args(context, task_args):
    context.response = context.shiva.client().maildb().space_balancer(task_args=jsn.loads(task_args))


@when('we make transfer_users request')
def step_when_transfer_users(context):
    context.response = context.shiva.client().shard().transfer_users(shard_id=context.config["shard_id"])


@when('we make transfer_users request with min_messages_per_user "{min_count:d}" and max_messages_per_user "{max_count:d}"')
def step_when_transfer_users_with_msg_limits(context, min_count, max_count):
    context.response = context.shiva.client().shard().transfer_users(
        shard_id=context.config["shard_id"],
        min_messages_per_user=min_count,
        max_messages_per_user=max_count,
    )


@when('we make transfer_active_users request')
def step_when_transfer_active_users(context):
    context.response = context.shiva.client().shard().transfer_active_users(shard_id=context.config["shard_id"])


@when('we make close_for_load request')
def step_when_close_for_load(context):
    sleep(3)
    context.response = context.shiva.client().shard().close_for_load(shard_id=context.config["shard_id"], max_delay=0)


@when('we make purge_backups request')
def step_when_purge_backups(context):
    context.response = context.shiva.client().shard().purge_backups(shard_id=context.config["shard_id"])


@when('we make purge_backups request with ttl_days "{ttl_days}"')
def step_when_purge_backups_with_ttl_days(context, ttl_days):
    context.response = context.shiva.client().shard().purge_backups(
        shard_id=context.config["shard_id"],
        ttl_days=ttl_days)


@when('we make settings_export request')
def step_when_settings_export(context):
    context.response = context.shiva.client().shard().settings_export(shard_id=context.config["shard_id"], max_delay=0)


@when('we make reactivate_users request')
def step_when_reactivate_users(context):
    context.response = context.shiva.client().shard().reactivate_users(shard_id=context.config["shard_id"], max_delay=0)


@when('we make deactivate_users request')
def step_when_deactivate_users(context):
    context.response = context.shiva.client().shard().deactivate_users(shard_id=context.config["shard_id"], max_delay=0)


@when('we make start_freezing_users request')
def step_when_start_freezing_users(context):
    context.response = context.shiva.client().shard().start_freezing_users(shard_id=context.config["shard_id"])


@when('we make notify_users request')
def step_when_notify_users(context):
    context.response = context.shiva.client().shard().notify_users(shard_id=context.config["shard_id"], max_delay=0)


@when('we make freeze_users request')
def step_when_freeze_users(context):
    context.response = context.shiva.client().shard().freeze_users(shard_id=context.config["shard_id"], max_delay=0)


@when('we make archive_users request')
def step_when_archive_users(context):
    context.response = context.shiva.client().shard().archive_users(shard_id=context.config["shard_id"], max_delay=0)


@when('we make purge_archives request')
def step_when_purge_archives(context):
    context.response = context.shiva.client().shard().purge_archives(shard_id=context.config["shard_id"], max_delay=0)


@when('we make clean_archives request')
def step_when_clean_archives(context):
    context.response = context.shiva.client().shard().clean_archives(shard_id=context.config["shard_id"], max_delay=0)


@when('we make clean_existing_archives request')
def step_when_clean_existing_archives(context):
    context.response = context.shiva.client().shard().onetime_task(
        shard_id=context.config["shard_id"],
        subtask='clean_existing_archives',
        max_delay=0,
    )


@when('we make pnl_estimation_export request')
def step_when_pnl_estimation_export(context):
    context.response = context.shiva.client().shard().pnl_estimation_export(shard_id=context.config["shard_id"], max_delay=0)


@when('we make clean request for callmebackdb')
def step_when_clean_for_callmebackdb(context):
    context.response = context.shiva.client().callmebackdb().clean()


@when('we make clean request for callmebackdb with ttl_days "{ttl_days}"')
def step_when_clean_for_callmebackdb_with_ttl_days(context, ttl_days):
    context.response = context.shiva.client().callmebackdb().clean(ttl_days=ttl_days)
