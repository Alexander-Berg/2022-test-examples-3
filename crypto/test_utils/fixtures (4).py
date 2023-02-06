import pytest

from crypta.lib.python.test_utils.mock_sandbox_server_with_udf import mock_sandbox_server_with_udf


@pytest.fixture()
def mock_sandbox_server_with_identifiers_udf():
    with mock_sandbox_server_with_udf('CRYPTA_IDENTIFIERS_UDF',
                                      'yql/udfs/crypta/identifiers/libcrypta_identifier_udf.so') as mock:
        yield mock
