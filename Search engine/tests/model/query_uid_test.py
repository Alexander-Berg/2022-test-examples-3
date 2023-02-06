import test_utils
from query import Query


def test_queries_uids():
    uids = [Query.from_json(queries_json).query_uid()
            for queries_json in test_utils.read_json_test_data("queries_uids.json")]
    expected_uids = [
        "53a671ba0523132006a48d2d7a75557a6750089b54e9af887d2e2f5c14cfbbee",
        "f42a1438dc4984d81e4b650dfe4c1ac669eb967327dca277c3714d73b7f6e533",
        "uid"
    ]
    assert uids == expected_uids
