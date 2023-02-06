import datetime

from crypta.lib.python import time_utils
from crypta.spine.pushers.common import mocks
from crypta.spine.pushers.yt_output_table_latencies import lib


DATETIME_02ND_STR = "2021-06-01T21:00:00.000000Z"  # Moscow is +3 hours ahead of GMT
DATETIME_03RD_STR = "2021-06-02T21:00:00.000000Z"
DATETIME_03RD_PLUS_1H_STR = "2021-06-02T22:00:00.000000Z"
DATETIME_04TH_STR = "2021-06-03T21:00:00.000000Z"
DATETIME_05TH_STR = "2021-06-04T21:00:00.000000Z"

DIR_NO_TABLES = "dir-no-tables"
DIR_ONE_TABLE = "dir-one-table"
DIR_TWO_TABLES = "dir-two-tables"
DIR_02TH_1H_LATE = "dir-02th-1h-late"
DIR_WITH_SUBDIR = "dir-with-subdir"
DIR_WITH_SUBDIR_CHILD = DIR_WITH_SUBDIR + "/subdir"


nodes = [
    mocks.Node(DIR_NO_TABLES, creation_time=DATETIME_02ND_STR, type="map_node"),

    mocks.Node(DIR_ONE_TABLE + "/2021-06-02", creation_time=DATETIME_03RD_STR),

    mocks.Node(DIR_TWO_TABLES + "/2021-06-02", creation_time=DATETIME_03RD_STR),
    mocks.Node(DIR_TWO_TABLES + "/2021-06-03", creation_time=DATETIME_04TH_STR),

    mocks.Node(DIR_02TH_1H_LATE + "/2021-06-02", creation_time=DATETIME_03RD_PLUS_1H_STR),

    mocks.Node(DIR_WITH_SUBDIR + "/__________", creation_time=DATETIME_02ND_STR),
    mocks.Node(DIR_WITH_SUBDIR + "/2021-06-02", creation_time=DATETIME_03RD_STR),
    mocks.Node(DIR_WITH_SUBDIR + "/2021-06-03", creation_time=DATETIME_04TH_STR),
    mocks.Node(DIR_WITH_SUBDIR + "/zzzzzzzzzz", creation_time=DATETIME_05TH_STR),
    mocks.Node(DIR_WITH_SUBDIR_CHILD + "/2021-06-03", creation_time=DATETIME_04TH_STR),
]


def test_get_metrics():
    DATETIME_02ND = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 2))
    DATETIME_03RD = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 3))
    DATETIME_03RD_PLUS_1H = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 3, 1))
    DATETIME_03RD_LAST = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 3, 23, 59, 59))
    DATETIME_04TH = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 4))
    DATETIME_05TH = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 5))
    DATETIME_06TH = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 6))
    DATETIME_14TH = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 14))

    mock_yt_client = mocks.MockYtClient(nodes)

    assert {} == lib.get_metrics(mock_yt_client, DIR_NO_TABLES, datetime_now=DATETIME_02ND)

    assert {} == lib.get_metrics(mock_yt_client, DIR_ONE_TABLE, datetime_now=DATETIME_02ND)
    assert {"output_table_latency": 0.} == lib.get_metrics(mock_yt_client, DIR_ONE_TABLE, datetime_now=DATETIME_03RD)
    assert {"output_table_latency": 0.} == lib.get_metrics(mock_yt_client, DIR_ONE_TABLE, datetime_now=DATETIME_04TH)
    assert {"output_table_latency": 86400.} == lib.get_metrics(mock_yt_client, DIR_ONE_TABLE, datetime_now=DATETIME_05TH)
    assert {"output_table_latency": 864000.} == lib.get_metrics(mock_yt_client, DIR_ONE_TABLE, datetime_now=DATETIME_14TH)

    assert {"output_table_latency": 0.} == lib.get_metrics(mock_yt_client, DIR_TWO_TABLES, datetime_now=DATETIME_03RD)
    assert {"output_table_latency": 0.} == lib.get_metrics(mock_yt_client, DIR_TWO_TABLES, datetime_now=DATETIME_04TH)

    assert {} == lib.get_metrics(mock_yt_client, DIR_02TH_1H_LATE, datetime_now=DATETIME_02ND)
    assert {"output_table_latency": 3600.} == lib.get_metrics(mock_yt_client, DIR_02TH_1H_LATE, datetime_now=DATETIME_03RD)
    assert {"output_table_latency": 3600.} == lib.get_metrics(mock_yt_client, DIR_02TH_1H_LATE, datetime_now=DATETIME_03RD_PLUS_1H)
    assert {"output_table_latency": 3600.} == lib.get_metrics(mock_yt_client, DIR_02TH_1H_LATE, datetime_now=DATETIME_03RD_LAST)
    assert {"output_table_latency": 0.} == lib.get_metrics(mock_yt_client, DIR_02TH_1H_LATE, datetime_now=DATETIME_04TH)

    assert {"output_table_latency": 0.} == lib.get_metrics(mock_yt_client, DIR_WITH_SUBDIR, datetime_now=DATETIME_03RD)
    assert {"output_table_latency": 0.} == lib.get_metrics(mock_yt_client, DIR_WITH_SUBDIR, datetime_now=DATETIME_04TH)
    assert {"output_table_latency": 0.} == lib.get_metrics(mock_yt_client, DIR_WITH_SUBDIR, datetime_now=DATETIME_05TH)
    assert {"output_table_latency": 86400.} == lib.get_metrics(mock_yt_client, DIR_WITH_SUBDIR, datetime_now=DATETIME_06TH)
