import numpy as np

from crypta.siberia.bin.custom_audience.lib.python.clustering import utils as clustering_utils


def test_join_small_clusters():
    labels, map_of_moves = clustering_utils.join_small_clusters(
        centroids_vectors=np.array([[0., 0.], [1., 1.]]),
        labels=np.array([0, 1, 1, 1, 1, 1]),
        min_cluster_size=5,
    )

    assert np.all(labels == np.array([1, 1, 1, 1, 1, 1]))
    assert map_of_moves == {0: 1}
