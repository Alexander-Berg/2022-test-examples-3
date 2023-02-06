from nile.api.v1.local import ListSink, StreamSource
from nile.api.v1.clusters import MockCluster
from extsearch.unisearch.medicine.data_processing.test_lib import resource_records, dump_result_records, cmp_tables

from extsearch.unisearch.medicine.data_processing.avatars.after_avatars.lib import after_avatars


def test_after_avatars():
    cluster = MockCluster()
    job = cluster.job().env(bytes_decode_mode='strict')

    prefix = 'resfs/file/extsearch/unisearch/medicine/data_processing/avatars/after_avatars/ut/data/'
    doctors = StreamSource(resource_records(prefix + 'doctors.yson'))
    urls = StreamSource(resource_records(prefix + 'urls.yson'))

    after_avatars(
        job,
        doctors=job.table('doctors').label('doctors'),
        urls=job.table('urls').label('urls'),
        url_format_string='https://avatars.mdst.yandex.net/get-unisearch/{group_id}/{imagename}/210x210',
    ).label('output')

    output = []

    job.local_run(
        sources={
            'doctors': doctors,
            'urls': urls,
        },
        sinks={
            'output': ListSink(output),
        },
    )

    dump_result_records(output, 'after_avatars_output.txt')

    expected = list(resource_records(prefix + 'output.yson'))
    cmp_tables(expected, output)
