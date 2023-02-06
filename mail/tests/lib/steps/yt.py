import datetime

from parse_type import TypeBuilder
from tests_common.pytest_bdd import given, then, BehaveParser
from hamcrest import (
    assert_that,
    has_properties,
    contains_inanyorder,
    has_entries,
    is_not,
    equal_to
)

FREEZING_TABLE_ROOT = '//home/mail-logs/core/mdb/freezing'

SETTINGS_TABLE_ROOT = '//home/mail-logs/mail-settings'
SETTINGS_PUBLIC_FIELD_LIST_PATH = '//home/mail-logs/mail-settings/field_list_public.txt'
SETTINGS_PRIVATE_FIELD_LIST_PATH = '//home/mail-logs/mail-settings/field_list_private.txt'

PNL_ESTIMATION_TABLE_ROOT = '//home/mail-logs/core/mdb/pnl-data'

YT_TABLE_TYPES = ['settings', 'pnl deleted estimation', 'pnl mailbox estimation']

BehaveParser.extra_types.update(dict(YtTable=TypeBuilder.make_choice(YT_TABLE_TYPES)))


@given('there are no tables in yt for settings')
def step_clear_yt_settings(context):
    context.yt.remove(SETTINGS_TABLE_ROOT, recursive=True, force=True)


@given('there are file with public fields for settings')
def step_make_public_fields_file_settings(context):
    context.yt.create('file', SETTINGS_PUBLIC_FIELD_LIST_PATH, recursive=True)
    context.yt.write_file(SETTINGS_PUBLIC_FIELD_LIST_PATH, context.text.encode('utf-8'))


@given('there are file with private fields for settings')
def step_make_private_fields_file_settings(context):
    context.yt.create('file', SETTINGS_PRIVATE_FIELD_LIST_PATH, recursive=True)
    context.yt.write_file(SETTINGS_PRIVATE_FIELD_LIST_PATH, context.text.encode('utf-8'))


@given('there are no tables in yt for pnl estimation')
def step_clear_yt_mailbox(context):
    context.yt.remove(PNL_ESTIMATION_TABLE_ROOT, recursive=True, force=True)


def get_table_name(context, table):
    date_prefix = datetime.datetime.today().strftime('%Y-%m-%d')
    path = {
        'settings': '{root}/{date}'.format(root=SETTINGS_TABLE_ROOT, date=date_prefix),
        'pnl deleted estimation': '{root}/{date}/deleted'.format(root=PNL_ESTIMATION_TABLE_ROOT, date=date_prefix),
        'pnl mailbox estimation': '{root}/{date}/mailbox'.format(root=PNL_ESTIMATION_TABLE_ROOT, date=date_prefix),
    }.get(table, '')
    return '{path}/{shard}'.format(path=path, shard=context.config['shard_id'])


def check_table_schema(context, table, check_empty):
    table_name = get_table_name(context, table)
    assert context.yt.exists(table_name)
    if check_empty:
        assert context.yt.row_count(table_name) == 0

    if context.table:
        expected_fields = [has_entries(f) for f in context.table.to_dicts()]
        schema = context.yt.get('{table}/@schema'.format(table=table_name))
        assert_that(schema, has_properties(attributes=has_entries({'strict': False})))
        assert_that(schema, contains_inanyorder(*expected_fields))


@then('there are empty shard table in yt for {table:YtTable}')
@then('there are empty shard table in yt for {table:YtTable} with schema')
def step_check_empty_table_schema(context, table):
    check_table_schema(context, table, check_empty=True)


@then('there are shard table in yt for {table:YtTable}')
@then('there are shard table in yt for {table:YtTable} with schema')
def step_check_table_schema(context, table):
    check_table_schema(context, table, check_empty=False)


def check_table(context, table, count, rows):
    table_name = get_table_name(context, table)
    assert context.yt.exists(table_name)
    assert context.yt.row_count(table_name) == count

    def row_matcher(row):
        result = {}
        for k, v in row.items():
            matcher = equal_to

            if v.startswith('!'):
                matcher = is_not
                v = v[1:]

            if v.startswith('$'):
                v = context.config.get(v[1:], 'null')

            try:
                v = int(v)
            except ValueError:
                pass

            if v == 'null':
                result[k] = matcher(None)
            elif v == 'true':
                result[k] = matcher(True)
            elif v == 'false':
                result[k] = matcher(False)
            elif k == 'uid':
                result[k] = matcher(context.users[v].uid)
            else:
                result[k] = matcher(v)

        return has_entries(result)

    expected_data = [row_matcher(r) for r in rows]
    data = list(context.yt.read_table(table_name))
    assert_that(data, contains_inanyorder(*expected_data))


@then('there are shard table in yt for {table:YtTable} with {count:d} rows')
def step_check_table(context, table, count):
    check_table(context, table, count, context.table.to_dicts())


@then('there are shard table in yt for {table:YtTable} with row for "{user_name:w}"')
def step_check_row_for_user(context, table, user_name):
    check_table(context, table, 1, [{'uid': user_name}])


@given('there are shard table in yt for {table:YtTable} with row for "{user_name:w}"')
def step_make_table(context, table, user_name):
    request = 'settings_export' if table == 'settings' else 'pnl_estimation_export'
    context.execute_steps('''
        When we make {request} request
        Then shiva responds ok
        And all shiva tasks finished
        And there are shard table in yt for {table} with row for "{user_name}"
    '''.format(request=request, table=table, user_name=user_name))


def create_yt_active_users_table(yt_client, table):
    if not yt_client.exists(table):
        schema = [
            {'name': 'uid', 'type': 'int64', 'sort_order': 'ascending'},
        ]
        yt_client.create_table(
            path=table,
            recursive=True,
            attributes={'schema': schema}
        )


def get_passport_active_users_table_name():
    date_prefix = datetime.datetime.today().strftime('%Y-%m-%d')
    return '{root}/active_users_dump_{date}'.format(
        root=FREEZING_TABLE_ROOT,
        date=date_prefix,
    )


@given('there are table in YT with passport active users')
def step_make_passport_active_users_table(context):
    table = get_passport_active_users_table_name()
    create_yt_active_users_table(context.yt, table)


@given('he {has:HasOrNot} activity in passport last 2 years')
def step_user_has_metadata_in_our_shard(context, has):
    if has:
        data = [{
            'uid': int(context.get_user().uid)
        }]
        table = get_passport_active_users_table_name()
        context.yt.write_table(
            table=table,
            input_stream=data,
            raw=False,
        )
