# coding=utf-8
import random
from nile.api.v1 import (
    clusters,
    Record,
)
from search.gta.ltv.hc1_distribuct.hottie.lib.crypta import (
    get_crypta_streams,
)


def test_create_age():
    cluster = clusters.MockCluster()

    job = cluster.job()
    debug_input = []
    for _ in range(10):
        debug_input.append(Record(
            yandexuid=random.randint(0, 10000),
            exact_socdem={
                'age_segment': random.choice(['0_17', '18_24', '55_99']),
            }
        ))
        debug_input.append(Record(
            yandexuid=random.randint(0, 10000),
            exact_socdem={
                'age_segment': '0_17',
            }
        ))

    streams = get_crypta_streams(
        job=job,
        date='2018-01-01',
        debug_input=debug_input,
    )
    out = []
    stream, output_schema_type = streams['age']
    stream.debug_output(out)

    job.debug_run()
    assert [x for x in out if x.type == 'value']
    assert [x for x in out if x.get('value') == '0_17']
