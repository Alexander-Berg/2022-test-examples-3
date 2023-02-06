import os

import yatest
from yt import yson

from crypta.lib.python import yaml_config


def get_config_path(yt_proxy, yandexuid_user_data_table_path, yandexuid_to_crypta_id_table_path, crypta_id_user_data_table_path):
    config = {
        "yt_proxy": yt_proxy,
        "yt_pool": "pool",
        "yandexuid_user_data_table": yandexuid_user_data_table_path,
        "yandexuid_to_crypta_id_table": yandexuid_to_crypta_id_table_path,
        "dst_table": crypta_id_user_data_table_path,
        "max_tokens_count": 500,
        "min_sample_ratio": 0,
    }

    working_dir = yatest.common.test_output_path("make_crypta_id_user_data")

    if not os.path.isdir(working_dir):
        os.makedirs(working_dir)

    config_path = os.path.join(working_dir, "config.yaml")

    return yaml_config.dump(config, config_path)


def get_yandexuid_user_data_schema():
    schema = [
        {"name": "yuid", "type": "string", "sort_order": "ascending"},
        {"name": "Strata", "type": "string"},
        {"name": "Attributes", "type": "string"},
        {"name": "Vectors", "type": "string"},
        {"name": "Segments", "type": "string"},
        {"name": "GroupID", "type": "string"},
        {"name": "Identifiers", "type": "string"},
        {"name": "CryptaID", "type": "string"},
        {"name": "Affinities", "type": "string"},
        {"name": "WithoutData", "type": "boolean"},
    ]
    schema = yson.YsonList(schema)
    schema.attributes["strict"] = True
    return schema


def get_yandexuid_to_crypta_id_schema():
    schema = [
        {"name": "id", "type": "string", "sort_order": "ascending"},
        {"name": "id_type", "type": "string", "sort_order": "ascending"},
        {"name": "target_id", "type": "string"},
        {"name": "target_id_type", "type": "string"},
    ]

    schema = yson.YsonList(schema)
    schema.attributes["strict"] = True
    return schema
