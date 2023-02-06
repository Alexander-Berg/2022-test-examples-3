import mock
from yql_utils import yql_binary_path

from crypta.graph.sampling.lib import RandomSampler, StaffSampler
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output, clean_up

TEST_DATE_1 = "2018-12-12"
TEST_DATE_2 = "2018-12-13"
TEST_PERCENT = 0.5
TEST_TMP_DIR = "//tmp/test"

COMPONENTS_PATH = "//home/components"
SOUP_EDGES_PATH = "//home/crypta/test/state/graph/v2/soup/cooked/soup_edges"
SOUP_PROPERTIES_PATH = "//home/crypta/test/state/graph/v2/soup/cooked/vertices_properties"


def select_all(yt, table_path):
    return list(yt.yt_client.read_table(table_path, format='json'))


@mock.patch.dict('os.environ', {"YT_TOKEN": "test", "ENV_TYPE": "test"})
@load_fixtures(
    (COMPONENTS_PATH, '/components.json'),
    (SOUP_EDGES_PATH, '/soup_edges.json'),
    (SOUP_PROPERTIES_PATH, '/vertices_properties.json'), )
@canonize_output
@clean_up()
def test_run_random_sample(yt):
    yt.yt_client.create("map_node", TEST_TMP_DIR + "/tmp", recursive=True, ignore_existing=True)
    yt.yt_client.set('{path}/@generate_date'.format(path=SOUP_EDGES_PATH), TEST_DATE_1)
    yt.yt_client.set('{path}/@generate_date'.format(path=SOUP_PROPERTIES_PATH), TEST_DATE_2)

    output_edges_path = "//home/soup_edges"
    output_properties_path = "//home/soup_properties"
    sampler = RandomSampler(
        components_table=COMPONENTS_PATH,
        percent=TEST_PERCENT,
        output_edges=output_edges_path,
        output_properties=output_properties_path,
        yt_proxy="localhost:{port}".format(port=yt.yt_proxy_port),
        is_embedded=True,
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
    )

    sampler.run(tmp_dir=TEST_TMP_DIR)

    assert yt.yt_client.get_attribute(output_edges_path, sampler.GENERATE_DATE_ATTR) == TEST_DATE_1
    assert yt.yt_client.get_attribute(output_properties_path, sampler.GENERATE_DATE_ATTR) == TEST_DATE_2
    assert yt.yt_client.get_attribute(output_properties_path, sampler.PERCENT_ATTR) == TEST_PERCENT

    output_tables = (output_edges_path, output_properties_path, )
    return {table: sorted(select_all(yt, table)) for table in output_tables}


@mock.patch.dict('os.environ', {"YT_TOKEN": "test", "ENV_TYPE": "test"})
@load_fixtures(
    (COMPONENTS_PATH, '/components.json'),
    (SOUP_EDGES_PATH, '/soup_edges.json'),
    (SOUP_PROPERTIES_PATH, '/vertices_properties.json'),
    ("//home/crypta/test/graph/staff", "/staff.json"), )
@canonize_output
@clean_up()
def test_run_staff_sample(yt):
    yt.yt_client.create("map_node", TEST_TMP_DIR + "/tmp", recursive=True, ignore_existing=True)
    yt.yt_client.set('{path}/@generate_date'.format(path=SOUP_EDGES_PATH), TEST_DATE_1)
    yt.yt_client.set('{path}/@generate_date'.format(path=SOUP_PROPERTIES_PATH), TEST_DATE_2)

    output_edges_path = "//home/soup_edges"
    output_properties_path = "//home/soup_properties"
    sampler = StaffSampler(
        components_table=COMPONENTS_PATH,
        output_edges=output_edges_path,
        output_properties=output_properties_path,
        yt_proxy="localhost:{port}".format(port=yt.yt_proxy_port),
        is_embedded=True,
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")]),
    )

    sampler.run(tmp_dir=TEST_TMP_DIR)

    assert yt.yt_client.get_attribute(output_edges_path, sampler.GENERATE_DATE_ATTR) == TEST_DATE_1
    assert yt.yt_client.get_attribute(output_properties_path, sampler.GENERATE_DATE_ATTR) == TEST_DATE_2

    output_tables = (output_edges_path, output_properties_path, )
    return {table: sorted(select_all(yt, table)) for table in output_tables}
