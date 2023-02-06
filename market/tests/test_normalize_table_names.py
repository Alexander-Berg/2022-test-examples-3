from lib.blueprints.parse_sql import normalize_table_names


def test_normalize_table_names():
    assert normalize_table_names("aaa") == "aaa"
    assert normalize_table_names('"aaa"') == "aaa"
    assert normalize_table_names('"database"."table_1"') == "table_1"
    assert normalize_table_names('`database`.`table_1`') == "table_1"
