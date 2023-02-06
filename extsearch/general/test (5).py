from nile.api.v1.local import ListSink, StreamSource
from nile.api.v1.clusters import MockCluster
from extsearch.unisearch.medicine.data_processing.test_lib import resource_records, dump_result_records, cmp_tables

from extsearch.unisearch.medicine.data_processing.dedublicate_doctors2.lib import dedublicate_doctors


def dedublicate_doctors_test(input_name, output_name):
    cluster = MockCluster()
    job = cluster.job().env(bytes_decode_mode='strict')

    prefix = 'resfs/file/extsearch/unisearch/medicine/data_processing/dedublicate_doctors2/ut/data/'
    doctors = StreamSource(resource_records(prefix + input_name))

    dedublicate_doctors(
        job,
        doctors=job.table('doctors').label('doctors'),
    )['doctors'].label('output')

    output = []

    job.local_run(
        sources={
            'doctors': doctors,
        },
        sinks={
            'output': ListSink(output),
        },
    )

    dump_result_records(output, output_name)

    expected = list(resource_records(prefix + output_name))
    cmp_tables(expected, output)


def test_dedublicate_doctors2_one_offer_per_host():
    dedublicate_doctors_test('one_offer_per_host.yson', 'one_offer_per_host_output.yson')


def test_group_doctor_ugc_reviews():
    dedublicate_doctors_test('group_ugc_reviews.yson', 'group_ugc_reviews_output.yson')
