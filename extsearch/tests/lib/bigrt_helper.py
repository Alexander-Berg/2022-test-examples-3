from __future__ import print_function

from ads.bsyeti.big_rt.py_test_lib import (
    BulliedProcess,
    waiting_iterable,
    launch_bullied_processes_reading_queue,
    suppressing_context
)

import itertools
import logging
import yatest.common
import yt.wrapper


class StatefulProcess(BulliedProcess):
    def __init__(self, config_path, binary_path, config_option):
        super(StatefulProcess, self).__init__(
            launch_cmd=[binary_path, config_option, config_path]
        )


def stateful_launch_k_process(stand, data, binary_path, k, stable=True):
    configs = [
        stand.make_stateful_config(worker_minor_name=str(worker), workers=k)
        for worker in range(k)
    ]
    statefuls = [StatefulProcess(config.path, binary_path, stand.config_option) for config in configs]
    restart_randmax = None if stable else stand.restart_max_seconds
    launch_bullied_processes_reading_queue(statefuls, stand.input_yt_queue, stand.queue_consumer, data, restart_randmax=restart_randmax, timeout=600)


def stateful_launch_one_process_stable(**args):
    stateful_launch_k_process(k=1, **args)


def stateful_launch_one_process_unstable(**args):
    stateful_launch_k_process(k=1, stable=False, **args)


def stateful_launch_two_process_stable(**args):
    stateful_launch_k_process(k=2, **args)


def stateful_launch_two_process_unstable(**args):
    stateful_launch_k_process(k=2, stable=False, **args)


stateful_launchers = [
    stateful_launch_one_process_stable,
    stateful_launch_one_process_unstable,
    stateful_launch_two_process_stable,
    stateful_launch_two_process_unstable,
]


class TStatefulProcessTester:
    def __init__(self,
                 stand):
        self.stand = stand
        self.input_data = None

    def write_input_queue(self, input_queue_data):
        '''
        :param input_queue_data: Dict<int,[str]> - Mapping shard_id to list serialised Queue Value Message protobuf
        :return: None
        '''
        self.stand.input_yt_queue["queue"].write(input_queue_data)
        self.input_data = input_queue_data

    def write_into_state(self, state_data, state_table_name):
        '''
        :param state_data: list[dict[Any, str]] testing state data.
        Each row in list is mapping of table column name to column value.
        Example [dict(Key="super", Value="duper")] for table with columns Key, Value.
        :param state_table_name: Table name in table_config.json
        :return: None
        '''
        self.stand.yt_client.insert_rows(
            self.stand.table_settings[state_table_name].table_path,
            state_data,
            format='yson',
        )

    def process(self, stateful_launcher, binary_path):
        stateful_launcher(stand=self.stand,
                          data=self.input_data,
                          binary_path=yatest.common.binary_path(binary_path))

        logging.info("Stateful processor read all messages and was finished")

    def __get_rows(self, query):
        yson_format = yt.wrapper.YsonFormat(encoding=None)
        for waiting_state in waiting_iterable(timeout=60, period=8):
            with suppressing_context(do_suppress=not waiting_state.is_last):
                rows = self.stand.yt_client.select_rows(query, yt.wrapper.SYNC_LAST_COMMITED_TIMESTAMP, format=yson_format)
                return rows

    def get_state_result(self, state_table_name):
        '''
        :param state_table_name: Table name in table_config.json
        '''
        rows = self.__get_rows(query="* from [{table}]".format(table=self.stand.table_settings[state_table_name].table_path))

        return rows

    def wait(self):
        for waiting_state in waiting_iterable(timeout=60):
            if waiting_state.is_last:
                break

    def read_from_queue(self, output_queue_name):
        '''
        :param output_queue_name: Output queue name from table_config.json
        :return: generator<int, str> Tuple (shard number, Serialized output queue protobuf messages)
        '''
        table_params, output_yt_queue = self.stand.output_queue_settings[output_queue_name]
        for shard in range(output_yt_queue["shards"]):
            shard_result = output_yt_queue["queue"].read(shard, 0, 10000000)
            for result in shard_result["rows"]:
                yield shard, result

    def get_output_queue_shards_number(self, output_queue_name):
        '''
        :param output_queue_name: Output queue name from table_config.json
        :return: shards in output_queue_name
        '''
        table_params, output_yt_queue = self.stand.output_queue_settings[output_queue_name]
        return output_yt_queue["shards"]

    @staticmethod
    def __gen_testing_data(data_part_length):
        for j in range(1, 1000):
            cnt = int(2.5 ** j)
            yield [i % cnt for i in range(data_part_length)]

    @staticmethod
    def gen_shard_testing_schemas(shards_count, data_part_length):
        schemas = itertools.islice(TStatefulProcessTester.__gen_testing_data(data_part_length), shards_count)

        data = {
            shard: ["%d" % ((u + 1) * shards_count + shard) for u in schema]
            for shard, schema in enumerate(schemas)
        }

        return data
