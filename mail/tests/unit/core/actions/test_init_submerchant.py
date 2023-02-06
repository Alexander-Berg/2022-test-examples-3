import pytest

from mail.payments.payments.core.actions.init_submerchant import InitSubmerchantAction
from mail.payments.payments.core.exceptions import TinkoffMerchantInvalidAddressError, TinkoffMerchantInvalidDataError
from mail.payments.payments.interactions.exceptions import TinkoffAddressError, TinkoffDataError, TinkoffError


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestInitSubmerchant:
    @pytest.fixture
    def local_submerchant_id(self, randn):
        return f'{randn()}'

    @pytest.fixture(autouse=True)
    def create_merchant_mock(self, tinkoff_client_mocker, local_submerchant_id):
        with tinkoff_client_mocker('create_merchant', local_submerchant_id) as mock:
            yield mock

    @pytest.fixture(params=['uid', 'merchant'])
    def params(self, request, merchant):
        data = {
            'uid': merchant.uid,
            'merchant': merchant,
        }
        return {request.param: data[request.param]}

    @pytest.mark.asyncio
    async def test_tinkoff_request(self, merchant, local_submerchant_id, params, create_merchant_mock):
        await InitSubmerchantAction(**params).run()
        merchant.submerchant_id = local_submerchant_id
        create_merchant_mock.assert_called_once_with(merchant)

    @pytest.mark.asyncio
    async def test_returns_merchant(self, local_submerchant_id, params, merchant):
        merchant.submerchant_id = local_submerchant_id
        assert await InitSubmerchantAction(**params).run() == merchant

    @pytest.mark.asyncio
    async def test_does_not_save_merchant(self, storage, params, merchant, local_submerchant_id):
        await InitSubmerchantAction(**params).run()
        assert (await storage.merchant.get(merchant.uid)).submerchant_id != local_submerchant_id

    @pytest.mark.asyncio
    async def test_saves_merchant(self, storage, params, merchant, local_submerchant_id):
        params['save'] = True
        await InitSubmerchantAction(**params).run()
        assert (await storage.merchant.get(merchant.uid)).submerchant_id == local_submerchant_id

    class TestTinkoffError:
        @pytest.fixture(params=(TinkoffError, TinkoffDataError, TinkoffAddressError))
        def tinkoff_error_cls(self, request):
            return request.param

        @pytest.fixture
        def tinkoff_error(self, tinkoff_error_cls):
            return tinkoff_error_cls(method='POST', params={'some': 'params'})

        @pytest.fixture
        def create_merchant_mock(self, tinkoff_client_mocker, submerchant_id, tinkoff_error):
            with tinkoff_client_mocker('create_merchant', submerchant_id) as mock:
                mock.side_effect = tinkoff_error
                yield mock

        @pytest.mark.asyncio
        async def test_tinkoff_error(self, params, tinkoff_error_cls, tinkoff_error):
            exc_map = {
                TinkoffAddressError: TinkoffMerchantInvalidAddressError,
                TinkoffDataError: TinkoffMerchantInvalidDataError,
                TinkoffError: TinkoffMerchantInvalidDataError
            }

            exc_raised = exc_map[tinkoff_error_cls]

            with pytest.raises(exc_raised) as error:
                await InitSubmerchantAction(**params).run()

            assert all((
                error.value.message == exc_raised.MESSAGE,
                error.value.params == tinkoff_error.params
            ))
