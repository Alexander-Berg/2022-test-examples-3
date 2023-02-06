from yt import yson

from crypta.cm.services.calc_expire.lib.python import schemas


def test_calc_expire_schema():
    return yson.yson_to_json(schemas.calc_expire_schema())


def test_calc_expire_errors_schema():
    return yson.yson_to_json(schemas.calc_expire_errors_schema())
