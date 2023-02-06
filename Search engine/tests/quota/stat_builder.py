# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import numpy as np

from collections import defaultdict

from search.resonance.pylib.loadgen import LoadgenClientLocal


def calc_metrics(values):
    sorted_values = sorted(values)
    if not sorted_values:
        sorted_values = (0,)
    values = np.array(values)
    return {
        'mean': values.mean(),
        'variance': values.std(),
        'quantile99': sorted_values[int(len(sorted_values) * 0.99)],
        'quantile95': sorted_values[int(len(sorted_values) * 0.95)],
        'quantile70': sorted_values[int(len(sorted_values) * 0.70)],
    }


def build_stat(loadgen: LoadgenClientLocal, quota_value, rps):
    d = defaultdict(lambda: defaultdict(int))
    for stat in loadgen.get_stats():
        d[int(stat.start)][stat.code] += 1
    sorted_stat = sorted(d.items(), key=lambda x: x[0])
    part_200 = [value['200'] / quota_value for x, value in sorted_stat]
    part_errors = [
        (sum(value.values()) - value['200'] - value['429'])
        / max(1, sum(value.values()))
        for x, value in sorted_stat
    ]
    real_requests = [value['200'] + value['429'] for x, value in sorted_stat]

    return {
        'quota': quota_value,
        'rps': rps,
        '200': calc_metrics(part_200),
        'real_rps': calc_metrics(real_requests),
        'errors': calc_metrics(part_errors),
    }


def print_stat_result(logger, task_result):
    logger.info(
        'quota=%s rps=%s real_rps=%s errors(prc)=%s consumed(prc)=%s',
        task_result['quota'],
        task_result['rps'],
        task_result['real_rps']['mean'],
        task_result['errors']['mean'],
        task_result['200']['mean'],
    )
