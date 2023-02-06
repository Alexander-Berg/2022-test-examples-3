from xmlrpc.client import DateTime

import pytest

from mail.payments.payments.conf import settings
from mail.payments.payments.core.actions.contract import InitContractAction, TerminateContractAction
from mail.payments.payments.core.entities.merchant import Merchant, MerchantOptions
from mail.payments.payments.core.entities.serial import Serial
from mail.payments.payments.core.entities.service import OfferSettings
from mail.payments.payments.interactions.balance.exceptions import BalanceContractRuleViolation


@pytest.fixture(params=('uid', 'merchant'))
def params(request, merchant):
    data = {
        'uid': merchant.uid,
        'merchant': merchant,
    }
    return {request.param: data[request.param]}


@pytest.fixture
async def parent_merchant(storage, unique_rand, randn, merchant_data):
    async with storage.conn.begin():
        m = await storage.merchant.create(Merchant(
            uid=unique_rand(randn, basket='uid'),
            name='test-parent-uid-set-name',
            data=merchant_data,
        ))
        await storage.serial.create(Serial(m.uid))
        return m


class TestInitContractAction:
    @pytest.fixture
    def contract_id(self):
        return None

    @pytest.fixture
    def parent_uid(self):
        return None

    @pytest.fixture
    def new_contract_id(self):
        return 'test-init-client-new_contract_id'

    @pytest.fixture
    def new_external_id(self):
        return 'test-init-client-new_offer_external_id'

    @pytest.fixture(autouse=True)
    def create_offer_mock(self, balance_client_mocker, new_contract_id, new_external_id):
        with balance_client_mocker('create_offer', (new_contract_id, new_external_id)) as mock:
            yield mock

    @pytest.fixture
    async def returned(self, params):
        return await InitContractAction(**params).run()

    @pytest.fixture
    async def returned_merchant(self, returned):
        return returned[0]

    @pytest.fixture
    async def updated_merchant(self, storage, returned_merchant):
        return await storage.merchant.get(returned_merchant.uid)

    def test_returned(self, merchant, new_contract_id, returned_merchant):
        assert returned_merchant.contract_id == new_contract_id

    def test_updated_merchant(self, returned_merchant, updated_merchant):
        assert updated_merchant == returned_merchant

    class TestCreateOfferCall:
        @pytest.fixture(params=(True, False))
        def merchant_options(self, request, rands):
            return MerchantOptions(
                offer_settings=OfferSettings(
                    pdf_template=rands(),
                    slug=rands(),
                    data_override={rands(): rands()}
                ) if request.param else OfferSettings(pdf_template=None, slug=None, data_override={})
            )

        def test_create_offer_call(self, merchant, create_offer_mock, returned_merchant):
            data_override = merchant.options.offer_settings.data_override
            create_offer_mock.assert_called_once_with(
                uid=merchant.uid,
                acquirer=merchant.acquirer,
                client_id=merchant.client_id,
                person_id=merchant.person_id,
                extra_params=data_override
            )

    class TestContractIDSet:
        @pytest.fixture
        def contract_id(self):
            return 'test-contract-id-set_contract_id'

        def test_contract_id_set__contract_id_not_changed(self, contract_id, returned_merchant):
            assert returned_merchant.contract_id == contract_id

        def test_contract_id_set__create_offer_not_called(self, create_offer_mock, returned_merchant):
            create_offer_mock.assert_not_called()

    class TestParentUIDSet:
        @pytest.fixture
        def parent_uid(self, parent_merchant):
            return parent_merchant.uid

        def test_parent_uid_set__contract_id_not_changed(self, returned_merchant):
            assert returned_merchant.contract_id is None

        def test_parent_uid_set__create_offer_not_called(self, create_offer_mock, returned_merchant):
            create_offer_mock.assert_not_called()

    class TestContractExistsInBalance:
        @pytest.fixture
        def balance_client_contract(self, payments_settings, merchant, new_contract_id, new_external_id):
            """What can actually come from balance client method."""
            return {
                'CURRENCY': 'RUR',
                'OFFER_ACCEPTED': 1,
                'IS_SUSPENDED': 0,
                'MANAGER_CODE': 12345,
                'IS_ACTIVE': 1,
                'IS_SIGNED': 1,
                'CONTRACT_TYPE': 9,
                'PERSON_ID': merchant.person_id,
                'IS_FAXED': 0,
                'SERVICES': [payments_settings.BALANCE_SERVICE_ID],
                'PAYMENT_TYPE': payments_settings.BALANCE_PAYMENT_TYPE,
                'PARTNER_COMMISSION_PCT2': payments_settings.BALANCE_COMISSION,
                'IS_CANCELLED': 0,
                'IS_DEACTIVATED': 0,
                'DT': DateTime('20190822T00:00:00'),
                'EXTERNAL_ID': new_external_id,
                'ID': new_contract_id
            }

        @pytest.fixture
        def rule_violation_message(self, new_contract_id):
            return "Rule violation: 'Для клиента с id=some-client-id уже существуют договоры с такими типом, фирмой " \
                   f"или сервисом: {new_contract_id}'"

        @pytest.fixture(autouse=True)
        def mock_balance_client_create_offer(self, balance_client_mocker, rule_violation_message):
            """Make sure create offer raises contract violation exception."""
            exc = BalanceContractRuleViolation(message=rule_violation_message)
            with balance_client_mocker(method_name='create_offer', exc=exc) as mock:
                yield mock

        @pytest.fixture(autouse=True)
        def mock_balance_client_get_client_contracts(self, balance_client_mocker, balance_client_contract):
            """Pretend there is an active contract in balance."""
            with balance_client_mocker(method_name='get_client_contracts', result=[balance_client_contract]) as mock:
                yield mock

        @pytest.fixture
        def set_reuse_id_setting(self, reuse_id):
            settings.BALANCE_REUSE_CONTRACT_ID_FROM_ERROR = reuse_id

        @pytest.fixture
        async def action_result(self, params):
            return await InitContractAction(**params).run()

        @pytest.mark.parametrize('reuse_id', [False, True])
        def test_returns_external_id(self, set_reuse_id_setting, action_result, new_external_id):
            assert action_result[1] == new_external_id

        @pytest.mark.parametrize('reuse_id', [False, True])
        def test_merchant_updated(self, set_reuse_id_setting, action_result, new_contract_id):
            merchant = action_result[0]
            assert merchant.contract_id == new_contract_id

        @pytest.mark.parametrize('reuse_id', [False, True])
        def test_get_client_contracts_call(self, mock_balance_client_get_client_contracts, set_reuse_id_setting,
                                           merchant, reuse_id, action_result):
            mock_balance_client_get_client_contracts.assert_called_once_with(
                client_id=merchant.client_id,
                person_id=None if reuse_id else merchant.person_id,
                is_active=True
            )


class TestTerminateContractAction:
    @pytest.fixture(autouse=True)
    def create_collateral_mock(self, balance_client_mocker):
        with balance_client_mocker('create_collateral') as mock:
            yield mock

    @pytest.fixture
    def contract_id(self):
        return 'test-terminate-contract-action-contract_id'

    @pytest.fixture
    def parent_uid(self):
        return None

    @pytest.fixture
    async def returned(self, params):
        return await TerminateContractAction(**params).run()

    @pytest.fixture
    async def updated_merchant(self, storage, returned):
        return await storage.merchant.get(returned.uid)

    def test_returned(self, returned):
        assert returned.contract_id is None

    def test_updated(self, returned, updated_merchant):
        assert updated_merchant == returned

    def test_create_collateral_call(self, merchant, contract_id, create_collateral_mock, returned):
        create_collateral_mock.assert_called_once_with(merchant.uid, contract_id)

    @pytest.mark.parametrize('contract_id', (None,))
    def test_contract_id_empty_collateral_not_created(self, create_collateral_mock, returned):
        create_collateral_mock.assert_not_called()

    class TestParentUIDSet:
        @pytest.fixture
        def parent_uid(self, parent_merchant):
            return parent_merchant.uid

        def test_parent_uid_set_collateral_not_created(self, parent_uid, create_collateral_mock, returned):
            create_collateral_mock.assert_not_called()
