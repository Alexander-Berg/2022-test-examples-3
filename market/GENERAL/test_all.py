import base64
import os
import subprocess

from google.protobuf.text_format import Parse, PrintMessage

import yatest.common

from market.access.adapter.proto.resource_pb2 import TResources
from search.base.blob_storage.config.protos.remote_chunked_blob_storage_index_config_pb2 import TRemoteBlobStorageIndexConfig

CONFIG_GENERATOR = yatest.common.binary_path("market/report/rs/config_generator/config_generator")
DELIVERY_INDEX_V1 = yatest.common.source_path("market/report/rs/config_generator/ut/delivery_rs_index/0.315.0")
DELIVERY_INDEX_V2 = yatest.common.source_path("market/report/rs/config_generator/ut/delivery_rs_index/0.329.0")


def serialize_base64(message):
    return base64.b64encode(message.SerializeToString())


def add_to_env(message):
    env = os.environ.copy()
    env["ACCESS_RESOURCES_PROTO"] = serialize_base64(message)
    return env


def cannonize_path(path):
    arcadia_pos = path.find(b"arcadia")
    return path[arcadia_pos:]


def prepare_for_canonnize(config_path):
    with open(config_path) as f:
        config_data = f.read()

    config = TRemoteBlobStorageIndexConfig()
    Parse(config_data, config)

    for chunk in config.Chunks:
        chunk.Path = cannonize_path(chunk.Path)

    with open(config_path, 'w') as f:
        PrintMessage(config, f)


def test_one_verson():
    r = TResources()
    resource_versions = r.resources["r1"]
    resource_version = resource_versions.versions["0.315.0"]
    resource_version.name = "delivery_rs_index"
    resource_version.version = "0.315.0"
    resource_version.path = DELIVERY_INDEX_V1

    env = add_to_env(r)

    dst_config = yatest.common.test_output_path("config.pb.txt")
    subprocess.check_call([CONFIG_GENERATOR, "--dst", dst_config], env=env)
    prepare_for_canonnize(dst_config)

    return yatest.common.canonical_file(dst_config, local=True)


def test_two_versons():
    r = TResources()
    resource_versions = r.resources["r1"]

    resource_version = resource_versions.versions["0.315.0"]
    resource_version.name = "delivery_rs_index"
    resource_version.version = "0.315.0"
    resource_version.path = DELIVERY_INDEX_V1

    resource_version = resource_versions.versions["0.329.0"]
    resource_version.name = "offer_rs_index"
    resource_version.version = "0.329.0"
    resource_version.path = DELIVERY_INDEX_V2

    env = add_to_env(r)

    dst_config = yatest.common.test_output_path("config.pb.txt")
    subprocess.check_call([CONFIG_GENERATOR, "--dst", dst_config], env=env)
    prepare_for_canonnize(dst_config)

    return yatest.common.canonical_file(dst_config, local=True)
