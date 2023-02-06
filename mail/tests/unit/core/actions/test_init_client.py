from copy import copy

import pytest

from mail.payments.payments.core.actions.init_client import InitClientAction
from mail.payments.payments.core.entities.client import Client
from mail.payments.payments.core.entities.merchant import AddressData
from mail.payments.payments.core.exceptions import CoreDataError, MerchantNotFoundError
from mail.payments.payments.interactions.balance.exceptions import BalanceDataError


class BaseTestInitClientAction:
    @pytest.fixture
    def client_id(self):
        return None

    @pytest.fixture
    def person_id(self):
        return None

    @pytest.fixture
    def new_client_id(self, randn):
        return str(randn())

    @pytest.fixture
    def client_entity(self, merchant, new_client_id):
        return Client(
            client_id=new_client_id,
            name=merchant.ceo.surname + ' ' + merchant.ceo.name,
            email=merchant.ceo.email,
            phone=merchant.ceo.phone,
        )

    @pytest.fixture
    def new_person_id(self, randn):
        return str(randn())

    @pytest.fixture(autouse=True)
    def create_client_mock(self, balance_client_mocker, new_client_id):
        with balance_client_mocker('create_client', new_client_id) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def create_association_mock(self, balance_client_mocker):
        with balance_client_mocker('create_user_client_association', None) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def create_person_mock(self, balance_client_mocker, new_person_id):
        with balance_client_mocker('create_person', new_person_id) as mock:
            yield mock

    @pytest.fixture(params=['uid', 'merchant'])
    def params_key(self, request):
        return request.param

    @pytest.fixture
    def params(self, base_merchant_action_data_mock, merchant, params_key):
        data = {
            'uid': merchant.uid,
            'merchant': copy(merchant),
        }
        return {params_key: data[params_key]}

    @pytest.fixture
    async def returned(self, params):
        return await InitClientAction(**params).run()

    @pytest.fixture
    async def updated_merchant(self, storage, returned):
        return await storage.merchant.get(returned.uid)


class MixinTestInitClientAction:
    @pytest.mark.parametrize('params_key', ('uid',))
    @pytest.mark.asyncio
    async def test_merchant_not_found(self, params):
        params['uid'] += 1
        with pytest.raises(MerchantNotFoundError):
            await InitClientAction(**params).run()

    def test_find_client_call(self, merchant, find_client_mock, returned):
        find_client_mock.assert_called_once_with(merchant.uid)

    def test_returned(self, returned, updated_merchant):
        assert returned == updated_merchant

    def test_updated_merchant(self, merchant, new_client_id, new_person_id, updated_merchant):
        assert all([
            updated_merchant.uid == merchant.uid,
            updated_merchant.revision == merchant.revision + 1,
            updated_merchant.updated > merchant.updated,
            updated_merchant.client_id == new_client_id,
            updated_merchant.person_id == new_person_id,
        ])

    class TestBalanceDataError:
        @pytest.fixture(autouse=True)
        def create_client_mock(self, balance_client_mocker, new_client_id):
            with balance_client_mocker('create_client', exc=BalanceDataError) as mock:
                yield mock

        @pytest.mark.asyncio
        async def test_raises_core_data_error(self, params):
            with pytest.raises(CoreDataError):
                await InitClientAction(**params).run()


class TestInitClientActionNewClient(MixinTestInitClientAction, BaseTestInitClientAction):
    @pytest.fixture(autouse=True)
    def find_client_mock(self, balance_client_mocker):
        with balance_client_mocker('find_client', None) as mock:
            yield mock

    def test_create_client_call(self, merchant, client_entity, create_client_mock, returned):
        client_entity.client_id = None
        create_client_mock.assert_called_once_with(
            uid=merchant.uid,
            client=client_entity,
        )

    def test_create_association_call(self, merchant, new_client_id, create_association_mock, returned):
        create_association_mock.assert_called_once_with(merchant.uid, new_client_id)


class TestInitClientActionExistingClient(MixinTestInitClientAction, BaseTestInitClientAction):
    @pytest.fixture(autouse=True)
    def find_client_mock(self, balance_client_mocker, client_entity):
        with balance_client_mocker('find_client', client_entity) as mock:
            yield mock

    def test_create_association_not_called(self, create_association_mock, returned):
        create_association_mock.assert_not_called()

    def test_create_client_call(self, merchant, client_entity, create_client_mock, returned):
        create_client_mock.assert_called_once_with(
            uid=merchant.uid,
            client=client_entity,
        )


class TestPersonData:
    @pytest.fixture
    def legal_address(self):
        return AddressData(
            type='legal',
            city='Moscow',
            country='RU',
            home='16',
            street='Lva',
            zip='123456',
        )

    @pytest.fixture
    def post_address(self):
        return AddressData(
            type='post',
            city='SPB',
            country='RU',
            home='17',
            street='Lva 2',
            zip='789',
        )

    @pytest.mark.parametrize('post_specified', (False, True))
    def test_data(self, merchant, legal_address, post_address, post_specified):
        if post_specified:
            merchant.data.addresses = [legal_address, post_address]
        else:
            merchant.data.addresses = [legal_address]
            post_address = None

        assert InitClientAction.person_data(merchant) == {
            'account': merchant.bank.account,
            'bik': merchant.bank.bik,
            'fname': merchant.ceo.name,
            'lname': merchant.ceo.surname,
            'mname': merchant.ceo.patronymic,
            'email': merchant.ceo.email,
            'phone': merchant.ceo.phone,

            'name': merchant.organization.name,
            'longname': merchant.organization.full_name,
            'inn': merchant.organization.inn,
            'kpp': merchant.organization.kpp,
            'ogrn': merchant.organization.ogrn,

            'legal_address_city': legal_address.city,
            'legal_address_home': legal_address.home,
            'legal_address_postcode': legal_address.zip,
            'legal_address_street': legal_address.street,

            **(
                {
                    'address_city': post_address.city,
                    'address_home': post_address.home,
                    'address_postcode': post_address.zip,
                    'address_street': post_address.street,
                } if post_address is not None else {}
            )
        }
