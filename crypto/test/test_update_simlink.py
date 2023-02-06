import numpy as np
import pandas as pd

from crypta.siberia.bin.custom_audience.lib.python.clustering import utils as clustering_utils
from crypta.siberia.bin.custom_audience.lib.python.clustering.test.utils import fields


def test_update_simlinks1():
    size_of_df = 5

    centroids_df = pd.DataFrame(
        {
            fields.name: ['site{}'.format(i) for i in range(size_of_df)],
            fields.id: [i for i in range(size_of_df)],
            fields.vector: [[float(i), float(i)] for i in range(size_of_df)],
            fields.cluster_id: [i for i in range(size_of_df)],
            fields.simlink: [1, 2, 4, None, 3],
            fields.neighbors: [['site{}'.format(i)] for i in range(size_of_df)],
        },
        dtype=np.object,
    )

    normilized_centroids_df = clustering_utils.update_simlinks(
        centroids_df=centroids_df,
        fields=fields,
    )

    assert normilized_centroids_df.shape == (5, 6)
    assert np.all(normilized_centroids_df[fields.name].tolist() == ['site3'] * size_of_df)
    assert np.all(normilized_centroids_df[fields.id].tolist() == [3] * size_of_df)
    assert np.all(normilized_centroids_df[fields.vector].tolist() == [[3., 3.]] * size_of_df)
    assert np.all(normilized_centroids_df[fields.cluster_id].tolist() == [i for i in range(size_of_df)])
    assert np.all(normilized_centroids_df[fields.simlink].tolist() == [3, 3, 3, None, 3])
    assert np.all(normilized_centroids_df[fields.neighbors].tolist() == [['site3']] * size_of_df)


def test_update_simlinks2():
    size_of_df = 5

    centroids_df = pd.DataFrame(
        {
            fields.name: ['site{}'.format(i) for i in range(size_of_df)],
            fields.id: [i for i in range(size_of_df)],
            fields.vector: [[float(i), float(i)] for i in range(size_of_df)],
            fields.cluster_id: [i for i in range(size_of_df)],
            fields.simlink: [None, 0, None, 4, None],
            fields.neighbors: [['site{}'.format(i)] for i in range(size_of_df)],
        },
        dtype=np.object,
    )

    normilized_centroids_df = clustering_utils.update_simlinks(
        centroids_df=centroids_df,
        fields=fields,
    )

    res_host_id = [0, 0, 2, 4, 4]
    assert normilized_centroids_df.shape == (5, 6)
    assert np.all(normilized_centroids_df[fields.name].tolist() == ['site{}'.format(el) for el in res_host_id])
    assert np.all(normilized_centroids_df[fields.id].tolist() == res_host_id)
    assert np.all(normilized_centroids_df[fields.vector].tolist() == [[float(el), float(el)] for el in res_host_id])
    assert np.all(normilized_centroids_df[fields.cluster_id].tolist() == [i for i in range(size_of_df)])
    assert np.all(normilized_centroids_df[fields.simlink].tolist() == [None, 0, None, 4, None])
    assert np.all(normilized_centroids_df[fields.neighbors].tolist() == [['site{}'.format(el)] for el in res_host_id])
