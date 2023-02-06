import requests
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def schema():
    return schema_utils.yt_schema_from_dict({
        "crypta_id": "string",
        "different": "string",
        "str": "string",
    })


def test_get_lab_segment_table_columns_with_id_types(api, local_yt):
    input_path = "//abc/a"

    def func():
        result = requests.get(
            f"http://localhost:{api.port}/lab/utils/get_lab_segment_table_columns_with_id_types?path={input_path}").json()
        for column in result['columns']:
            column['idTypes'].sort()
        return result

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=func,
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.get_yson_table_with_schema("table.yson", input_path, schema()),
                tests.TableIsNotChanged(),
            ),
        ],
        return_result=True,
    )
