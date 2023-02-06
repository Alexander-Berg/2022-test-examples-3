import pytest

from market.yamarec.yamarec.bin.yamarec_dt_uploader.uploader import CustomDataParser


@pytest.mark.parametrize("data", [
    ["", "", "4771eed4b3", "1478402992", "{view__model: 1705580525}"],
    ["table", "accessories", "4771eed4b3", "1478402992", "{view__model: 1705580525}"],
    ["table", "accessories", "4771eed4b3", "\N", "{view__model: 1705580525}"],
    ["  NULL  ", "accessories", "4771eed4b3", "1478402992", "{view__model: 1705580525}"],
    ["\N  ", "accessories", "4771eed4b3", "1478402992", "{view__model: 1705580525}"],
])
def test_parser(data):
    default_table_name = "default_table"
    default_partition_name = "category"
    default_timestamp = 755481600
    parser = CustomDataParser(table_name=default_table_name, partition_name=default_partition_name)
    table_name, partition_name, key, timestamp, value = data
    data_line = "\t".join(data)
    record = next(parser.parse_line(data_line))
    timestamp = int(timestamp) if timestamp.isdigit() else default_timestamp
    assert record.table_name == str_null(table_name) or default_table_name
    assert record.timestamp == int(timestamp) * 1000
    content = {
        "key": key,
        "partition": partition_name or default_partition_name,
        "timestamp": int(timestamp) * -1000,
        "value": value,
    }
    assert record.content == content


@pytest.mark.parametrize("data", [
    # empty table name
    pytest.param(["", "x", "4771eed4b3", "1478402992", "{view__model: 1705580525}"], marks=pytest.mark.xfail),
    # empty partition name
    pytest.param(["table", "", "4771eed4b3", "1478402992", "{view__model: 1705580525}"], marks=pytest.mark.xfail),
])
def test_parser_failed(data):
    parser = CustomDataParser()
    data_line = "\t".join(data)
    next(parser.parse_line(data_line))


def str_null(string):
    return None if string.strip() in ["NULL", "\N", ""] else string
