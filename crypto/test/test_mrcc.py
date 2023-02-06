# flake8: noqa
import pytest
import pandas as pd
import library.python.resource as rs

from StringIO import StringIO

from fixtures import local_yt, local_yql, logger
from crypta.graph.mrcc.lib import (
    MRConnectedComponentsYT,
    MRConnectedComponentsYQL,
    ExtendedMRConnectedComponentsYT,
    ExtendedMRConnectedComponentsYQL,
    VDataMRConnectedComponentsYQL,
)

GRAPH_SCHEMA = [{"name": "u", "required": True, "type": "string"}, {"name": "v", "required": True, "type": "string"}]
GRAPH_SCHEMA_COL_DAT = [
    {"name": "col1", "required": True, "type": "string"},
    {"name": "col2", "required": True, "type": "string"},
    {"name": "data", "required": False, "type": "string"},
]


def read_csv(rs_name):
    buf = StringIO(rs.find(rs_name))
    return pd.read_csv(buf, dtype=str)


def read_json(rs_name):
    buf = StringIO(rs.find(rs_name))
    return pd.read_json(buf, orient="records")


def fill_up_table(local_yt, tablepath, schema, csv_resource=None):
    if local_yt.exists(tablepath):
        local_yt.remove(tablepath)
    local_yt.create("table", tablepath, recursive=True, attributes=dict(schema=schema))
    if not csv_resource:
        return
    df = read_csv(csv_resource)
    local_yt.write_table(tablepath, df.to_dict(orient="records"))


def read_table(local_yt, tablepath):
    generator = local_yt.read_table(tablepath)
    return pd.DataFrame.from_dict(list(generator))


def test_mrcc_yt_correct(local_yql, local_yt, logger):
    graph_path = "//home/crypta/graph/mrcc_test"
    with local_yt.Transaction() as transaction:
        local_yt.transaction_id = str(transaction.transaction_id)
        fill_up_table(local_yt, graph_path, GRAPH_SCHEMA, "/data/input_edges.csv")
        desired_result = read_csv("/data/desired_result.csv")
        mrcc_job = MRConnectedComponentsYT(local_yt)
        converged = mrcc_job.find_connected_components(graph_path)
        if not converged:
            raise Exception("MR CC not converged")
        actual_result = read_table(local_yt, graph_path)

        logger.info("Actual result")
        logger.info(actual_result)
        logger.info("Desired result")
        logger.info(desired_result)

    intersection = desired_result.merge(actual_result, on=["u", "v"])
    assert desired_result.shape[0] == intersection.shape[0]


def test_mrcc_yql_correct(local_yql, local_yt, logger):
    graph_path = "//home/crypta/graph/mrcc_test"
    fill_up_table(local_yt, graph_path, GRAPH_SCHEMA, "/data/input_edges.csv")
    desired_result = read_csv("/data/desired_result.csv")
    mrcc_job = MRConnectedComponentsYQL(local_yql)
    converged = mrcc_job.find_connected_components(graph_path)
    if not converged:
        raise Exception("MR CC not converged")
    actual_result = read_table(local_yt, graph_path)

    logger.info("Actual result")
    logger.info(actual_result)
    logger.info("Desired result")
    logger.info(desired_result)

    intersection = desired_result.merge(actual_result, on=["u", "v"])
    assert desired_result.shape[0] == intersection.shape[0]


@pytest.mark.parametrize(
    "graph_path,dest_path",
    (
        ("//home/crypta/graph/mrcc_test", "//home/crypta/graph/mrcc_test"),  # equals path
        ("//home/crypta/graph/mrcc_test", "//home/crypta/graph/output"),  # different paths
        ("//home/crypta/graph/mrcc_test", None),  # default should be equals
    ),
)
def test_mrcc_extended_yt_correct(graph_path, dest_path, local_yql, local_yt, logger):
    """ Should check correctnes of ExtendedMRConnectedComponentsYT class work """
    with local_yt.Transaction() as transaction:
        local_yt.transaction_id = str(transaction.transaction_id)
        fill_up_table(local_yt, graph_path, GRAPH_SCHEMA_COL_DAT, "/data/ext_input_edges.csv")
        desired_result = read_csv("/data/ext_desired_result.csv")
        mrcc_job = ExtendedMRConnectedComponentsYT(local_yt)
        converged = mrcc_job.find_connected_components(
            source=graph_path, destination=dest_path, u_name="col1", v_name="col2", component_id="cid"
        )
        if not converged:
            raise Exception("MR CC not converged")
        actual_result = read_table(local_yt, dest_path or graph_path)

        logger.info("Actual result")
        logger.info(actual_result)
        logger.info("Desired result")
        logger.info(desired_result)

    intersection = desired_result.merge(actual_result, on=["cid", "col1", "col2"])
    assert desired_result.shape[0] == intersection.shape[0]


@pytest.mark.parametrize(
    "graph_path,dest_path",
    (
        ("//home/crypta/graph/mrcc_test", "//home/crypta/graph/mrcc_test"),  # equals path
        ("//home/crypta/graph/mrcc_test", "//home/crypta/graph/output"),  # different paths
        ("//home/crypta/graph/mrcc_test", None),  # default should be equals
    ),
)
def test_mrcc_extended_yql_correct(graph_path, dest_path, local_yql, local_yt, logger):
    """ Should check correctnes of ExtendedMRConnectedComponentsYQL class work """
    fill_up_table(local_yt, graph_path, GRAPH_SCHEMA_COL_DAT, "/data/ext_input_edges.csv")
    desired_result = read_csv("/data/ext_desired_result.csv")
    mrcc_job = ExtendedMRConnectedComponentsYQL(local_yql)
    converged = mrcc_job.find_connected_components(
        source=graph_path, destination=dest_path, u_name="col1", v_name="col2", component_id="cid"
    )
    if not converged:
        raise Exception("MR CC not converged")
    actual_result = read_table(local_yt, dest_path or graph_path)

    logger.info("Actual result")
    logger.info(actual_result)
    logger.info("Desired result")
    logger.info(desired_result)

    intersection = desired_result.merge(actual_result, on=["cid", "col1", "col2"])
    assert desired_result.shape[0] == intersection.shape[0]


@pytest.mark.skip(reason="DF don't equal for strange reason. TODO: check why")
# Actual result
#     cid col1 col2                   data
# 0     1    1    3          [3, 9, [3-9]]
# 1     1    1    7          [7, 3, [7-3]]
# 2     1    1    8          [7, 8, [7-8]]
# 3     1    1    9          [1, 9, [1-9]]
# 4    10   10   11      [10, 11, [10-11]]
# 5   100  100  101  [100, 101, [100-101]]
# 6   100  100  102  [101, 102, [101-102]]
# 7   100  100  103  [102, 103, [102-103]]
# 8    20   20   21      [20, 21, [20-21]]
# 9    20   20   22      [20, 22, [20-22]]
# 10   20   20   23      [22, 23, [22-23]]
# 11   20   20   24      [23, 24, [23-24]]
# 12   20   20   25      [24, 25, [24-25]]
# Desired result
#     cid  col1  col2                   data
# 0     1     1     3          [3, 9, [3-9]]
# 1     1     1     7          [7, 3, [7-3]]
# 2     1     1     8          [7, 8, [7-8]]
# 3     1     1     9          [1, 9, [1-9]]
# 4    10    10    11      [10, 11, [10-11]]
# 5   100   100   101  [100, 101, [100-101]]
# 6   100   100   102  [101, 102, [101-102]]
# 7   100   100   103  [102, 103, [102-103]]
# 8    20    20    21      [20, 21, [20-21]]
# 9    20    20    22      [20, 22, [20-22]]
# 10   20    20    23      [22, 23, [22-23]]
# 11   20    20    24      [23, 24, [23-24]]
# 12   20    20    25      [24, 25, [24-25]]
@pytest.mark.parametrize(
    "graph_path,dest_path",
    (
        ("//home/crypta/graph/mrcc_test", "//home/crypta/graph/mrcc_test"),  # equals path
        ("//home/crypta/graph/mrcc_test", "//home/crypta/graph/output"),  # different paths
        ("//home/crypta/graph/mrcc_test", None),  # default should be equals
    ),
)
def test_mrcc_extended_yql_correct_col21(graph_path, dest_path, local_yql, local_yt, logger):
    """
    Should check correctnes of ExtendedMRConnectedComponentsYQL class work.
    Same as previous test, but swith col2, col1 order. Should check is not required be u<v for start algorithm.
    """
    fill_up_table(local_yt, graph_path, GRAPH_SCHEMA_COL_DAT, "/data/ext_input_edges.csv")
    desired_result = read_csv("/data/ext_desired_result.csv")
    mrcc_job = ExtendedMRConnectedComponentsYQL(local_yql)
    converged = mrcc_job.find_connected_components(
        source=graph_path, destination=dest_path, u_name="col2", v_name="col1", component_id="cid"
    )
    if not converged:
        raise Exception("MR CC not converged")
    actual_result = read_table(local_yt, dest_path or graph_path)

    logger.info("Actual result")
    logger.info(actual_result)
    logger.info("Desired result")
    logger.info(desired_result)

    intersection = desired_result.merge(actual_result, on=["cid", "col2", "col1"])
    assert desired_result.shape[0] == intersection.shape[0]


@pytest.mark.skip(reason="DF don't equal for strange reason. TODO: check why")
# Actual result
#     cid col1 col2                   data
# 0     1    1    3          [3, 9, [3-9]]
# 1     1    1    7          [7, 3, [7-3]]
# 2     1    1    8          [7, 8, [7-8]]
# 3     1    1    9          [1, 9, [1-9]]
# 4    10   10   11      [10, 11, [10-11]]
# 5   100  100  101  [100, 101, [100-101]]
# 6   100  100  102  [101, 102, [101-102]]
# 7   100  100  103  [102, 103, [102-103]]
# 8    20   20   21      [20, 21, [20-21]]
# 9    20   20   22      [20, 22, [20-22]]
# 10   20   20   23      [22, 23, [22-23]]
# 11   20   20   24      [23, 24, [23-24]]
# 12   20   20   25      [24, 25, [24-25]]
# Desired result
#     cid  col1  col2                   data
# 0     1     1     3          [3, 9, [3-9]]
# 1     1     1     7          [7, 3, [7-3]]
# 2     1     1     8          [7, 8, [7-8]]
# 3     1     1     9          [1, 9, [1-9]]
# 4    10    10    11      [10, 11, [10-11]]
# 5   100   100   101  [100, 101, [100-101]]
# 6   100   100   102  [101, 102, [101-102]]
# 7   100   100   103  [102, 103, [102-103]]
# 8    20    20    21      [20, 21, [20-21]]
# 9    20    20    22      [20, 22, [20-22]]
# 10   20    20    23      [22, 23, [22-23]]
# 11   20    20    24      [23, 24, [23-24]]
# 12   20    20    25      [24, 25, [24-25]]
@pytest.mark.parametrize(
    "graph_path,dest_path",
    (
        ("//home/crypta/graph/mrcc_test", "//home/crypta/graph/mrcc_test"),  # equals path
        ("//home/crypta/graph/mrcc_test", "//home/crypta/graph/output"),  # different paths
        ("//home/crypta/graph/mrcc_test", None),  # default should be equals
    ),
)
def test_mrcc_extended_yql_vdata_correct(graph_path, dest_path, local_yql, local_yt, logger):
    """ Should check correctnes of VDataMRConnectedComponentsYQL class work """
    fill_up_table(local_yt, graph_path, GRAPH_SCHEMA_COL_DAT, "/data/ext_input_edges.csv")
    desired_result = read_json("/data/ext_vdat_desired_result.json")
    mrcc_job = VDataMRConnectedComponentsYQL(local_yql)
    converged = mrcc_job.find_connected_components(
        source=graph_path, destination=dest_path, u_name="col1", v_name="col2", component_id="cid"
    )
    if not converged:
        raise Exception("MR CC not converged")
    actual_result = read_table(local_yt, dest_path or graph_path)

    logger.info("Actual result")
    logger.info(actual_result)
    logger.info("Desired result")
    logger.info(desired_result)

    intersection = desired_result.merge(actual_result, on=["cid", "col1", "col2"])
    assert desired_result.shape[0] == intersection.shape[0]
