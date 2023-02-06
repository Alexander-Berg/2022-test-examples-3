# -*- coding: utf-8 -*-

"""
    Author: Alexei Gorbatyy <alexei-gor@yandex-team.ru>
    Python Version: 2.7

    Формирует таблицу эксперимента по браузерному test_id.
"""

import json

from nile.api.v1 import (
    Record,
    with_hints,
    filters as nf,
)

from qb2.api.v1 import (
     typing as qt,
)

from environment import init_environment


def parse_experiment(
    yt_cluster,
    yt_client,
    experiment_name,
    sessions_path,
    output_path,
    start,
    stop,
    test_ids,
):
    """Формирует таблицу бразуерного эксперимента."""

    class Reducer(object):
        def __init__(self, test_ids):
            self.test_ids = test_ids

        def __call__(self, sessions):
            for key, records in sessions:
                involved = 0
                involved_exp_name = ''
                involved_exp_date = None
                involved_exp_ts = 0

                for record in records:
                    exp_days_abt = record.exp_days_abt
                    exp_timestamps_abt = record.exp_timestamps_abt

                    if exp_days_abt:
                        for exp_name in self.test_ids:
                            if exp_name in exp_days_abt:
                                pos = exp_days_abt.index(exp_name)
                                exp_day = exp_days_abt[pos + 1]

                                pos = exp_timestamps_abt.index(exp_name)
                                involved_exp_ts = exp_timestamps_abt[pos + 1] if pos != -1 else 0

                                if exp_day == 1 and involved_exp_name != exp_name:
                                    involved += 1
                                    involved_exp_name = exp_name
                                    involved_exp_date = record.get('date')

                if involved == 1 and involved_exp_ts > 0:
                    yield Record(
                        uuid=key.uid,
                        test_id=involved_exp_name,
                        exp_date=involved_exp_date,
                        exp_ts=involved_exp_ts,

                    )

    job = yt_cluster.job()

    dates = '{%s..%s}' % (start, stop)
    output_name = '{}/{}/users/exp_users'.format(output_path, experiment_name)

    if not yt_client.exists(output_name) \
            or yt_client.is_empty(output_name):

        sessions_schema = {
            'uid': str,
            'exp_days_abt': qt.List[qt.Any],
            'date': str,
        }

        scheme = dict(
            uuid=str,
            exp_date=str,
            test_id=str,
        )

        sessions = job.table('{}/{}'.format(sessions_path, dates),
                            weak_schema=sessions_schema)

        sessions \
            .filter(
                nf.equals('app_platform', 'Android'),
                nf.equals('device_type', 'PHONE'),
                nf.equals('api_key', 106400),
            ) \
            .groupby('uid') \
            .reduce(with_hints(output_schema=scheme)(Reducer(test_ids))) \
            .put(output_name)

        job.run()

        yt_cluster.driver.set_attribute(output_name, 'experiment_start_date', start)
        yt_cluster.driver.set_attribute(output_name, 'experiment_stop_date', stop)
        yt_cluster.driver.set_attribute(output_name, 'experiments', test_ids)

    return output_name


def main():
    # инициализация окружения
    yt_cluster, yt_client, parameters, input_items, output_items = init_environment()

    start_date = parameters['start_date']
    stop_date = parameters['stop_date']

    test_ids = parameters['test_ids']
    output_path = parameters['output']
    experiment_name = parameters['experiment_name']

    sessions_path = None
    with open(input_items['sessions'][0]['unpackedFile']) as f:
        d = json.loads(f.read())
        sessions_path = d['path']

    output_name = parse_experiment(
            yt_cluster,
            yt_client,
            experiment_name,
            sessions_path,
            output_path,
            start_date,
            stop_date,
            test_ids,
        )

    # возвращаем результат
    with open(output_items['exp_table'][0]['unpackedFile'], 'w') as f:
        json.dump({'cluster': 'hahn', 'table': output_name}, f, indent=2)


if __name__ == '__main__':
    main()
