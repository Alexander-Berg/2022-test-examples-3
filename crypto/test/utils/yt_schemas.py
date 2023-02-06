from crypta.lib.python.yt import schema_utils
from crypta.siberia.bin.custom_audience.apps_clustering.lib.utils import (
    fields,
    yt_schemas,
)


app_data_schema = schema_utils.yt_schema_from_dict(
    {
        'BundleId': 'string',
        'SourceID': 'uint32',
        'AppHashMd5': 'uint64',
        'RegionName': 'string',
        'Description': 'string',
    },
)


devid_by_app_schema = schema_utils.yt_schema_from_dict(
    {
        'app': 'string',
        'id': 'string',
        'id_type': 'string',
    },
)


app_dict_schema = schema_utils.yt_schema_from_dict(
    {
        'bundle_id': 'string',
        'id_type': 'string',
        'app_id': 'uint64',
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

segments_vectors_with_info_schema = schema_utils.yt_schema_from_dict(
    {
        fields.name: 'string',
        fields.id: 'uint64',
        fields.id_type: 'string',
        fields.md5_hash: 'uint64',
        fields.vector: 'any',
        fields.description: 'string',
        fields.users_count: 'uint64',
    },
)

centroids_schema = yt_schemas.centroids_schema
