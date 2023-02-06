import os

import numpy as np
import pandas as pd
import yatest.common

from crypta.lib.python.yt.test_helpers import tables, tests
from crypta.siberia.bin.custom_audience.lib.python.clustering import utils as clustering_utils
from crypta.siberia.bin.custom_audience.lib.python.clustering.test.utils import fields as test_clustering_fields


def test_get_clusterid_and_simlink(local_yt, local_yt_and_yql_env):
    os.environ.update(local_yt_and_yql_env)
    yt_client = local_yt.get_yt_client()

    segments_vectors_table = '//tmp/segments_vectors'

    input_new_centroids_df = pd.DataFrame(
        {
            test_clustering_fields.id: [1, 2],
            test_clustering_fields.vector: [[1., 1.], [-2., -2.]],
            test_clustering_fields.cluster_id: [3, 4],
            test_clustering_fields.users_count: [0, 0],
        }
    )
    input_new_segment_df = pd.DataFrame(
        {
            test_clustering_fields.id: [1, 2, 3, 4, 5],
            test_clustering_fields.vector: [[1., 1.], [1., 1.], [-2., -2.], [-2., -2.], [-2., -2.]],
            test_clustering_fields.users_count: [10, 20, 30, 40, 50],
        }
    )

    res = tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: clustering_utils.get_clusterid_and_simlink(
            yt_client=yt_client,
            segments_vectors_table=segments_vectors_table,
            new_centroids_df=input_new_centroids_df,
            new_segments_df=input_new_segment_df,
            min_cluster_size=5,
            fields=test_clustering_fields,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=True,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'test_get_clusterid_and_simlink_segments_vectors.yson',
                    segments_vectors_table,
                    [
                        {
                            'name': test_clustering_fields.id,
                            'type': 'int64',
                        },
                        {
                            'name': test_clustering_fields.vector,
                            'type': 'any',
                        },
                    ],
                ),
                tests.TableIsNotChanged(),
            ),
        ],
    )
    output_new_centroids_df, output_new_sites_df = res[0]

    assert output_new_centroids_df.shape == (2, 5)
    assert np.all(input_new_centroids_df[test_clustering_fields.id].tolist() == output_new_centroids_df[test_clustering_fields.id].tolist())
    assert np.all(input_new_centroids_df[test_clustering_fields.vector].tolist() == output_new_centroids_df[test_clustering_fields.vector].tolist())
    assert np.all(input_new_centroids_df[test_clustering_fields.cluster_id].tolist() == output_new_centroids_df[test_clustering_fields.cluster_id].tolist())
    assert np.all(output_new_centroids_df[test_clustering_fields.users_count].tolist() == [0, 150])
    assert np.all(output_new_centroids_df[test_clustering_fields.simlink].tolist() == [4, None])

    assert output_new_sites_df.shape == (5, 4)
    assert np.all(output_new_sites_df[test_clustering_fields.id].tolist() == output_new_sites_df[test_clustering_fields.id].tolist())
    assert np.all(output_new_sites_df[test_clustering_fields.vector].tolist() == output_new_sites_df[test_clustering_fields.vector].tolist())
    assert np.all(output_new_sites_df[test_clustering_fields.users_count].tolist() == output_new_sites_df[test_clustering_fields.users_count].tolist())
    assert np.all(output_new_sites_df[test_clustering_fields.cluster_id].tolist() == [4, 4, 4, 4, 4])
