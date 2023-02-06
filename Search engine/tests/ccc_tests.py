from search.metrics.ccc.ccc_lib.ccc import CCC
from search.metrics.ccc.ccc_lib.model import Query, CompareTwoRequestWithValues
from search.metrics.ccc.ccc_lib.calculator_types import ABSOLUTE
from search.metrics.ccc.ccc_lib.calculators.diff_calculator import DiffResult


def test_query_equality():
    query_1 = Query(text="meow", region_id=2, device="DESKTOP", country="RU")
    query_2 = Query(text="meow", region_id=2, device="DESKTOP", country="RU")
    assert query_1 == query_2


def get_left():
    return [
        Query(text="meow", region_id=2, device="DESKTOP", country="RU"),
        Query(text="woof", region_id=2, device="DESKTOP", country="RU"),
        Query(text="moo", region_id=2, device="DESKTOP", country="RU"),
    ]


def get_right():
    return [
        Query(text="meow", region_id=2, device="DESKTOP", country="RU"),
        Query(text="woof", region_id=2, device="DESKTOP", country="RU"),
        Query(text="hello", region_id=2, device="DESKTOP", country="RU"),
    ]


def test_intersect():
    intersected = CCC._intersect(get_left(), get_right())
    assert intersected == {
        Query(text="meow", region_id=2, device="DESKTOP", country="RU"),
        Query(text="woof", region_id=2, device="DESKTOP", country="RU"),
    }


def test_unite():
    intersected = CCC._unite(get_left(), get_right())
    assert intersected == {
        Query(text="meow", region_id=2, device="DESKTOP", country="RU"),
        Query(text="woof", region_id=2, device="DESKTOP", country="RU"),
        Query(text="moo", region_id=2, device="DESKTOP", country="RU"),
        Query(text="hello", region_id=2, device="DESKTOP", country="RU"),
    }


def test_compare():
    compare_two_request = CompareTwoRequestWithValues(
        left_serpset_id=-1,
        right_serpset_id=-2,
        left_metric_name="onlySearchResult.judged-pfound",
        right_metric_name="onlySearchResult.judged-pfound",
        type=ABSOLUTE,
        left_queries=["abc", "def"],
        left_values=[1.0, 1.5],
        right_queries=["ghi", "jkl"],
        right_values=[2.0, 2.5]
    )
    ccc = CCC(
        table_paths={
            "serpset_queries_table": "",
            "serp_metrics_table": "",
            "observations": ""
        },
        yt_token=""
    )
    result: DiffResult = ccc.compare(
        compare_request=compare_two_request,
        upload_metrics=False,
        upload_queries=False,
        upload_observation=False
    )
    assert result.ok is True
    assert result.type == ABSOLUTE
