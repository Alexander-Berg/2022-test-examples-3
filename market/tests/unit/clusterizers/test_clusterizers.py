from edera.clusterizer import Cluster


def test_clusterizer_splits_items_correctly(clusterizer, items):
    clusters = clusterizer.clusterize(items)
    assert len(clusters) == 2
    assert sorted(clusters) == [
        Cluster({0, 4}, {0: 0, 1: None, 2: None, 3: None, 4: 4}),
        Cluster({1, 2, 3}, {0: None, 1: 1, 2: 2, 3: 3, 4: None}),
    ]
