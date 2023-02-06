import pandas as pd

from crypta.siberia.bin.custom_audience.lib.python.clustering import utils as clustering_utils
from crypta.siberia.bin.custom_audience.lib.python.clustering.test.utils import (
    config as test_clustering_config,
    fields as test_clustering_fields,
)


def test_get_new_centroids_df():
    old_centroids_df = pd.DataFrame(
        {
            test_clustering_fields.id: [1, 2],
            test_clustering_fields.cluster_id: [3, 2],
            test_clustering_fields.name: ['site1', 'site2'],
            test_clustering_fields.simlink: [None, 1],
            test_clustering_fields.vector: [[0., 0.], [0., 0.]],
            test_clustering_fields.neighbors: [['site1'], ['site2']]
        }
    )
    new_sites_df = pd.DataFrame(
        {
            test_clustering_fields.id: [2, 1],
            test_clustering_fields.name: ['site2', 'site1'],
            test_clustering_fields.vector: [[-1., -1.], [1., 1.]],
            test_clustering_fields.users_count: [20, 10],
        }
    )
    kmeans_centroids = [[1., 1.]]

    centroids_df = clustering_utils.get_new_centroids_df(
        old_centroids_df=old_centroids_df,
        segments_df=new_sites_df,
        kmeans_centroids=kmeans_centroids,
        config=test_clustering_config,
        fields=test_clustering_fields,
    )

    assert centroids_df.shape == (1, 5)
    assert centroids_df[test_clustering_fields.id][0] == 1
    assert centroids_df[test_clustering_fields.name][0] == 'site1'
    assert centroids_df[test_clustering_fields.vector][0] == [1., 1.]
    assert centroids_df[test_clustering_fields.cluster_id][0] == 3
    assert centroids_df[test_clustering_fields.neighbors][0] == ['site1', 'site2']
