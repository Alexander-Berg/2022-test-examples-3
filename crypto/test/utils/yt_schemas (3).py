from crypta.lib.python.yt import schema_utils
from crypta.siberia.bin.custom_audience.sites_clustering.lib.utils import (
    fields,
    yt_schemas,
)


metrics_yandexuid_flattened_hits_schema = schema_utils.yt_schema_from_dict(
    {
        'host': 'string',
        'weight': 'double',
        'yandexuid': 'uint64',
    },
)


bar_yandexuid_flattened_hits_schema = metrics_yandexuid_flattened_hits_schema


metrics_crypta_id_flattened_hits_schema = schema_utils.yt_schema_from_dict(
    {
        'host': 'string',
        'weight': 'double',
        'crypta_id': 'uint64',
    },
)


bar_crypta_id_flattened_hits_schema = metrics_crypta_id_flattened_hits_schema


crypta_id_metrica_browser_visitor_counter_schema = schema_utils.yt_schema_from_dict(
    {
        'bar_visitors_count': 'uint64',
        'metrica_visitors_count': 'uint64',
        'site': 'string',
    },
)


sites_to_describe_schema = schema_utils.yt_schema_from_dict(
    {
        'GroupID': 'string',
        'IdType': 'string',
        'IdValue': 'string',
    },
)


site_dict_schema = schema_utils.yt_schema_from_dict(
    {
        'site': 'string',
        'site_id': 'uint64',
    },
)


clustering_schema = schema_utils.yt_schema_from_dict(
    yt_schemas.get_clustering_schema(),
)


id_to_crypta_id_schema = schema_utils.yt_schema_from_dict(
    {
        'id': 'string',
        'id_type': 'string',
        'crypta_id': 'uint64',
    },
)


matching_yandexuid_crypta_id_schema = schema_utils.yt_schema_from_dict(
    {
        'id': 'string',
        'id_type': 'string',
        'target_id': 'string',
        'target_id_type': 'string',
        'date_begin': 'string',
        'date_end': 'string',
        'direct': 'boolean',
        'fuzzy': 'boolean',
    },
)


centroids_schema = [
    {
        'name': fields.name,
        'type': 'string',
    },
    {
        'name': fields.id,
        'type': 'uint64',
    },
    {
        'name': fields.vector,
        'type': 'any',
    },
    {
        'name': fields.cluster_id,
        'type': 'uint32',
    },
    {
        'name': fields.users_count,
        'type': 'uint64',
    },
    {
        'name': fields.simlink,
        'type': 'uint64',
    },
    {
        'name': fields.neighbors,
        'type': 'any',
        'type_v3': {'type_name': 'optional', 'item': {'type_name': 'list', 'item': 'string'}},
    },
]
