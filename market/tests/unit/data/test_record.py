from yamarec1.data import DataRecord


def test_record_accepts_any_number_of_fields():
    record = DataRecord(0, "1", [2, 3, 4])
    assert isinstance(record, DataRecord)
    assert isinstance(record, tuple)
    assert record[0] == 0
    assert record[1] == "1"
    assert record[2] == [2, 3, 4]
