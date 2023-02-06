# coding: utf-8

from mail.pypg.pypg.common import fetch_as_dicts
from tests_common.pytest_bdd import then


def get_table_data(conn, table_name, uid):
    query = "SELECT * FROM {table_name} WHERE uid=%s".format(
        table_name=table_name)
    cur = conn.cursor()
    cur.execute(query, [uid])
    conn.wait()
    return list(fetch_as_dicts(cur))


def get_table_names(context, one_table_name):
    if one_table_name:
        return [one_table_name]
    return [r['table_name'] for r in context.table]


def check_tables_not_empty(context, one_table_name, not_empty, message):
    for table_name in get_table_names(context, one_table_name):
        table_data = get_table_data(
            context.conn,
            table_name=table_name,
            uid=context.uid
        )
        assert bool(len(table_data)) == not_empty, \
            message.format(
                table_name=table_name,
                table_data=table_data)


@then(u'in this tables there are no his data')
def step_tables_empty(context):
    check_tables_not_empty(
        context,
        one_table_name=None,
        not_empty=False,
        message='Found user data in {table_name}: {table_data!r}')


@then(u'in table "{one_table_name:TableName}" there are no his data')
def step_target_table_empty(context, one_table_name):
    check_tables_not_empty(
        context,
        one_table_name=one_table_name,
        not_empty=False,
        message='Found user data in {table_name}: {table_data!r}')


@then(u'in this tables his data exists')
def step_tables_not_empty(context):
    check_tables_not_empty(
        context,
        one_table_name=None,
        not_empty=True,
        message='Data in {table_name} not found')


@then(u'in table "{one_table_name:TableName}" his data exists')
def step_target_table_not_empty(context, one_table_name):
    check_tables_not_empty(
        context,
        one_table_name=one_table_name,
        not_empty=True,
        message='Data in {table_name} not found')
