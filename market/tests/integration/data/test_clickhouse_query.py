import pytest

from yamarec1.data.clickhouse import ClickhouseQueryData


@pytest.mark.skip(reason="TODO: remove all Clickhouse-related code after moving to //logs/*")
def test_clickhouse_query_can_select_literals_correctly():
    data = ClickhouseQueryData("SELECT 1, 'abc', inf, nan")
    rows = list(data)
    assert len(rows) == 1
    assert rows == [("1", "abc", "inf", "nan")]
