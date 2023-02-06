# coding=utf-8
import random
from nile.api.v1 import (
    clusters,
    Record,
)
from search.gta.ltv.hc1_distribuct.hottie.lib.requests import (
    get_requests_streams,
)


def test_create_requests_geo_id():
    cluster = clusters.MockCluster()

    job = cluster.job()
    debug_input = []
    for _ in range(10):
        debug_input.append(Record(
            yandexuid="".join(random.choice('1234567890') for _ in range(2)),
            user_redirs_count=random.choice(list(range(10))),
            geo_id=1,
        ))

    streams = get_requests_streams(
        use_6p=False,
        job=job,
        date='2018-01-01',
        debug_input=debug_input,
    )
    out = []
    streams['requests_geo_id'].debug_output(out)

    job.debug_run()
    assert [x for x in out if x.type == 'has']
    assert [x for x in out if x.type == 'value']
    assert [x for x in out if x.get('value') == 1]


def test_create_requests():
    cluster = clusters.MockCluster()

    job = cluster.job()
    debug_input = []
    for _ in range(1000):
        debug_input.append(Record(
            yandexuid="00" + "".join(random.choice('1234567890') for _ in range(2)),
            user_redirs_count=random.choice(list(range(10))),
            geo_id=1,
        ))

    streams = get_requests_streams(
        use_6p=False,
        job=job,
        date='2018-01-01',
        debug_input=debug_input,
        bit_count_exp=10,
    )
    out = []
    streams['requests'].debug_output(out)

    job.debug_run()

    assert [x for x in out if x.filter_type == 'bloom' and x.value_type == 'requests' and x.type == 'has']
    assert [x for x in out if x.filter_type == 'bloom' and x.value_type == 'requests' and x.type == 'value']
