from utils import execute_directbytypes, execute_main_task
from crypta.lib.python.yql_runner.tests import canonize_output
from data import (
    generate_test_dates_graph,
    generate_test_fuzzy_direct_graph,
)


@canonize_output
def test_directbytypes(directbytypes_table, config):

    yt, edges_by_crypta_id, vertices_no_multi_profile_by_id_type, version_edges, run_date = directbytypes_table
    assert yt.exists(edges_by_crypta_id)
    execute_directbytypes(run_date)

    by_id_path = '{root}/state/graph/v2/matching/by_id/{{table}}'.format(
        root=config.direct_config.paths.root)

    assert yt.exists(by_id_path.format(table='avito_hash/crypta_id'))
    assert yt.exists(by_id_path.format(table='avito_hash/direct/email'))
    assert yt.exists(by_id_path.format(table='crypta_id/crypta_id1'))

    assert yt.get_attribute(by_id_path.format(table='avito_hash/crypta_id'), 'sorted')
    assert yt.get_attribute(by_id_path.format(
        table='avito_hash/direct/email'), 'sorted_by') == ['id', 'id_type']

    assert yt.row_count(by_id_path.format(table='avito_hash/crypta_id')) == 1
    assert yt.row_count(by_id_path.format(table='avito_hash/direct/email')) == 1
    assert yt.row_count(by_id_path.format(table='crypta_id/crypta_id1')) == 5

    return {
        path.replace(config.direct_config.paths.root, 'direct_ROOT'):
            sorted(yt.read_table(path, format='json'))
        for path in yt.search(config.direct_config.paths.root, node_type=['table', ], )
    }


def test_main_task(main_task_table, config):
    yt, edges_by_crypta_id, vertices_no_multi_profile_by_id_type, version_edges, _ = main_task_table
    direct_yandexuid_by_id_type_and_id = config.direct_config.paths.direct.direct_yandexuid_by_id_type_and_id
    assert yt.exists(edges_by_crypta_id)
    assert yt.exists(vertices_no_multi_profile_by_id_type)
    assert yt.exists(version_edges)

    execute_main_task()

    by_id_path = '{root}/state/graph/v2/matching/by_id/{{table}}'.format(
        root=config.direct_config.paths.root)

    assert yt.exists(by_id_path.format(table='avito_hash/crypta_id'))
    assert yt.exists(by_id_path.format(table='avito_hash/direct/email'))
    assert yt.exists(by_id_path.format(table='crypta_id/crypta_id1'))

    assert yt.get_attribute(by_id_path.format(table='avito_hash/crypta_id'), 'sorted')
    assert yt.get_attribute(by_id_path.format(
        table='avito_hash/direct/email'), 'sorted_by') == ['id', 'id_type']

    assert yt.row_count(by_id_path.format(table='avito_hash/crypta_id')) == 1
    assert yt.row_count(by_id_path.format(table='avito_hash/direct/email')) == 1
    assert yt.row_count(by_id_path.format(table='crypta_id/crypta_id1')) == 5

    assert yt.exists(direct_yandexuid_by_id_type_and_id)
    assert yt.get_attribute(direct_yandexuid_by_id_type_and_id, 'row_count') == 38

    check_dates(yt, by_id_path)
    check_fuzzy_direct_columns(yt, by_id_path)


def check_dates(yt, by_id_path):
    edges, expected_result = generate_test_dates_graph()
    for (id_type, target_id_type), values in expected_result.items():
        path = by_id_path.format(table='{}/direct/{}'.format(id_type, target_id_type))
        for record in yt.read_table(path):
            true_record = values.get((record["id"], record["target_id"]), None)
            if true_record is not None:
                assert record["date_begin"] == true_record["date_begin"], \
                    "{} begin {} != {}".format(true_record["name"], record, true_record)
                assert record["date_end"] == true_record["date_end"], \
                    "{} end {} != {}".format(true_record["name"], record, true_record)


def check_meta_columns(yt, by_id_path, real_pairs, column="direct", expected_value=True):
    for (id1, id1_type), (id2, id2_type) in real_pairs:
        path = by_id_path.format(table='{}/direct/{}'.format(id1_type, id2_type))
        if "crypta_id" in (id1_type, id2_type):
            path = by_id_path.format(
            table='{}/{}'.format(id1_type, id2_type))

        for record in yt.read_table(path):
            if (id1 == record["id"]) and (id2 == record["target_id"]):
                assert record[column] == expected_value, "({}, {}) {} != {}".format(id1_type, id2_type, column, expected_value)
                break
        else:
            assert False, "record {} is not found".format(((id1, id1_type), (id2, id2_type)))


def check_fuzzy_direct_columns(yt, by_id_path):

    edges, fuzzy_pairs, direct_pairs = generate_test_fuzzy_direct_graph()
    check_meta_columns(yt, by_id_path, fuzzy_pairs, "fuzzy", True)
    check_meta_columns(yt, by_id_path, direct_pairs, "direct", True)
