import yatest.common

from crypta.audience.lib.tasks.audience import (
    precompute_communalities,
    tables as audience_tables,
)
from crypta.lib.python import time_utils
from crypta.lib.python.bt import test_helpers
from crypta.lib.python.bt.conf import conf
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def test_precompute_communalities(local_yt, default_conf, frozen_time):
    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=lambda: test_helpers.execute(precompute_communalities.PrecomputeCommunalities(day=str(time_utils.get_current_moscow_datetime().date()))),
        data_path=yatest.common.test_source_path('data'),
        input_tables=[(
            tables.DynamicYsonTable(
                "segment_properties_storage.yson",
                conf.paths.audience.dynamic.properties,
                on_write=tables.OnWrite(attributes={"schema": audience_tables.SegmentPropertiesStorage.SCHEMA}),
            ),
            None,
        )],
        output_tables=[
            (
                tables.DynamicYsonTable(
                    "segment_properties_storage.yson",
                    conf.paths.audience.dynamic.properties,
                    yson_format="pretty",
                ),
                tests.Diff(),
            ),
        ],
    )
