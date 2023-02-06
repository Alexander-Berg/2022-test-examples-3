import os
import string  # noqa
from contextlib import contextmanager
from copy import deepcopy  # noqa
from decimal import Decimal  # noqa

import pytest  # noqa
import ujson

from sendr_pytest import *  # noqa
from sendr_pytest import collision_watcher
from sendr_utils import utcnow  # noqa

from mail.payments.payments.api.app import PaymentsApplication
from mail.payments.payments.api_admin.app import AdminApplication
from mail.payments.payments.api_sdk.app import SDKApplication
from mail.payments.payments.core.entities.enums import PaymentsTestCase, ShopType
from mail.payments.payments.storage import Storage
from mail.payments.payments.storage.logbroker.factory import LogbrokerFactory
from mail.payments.payments.tests.db import *  # noqa
from mail.payments.payments.tests.interactions import *  # noqa
from mail.payments.payments.tests.storage import *  # noqa
from mail.payments.payments.tests.utils import *  # noqa
from mail.payments.payments.tests.utils import create_class_mocker, dummy_async_function

pytest_plugins = ['aiohttp.pytest_plugin']


def pytest_configure(config):
    collision_watcher.pytest_configure(config)


def pytest_addoption(parser):
    parser.addoption("--without-roles", action="store_true", default=False, help="Skip roles checking")
    collision_watcher.pytest_addoption(parser)


def pytest_generate_tests(metafunc):
    collision_watcher.pytest_generate_tests(metafunc)


def pytest_collection_modifyitems(session, config, items):
    collision_watcher.pytest_collection_modifyitems(session, config, items)


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture(autouse=True)
def env(randn, rands):
    os.environ['QLOUD_TVM_TOKEN'] = rands()


@pytest.fixture(autouse=True)
def payments_settings(root):
    from mail.payments.payments.conf import settings
    data = deepcopy(settings._settings)

    # setup for all tests
    settings.ENCRYPTION_KEY = {1: '1' * 16}
    settings.ORDER_FILE_PATH = root + '/logs/order.log'
    settings.TRUST_SANDBOX_PAYMENTS = True

    yield settings

    settings._settings = data


@pytest.fixture
def crypto_mock(mocker):
    return mocker.Mock()


@pytest.fixture
async def payments_app(db_engine, crypto_mock) -> PaymentsApplication:
    return PaymentsApplication(db_engine=db_engine, crypto=crypto_mock)


@pytest.fixture
async def admin_app(db_engine, crypto_mock) -> AdminApplication:
    return AdminApplication(db_engine=db_engine, crypto=crypto_mock)


@pytest.fixture
async def sdk_app(db_engine, crypto_mock) -> SDKApplication:
    return SDKApplication(db_engine=db_engine, crypto=crypto_mock)


@pytest.fixture
async def returned(returned_func):
    return await returned_func()


@pytest.fixture
async def response(response_func):
    return await response_func()


@pytest.fixture(autouse=True)
def clear_registry():
    from sendr_qstats import REGISTRY as GLOBAL_REGISTRY

    from mail.payments.payments.utils.stats import REGISTRY
    GLOBAL_REGISTRY._names = {}
    REGISTRY._names = {}


@pytest.fixture
def partner_crypto_mock(mocker):
    return mocker.Mock()


@pytest.fixture
def test_logger():
    import logging

    from sendr_qlog import LoggerContext
    return LoggerContext(logging.getLogger('test_logger'), {})


@pytest.fixture
def storage(test_logger, db_conn):
    return Storage(db_conn, logger=test_logger)


@pytest.fixture
def pushers_order_calls():
    return []


@pytest.fixture
def pushers_mock(mocker, pushers_order_calls):
    mock = mocker.Mock()
    side_effect = dummy_async_function(calls=pushers_order_calls)
    mock.log.push = mocker.Mock(side_effect=side_effect)
    mock.response_log.push = mocker.Mock(side_effect=side_effect)
    return mock


@pytest.fixture
async def lb_factory_mock(loop, mocker, test_logger):
    mocker.patch(
        'mail.payments.payments.storage.logbroker.factory.LogbrokerClient.run',
        dummy_async_function(),
    )
    mocker.patch(
        'mail.payments.payments.storage.logbroker.consumers.base.BaseConsumer.run',
        dummy_async_function(),
    )
    mocker.patch(
        'mail.payments.payments.storage.logbroker.producers.base.BaseProducer.run',
        dummy_async_function(),
    )
    return LogbrokerFactory(test_logger)


@pytest.fixture
def response_status():
    return 200


@pytest.fixture
def response_json():
    return {}


@pytest.fixture
def response_data():
    return b''


@pytest.fixture
def response_headers():
    return {'header': 'value'}


@pytest.fixture
def response_mock(mocker, response_headers, response_status, response_json, response_data):
    mock = mocker.Mock()
    mock.status = response_status
    mock.headers = response_headers
    mock.json = dummy_async_function(response_json)
    mock.text = dummy_async_function(ujson.dumps(response_json))
    mock.read = dummy_async_function(response_data)
    return mock


@pytest.fixture
def arbiter_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.arbiter.ArbiterClient')


@pytest.fixture
def balance_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.balance.BalanceClient')


@pytest.fixture
def balance_http_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.balance_http.BalanceHttpClient')


@pytest.fixture
def blackbox_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.blackbox.BlackBoxClient')


@pytest.fixture
def blackbox_corp_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.blackbox.BlackBoxCorpClient')


@pytest.fixture
def callback_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.callback.CallbackClient')


@pytest.fixture
def developer_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.developer.DeveloperClient')


@pytest.fixture
def floyd_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.floyd.FloydClient')


@pytest.fixture
def mds_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.mds.MDSClient')


@pytest.fixture
def oauth_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.oauth.OAuthClient')


@pytest.fixture
def kassa_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.kassa.KassaClient')


@pytest.fixture
def tinkoff_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.tinkoff.TinkoffClient')


@pytest.fixture
def trust_client_mocker(mocker):
    @contextmanager
    def _inner(shop_type, *args, **kwargs):
        assert isinstance(shop_type, (ShopType, type(None))), 'Can\'t determinate trust mock'

        trust_mocker = create_class_mocker(mocker, *(
            'mail.payments.payments.interactions.trust.TrustProductionClient',
            'mail.payments.payments.interactions.trust.TrustSandboxClient',
        ))

        with trust_mocker(*args, **kwargs) as (mock_prod, mock_sandbox):
            if shop_type == ShopType.TEST:
                yield mock_sandbox
            else:
                yield mock_prod

    return _inner


@pytest.fixture
def sender_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.sender.SenderClient')


@pytest.fixture
def so_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.so.SoClient')


@pytest.fixture
def service_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.service.ServiceClient')


@pytest.fixture
def geobase_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.geobase.GeobaseClient')


@pytest.fixture
def refs_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.refs.RefsClient')


@pytest.fixture
def spark_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.spark.SparkClient')


@pytest.fixture
def spark_suggest_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.spark_suggest.SparkSuggestClient')


@pytest.fixture
def search_wizard_client_mocker(mocker):
    return create_class_mocker(mocker, 'mail.payments.payments.interactions.search_wizard.SearchWizardClient')


@pytest.fixture
def uid():
    return 113000


@pytest.fixture
def test_param():
    return PaymentsTestCase.TEST_OK_HELD


@pytest.fixture
def purchase_token():
    return 'xxx-purchase_token'


@pytest.fixture
def trust_payment_id():
    return 'unit_payment_id'


@pytest.fixture
def payment_create_result(purchase_token, trust_payment_id):
    return {
        'purchase_token': purchase_token,
        'trust_payment_id': trust_payment_id
    }


@pytest.fixture
def root():
    return os.path.dirname(os.path.abspath(__file__))


@pytest.fixture
def mock_action(mocker):
    def _inner(action_cls, action_result=None):
        async def run(self):
            if (
                isinstance(action_result, Exception)
                or isinstance(action_result, type) and issubclass(action_result, Exception)
            ):
                raise action_result
            return action_result

        mocker.patch.object(action_cls, 'run', run)
        return mocker.patch.object(action_cls, '__init__', mocker.Mock(return_value=None))

    return _inner


@pytest.fixture
def request_id():
    return 'unittest'


@pytest.fixture
def payment_batch_id(randn):
    return randn()


@pytest.fixture
def payment_number(rands):
    return rands()


@pytest.fixture
def update_dt(rands):
    return rands()


@pytest.fixture
def payment_status(rands):
    return rands()


@pytest.fixture
def batch_payment_amount(rands):
    return rands()


@pytest.fixture
def dt(rands):
    return rands()


@pytest.fixture
def payment_details(rands):
    return rands()


@pytest.fixture
def payouts_data(
    payment_number,
    payment_batch_id,
    update_dt,
    payment_status,
    batch_payment_amount,
    dt,
    payment_details,
):
    return {
        "payouts": [
            {
                "status": "DONE",
                "payment_number": payment_number,
                "payment_batch_id": payment_batch_id,
                "update_dt": update_dt,
                "payment_status": payment_status,
                "batch_payment_amount": batch_payment_amount,
                "dt": dt,
                "payment_details": payment_details,
            },
        ]
    }


@pytest.fixture
def payouts_data_composite(payment_number, payment_batch_id):
    return {
        "payments": [{"child_payments": ["token_1", "token_2"]}],
        "payouts": []
    }
