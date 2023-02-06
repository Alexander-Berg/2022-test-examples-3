#!/usr/bin/env python
# -*- coding: utf-8 -*-

from crypta.lib.python import templater
from crypta.profile.runners.export_profiles.lib.affinity.host_affinity import YQL_QUERY_TEMPLATE


def test_precalculated_hosts_affinity_query():
    query = templater.render_template(
        YQL_QUERY_TEMPLATE,
        {
            'flattened_metrics_hits': 'flattened_metrics_hits_table',
            'flattened_bar_hits': 'flattened_bar_hits_table',
            'metrics_idf': 'metrics_idf_table',
            'bar_idf': 'bar_idf_table',
            'hosts_counter': 'hosts_counter_table',
            'output_table': 'output_table_table',
            'id_type': 'yandexuid',
            'min_id_count_with_site': 2,
        }
    )
    return query
