# -*- coding: utf-8 -*-

from datetime import datetime, timedelta
import market.dynamic_pricing.deprecated.utilities.lib.yt as yt
import time
import logging

from nile.api.v1 import (
    Record,
    clusters,
)

class YtClient:

    format_dict = {
        'sku_price': '%Y-%m-%d',
        'elasticity': '%Y-%m-%d',
        'config': '%Y-%m-%dT%H:%M:%S',
        'groups': '%Y-%m-%dT%H:%M:%S',
        'exp_schedule': '%Y-%m-%dT%H:%M:%S',
        'filter_prices': '%Y-%m-%dT%H:%M:%S',
        'deadstock_sales_path': '%Y-%m-%d',
    }

    def __init__(self, cluster, cur_ts, yt_token=None):
        cluster_params = {
            'proxy': cluster,
        }
        if yt_token is not None:
            cluster_params['token'] = yt_token
        self.yt = clusters.YT(**cluster_params)
        self.client = self.yt.driver.client
        self.cur_ts = cur_ts
        self.date = cur_ts[:10]

    # Get tables from folder by filter
    def _get_tables_in_dir(self, folder, filter_func=None):
        # get list of objects' names in folder
        content = self.client.list(folder)
        if filter_func:
            content = list(filter(filter_func, content))
        return content


    # Check that the tables in prod have their image in canon and update
    def update_tables(self, prod_paths, canon_paths, additional_paths):
        for name in prod_paths:
            # Checking/updating the name of table in canon path
            if name in canon_paths and name in self.format_dict:
                logging.info("Checking {0} table in canon.".format(name))
                prod_name = yt.latest_by_title_ts(self.cur_ts, prod_paths[name], self.client, self.format_dict[name])[-1]
                latest_canon_name = yt.latest_by_title_ts(self.cur_ts, canon_paths[name], self.client, self.format_dict[name])
                canon_name = latest_canon_name[-1] if latest_canon_name else 'does not exist'
                logging.info("prod_name {0}".format(prod_name))
                logging.info("canon_name {0}".format(canon_name))

                # If the title of table in prod is different from the one in canon, replace canon
                if str(canon_name) != str(prod_name):
                    self.client.copy(
                        "{0}/{1}".format(prod_paths[name], prod_name),
                        "{0}/{1}".format(canon_paths[name], prod_name),
                        preserve_expiration_time=True,
                        preserve_creation_time=True,
                        force=True,
                    )
                    logging.info("Renewed {0} table in canon, new table name is {1}.".format(name, prod_name))
                    # Also refresh the link in the additional folder
                    if name in additional_paths:
                        self.client.link(
                            "{0}/{1}".format(canon_paths[name], prod_name),
                            "{0}/latest".format(additional_paths[name]),
                            force=True,
                        )
                else:
                    logging.info("No need to renew {0} table in canon.".format(name))
            else:
                raise ValueError("Folder of type {} is neither in 'canon_paths' nor 'additional_paths'".format(name))

    # Remove previous result tables
    def cleanup(self, paths):
        for path in paths:
            filter_func = lambda x: self.cur_ts in x
            tables = self._get_tables_in_dir(path, filter_func)
            for table in tables:
                self.client.remove(path + '/' + table, force=True)
            self.client.remove(path + '/latest', force=True)

    # Shorthand way to find the target tables for the test
    def latest_table_by_path(self, table_type, path):
        logging.info("Get latest {0} by path {1}".format(table_type, path))
        table = yt.latest_by_title_ts(self.cur_ts, path, self.client, self.format_dict[table_type])[-1]
        return "{0}/{1}".format(path, table)

    # Compare tables
    def compare(self, expected_table, result_table, diff_path):
        logging.info("Compare tables {0} and {1}".format(expected_table, result_table))
        # These are the only columns are the keys of the output tables
        key_columns = frozenset(('market_sku', 'msku', 'ssku', 'group_id'))
        # A mapper to change the names of columns in he table with expected results
        def renaming_mapper(records):
            for r in records:
                out = {}
                r = r.to_frozen_dict()
                for col in r:
                    if col not in key_columns:
                        out['{}_exp'.format(col)] = r.get(col)
                    else:
                        out[col] = r.get(col)
                yield Record(out)
        # A mapper to compare columns in the merged table
        def comparing_mapper(records):
            for r in records:
                diff = set()
                r = r.to_dict()
                for col in r:
                    if col not in key_columns:
                        col = col.split('_exp')[0]
                        if r.get(col) != r.get('{}_exp'.format(col)):
                            diff.add(col)
                if len(diff) > 0:
                    yield Record(r, difference=list(diff))

        ts = datetime.now()
        diff_table = diff_path + '/' + ts.strftime('%Y-%m-%dT%H:%M:%S')
        with self.yt.driver.transaction():
            job = self.yt.job(name="market_autostrategy_pipeline_test").env(parallel_operations_limit=10)
            result = job.table(expected_table) \
                .map(renaming_mapper) \
                .join(
                    job.table(result_table),
                    by=list(key_columns),
                    type='full',
                    assume_unique=True
                ) \
                .map(comparing_mapper) \
                .put(diff_table, ttl=timedelta(days=90))

            job.run()
            self.client.link(diff_table, diff_path + '/latest', force=True)

        # check diff
        assert self.client.is_empty(diff_table), 'Test and canonic tables are different. Diff table: \"{}\"'.format(diff_table)

    # check tables existance
    def check_tables_exist(self, checked_paths, interval, timeout):
        # TODO: Test the diffs of the tables as they appear - should save some time
        def wait_until(predicate, interval, timeout, *args, **kwargs):
            timeout_fired_time = time.time() + timeout
            while time.time() < timeout_fired_time:
                if predicate(*args, **kwargs):
                    return
                time.sleep(interval)
            raise RuntimeError('Timeout expired')

        filter_func = lambda x: self.cur_ts in x # Check that the file is created
        check_func = lambda paths: all(self._get_tables_in_dir(p, filter_func) for p in paths)
        wait_until(
            check_func,
            interval,
            timeout,
            checked_paths
        )
