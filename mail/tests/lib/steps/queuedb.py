from tests_common.pytest_bdd import given, when, then
from mail.pypg.pypg.common import transaction

from .mdb_actions import step_some_partitions, step_drop_partitions, step_check_partitions


def get_dsn(context):
    return context.config['queuedb']


@given('there are some partitions for "{table}" in queuedb')
def step_some_partitions_in_huskydb(context, table):
    with transaction(get_dsn(context)) as conn:
        step_some_partitions(context, conn, table)


@when('we drop "{count:d}" partitions for "{table}" in queuedb')
def step_drop_partitions_in_huskydb(context, table, count):
    with transaction(get_dsn(context)) as conn:
        step_drop_partitions(conn, table, count)


@then('there are the same partitions for "{table}" in queuedb')
def step_check_partitions_in_huskydb(context, table):
    with transaction(get_dsn(context)) as conn:
        step_check_partitions(context, conn, table)
