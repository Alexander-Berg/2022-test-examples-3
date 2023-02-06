from psycopg2.extras import Json


def _sql_type(val):
    if isinstance(val, str):
        return "text"
    if isinstance(val, int):
        return "bigint"
    if isinstance(val, dict):
        return "jsonb"


def _to_sql_type(val):
    if _sql_type(val) == "jsonb":
        return Json(val)
    # no conversion required for simple types
    return val


def _rows_to_dicts(rows):
    return [{k: v for k, v in row.items()} for row in rows]


def exec_procedure(cursor, name, **kwargs):
    sql_args = ", ".join([f"i_{key} => %({key})s::{_sql_type(val)}" for key, val in kwargs.items()])
    sql = f"SELECT * FROM {name}({sql_args})"

    cursor.execute(sql, {key: _to_sql_type(val) for key, val in kwargs.items()})
    return _rows_to_dicts(cursor.fetchall())


def all_links(cursor):
    cursor.execute("SELECT * FROM botdb.links")
    return _rows_to_dicts(cursor.fetchall())


def all_otps(cursor):
    cursor.execute("SELECT * FROM botdb.otps")
    return _rows_to_dicts(cursor.fetchall())
