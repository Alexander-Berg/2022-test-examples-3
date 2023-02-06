from crypta.lookalike.lib.python.utils import fields


autobudget_log_schema = {
    'GoalID': 'int64',
    'LastUpdateTime': 'int64'
}

retargeting_log_schema = {
    'retargeting_id': 'uint64',
    'date': 'string'
}

goals_ids_schema = {
    'goal_id': 'uint64',
    'date_last_used': 'string',
}

goals_yandexuid_schema = {
    'goal_id': 'uint64',
    'ts': 'int64',
    'yandexuid': 'uint64',
}

metrika_yandexuid_schema = {
    'segment_id': 'uint64',
    'ts': 'int64',
    'yandexuid': 'uint64',
}

segments_simple_schema = {
    'id': 'uint64',
    'content_type': 'string',
    'deleted': 'uint64',
}

segments_full_schema = {
    'SegmentID': 'int64',
    'yandexuid': 'uint64',
}

update_dates_schema = {
    'meaningful_goals_updated': 'string',
    'multipliers_ids_updated': 'string',
    'retargeting_ids_updated': 'string',
}

user_data_schema = {
    'yuid': 'string',
}

user_data_by_cryptaid_schema = {
    'CryptaID': 'string',
}

goals_for_training_schema = [
    {
        'name': 'date_last_used',
        'type': 'string',
    },
    {
        'name': 'goal_id',
        'type': 'uint64',
    },
    {
        'name': 'ad_types',
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'string'}},
    },
]

meaningful_goals_log_schema = [
    {
        "name": "campaign_meaningful_goals",
        "type": "any",
        "type_v3": {"type_name": "optional", "item": {"type_name": "list", "item": "uint64"}},
    },
    {
        "name": "cube_date",
        "type": "string",
    }
]

segments_counts_schema = [
    {
        'name': 'GroupID',
        'type': 'string',
    },
    {
        'name': 'segment_type',
        'type': 'string',
    },
    {
        'name': 'ad_types',
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'string'}},
    },
    {
        'name': 'yuids_count',
        'type': 'uint64',
    },
    {
        'name': 'ids_cnt',
        'type': 'float',
    },
]

output_segments_schema = [
    {
        'name': 'GroupID',
        'type': 'string',
    },
    {
        'name': 'IdType',
        'type': 'string',
    },
    {
        'name': 'IdValue',
        'type': 'string',
    },
    {
        'name': 'segment_type',
        'type': 'string',
    },
    {
        'name': 'ad_types',
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'string'}},
    },
]

segments_for_training_schema = [
    {
        'name': 'date_last_used',
        'type': 'string',
    },
    {
        'name': 'segment_id',
        'type': 'uint64',
    },
    {
        'name': 'ad_types',
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'string'}},
    },
]

app_dssm_vectors_schema = [
    {
        'name': fields.app_id,
        'type': 'string',
    },
    {
        'name': fields.id_type,
        'type': 'string',
    },
    {
        'name': fields.MD5Hash,
        'type': 'uint64',
    },
    {
        'name': fields.app_vector,
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'float'}},
    },
]

cluster_centroids_vectors_schema = [
    {
        'name': fields.centroid_id,
        'type': 'string',
    },
    {
        'name': fields.centroid_rank,
        'type': 'uint64',
    },
    {
        'name': fields.centroid_vector,
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'float'}},
    },
    {
        'name': fields.cluster_id,
        'type': 'uint64',
    },
    {
        'name': fields.store_id,
        'type': 'string',
    },
    {
        'name': fields.old_bundle_ids,
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'string'}},
    },
]

users_dssm_vectors_schema = {
    fields.apps: 'any',
    fields.cryptaId: 'string',
    fields.id_type: 'string',
    fields.user_vector: 'any',
}

apps_dssm_vectors_schema = {
    fields.group_id: 'string',
    fields.segment_vector: 'any',
}

apps_info_schema = {
    fields.app_id: 'string',
    fields.id_type: 'string',
    fields.game: 'boolean',
}

datalens_data_table_schema = {
    'date': 'string',
    'itunes_mean_' + fields.old_centroids_ratio: 'double',
    'itunes_mean_' + fields.old_cluster_distance: 'double',
    'google_play_mean_' + fields.old_centroids_ratio: 'double',
    'google_play_mean_' + fields.old_cluster_distance: 'double',
}

matching_schema = {
    fields.device_id: 'string',
    fields.id_type: 'string',
    fields.target_id: 'string',
}

mobile_goals_tracker_schema = {
    fields.goal_id: 'uint64',
}

postback_log_schema = {
    'GoogleAdID': 'string',
    'IDFA': 'string',
    'IDFV': 'string',
    'OAID': 'string',
    fields.goal_id: 'int64',
}

users_vectors_schema = [
    {
        'name': 'segments',
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'string'}},
    },
    {
        'name': fields.user_vector,
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'float'}},
    },
    {
        'name': fields.yandexuid,
        'type': 'uint64',
    },
]

segment_vectors_schema = [
    {
        'name': fields.segment_vector,
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'float'}},
    },
    {
        'name': fields.group_id,
        'type': 'string',
    },
]

yuids_ranked = {
    fields.yandexuid: 'uint64',
    fields.row_rank: 'uint64',
}

cities_dict_schema = {
    'feature_index': 'int64',
    'city_name': 'string',
    'feature': 'string',
}

features_dicts_schema = {
    'feature_index': 'int64',
    'feature': 'int64',
}

features_mapping_schema = {
    'feature_index': 'uint64',
    'feature': 'string',
}

apps_by_devid_schema = [
    {
        'name': fields.apps,
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'string'}},
    },
    {
        'name': 'id',
        'type': 'string',
    },
    {
        'name': 'id_type',
        'type': 'string',
    },
]
