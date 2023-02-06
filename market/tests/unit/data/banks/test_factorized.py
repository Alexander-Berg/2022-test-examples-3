from yamarec1.data import LeftJoinModeSwitcher
from yamarec1.data import QueryableData


def test_bank_handles_joins(factorized_data_bank):
    storage = factorized_data_bank[["left", "central", "right"]]
    assert isinstance(storage.data, QueryableData)
    assert storage.data.query.body == (
        "SELECT *\nFROM (\n    SELECT 0, '0'\n) AS __left__\n"
        "INNER JOIN (\n    SELECT 0, '0'\n) AS __central__\n"
        "ON\n    __central__.user == __left__.user\n"
        "    AND __central__.item == __left__.item\n"
        "INNER JOIN (\n    SELECT 0, '0'\n) AS __right__\n"
        "ON\n    __right__.user == __central__.user\n"
        "    AND __right__.item == __central__.item"
    )
    assert storage.data.query.preamble is not None


def test_bank_handles_left_joins_with_switcher(factorized_data_bank):
    storage = factorized_data_bank[[
        "left",
        LeftJoinModeSwitcher("central"),
        LeftJoinModeSwitcher("right")
    ]]
    assert isinstance(storage.data, QueryableData)
    assert storage.data.query.body == (
        "SELECT *\nFROM (\n    SELECT 0, '0'\n) AS __left__\n"
        "LEFT JOIN (\n    SELECT 0, '0'\n) AS __central__\n"
        "ON\n    __central__.user == __left__.user\n"
        "    AND __central__.item == __left__.item\n"
        "LEFT JOIN (\n    SELECT 0, '0'\n) AS __right__\n"
        "ON\n    __right__.user == __central__.user\n"
        "    AND __right__.item == __central__.item"
    )
    assert storage.data.query.preamble is not None
