import functools

from google.protobuf import json_format
import mock
import pytest
from yabs.proto import user_profile_pb2
from yt.yson.convert import yson_to_json

from crypta.lib.python import test_utils
from crypta.lib.python.yt.test_helpers import tables
from crypta.rt_socdem.lib.python.model.config import config


pytest_plugins = [
    'crypta.lib.python.nirvana.test_helpers.fixtures',
]


@pytest.fixture(scope='session')
def mock_sandbox_server():
    with test_utils.mock_sandbox_server_with_udf('BIGB_UDF', 'yql/udfs/bigb/libbigb_udf.so') as mock_bigb_udf:
        with mock.patch.object(config, 'BIGB_UDF_URL', mock_bigb_udf.get_udf_url()):
            yield mock_bigb_udf


@pytest.fixture
def date():
    return '2021-10-10'


@pytest.fixture
def get_table_with_beh_profile():
    def row_transformer(row, beh_profile_field):
        proto = json_format.ParseDict(yson_to_json(row[beh_profile_field]), user_profile_pb2.Profile())
        row[beh_profile_field] = proto.SerializeToString()
        return row

    def get_table(file_path, cypress_path, schema, beh_profile_field):
        on_write = tables.OnWrite(
            attributes={
                'schema': schema,
            },
            row_transformer=functools.partial(row_transformer, beh_profile_field=beh_profile_field),
        )

        return tables.YsonTable(file_path, cypress_path, on_write=on_write)

    return get_table
