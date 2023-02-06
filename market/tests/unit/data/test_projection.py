from yamarec1.data import DataColumnSchema
from yamarec1.data import DataProjection
from yamarec1.data import DataRecord
from yamarec1.data import DataSchema
from yamarec1.data.storages import ArrayData


def test_data_projection_alters_records_and_schema():
    records = [
        DataRecord("hello", 1, False),
        DataRecord("world", 2, True),
    ]
    schema = DataSchema(
        [
            DataColumnSchema("s", "String"),
            DataColumnSchema("i", "Int64"),
            DataColumnSchema("b", "Bool"),
        ])
    data = ArrayData(records, schema=schema)
    projection = DataProjection(data, ["b", "i"])
    assert projection.schema == DataSchema(
        [
            DataColumnSchema("b", "Bool"),
            DataColumnSchema("i", "Int64"),
        ])
    assert list(projection) == [
        DataRecord(False, 1),
        DataRecord(True, 2),
    ]
