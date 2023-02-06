import pytest

from mail.payments.payments.core.actions.interactions.bank_requisites import BankRequisitesAction
from mail.payments.payments.core.entities.bank_requisites import BankRequisites
from mail.payments.payments.core.exceptions import BankNotFoundError
from mail.payments.payments.interactions.refs.exceptions import RefsClientNotFoundError


class TestBankRequisitesAction:
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

    @pytest.fixture
    def cbrf_bank_exc(self):
        return None

    @pytest.fixture(autouse=True)
    def refs_mock(self, refs_client_mocker, bank_requisites, cbrf_bank_exc):
        with refs_client_mocker('cbrf_bank', result=bank_requisites, exc=cbrf_bank_exc) as mock:
            yield mock

    @pytest.fixture
    async def returned(self, bic):
        return await BankRequisitesAction(bic=bic).run()

    def test_result(self, bank_requisites, returned):
        assert bank_requisites == returned

    @pytest.mark.parametrize('cbrf_bank_exc', (RefsClientNotFoundError,))
    @pytest.mark.asyncio
    async def test_code_error(self, bic):
        with pytest.raises(BankNotFoundError):
            await BankRequisitesAction(bic=bic).run()
