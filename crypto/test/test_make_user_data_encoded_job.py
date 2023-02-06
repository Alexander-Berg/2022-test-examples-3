import os
import time

import yatest.common

from crypta.lib.proto.user_data import user_data_pb2
from crypta.lib.proto.user_data.attribute_names_pb2 import TAttributeNames
from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)
from crypta.lib.python.yt.tm_utils.test_with_tm import TestWithTm
from crypta.siberia.bin.common import yt_schemas

BINARY_PATH = "crypta/siberia/bin/make_user_data_encoded/bin/crypta-make-user-data-encoded"
SRC_TABLE_FIELD = "src_user_data_table"
DST_TABLE_FIELD = "dst_user_data_table"
DST_WORD_DICT_TABLE_FIELD = "dst_word_dict_table"
DST_HOST_DICT_TABLE_FIELD = "dst_host_dict_table"
DST_APP_DICT_TABLE_FIELD = "dst_app_dict_table"
LAST_UPDATE_TIMESTAMP_ATTR = TAttributeNames().LastUpdateTimestamp


def convert_field_name(row, old_name, new_name):
    row[new_name] = row[old_name]
    del row[old_name]
    return row


def get_input_user_data_table(file_name, path, last_update_timestamp):
    on_write = tables.OnWrite(
        row_transformer=row_transformers.proto_dict_to_yson(user_data_pb2.TUserData),
        attributes={
            "schema": yt_schemas.get_user_data_schema(),
            LAST_UPDATE_TIMESTAMP_ATTR: last_update_timestamp,
        }
    )

    return tables.YsonTable(file_name, path, on_write=on_write)


def now():
    return int(time.time())


class TestMakeUserDataEncoded(TestWithTm):
    def get_output_table_tests(self, config, dst_timestamp):
        row_transformer = row_transformers.yson_to_proto_dict(user_data_pb2.TUserData)
        return sum([
            [
                (
                    tables.YsonTable("{}_user_data_encoded.yson".format(cluster), config[DST_TABLE_FIELD], yson_format="pretty", on_read=tables.OnRead(row_transformer=row_transformer)),
                    [
                        tests.Diff(yt_client=yt_client),
                        tests.SchemaEquals(yt_schemas.get_user_data_schema(), yt_client=yt_client),
                        tests.AttrEquals(LAST_UPDATE_TIMESTAMP_ATTR, dst_timestamp, yt_client=yt_client),
                    ]
                ),
                (tables.YsonTable("{}_word_dict.yson".format(cluster), config[DST_WORD_DICT_TABLE_FIELD], yson_format="pretty"), tests.Diff(yt_client=yt_client)),
                (tables.YsonTable("{}_host_dict.yson".format(cluster), config[DST_HOST_DICT_TABLE_FIELD], yson_format="pretty"), tests.Diff(yt_client=yt_client)),
                (tables.YsonTable("{}_app_dict.yson".format(cluster), config[DST_APP_DICT_TABLE_FIELD], yson_format="pretty"), tests.Diff(yt_client=yt_client)),
            ]
            for cluster, yt_client in (
                ("first", self.first_cluster_client),
                ("second", self.second_cluster_client),
            )
        ], [])

    def get_config_path(self, test_name):
        template_path = yatest.common.source_path("crypta/siberia/bin/make_user_data_encoded/bundle/config.yaml")
        working_dir = yatest.common.test_output_path("config.yaml")
        if not os.path.isdir(working_dir):
            os.makedirs(working_dir)

        config_path = os.path.join(working_dir, "config.yaml")

        print self.tm_proxy
        tm_host, tm_port = self.tm_proxy.rsplit(":", 1)
        print tm_host, tm_port

        parameters = {
            "environment": "qa",
            "yt_proxy": self.first_cluster,
            "yt_pool": "pool",
            "yt_working_dir": "//{}".format(test_name),
            "converter_memory_limit_mb": 512,
            "replica_yt_proxy": self.second_cluster,
            "tm_host": tm_host,
            "tm_port": tm_port,
        }

        templater.render_file(template_path, config_path, parameters)
        return config_path

    def test_with_no_prior_dict(self, frozen_time):
        config_path = self.get_config_path("test_with_no_prior_dict")
        config = yaml_config.load(config_path)

        src_timestamp = frozen_time

        return tests.yt_test(
            self.first_cluster_client,
            binary=yatest.common.binary_path(BINARY_PATH),
            args=[
                "--config", config_path,
            ],
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (get_input_user_data_table("user_data_1st_iter.yson", config[SRC_TABLE_FIELD], src_timestamp), tests.TableIsNotChanged()),
            ],
            output_tables=self.get_output_table_tests(config, src_timestamp),
        )

    def test_with_prior_dict(self, frozen_time):
        config_path = self.get_config_path("test_with_prior_dict")
        config = yaml_config.load(config_path)

        src_timestamp = frozen_time
        dst_timestamp = src_timestamp - 1

        return tests.yt_test(
            self.first_cluster_client,
            binary=yatest.common.binary_path(BINARY_PATH),
            args=[
                "--config", config_path,
            ],
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (get_input_user_data_table("user_data_2nd_iter.yson", config[SRC_TABLE_FIELD], src_timestamp), tests.TableIsNotChanged()),
                (get_input_user_data_table("user_data_encoded.yson", config[DST_TABLE_FIELD], dst_timestamp), None),
                (tables.YsonTable("host_dict_1st_iter.yson", config[DST_HOST_DICT_TABLE_FIELD]), None),
                (tables.YsonTable("word_dict_1st_iter.yson", config[DST_WORD_DICT_TABLE_FIELD]), None),
                (tables.YsonTable("app_dict_1st_iter.yson", config[DST_APP_DICT_TABLE_FIELD]), None),
            ],
            output_tables=self.get_output_table_tests(config, src_timestamp),
        )

    def test_with_up_to_date_user_data(self, frozen_time):
        config_path = self.get_config_path("test_with_up_to_date_user_data")
        config = yaml_config.load(config_path)

        src_timestamp = frozen_time
        dst_timestamp = frozen_time

        return tests.yt_test(
            self.first_cluster_client,
            binary=yatest.common.binary_path(BINARY_PATH),
            args=[
                "--config", config_path,
            ],
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (get_input_user_data_table("user_data_2nd_iter.yson", config[SRC_TABLE_FIELD], src_timestamp), tests.TableIsNotChanged()),
                (get_input_user_data_table("user_data_encoded.yson", config[DST_TABLE_FIELD], dst_timestamp), tests.TableIsNotChanged()),
                (tables.YsonTable("host_dict_1st_iter.yson", config[DST_HOST_DICT_TABLE_FIELD]), tests.TableIsNotChanged()),
                (tables.YsonTable("word_dict_1st_iter.yson", config[DST_WORD_DICT_TABLE_FIELD]), tests.TableIsNotChanged()),
                (tables.YsonTable("app_dict_1st_iter.yson", config[DST_APP_DICT_TABLE_FIELD]), tests.TableIsNotChanged()),
            ],
            output_tables=self.get_output_table_tests(config, src_timestamp)[3:],
        )
