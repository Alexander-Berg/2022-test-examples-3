import os

import numpy as np
import pandas as pd
import yatest.common

from crypta.lib.python.yt.test_helpers import tables, tests
from crypta.siberia.bin.custom_audience.lib.python.clustering import utils as clustering_utils
from crypta.siberia.bin.custom_audience.lib.python.clustering.test.utils import fields


def test_get_index_of_nearest_vector(local_yt, local_yt_and_yql_env):
    os.environ.update(local_yt_and_yql_env)
    yt_client = local_yt.get_yt_client()

    segments_vectors = '//tmp/segment_vectors_with_info_table'

    segments_df = pd.DataFrame(
        {
            fields.id: ['1', '2', '3'],
            fields.vector: [[1., 1.], [-1., -1.], [2., 2.]],
        }
    )

    res = tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: clustering_utils.get_index_of_nearest_vector(
            yt_client=yt_client,
            segments_vectors_table=segments_vectors,
            segments_df=segments_df,
            vectors=[[1., 1], [-1., -1.]],
            fields=fields,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=True,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'test_get_index_of_nearest_vector_segments_vectors.yson',
                    segments_vectors,
                    [
                        {
                            'name': fields.id,
                            'type': 'string',
                        },
                        {
                            'name': fields.vector,
                            'type': 'any',
                        },
                    ],
                ),
                tests.TableIsNotChanged(),
            ),
        ],
    )
    assert np.any(res == np.array([0, 1, 0]))
    return
