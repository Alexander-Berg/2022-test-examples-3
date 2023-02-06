import six
import yatest.common
import yt.wrapper as yt

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.runners.lal_samples.lib import new_settlers
from crypta.profile.utils.config import config
from crypta.profile.utils import utils


def attrs_transform(attrs):
    return {k: v for k, v in six.iteritems(attrs) if not k.startswith("_")}


def get_input_tables():
    return [
        (tables.get_yson_table_with_schema(
            date + ".yson",
            new_settlers.get_state_path(date),
            schema=schema_utils.get_strict_schema(
                [
                    {"name": "unified_id", "type": "string", "sort_order": "ascending"},
                    {"name": "source_unified_id", "type": "string", "sort_order": "ascending"},
                    {"name": "manual_homes", "type": "any"},
                    {"name": "predicted_home", "type": "any"},
                ]
            ),
        ), tests.TableIsNotChanged())
        for date in ["2021-02-27", "2021-03-06", "2021-03-13", "2021-03-20", "2021-03-27"]
    ] + [
        (tables.get_yson_table_with_schema(
            'cryptaid_yandexuid.yson',
            utils.get_matching_table("crypta_id", "yandexuid"),
            schema=schema_utils.get_strict_schema(
                [
                    {"name": "id", "type": "string", "sort_order": "ascending"},
                    {"name": "id_type", "type": "string", "sort_order": "ascending"},
                    {"name": "target_id", "type": "string"},
                ]
            ),
        ), tests.TableIsNotChanged()),
    ]


def test_new_settlers(local_yt, patched_config, date):
    task = new_settlers.NewSettlers(date=date)

    data_dir = yatest.common.test_source_path('data/new_settlers')

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=task.run,
        data_path=data_dir,
        input_tables=get_input_tables(),
        output_tables=[
            (tables.YsonTable(
                'new_settlers.yson',
                yt.ypath_join(config.LAL_SAMPLE_FOLDER, new_settlers.NewSettlers.__name__),
                yson_format='pretty',
            ), (tests.Diff(transformer=attrs_transform))),
            (tables.YsonTable(
                'lal_input.yson',
                yt.ypath_join(config.REGULAR_LAL_INPUT_DIRECTORY, "audience_segments/19376956"),
                yson_format='pretty',
            ), (tests.Diff(transformer=attrs_transform))),
        ],
    )
