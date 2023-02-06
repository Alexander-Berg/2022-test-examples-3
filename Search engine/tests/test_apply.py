# coding=utf-8
from nile.api.v1 import (
    clusters,
    Record,
    extractors as ne,
)
from search.gta.ltv.hc1_distribuct.hottie.lib import apply
from search.gta.ltv.hc1_distribuct.hottie.lib.requests import (
    get_stream_log,
    get_stream_requests_has,
    get_stream_requests_value,
    get_requests_streams,
)


def get_create_sample():
    cluster = clusters.MockCluster()
    job = cluster.job()

    debug_input = []

    # Добавляем всякие ненужные записи, чтобы составились блумовские фильтры.
    for i in range(1000):
        yandexuid = "0000" + str(i)
        debug_input.append(Record(yandexuid=yandexuid, geo_id=1, ))
        debug_input.append(Record(yandexuid=yandexuid, geo_id=1, ))

    # Добавляем нужные записи. Три поиска у yandexuid=111
    debug_input += [
        Record(yandexuid='1111', user_redirs_count=1, geo_id=1),
        Record(yandexuid='1111', user_redirs_count=1, geo_id=2),
        Record(yandexuid='1111', user_redirs_count=2, geo_id=2),
    ]

    streams = get_requests_streams(
        use_6p=False,
        job=job,
        date='2018-01-01',
        debug_input=debug_input,
        bit_count_exp=18,
    )
    out = []
    streams['requests'].debug_output(out)
    job.debug_run()
    return out


def test_apply_one():
    cluster = clusters.MockCluster()
    job = cluster.job()
    stream_hottie = job.table('').debug_input(get_create_sample())

    # Это три yandexuid , которые мы будем искать в фильтрах
    debug_input = [
        Record(yandexuid='1111'),
        Record(yandexuid='2222'),
        Record(yandexuid='3333'),
    ]
    stream_activation = job.table('').debug_input(debug_input)

    stream_hypercube = apply.get_stream_userhistory(
        stream_hottie,
        stream_activation,
        '2018-01-01',
        '2018-01-01',
        add_initial=True,
    )
    out = []
    stream_hypercube.debug_output(out)
    job.debug_run()
    assert out
    assert next((x for x in out if x.yandexuid == '1111' and x.date == '2018-01-01')).requests == 3


def test_apply_many():
    cluster = clusters.MockCluster()

    job = cluster.job()

    stream_hottie = job.table('').debug_input(get_create_sample())

    debug_input = []
    for i in range(1000):
        yandexuid = "0000" + str(i)
        debug_input.append(Record(
            yandexuid=yandexuid,
        ))

    stream_activation = job.table('').debug_input(debug_input)

    stream_hypercube = apply.get_stream_userhistory(
        stream_hottie,
        stream_activation,
        '2018-01-01',
        '2018-01-01',
    )
    out = []
    stream_hypercube.debug_output(out)
    job.debug_run()
    assert len(out) == 1000
    assert {x.requests for x in out} == {2}


def test_create_multiple_jobs():
    cluster = clusters.MockCluster()

    N = 5
    bit_count_exp = 8
    hottie_records = []

    for i in range(N):
        create_job = cluster.job()
        out = []
        request_records = []
        for j in range(i * i):
            request_records.append(Record(
                yandexuid="__{}_{}".format(i, j)
            ))

        requests_input_stream = create_job.table('').debug_input(request_records)
        stream_log = get_stream_log(requests_input_stream)
        stream_requests_has = get_stream_requests_has(stream_log, bit_count_exp)
        stream_requests_value = get_stream_requests_value(stream_log, bit_count_exp)
        stream_requests_result = stream_requests_has.concat(stream_requests_value).project(
            ne.all(),
            date=ne.const('2018-01-01'),
        )

        stream_requests_result.debug_output(out)
        create_job.debug_run()

        hottie_records += out

    for o in hottie_records:
        print(o.filter_type)

    apply_job = cluster.job()
    stream_hottie = apply_job.table('').debug_input(hottie_records)
    activations_records = []
    activations_count = 0
    for i in range(N):
        for j in range(i * i):
            activations_count += 1
            activations_records.append(Record(
                yandexuid="__{}_{}".format(i, j)
            ))
    stream_activation = apply_job.table('').debug_input(activations_records)
    stream_hypercube = apply.get_stream_userhistory(
        stream_hottie,
        stream_activation,
        '2018-01-01',
        '2018-01-01',
    )
    out = []
    stream_hypercube.debug_output(out)
    apply_job.debug_run()
    assert len(out) == activations_count
    assert {x.requests for x in out} == {1}
