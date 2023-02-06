import itertools
import pytest
import random

from dateutil.relativedelta import relativedelta

from yt.yson import YsonList

from .util import (
    CONNECT_TABLES_DIR,
    create_tables_for_connect,
    CURRENT_UTC_DATE,
    days_range,
    generate_table_paths_impl,
    generate_table_paths_for_dates,
    get_query,
    OUTPUT_TABLE,
    prepare_tables,
    prepare_tables_for_connect,
    prepare_tables_for_dates,
    remove_tables,
    run_query,
    TABLES_PER_DAY,
)
from mail.freezing_tests.lib.yt_yql.yql import get_yql_cli
from mail.freezing_tests.lib.yt_yql.yt import get_yt_cli

UIDS_PER_TABLE = 10


@pytest.fixture(scope="module", autouse=True)
def context():
    class Context:
        pass

    context = Context()
    context.yql = get_yql_cli()
    context.yt = get_yt_cli()
    return context


@pytest.fixture(scope="function", autouse=True)
def test_setup(request, context):
    create_tables_for_connect(context)

    def test_teardown():
        context.yt.remove(CONNECT_TABLES_DIR, recursive=True, force=True)

    request.addfinalizer(test_teardown)


def sorted_by_uid(rows):
    rows.sort(key=lambda row: row["uid"])
    return rows


def build_rows(uids):
    return [{"uid": uid} for uid in set(uids)]


class TestFiltrationByTableName:
    @staticmethod
    def generate_incorrect_table_paths(dates):
        mutations = [
            lambda dir, date, auth_mth: dir + "/" + "_" + date + "_" + auth_mth,
            lambda dir, date, auth_mth: dir + "/" + date + "_" + auth_mth + "_",
            lambda dir, date, auth_mth: dir + "/" + date + auth_mth,
            lambda dir, date, auth_mth: dir + "/" + date + "_" + "how about that",
        ]
        mutation = itertools.cycle(mutations)
        builder = lambda dir, date, auth_mth: next(mutation)(dir, date, auth_mth)
        return generate_table_paths_impl(dates, builder)

    @remove_tables
    def test_table_filtration_by_date(self, context):
        """
        only the tables for dates within 2-years range should be considered
        """

        DAYS_TO_SAMPLE = 13

        out_of_range_uids = random.sample(
            range(10 ** 2, 10 ** 3), DAYS_TO_SAMPLE * TABLES_PER_DAY * UIDS_PER_TABLE
        )
        out_of_range_dates = random.sample(
            days_range(
                CURRENT_UTC_DATE - relativedelta(months=100),
                CURRENT_UTC_DATE - relativedelta(months=24),
            ),
            DAYS_TO_SAMPLE,
        )
        prepare_tables_for_dates(out_of_range_dates, out_of_range_uids, context)

        within_range_uids = random.sample(
            range(10 ** 4, 10 ** 5), DAYS_TO_SAMPLE * TABLES_PER_DAY * UIDS_PER_TABLE
        )
        within_range_dates = random.sample(
            days_range(CURRENT_UTC_DATE - relativedelta(months=24), CURRENT_UTC_DATE),
            DAYS_TO_SAMPLE,
        )
        prepare_tables_for_dates(within_range_dates, within_range_uids, context)

        run_query(context)

        results = list(context.yt.read_table(OUTPUT_TABLE))
        assert sorted_by_uid(build_rows(within_range_uids)) == sorted_by_uid(results)

    @remove_tables
    def test_table_filtration_by_regex(self, context):
        """
        table names should match the regex
        """

        DAYS_TO_SAMPLE = 13

        dates = random.sample(
            days_range(CURRENT_UTC_DATE - relativedelta(months=24), CURRENT_UTC_DATE),
            DAYS_TO_SAMPLE,
        )

        out_of_range_uids = random.sample(
            range(10 ** 2, 10 ** 3), DAYS_TO_SAMPLE * TABLES_PER_DAY * UIDS_PER_TABLE
        )
        prepare_tables(
            self.generate_incorrect_table_paths(dates), out_of_range_uids, context
        )

        within_range_uids = random.sample(
            range(10 ** 4, 10 ** 5), DAYS_TO_SAMPLE * TABLES_PER_DAY * UIDS_PER_TABLE
        )
        prepare_tables_for_dates(dates, within_range_uids, context)

        run_query(context)

        results = list(context.yt.read_table(OUTPUT_TABLE))
        assert sorted_by_uid(build_rows(within_range_uids)) == sorted_by_uid(results)

    @staticmethod
    def tables_near_the_border_date_are_considered_test_internal(dates, context):
        uids = random.sample(
            range(0, 10 ** 5), len(dates) * TABLES_PER_DAY * UIDS_PER_TABLE
        )
        paths = generate_table_paths_for_dates(dates)

        prepare_tables(paths, uids, context)

        run_query(context)

        results = list(context.yt.read_table(OUTPUT_TABLE))
        return uids, results

    @remove_tables
    def test_last_tables_within_range_are_considered(self, context):
        last_days = days_range(
            CURRENT_UTC_DATE - relativedelta(months=24),
            CURRENT_UTC_DATE - relativedelta(months=23, days=25),
        )
        uids, results = self.tables_near_the_border_date_are_considered_test_internal(
            last_days, context
        )
        assert sorted_by_uid(build_rows(uids)) == sorted_by_uid(results)

    @remove_tables
    def test_few_recent_days_are_considered(self, context):
        recent_days = days_range(
            CURRENT_UTC_DATE - relativedelta(days=5), CURRENT_UTC_DATE
        )
        uids, results = self.tables_near_the_border_date_are_considered_test_internal(
            recent_days, context
        )
        assert sorted_by_uid(build_rows(uids)) == sorted_by_uid(results)

    @remove_tables
    def test_few_days_past_2_years_border_are_not_considered(self, context):
        past_the_last_days = days_range(
            CURRENT_UTC_DATE - relativedelta(months=24, days=5),
            CURRENT_UTC_DATE - relativedelta(months=24),
        )
        uids = random.sample(
            range(0, 10 ** 5), len(past_the_last_days) * TABLES_PER_DAY * UIDS_PER_TABLE
        )
        prepare_tables(
            generate_table_paths_for_dates(past_the_last_days), uids, context
        )

        request = context.yql.query(get_query(), syntax_version=1)
        request.run()
        results = request.get_results()
        assert not results.is_success
        assert list(results) == []


class TestPostconditions:
    @staticmethod
    def postconditions_test_internal(context):
        DAYS_TO_SAMPLE = 13
        dates = random.sample(
            days_range(CURRENT_UTC_DATE - relativedelta(months=24), CURRENT_UTC_DATE),
            DAYS_TO_SAMPLE,
        )
        uids = random.sample(
            range(0, 10 ** 5), len(dates) * TABLES_PER_DAY * UIDS_PER_TABLE
        )
        uids = uids * 3  # not unique

        prepare_tables_for_dates(dates, uids, context)

        run_query(context)

        return uids

    @staticmethod
    def get_expected_schema():
        expected = [
            {
                "name": "uid",
                "required": True,
                "sort_order": "ascending",
                "type": "int64",
                "type_v3": "int64",
            },
        ]
        expected = YsonList(expected)
        expected.attributes["strict"] = True
        expected.attributes["unique_keys"] = True
        return expected

    @remove_tables
    def test_result_rows_distinct_and_ordered(self, context):
        uids = self.postconditions_test_internal(context)

        results = list(context.yt.read_table(OUTPUT_TABLE))
        assert len(results) == len(set(uids))  # just double-check
        assert results == sorted_by_uid(build_rows(uids))

    @remove_tables
    def test_output_table_schema(self, context):
        self.postconditions_test_internal(context)

        schema = context.yt.get(f"{OUTPUT_TABLE}/@schema")
        assert schema == self.get_expected_schema()


class TestUnionWithConnectTables:
    @remove_tables
    def test_table_union_and_distinct(self, context):
        """
        uids should be unique and from all tables
        """

        DAYS_TO_SAMPLE = 1

        passport_range_uids = random.sample(
            range(10 ** 2, 10 ** 3), DAYS_TO_SAMPLE * TABLES_PER_DAY * UIDS_PER_TABLE
        )
        passport_range_dates = random.sample(
            days_range(CURRENT_UTC_DATE - relativedelta(months=24), CURRENT_UTC_DATE),
            DAYS_TO_SAMPLE,
        )
        prepare_tables_for_dates(passport_range_dates, passport_range_uids, context)

        pdd_range_uids = random.sample(
            range(10 ** 3, 10 ** 4), DAYS_TO_SAMPLE * TABLES_PER_DAY * UIDS_PER_TABLE
        )
        maillist_range_uids = random.sample(
            range(10 ** 4, 10 ** 5), DAYS_TO_SAMPLE * TABLES_PER_DAY * UIDS_PER_TABLE
        )
        prepare_tables_for_connect(
            pdd_uids=pdd_range_uids + passport_range_uids[:int(len(passport_range_uids) / 3)],
            maillist_uids=maillist_range_uids + passport_range_uids[-int(len(passport_range_uids) / 3):],
            context=context,
        )

        run_query(context)

        results = list(context.yt.read_table(OUTPUT_TABLE))
        expected_uids = passport_range_uids + pdd_range_uids + maillist_range_uids
        assert sorted_by_uid(build_rows(expected_uids)) == sorted_by_uid(results)
