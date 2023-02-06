import argparse
import logging
import yt.wrapper as yt
from conf import Config
from sys import exit
from time import time


def get_scheme():
    return [{'name': 'job_id', 'type': 'string'},
            {'name': 'started', 'type': 'uint64'},
            {'name': 'url', 'type': 'string'},
            {'name': 'data', 'type': 'string'}]


class ResultMapper(object):
    def __init__(self, ttl):
        self.ttl = ttl

    def __call__(self, row):
        if row.get('job_id') is None:
            return
        if row['started'] + self.ttl < int(time()):
            return
        yield row


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    yt.initialize_python_job_processing()
    yt.config['pickling']['module_filter'] = lambda module: 'hashlib' not in getattr(module, '__name__', '') and 'hmac' != getattr(module, '__name__', '')
    config = Config()
    portions = map(lambda t: yt.ypath_join(config.yt_writer.output_prefix, t), yt.list(config.yt_writer.output_prefix)[:config.yt_merge.max_portions])
    if not portions:
        logging.info('empty portion list')
        exit(0)
    data_scheme = None
    if yt.exists(config.yt_merge.prev_state):
        portions.append(config.yt_merge.prev_state)
        data_scheme = yt.get_attribute(config.yt_merge.prev_state, '_yql_proto_field_data')
    with yt.Transaction():
        yt.run_map(ResultMapper(config.yt_merge.result_ttl), portions, yt.TablePath(config.yt_merge.new_state, schema=get_scheme()))
        yt.run_sort(config.yt_merge.new_state, sort_by=['job_id', 'started'])
        if data_scheme:
            yt.set_attribute(config.yt_merge.new_state, '_yql_proto_field_data', data_scheme)
        yt.move(config.yt_merge.new_state, config.yt_merge.prev_state, force=True)
        for tab in portions[:-1]:
            yt.remove(tab)
