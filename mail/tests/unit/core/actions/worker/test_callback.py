import pytest

from mail.payments.payments.core.actions.worker.callback import CallbackWorkerAction
from mail.payments.payments.core.entities.enums import APICallbackSignMethod, CallbackMessageType
from mail.payments.payments.core.entities.merchant import APICallbackParams
from mail.payments.payments.core.entities.task import Task, TaskType


class BaseCallbackWorkerAction:
    """Обработчик задач должен вызвать методы клиентов, используя параметры задачи для формирования аргументов."""

    @pytest.fixture
    def partner_crypto(self, partner_crypto_mock):
        partner_crypto_mock.sign.return_value = b'to be encoded'
        return partner_crypto_mock

    @pytest.fixture(autouse=True)
    def service_client_callback_method(self, service_client_mocker):
        with service_client_mocker('callback_service', result=None) as mock:
            yield mock

    @pytest.fixture
    def sign_method(self):
        return APICallbackSignMethod.ASYMMETRIC

    @pytest.fixture(autouse=True)
    def callback_client_post_signed_message_method(self, callback_client_mocker):
        with callback_client_mocker('post_signed_message', result=None) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def callback_client_post_jwt_message_method(self, callback_client_mocker):
        with callback_client_mocker('post_jwt_message', result=None) as mock:
            yield mock

    @pytest.fixture
    def tvm_id(self, unique_rand, randn):
        return unique_rand(randn, basket='tvm_id')

    @pytest.fixture
    def params(self, sign_method, rands, tvm_id):
        return dict(
            callback_url='https://service.domain',
            message={'a': 'a', 'b': 'b'},
            tvm_id=tvm_id,
            callback_params=APICallbackParams(sign_method=sign_method, secret=rands()),
            callback_message_type=CallbackMessageType.ORDER_STATUS_UPDATED,
        )

    @pytest.fixture
    async def task(self, storage, params) -> Task:
        return await storage.task.create(Task(task_type=TaskType.API_CALLBACK, params=params))

    @pytest.fixture
    async def returned(self, params):
        return await CallbackWorkerAction(**params).run()


class TestTvmCase(BaseCallbackWorkerAction):
    def test_calls_service_client(self, params, returned, service_client_callback_method):
        service_client_callback_method.assert_called_once_with(
            url=params['callback_url'],
            json={
                'type': params['callback_message_type'].value,
                'data': params['message'],
            },
        )


@pytest.mark.parametrize('tvm_id', [None])
class TestNoTvmCase(BaseCallbackWorkerAction):
    def test_calls_callback_client_asymmetric(self,
                                              params,
                                              returned,
                                              tvm_id,
                                              callback_client_post_signed_message_method,
                                              partner_crypto):
        callback_client_post_signed_message_method.assert_called_once_with(
            url=params['callback_url'],
            message=params['message'],
            signer=partner_crypto.sign,
        )

    @pytest.mark.parametrize('sign_method', [APICallbackSignMethod.JWT])
    def test_calls_callback_client_jwt(self,
                                       params,
                                       returned,
                                       tvm_id,
                                       callback_client_post_jwt_message_method):
        callback_client_post_jwt_message_method.assert_called_once_with(
            url=params['callback_url'],
            message=params['message'],
            secret=params['callback_params'].secret,
        )
