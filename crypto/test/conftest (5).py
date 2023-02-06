import mock
import pytest

from crypta.affinitive_geo.services.org_embeddings.lib.utils import config
from crypta.lib.python import test_utils


pytest_plugins = [
    'crypta.lib.python.nirvana.test_helpers.fixtures',
    'crypta.lib.python.test_utils.user_data_fixture',
]


@pytest.fixture
def date():
    return '2022-05-26'


@pytest.fixture(scope='session')
def mock_bigb_udf():
    with test_utils.mock_sandbox_server_with_udf('BIGB_UDF', 'yql/udfs/bigb/libbigb_udf.so') as mocked_bigb_udf:
        with mock.patch.object(config, 'BIGB_UDF_URL', mocked_bigb_udf.get_udf_url()):
            yield mocked_bigb_udf
