def get_centroids_schema():
    return {
        'segment_vector': 'any',
        'cluster_id': 'uint32',
        'id_type': 'string',
    }


def get_clustering_schema():
    return {
        'segment_name': 'string',
        'cluster_id': 'uint32',
        'users_count': 'uint64',
    }
