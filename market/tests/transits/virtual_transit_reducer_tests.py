import json
import yatest.common
import pytest
from nile.api.v1 import Record
from market.replenishment.algorithms.lib23.data_preparation.transits.virtual_transits import merge_virtual_transit_reducer


def get_record(**kwargs):
    key = kwargs["key"]
    value = kwargs["rec"]

    return (key, [Record(**kwargs) for kwargs in value]),


class TestReducer:
    @pytest.mark.parametrize("test_case", (
        "test_one_result_record",
        "test_two_result_record",
        "test_without_result_record"
    ))
    def test_virtual_transit_reducer_positive(self, test_case):
        cases_dir = yatest.common.source_path("market/replenishment/algorithms/tests/transits/resources/cases.json")
        with open(cases_dir) as f:
            cases = json.load(f)

        case = cases[test_case]
        record = get_record(**case["record"])
        expected_record = [Record(**c) for c in case["expected"]]

        record_reducer = merge_virtual_transit_reducer(record)
        record = [r for r in record_reducer]

        assert len(record) == len(expected_record)
        for i in range(len(record)):
            assert record[i].to_dict() == expected_record[i].to_dict()
