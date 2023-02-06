# flake8: noqa

from yql_api import yql_api
from mongo_runner import mongo
from yt_runner import yt
from yql_utils import tmpdir_module

from fixtures import local_yt_and_yql, yt_client

from StringIO import StringIO
import pandas as pd
import library.python.resource as rs
import crypta.graph.fuzzy.lib.config as conf
from crypta.lib.python.native_yt import run_native_reduce
import numpy as np


from crypta.graph.fuzzy.lib.tasks.sources.ssid import ImportSsidMobileMetrikaDayTask, ImportSsidMobileMetrikaTask
from crypta.graph.fuzzy.lib.common.dates import yesterday
from crypta.lib.python.yql.client import create_yql_client
import logging

logger = logging.getLogger(__name__)


def convert_schema(dict_schema):
    return [dict(name=name, type=dtype) for name, dtype in dict_schema.iteritems()]


mobile_log_schema = convert_schema(
    {
        "DeviceID": "string",
        "OriginalDeviceID": "string",
        "Wifi_Macs": "string",
        "Wifi_Ssids": "string",
        "Wifi_SignalsStrengths": "string",
        "Wifi_AreConnected": "string",
    }
)


DTYPES = {"string": str, "uint64": np.uint64, "int64": np.int64}


def get_dtype(schema):
    if isinstance(schema, list):
        return {record["name"]: DTYPES[record["type"]] for record in schema}
    if isinstance(schema, dict):
        return {attr: DTYPES[attr_type] for attr, attr_type in schema.iteritems()}
    return None


def read_csv(rs_name, schema=None):
    buf = StringIO(rs.find(rs_name))
    return pd.read_csv(buf, dtype=get_dtype(schema))


def fill_up_table(yt_client, tablepath, schema, csv_resource=None):
    if yt_client.exists(tablepath):
        yt_client.remove(tablepath)
    if isinstance(schema, dict):
        schema = convert_schema(schema)
    yt_client.create("table", tablepath, recursive=True, attributes=dict(schema=schema))
    if not csv_resource:
        return
    df = read_csv(csv_resource, schema)
    data = (row for row in df.to_dict(orient="records"))
    yt_client.write_table(tablepath, list(data))


def read_table(yt_client, tablepath):
    generator = yt_client.read_table(tablepath)
    return pd.DataFrame.from_dict(list(generator))


def test_daily_query_is_valid(local_yt_and_yql, yt_client):
    task = ImportSsidMobileMetrikaDayTask(date=yesterday(), target_date=yesterday(), ssid_threshold=10)
    fill_up_table(yt_client, task.source, mobile_log_schema)
    plan, ast = task.yql.explain(task.query, syntax_version=1)
    logger.debug("Plan:\n%s", plan)
    logger.debug("AST:\n%s", ast)


def test_main_query_is_valid(local_yt_and_yql, yt_client):
    task = ImportSsidMobileMetrikaTask(date=yesterday())
    daily_task_schema = convert_schema(task.sources_schema)
    for source in task.sources:
        fill_up_table(yt_client, source, daily_task_schema)
    devid_dict_schema = {"devid": "string", "mmetric_devid": "string", "yuid": "string"}
    fill_up_table(yt_client, task.source, devid_dict_schema)
    with yt_client.TempTable() as tmp:
        plan, ast = task.yql.explain(task.query(tmp), syntax_version=1)
    logger.debug("Plan:\n%s", plan)
    logger.debug("AST:\n%s", ast)


def test_daily_query_correct(local_yt_and_yql, yt_client):
    with yt_client.Transaction() as transaction, yt_client.TempTable() as source, yt_client.TempTable() as destination:
        fill_up_table(yt_client, source, mobile_log_schema, "/data/mobile-metrika-log.1d.csv")
        fill_up_table(yt_client, destination, conf.Paths.sources.ssid.DAY_TABLE_SCHEMA)
        yql = create_yql_client(
            yt_proxy=conf.Yt.PROXY,
            token=conf.Yql.TOKEN,
            transaction=str(transaction.transaction_id),
            db=conf.Yql.DB,
            yql_server=conf.Yql.SERVER,
            yql_port=conf.Yql.PORT,
        )
        query = rs.find("/yql/export_ssid_devid_day_table").format(
            source_mmetric_table=source, destination=destination, ssid_threshold=2
        )
        yql.execute(query, syntax_version=1)
        actual_result = read_table(yt_client, destination)
        logger.debug("Actual result:\n%s", actual_result)
        assert actual_result.shape[0] > 0
        desired_result = read_csv("/data/ssid_day.csv", conf.Paths.sources.ssid.DAY_TABLE_SCHEMA)
        intersection = desired_result.merge(actual_result, on=["ssid", "mmetric_devid"])
        assert desired_result.shape[0] == intersection.shape[0]


def test_main_query_correct(local_yt_and_yql, yt_client):
    with yt_client.Transaction() as transaction, yt_client.TempTable() as source, yt_client.TempTable() as destination, yt_client.TempTable() as source_nolimit_table:
        fill_up_table(yt_client, source, conf.Paths.sources.ssid.DAY_TABLE_SCHEMA, "/data/ssid_day.csv")
        destination_schema = {"ssid": "string", "devid": "string", "yandexuid": "uint64"}
        fill_up_table(yt_client, destination, destination_schema)
        source_nolimit_table_schema = {"mmetric_devid": "string", "devid": "string", "yuid": "string"}
        fill_up_table(yt_client, source_nolimit_table, source_nolimit_table_schema, "/data/source_nolimit_table.csv")
        query = rs.find("/yql/export_ssid_yuids").format(
            sources="`{source}`".format(source=source),
            source_nolimit_table=source_nolimit_table,
            destination=destination,
            yuid_threshold=2,
        )
        yql = create_yql_client(
            yt_proxy=conf.Yt.PROXY,
            token=conf.Yql.TOKEN,
            transaction=str(transaction.transaction_id),
            db=conf.Yql.DB,
            yql_server=conf.Yql.SERVER,
            yql_port=conf.Yql.PORT,
        )
        yql.execute(query, syntax_version=1)
        actual_result = read_table(yt_client, destination)
        logger.debug("Actual result:\n%s", actual_result)
        assert actual_result.shape[0] > 0
        desired_result = read_csv("/data/ssid_month.csv", destination_schema)
        intersection = desired_result.merge(actual_result, on=["ssid", "devid", "yandexuid"])
        assert desired_result.shape[0] == intersection.shape[0]


def test_explode(local_yt_and_yql, yt_client):
    with yt_client.Transaction() as transaction, yt_client.TempTable() as source, yt_client.TempTable() as destination:
        source_schema = {"ssid": "string", "devid": "string", "yandexuid": "uint64"}
        destination_schema = {"ssid": "string", "yandexuid_left": "uint64", "yandexuid_right": "uint64"}
        fill_up_table(yt_client, source, source_schema, "/data/ssid_month_unexploded.csv")
        fill_up_table(yt_client, destination, destination_schema)
        yt_client.run_sort(source, sort_by=["ssid"])
        run_native_reduce(
            reducer_name="NCommonWifiAP::TExploder",
            source=source,
            destination=destination,
            proxy=conf.Yt.PROXY,
            token=conf.Yt.TOKEN,
            transaction=str(transaction.transaction_id),
            pool=conf.Yt.POOL,
            title="Explode yandexuids with common wifi access point",
            reduce_by=["ssid"],
        )
        actual_result = read_table(yt_client, destination)
        logger.debug("Actual result:\n%s", actual_result)
        assert actual_result.shape[0] > 0
        desired_result = read_csv("/data/ssid_month_exploded.csv", destination_schema)
        merge_attrs = [conf.Constants.YUID_LEFT, conf.Constants.YUID_RIGHT]
        intersect = desired_result.merge(actual_result, on=merge_attrs)
        assert intersect.shape[0] == desired_result.shape[0]
