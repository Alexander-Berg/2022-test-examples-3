from psycopg2.extensions import cursor, Column
from typing import Sequence


class MemoryviewComparableBytes:
    def __init__(self, value: bytes):
        self._value = value

    def __eq__(self, other):
        if isinstance(other, memoryview):
            return self._value == other.tobytes()
        return NotImplemented


def fetch_one(query, params, db_cur: cursor):
    db_cur.execute(query, params)
    row = db_cur.fetchone()
    if row is None:
        raise RuntimeError("Failed to fetch a row")
    return _row_to_dict(row, db_cur.description)


def fetch_all(query, params, db_cur: cursor):
    db_cur.execute(query, params)
    return [_row_to_dict(row, db_cur.description) for row in db_cur]


def _row_to_dict(row: tuple, description: Sequence[Column]):
    ret = {}
    for i in range(len(row)):
        ret[description[i].name] = row[i]
    return ret
