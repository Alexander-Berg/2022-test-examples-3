import functools

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.training.lib import clusters


def test_prepare(yt_client, get_table_with_beh_profile, run_and_write_output_to_yt):
    clusters_json_file = files.YtFile(
        file_path='clusters.json',
        cypress_path='//clusters.json',
    )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            run_and_write_output_to_yt,
            func=functools.partial(
                clusters.match,
                yt_client=yt_client,
            ),
            cypress_path=clusters_json_file.cypress_path,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='prism_cluster_mapping.yson',
                    cypress_path=config.PRISM_CLUSTER_MAPPING_TABLE,
                    schema=schemas.prism_cluster_mapping_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (clusters_json_file, tests.Diff()),
        ],
    )
