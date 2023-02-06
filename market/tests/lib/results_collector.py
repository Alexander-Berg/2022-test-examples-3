# -*- coding: utf-8 -*-

import logging
from nile.api.v1 import clusters
import market.dynamic_pricing.deprecated.utilities.lib.yt as yt

format_dict = dict()  # A placeholder for various formats that can be in the pipeline


def publish_func(cluster, test_output_dir, canon_dir, results_paths_dict, cur_ts):
    client = cluster.driver.client
    # Copying the tables from test to canon
    for name, path in results_paths_dict.items():
        logging.info("Transferring {0} table to canon.".format(name))
        output_path = "{0}/{1}".format(test_output_dir, path)
        output_name = yt.latest_by_title_ts(cur_ts, output_path, client, format_dict.get(name, '%Y-%m-%dT%H:%M:%S'))[-1]
        client.copy(
            "{0}/{1}".format(output_path, output_name),
            "{0}/{1}/{2}".format(canon_dir, path, output_name),
            preserve_expiration_time=True,
            preserve_creation_time=True,
            force=True,
        )
        logging.info("Renewed {0} table in canon.".format(name))


def results_collector(yt_token, cluster, test_output_dir, canon_dir, results_paths_dict, cur_ts):
    cluster_params = {
        'proxy': cluster,
    }
    if yt_token is not None:
        cluster_params['token'] = yt_token
    cluster = clusters.YT(**cluster_params)
    with cluster.driver.transaction():
        # Compute tables
        publish_func(cluster, test_output_dir, canon_dir, results_paths_dict, cur_ts)
