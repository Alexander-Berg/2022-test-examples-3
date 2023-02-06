import yatest.common

from crypta.audience.lib import matching
from crypta.lib.python.bt import test_helpers
from crypta.lib.python.bt.conf import conf
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def test_prepare_crypta_ids(local_yt, date, default_conf):
    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=lambda: test_helpers.execute(matching.PrepareCryptaIds(day=date)),
        data_path=yatest.common.test_source_path("data/prepare_crypta_ids"),
        input_tables=[
            (tables.get_yson_table_with_schema(
                "crypta_id_profiles.yson",
                conf.paths.crypta.cryptaid_profiles_for_14days,
                schema_utils.yt_schema_from_dict({"crypta_id": "uint64"}),
            ), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable(
                "crypta_ids.yson",
                conf.paths.audience.matching.cryptaids,
                yson_format="pretty",
            ), tests.Diff()),
        ],
    )
