import pytest

from yamarec1.data import DataQuery
from yamarec1.data import DataQueryResult
from yamarec1.data.exceptions import DataQueryError


def test_query_assembles_its_text_correctly():
    query = DataQuery("SELECT", preamble=("$a = 1", "$b = 2"))
    assert query.text.endswith("$a = 1;\n$b = 2;\nSELECT")


def test_query_can_select_data_correctly(table_with_data, data):
    fields = [column.name for column in data.schema.columns]
    query = DataQuery("SELECT %s FROM `%s`" % (", ".join(fields), table_with_data))
    assert query.result is None
    query.run()
    assert isinstance(query.result, DataQueryResult)
    assert query.result.schema == data.schema
    assert set(query.result) == set(data)


def test_query_can_insert_data_correctly(table_with_data, data):
    filler = DataQuery(
        "INSERT INTO `%s` WITH TRUNCATE\n%s" % (
            table_with_data,
            "\nUNION ALL\n".join(
                [
                    "SELECT '%d', '%d', '%d', '%d'" % (4 * i, 4 * i + 1, 4 * i + 2, 4 * i + 3)
                    for i in range(10)
                ])
        ))
    filler.run()
    assert filler.result is None
    checker = DataQuery("SELECT * FROM `%s`" % table_with_data)
    checker.run()
    assert len(list(checker.result)) == 10


def test_invalid_query_fails():
    query = DataQuery("SELECT X")
    with pytest.raises(DataQueryError):
        query.run()
    assert query.result is None
