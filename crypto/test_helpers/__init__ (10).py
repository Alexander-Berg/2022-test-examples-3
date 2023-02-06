from crypta.lib.python.yt import schema_utils


def get_direct_client_id_to_puid_table_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        column: "int64" for column in ["uid", "ClientID"]
    }, sort_by=["uid"]))


def get_matching_table_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        column: "string" for column in ["id", "id_type", "date_begin", "date_end", "target_id", "target_id_type"]
    }, sort_by=["id", "id_type"]))
