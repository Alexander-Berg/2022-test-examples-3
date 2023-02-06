from unittest.mock import patch

from search.metrics.ccc.ccc_lib.dt_client import DTClient
from search.metrics.ccc.ccc_lib.ccc import CCC
from search.metrics.ccc.ccc_lib.model import CompareTwoRequest
from search.metrics.ccc.ccc_lib.calculators.diff_calculator import DiffResult


def test_upload():
    with patch("yt.wrapper.insert_rows") as yt_mock:
        observation = CCC.create_observation(
            CompareTwoRequest(
                left_serpset_id=-1,
                right_serpset_id=-2,
                left_metric_name="onlySearchResult.judged-pfound",
                right_metric_name="onlySearchResult.judged-pfound",
                type="intersected",
            ),
            DiffResult("intersected", True, None, 1, 1, 1, 11, 1, 1, 1, 0.5),
        )
        tables = {
            DTClient.SERPSET_QUERIES_TABLE: "//home/metrics/ytcompare/develop/serpset_queries",
            DTClient.SERP_METRICS_TABLE: "//home/metrics/ytcompare/develop/serp_query_metrics",
            DTClient.OBSERVATIONS_TABLE: "//home/metrics/ytcompare/develop/observations",
        }
        DTClient(tables, yt_token="meow-meow").upload_observation(observation)
        assert yt_mock.call_count == 1
        first_call = yt_mock.call_args_list[0]
        assert first_call[0][0] == "//home/metrics/ytcompare/develop/observations"
        assert first_call[0][1] == [observation]
