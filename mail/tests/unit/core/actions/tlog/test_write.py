import pytest

from mail.payments.payments.core.actions.base.action import BaseAction
from mail.payments.payments.core.actions.tlog.write import WriteToTLogAction
from mail.payments.payments.tests.utils import dummy_async_context_manager, dummy_coro
from mail.payments.payments.utils.helpers import temp_setattr


@pytest.fixture
def producer_mock(mocker):
    mock = mocker.Mock()
    mock.write_dict.return_value = dummy_coro()
    yield mock
    mock.write_dict.return_value.close()


@pytest.fixture(autouse=True)
def mock_lb_factory(lb_factory_mock):
    with temp_setattr(BaseAction.context, 'lb_factory', lb_factory_mock):
        yield


@pytest.fixture(autouse=True)
def producer_cls_mock(mocker, producer_mock):
    yield mocker.patch(
        'mail.payments.payments.core.actions.tlog.write.TLogLogbrokerProducer',
        mocker.Mock(return_value=dummy_async_context_manager(producer_mock)),
    )


class TestTLogWriteDict:
    @pytest.fixture
    def data(self):
        return {'field': 'value'}

    @pytest.fixture
    async def returned(self, data):
        return await WriteToTLogAction(data).run()

    def test_written_data(self, returned, producer_mock, data):
        producer_mock.write_dict.assert_called_once_with(data=data)
