import os

import numpy as np
import pandas as pd
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.siberia.bin.custom_audience.lib.python.clustering import utils as clustering_utils
from crypta.siberia.bin.custom_audience.lib.python.clustering.test.utils import fields


def test_get_restore_missing_centroids(local_yt, local_yt_and_yql_env):
    os.environ.update(local_yt_and_yql_env)
    yt_client = local_yt.get_yt_client()

    old_centroids_table = '//tmp/old_centroids'

    input_new_centroids_df = pd.DataFrame(
        {
            fields.id: ['0_id', '2_id'],
            fields.cluster_id: [0, 2]
        }
    )

    res = tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: clustering_utils.restore_missing_centroids(
            yt_client=yt_client,
            old_centroids_table=old_centroids_table,
            new_centroids_df=input_new_centroids_df,
            fields=fields,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=True,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'test_restore_missing_centroids_old_centroids.yson',
                    old_centroids_table,
                    [
                        {
                            'name': fields.id,
                            'type': 'string',
                        },
                        {
                            'name': fields.cluster_id,
                            'type': 'any',
                        },
                        {
                            'name': fields.simlink,
                            'type': 'uint64',
                        },
                    ],
                ),
                tests.TableIsNotChanged(),
            ),
        ],
    )
    output_new_centroids_df = res[0]
    assert output_new_centroids_df.shape == (3, 3)
    assert np.all(output_new_centroids_df[fields.id].tolist() == ['0_id', '1_id', '2_id'])
    assert np.all(output_new_centroids_df[fields.cluster_id].tolist() == [0, 1, 2])
