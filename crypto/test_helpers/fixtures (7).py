import os

import mock
import pytest

from crypta.lib.python.nirvana.nirvana_helpers.nirvana_transaction import NirvanaTransaction
import crypta.lib.python.yql.client as yql_helpers


pytest_plugins = [
    'crypta.lib.python.yql.test_helpers.fixtures',
    'crypta.lib.python.yt.test_helpers.fixtures',
]


@pytest.fixture
def nirvana_operation_environ(clean_local_yt, local_yt_and_yql_env):
    os.environ.update(local_yt_and_yql_env)
    with mock.patch.object(NirvanaTransaction, 'TRANSACTION_ID_ENVIRON_FIELD', 'YT_TRANSACTION_TEST'), \
            clean_local_yt.get_yt_client().Transaction() as transaction:
        os.environ[NirvanaTransaction.TRANSACTION_ID_ENVIRON_FIELD] = str(transaction.transaction_id)
        yield os.environ


@pytest.fixture
def nirvana_operation_environ_for_binary(clean_local_yt, local_yt_and_yql_env):
    os.environ.update(local_yt_and_yql_env)
    with clean_local_yt.get_yt_client().Transaction() as transaction:
        os.environ[NirvanaTransaction.TRANSACTION_ID_ENVIRON_FIELD] = str(transaction.transaction_id)
        yield os.environ


@pytest.fixture
def yt_client(clean_local_yt, nirvana_operation_environ):
    yt_client_instance = clean_local_yt.get_yt_client()
    yt_client_instance.config['pool'] = 'fake_pool'
    yt_client_instance.config['token'] = 'fake_token'
    return yt_client_instance


@pytest.fixture
def yt_client_for_binary(clean_local_yt, nirvana_operation_environ_for_binary):
    yt_client_instance = clean_local_yt.get_yt_client()
    yt_client_instance.config['pool'] = 'fake_pool'
    yt_client_instance.config['token'] = 'fake_token'
    return yt_client_instance


@pytest.fixture
def yt_server(clean_local_yt):
    return clean_local_yt.get_server()


@pytest.fixture
def yql_client(yt_server):
    return yql_helpers.create_yql_client(
        yt_proxy=yt_server,
        pool='fake_pool',
        token=os.getenv('YQL_TOKEN'),
    )
