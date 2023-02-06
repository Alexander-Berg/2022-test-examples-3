from __future__ import print_function

from ads.bsyeti.big_rt.py_test_lib import (
    make_namedtuple, make_json_file,
    create_yt_queue, execute_cli)

from library.python.sanitizers import asan_is_on


import contextlib
import itertools
import jinja2
import logging
import os
import pytest
import re
import sys
import yatest.common

import json


def create_state_table(yt_client, path, schema):
    print(">>> CREATE_TABLE_PATH: " + path, file=sys.stderr)

    logging.info("Creating state table %s", path)
    yt_client.create("table", path, recursive=True, attributes={
        "dynamic": True,
        "schema": schema
    })
    logging.info("Mounting state table %s", path)
    yt_client.mount_table(path, sync=True)
    logging.info("Mounted state table %s", path)
    return path


def remove_state_table(yt_client, path):
    logging.info("Unmounting state table %s", path)
    yt_client.unmount_table(path, sync=True)
    logging.info("Removing state table %s", path)
    yt_client.remove(path)
    logging.info("Removed state table %s", path)


class TStand:
    class TTableSettings:
        def __init__(self, table_name, table_path, table_scheme, shard_count_param_name=None, shard_count=None):
            self.table_name = table_name
            self.table_path = table_path
            self.table_scheme = table_scheme
            self.shard_count_param_name = shard_count_param_name
            self.shard_count = shard_count

    def __init__(self,
                 test_id,
                 standalone_yt_cluster,
                 port_manager,
                 bigrt_config_path,
                 table_config,
                 config_option):

        print("STAND " + test_id, file=sys.stderr)

        self.test_id = test_id
        self.consuming_system_path = "//tmp/foxxmary/test_consuming_system_" + test_id
        self.yt_client = standalone_yt_cluster.get_yt_client()

        self.standalone_yt_cluster = standalone_yt_cluster
        self.port_manager = port_manager
        self.bigrt_config_path = bigrt_config_path
        self.table_config = table_config
        self.config_option = config_option

        self.queue_consumer = "consumer_name"
        self.table_settings = {}
        self.input_queue_settings = None
        self.output_queue_settings = {}
        self.input_yt_queue = None
        self.input_shards_count = 10

        if not asan_is_on():
            self.data_part_length = 100
            self.restart_max_seconds = 40
        else:
            self.data_part_length = 40
            self.restart_max_seconds = 100

    @contextlib.contextmanager
    def prepare_tables(self, config):
        try:
            for table_settings in config["table_settings"]:
                table_path = table_settings["table_path"] + "_" + self.test_id
                table_params = TStand.TTableSettings(table_settings["table_name"],
                                                     table_path,
                                                     table_settings["table_scheme"])
                self.table_settings[table_params.table_name] = table_params

                print("CREATE TABLE " + table_path, file=sys.stderr)

                create_state_table(self.yt_client,
                                   table_params.table_path,
                                   table_params.table_scheme)

            if "input_queue_settings" in config:
                input_queue_settings = config["input_queue_settings"]
                input_queue_settings["table_path"] += "_" + self.test_id
                self.input_queue_settings = TStand.TTableSettings(input_queue_settings["table_name"],
                                                                  input_queue_settings["table_path"],
                                                                  None)

                self.input_yt_queue = create_yt_queue(self.standalone_yt_cluster.get_yt_client(),
                                                      input_queue_settings["table_path"],
                                                      input_queue_settings["input_shards_count"])

                self.shards_count = self.input_yt_queue["shards"]
                self.input_shards_count = input_queue_settings["input_shards_count"]

                execute_cli(["consumer",
                             "create",
                             self.input_yt_queue["path"],
                             self.queue_consumer,
                             "--ignore-in-trimming",
                             "0"])

            for output_queue_settings in config["output_queue_settings"]:
                table_path = output_queue_settings["table_path"] + "_" + self.test_id
                table_params = TStand.TTableSettings(output_queue_settings["table_name"],
                                                     table_path,
                                                     None,
                                                     output_queue_settings["shard_count_param_name"],
                                                     output_queue_settings["shard_count"])

                print("CREATE OUTPUT QUEUE " + table_path, file=sys.stderr)

                output_yt_queue = create_yt_queue(self.standalone_yt_cluster.get_yt_client(),
                                                  table_params.table_path,
                                                  table_params.shard_count)

                self.output_queue_settings[table_params.table_name] = (table_params, output_yt_queue)

                execute_cli(["consumer",
                             "create",
                             output_yt_queue["path"],
                             self.queue_consumer,
                             "--ignore-in-trimming",
                             "0"])

            yield None
        finally:
            for table_params in self.table_settings.values():
                remove_state_table(self.yt_client, table_params.table_path)

    def make_stateful_config(self, worker_minor_name, workers=1):
        config_params = {"shards_count": self.shards_count,
                         "max_shards": self.shards_count,
                         "port": self.port_manager.get_port(),
                         "consuming_system_main_path": self.consuming_system_path,
                         "consumer": self.queue_consumer,
                         "yt_cluster": os.environ["YT_PROXY"],
                         "global_log": os.path.join(yatest.common.output_path(), "global_{}.log".format(worker_minor_name)),
                         "worker_minor_name": worker_minor_name}

        for table_params in itertools.chain(self.table_settings.values(), [self.input_queue_settings]):
            config_params[table_params.table_name] = table_params.table_path

        for table_params, output_yt_queue in self.output_queue_settings.values():
            config_params[table_params.table_name] = table_params.table_path
            config_params[table_params.shard_count_param_name] = table_params.shard_count

        with open(self.bigrt_config_path) as f:
            conf_s = jinja2.Template(f.read()).render(config_params)

        print(">>> CONFIG: " + conf_s, file=sys.stderr)
        return make_namedtuple("StatefulConfig", path=make_json_file(conf_s, name_template="sharding_config_{json_hash}.json"))


@contextlib.contextmanager
@pytest.fixture()
def testing_stand(request,
                  standalone_yt_cluster,
                  standalone_yt_ready_env,
                  port_manager,
                  config_test_default_enabled,
                  bigrt_config_path,
                  table_config,
                  config_option):
    '''
    [
        {
            soure_name: "table_name",
            source_path: "source_path",
            source_schema: "{}"
        }, ...
    ]
    '''

    test_id = re.sub(r'[^\w\d]', '_', request.node.name)

    stand = TStand(test_id, standalone_yt_cluster, port_manager, bigrt_config_path, table_config, config_option)
    with stand.prepare_tables(table_config):
        yield stand


@pytest.fixture()
def table_config():
    with open(yatest.common.source_path('extsearch/images/robot/rt/tests/table_config.json')) as json_file:
        return json.load(json_file)


@pytest.fixture()
def bigrt_config_path():
    return yatest.common.source_path("extsearch/images/robot/rt/tests/bigrt_config.json")


@pytest.fixture()
def config_option():
    return "-c"
