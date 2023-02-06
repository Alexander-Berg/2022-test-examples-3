# -*- coding: utf-8 -*-
from datetime import datetime, timedelta
import logging

from nile.api.v1 import Record, clusters


class TablesDiff:

    def __init__(self, cluster, pool, yt_token=None):
        cluster_params = {
            'proxy': cluster,
            'pool': pool,
        }
        if yt_token is not None:
            cluster_params['token'] = yt_token
        self.yt = clusters.YT(**cluster_params)
        self.client = self.yt.driver.client

    # Compare tables
    def compare(self, expected_table, actual_table, diff_path, key_columns, skip_columns, diff_eps=0.01, exception_on_diff=True):
        logging.info("Compare tables {0} and {1}".format(expected_table, actual_table))
        get_exp = lambda col: '{}_exp'.format(col)

        # A mapper to change the names of columns in the table with expected results
        def renaming_mapper(records):
            logging.info("Start renaming")
            for r in records:
                out = {}
                r = r.to_frozen_dict()
                for col in r:
                    val = r[col]
                    col = col.decode()
                    new_col = col if col in key_columns else get_exp(col)
                    out[new_col] = val
                yield Record(out)

        # A mapper to compare columns in the merged table
        def comparing_mapper(records):
            logging.info("Start compare")
            for r in records:
                diff = set()
                r = r.to_dict()
                for col in r:
                    val = r[col]
                    col = col.decode() if type(col) is bytes else col
                    if col.endswith("_exp") or col in key_columns or col in skip_columns:
                        continue
                    exp_val = r.get(get_exp(col))
                    if type(val) is float and type(exp_val) is float and diff_eps is not None and diff_eps != 'None':
                        if abs(val - exp_val) >= float(diff_eps):
                            diff.add(col)
                    elif val != exp_val:
                        diff.add(col)
                if len(diff) > 0:
                    yield Record(r, difference=list(diff))

        ts = datetime.now()
        diff_table = diff_path + '/' + ts.strftime('%Y-%m-%dT%H:%M:%S')
        with self.yt.driver.transaction():
            job = self.yt.job(name="market_autostrategy_canonic_test").env(parallel_operations_limit=10)
            job.table(expected_table) \
                .map(renaming_mapper) \
                .join(
                    job.table(actual_table),
                    by=list(key_columns),
                    type='full',
                    assume_unique=True
                ) \
                .map(comparing_mapper) \
                .put(diff_table, ttl=timedelta(days=30))

            job.run()
            self.client.link(diff_table, diff_path + '/latest', force=True)

        # check diff
        error_message = 'Test and canonic tables are different. Diff table: \"{}\"'.format(diff_table)
        if exception_on_diff:
            assert self.client.is_empty(diff_table), error_message
        else:
            logging.error(error_message)

    def copy_from_prod(self, production_table, expected_table):
        if not self.client.exists(expected_table):
            logging.info("Copy from production table {0} to {1}".format(production_table, expected_table))
            self.client.copy(production_table, expected_table)
