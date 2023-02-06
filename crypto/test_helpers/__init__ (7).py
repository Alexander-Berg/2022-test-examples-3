from google.protobuf import json_format
import yaml
import yatest

from crypta.lib.proto.user_data import user_data_stats_pb2


def get_crypta_id_user_data(filename):
    crypta_id_user_data = []

    with open(yatest.common.test_source_path(filename)) as f:
        for item in yaml.safe_load(f):
            crypta_id_user_data.append({
                "crypta_id": item["crypta_id"],
                "stats": json_format.ParseDict(item["stats"], user_data_stats_pb2.TUserDataStats()).SerializeToString(),
            })

    return crypta_id_user_data
