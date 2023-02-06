import pytest

from mail.payments.payments.core.entities.bank_requisites import BankRequisites


class TestBankRequisitesHandler:
    @pytest.fixture
    def bic(self):
        return '046577674'

    @pytest.fixture
    def name_full(self):
        return 'УРАЛЬСКИЙ БАНК ПАО СБЕРБАНК'

    @pytest.fixture
    def corr(self):
        return '30101810500000000674'

    @pytest.fixture
    def bank_requisites(self, bic, name_full, corr):
        return BankRequisites(
            bic=bic,
            name_full=name_full,
            corr=corr
        )

    @pytest.fixture(autouse=True)
    def action(self, mock_action, bank_requisites):
        from mail.payments.payments.core.actions.interactions.bank_requisites import BankRequisitesAction
        return mock_action(BankRequisitesAction, bank_requisites)

    @pytest.fixture
    async def response(self, payments_client, bic):
        return await payments_client.get(f'/v1/refs/bank/{bic}')

    @pytest.fixture
    async def response_data(self, response):
        return (await response.json())['data']

    @pytest.mark.asyncio
    async def test_params(self, action, bic, response):
        action.assert_called_once_with(bic=bic)

    @pytest.mark.asyncio
    async def test_response_status(self, response):
        assert 200 == response.status

    @pytest.mark.asyncio
    async def test_response_bic(self, bic, response_data):
        assert bic == response_data['bic']

    @pytest.mark.asyncio
    async def test_response_name_full(self, name_full, response_data):
        assert name_full == response_data['name_full']

    @pytest.mark.asyncio
    async def test_response_corr(self, corr, response_data):
        assert corr == response_data['corr']
