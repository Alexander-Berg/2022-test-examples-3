import os

import yatest.common

from datetime import date
from dateutil.relativedelta import relativedelta
from itertools import product
from random import randint

from mail.freezing_tests.lib.yt_yql.yt import create_table, write_table

PASSPORT_TABLES_DIR = "//home/mail-logs/passport-auth-export"
PASSPORT_TABLES_SCHEMA = [{"name": "uid", "type": "string"}]

CONNECT_TABLES_DIR = "//home/mail-logs/core/mdb/freezing/connect"
CONNECT_TABLES_SCHEMA = [{"name": "uid", "type": "uint64"}]

CURRENT_UTC_DATE = date(randint(2000, 2020), randint(1, 12), randint(1, 28))

PLACEHOLDER_VALUES = {
    "%CLUSTER%": os.environ["YQL_DB"],
    "%UTC_DAY%": "%02d" % CURRENT_UTC_DATE.day,
    "%UTC_MONTH%": "%02d" % CURRENT_UTC_DATE.month,
    "%UTC_YEAR%": CURRENT_UTC_DATE.year,
}

OUTPUT_TABLE = "//home/mail-logs/core/mdb/freezing/active_users_dump_" + str(
    CURRENT_UTC_DATE
)

TABLES_PER_DAY = 3  # for each of the three auth methods


def get_query():
    QUERY_RESOURCE_NAME = "yql_query"
    path = yatest.common.work_path(QUERY_RESOURCE_NAME)
    query_templ = open(path).read()
    for placeholder, value in PLACEHOLDER_VALUES.items():
        query_templ = query_templ.replace(placeholder, str(value))
    return query_templ


def run_query(context):
    request = context.yql.query(get_query(), syntax_version=1)
    request.run()
    results = request.get_results()
    assert results.is_success, [str(error) for error in results.errors]
    assert list(results) == []


def days_range(start, end):
    """
    includes start and does not include end
    """
    assert start < end
    length = int((end - start).days)
    return [start + relativedelta(days=n) for n in range(length)]


def generate_table_paths_impl(dates, builder):
    auth_methods = ("login", "oauthcheck", "sescheck")
    paths = [
        builder(PASSPORT_TABLES_DIR, str(day), auth_mth)
        for day, auth_mth in product(dates, auth_methods)
    ]
    assert len(paths) == len(dates) * TABLES_PER_DAY
    return paths


def generate_table_paths_for_dates(dates):
    builder = lambda dir, date, auth_mth: dir + "/" + date + "_" + auth_mth
    return generate_table_paths_impl(dates, builder)


def generate_table_paths(start_date, end_date):
    return generate_table_paths_for_dates(days_range(start_date, end_date))


def prepare_tables(paths, uids, context):
    """
    i-th table will receive the uids at positions i, i+len(paths), i+2*len(paths), ...
    """
    assert len(paths) <= len(uids)
    uids_used = 0
    for idx, path in enumerate(paths):
        create_table(path, context.yt, PASSPORT_TABLES_SCHEMA)
        data = [{"uid": str(uid)} for uid in uids[idx :: len(paths)]]
        write_table(path, data, context.yt)
        uids_used += len(data)
    assert uids_used == len(uids)


def prepare_tables_for_dates(dates, uids, context):
    paths = generate_table_paths_for_dates(dates)
    prepare_tables(paths, uids, context)


def create_tables_for_connect(context):
    create_table(CONNECT_TABLES_DIR + "/pdd_uids", context.yt, CONNECT_TABLES_SCHEMA)
    create_table(CONNECT_TABLES_DIR + "/maillist_uids", context.yt, CONNECT_TABLES_SCHEMA)


def prepare_tables_for_connect(pdd_uids, maillist_uids, context):
    def prepare_single(table_name, uids):
        path = f"{CONNECT_TABLES_DIR}/{table_name}"
        assert context.yt.exists(path)
        if uids:
            data = [{"uid": int(uid)} for uid in uids]
            write_table(path, data, context.yt)

    prepare_single("pdd_uids", pdd_uids)
    prepare_single("maillist_uids", maillist_uids)


def remove_tables(foo):
    def wrapped(self, context):
        try:
            foo(self, context)
        finally:
            context.yt.remove(PASSPORT_TABLES_DIR, recursive=True, force=True)
            context.yt.remove(OUTPUT_TABLE, force=True)

    return wrapped
