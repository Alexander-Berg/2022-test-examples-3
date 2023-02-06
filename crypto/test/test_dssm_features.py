import functools
import os

import yatest.common

from crypta.affinitive_geo.services.org_embeddings.lib import dssm_features
from crypta.affinitive_geo.services.org_embeddings.lib.utils import config
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)


def get_user_data_schema():
    return [
        {'name': 'GroupID', 'type': 'string'},
        {'name': 'Stats', 'type': 'string'},
    ]


def test_dssm_features(yt_client):
    input_table = config.ORGS_USER_DATA_STATS_TABLE
    output_table = config.ORGS_DSSM_FEATURES_TABLE

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            dssm_features.get,
            yt_client=yt_client,
            input_table=input_table,
            output_table=output_table,
        ),
        data_path=yatest.common.test_source_path('data/test_dssm_features'),
        input_tables=[
            (
                files.YtFile(
                    file_path=yatest.common.build_path('crypta/affinitive_geo/services/org_embeddings/lib/test/sandbox_data/crypta_look_alike_model/segments_dict'),
                    cypress_path=os.path.join(config.LOOKALIKE_VERSION_DIR, 'segments_dict.json'),
                ),
                tests.YtTest(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='user_data_stats.yson',
                    cypress_path=input_table,
                    schema=get_user_data_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='dssm_features.yson',
                    cypress_path=output_table,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
