import uuid
from copy import copy

import pytest

from hamcrest import has_property, match_equality

from mail.payments.payments.core.actions.base.merchant import BaseMerchantAction
from mail.payments.payments.core.entities.enums import (
    FunctionalityType, MerchantDraftPolicy, YandexPayPaymentGatewayType
)
from mail.payments.payments.core.entities.functionality import (
    Functionalities, MerchantFunctionality, PaymentsFunctionalityData, YandexPayPaymentGatewayFunctionalityData
)
from mail.payments.payments.core.entities.merchant import (
    AddressData, BankData, Merchant, OrganizationData, PersonData, PersonType
)
from mail.payments.payments.core.entities.moderation import ModerationData
from mail.payments.payments.core.entities.not_fetched import NOT_FETCHED
from mail.payments.payments.core.exceptions import (
    CoreActionDenyError, CoreFailError, MerchantIsAlreadyRegistered, MerchantNotFoundError
)
from mail.payments.payments.tests.utils import dummy_coro_ctx
from mail.payments.payments.utils.helpers import temp_setattr


@pytest.fixture(autouse=True)
def balance_get_person_mock(balance_client_mocker, person_entity):
    with balance_client_mocker('get_person', person_entity) as mock:
        yield mock


@pytest.fixture
def context_merchant(merchant):
    return merchant


@pytest.fixture
def params(context_merchant):
    return {'uid': context_merchant.uid}


@pytest.fixture
def class_vars():
    return {}


@pytest.fixture
def action_cls():
    return BaseMerchantAction


@pytest.fixture
def action(storage, action_cls, params, class_vars):
    class FinalMerchantAction(action_cls):
        async def handle(self):
            return self.merchant

    for key, value in class_vars.items():
        setattr(FinalMerchantAction, key, value)

    with temp_setattr(FinalMerchantAction.context, 'storage', storage):
        yield FinalMerchantAction(**params)


class TestLoadParent:
    @pytest.fixture
    async def returned(self, merchant_with_parent, action):
        action.merchant = copy(merchant_with_parent)
        action.merchant.parent = None
        await action._load_parent()
        return action.merchant

    def test_loads_parent(self, merchant_with_parent, returned):
        merchant_with_parent.oauth = NOT_FETCHED
        merchant_with_parent.parent.oauth = NOT_FETCHED
        merchant_with_parent.parent.functionalities = NOT_FETCHED
        assert returned == merchant_with_parent


class BaseLoadOAuthTest:
    @pytest.fixture
    async def returned(self, context_merchant, action):
        action.merchant = copy(context_merchant)
        await action._load_oauth()
        return action.merchant

    def test_no_oauth(self, returned):
        assert returned.oauth == []

    def test_loads_oauth(self, merchant_oauth, returned):
        assert returned.oauth == [merchant_oauth]


class TestLoadOAuthWithoutParent(BaseLoadOAuthTest):
    @pytest.fixture
    def context_merchant(self, merchant):
        return merchant

    @pytest.fixture
    async def merchant_oauth(self, merchant, create_merchant_oauth):
        return await create_merchant_oauth(uid=merchant.uid)


class TestLoadOAuthWithParent(BaseLoadOAuthTest):
    @pytest.fixture
    def context_merchant(self, merchant_with_parent):
        return merchant_with_parent

    @pytest.fixture
    async def merchant_oauth(self, storage, merchant_with_parent, create_merchant_oauth):
        return await create_merchant_oauth(uid=merchant_with_parent.uid)

    @pytest.fixture
    async def parent_oauth(self, storage, merchant_with_parent, create_merchant_oauth):
        parent = await storage.merchant.get(merchant_with_parent.parent_uid)
        return await create_merchant_oauth(uid=parent.uid)

    @pytest.mark.asyncio
    async def test_not_loads_for_parent(self, parent_oauth, returned):
        assert returned.parent.oauth == []  # FIXME: Кривовато


class TestLoadMerchantLoads:
    @pytest.fixture
    async def returned(self, action):
        return await action.run()

    @pytest.mark.parametrize('class_vars', (
        {'skip_parent': True},
        {'skip_parent': True, 'skip_data': False},
    ))
    @pytest.mark.asyncio
    async def test_raises_on_loading_data_without_parent(self, action):
        with pytest.raises(CoreFailError):
            await action.run()

    @pytest.mark.parametrize('class_vars', (
        {},
        {'skip_parent': False, 'skip_data': False, 'skip_moderation': False},
    ))
    def test_load_all(self, merchant, person_entity, returned):
        no_moderation = ModerationData(
            approved=False,
            reasons=[],
            has_moderation=False,
            has_ongoing=False,
        )
        merchant.moderation = no_moderation
        merchant.moderations = {
            FunctionalityType.PAYMENTS: no_moderation,
            FunctionalityType.YANDEX_PAY: no_moderation,
        }
        returned.updated = merchant.updated  # sync updated timestamp
        returned.data_updated_at = merchant.data_updated_at  # sync data_updated timestamp
        returned.revision = merchant.revision  # sync revision
        assert returned == merchant

    class TestMerchantNotFound:
        @pytest.fixture
        def params(self, unique_rand, randn):
            return {'uid': unique_rand(randn, basket='uid')}

        @pytest.mark.parametrize('class_vars', (
            {},
            {'allow_none': False},
        ))
        @pytest.mark.asyncio
        async def test_raises(self, action):
            with pytest.raises(MerchantNotFoundError):
                await action.run()

        @pytest.mark.parametrize('class_vars', ({'allow_none': True},))
        def test_allow_none(self, returned):
            assert returned is None

    class TestLoadCalls:
        @pytest.fixture(autouse=True)
        def load_parent_mock(self, mocker, action):
            with dummy_coro_ctx() as coro:
                yield mocker.patch.object(action, '_load_parent', mocker.Mock(return_value=coro))

        @pytest.fixture(autouse=True)
        def load_data_mock(self, mocker, action):
            with dummy_coro_ctx() as coro:
                yield mocker.patch.object(action, '_load_data', mocker.Mock(return_value=coro))

        @pytest.fixture(autouse=True)
        def load_moderation_mock(self, mocker, action):
            with dummy_coro_ctx() as coro:
                yield mocker.patch.object(action, '_load_moderation', mocker.Mock(return_value=coro))

        @pytest.fixture(autouse=True)
        def load_oauth_mock(self, mocker, action):
            with dummy_coro_ctx() as coro:
                yield mocker.patch.object(action, '_load_oauth', mocker.Mock(return_value=coro))

        @pytest.mark.parametrize('class_vars', (
            {},
            {'skip_data': False},
            {'skip_data': True},
        ))
        def test_load_data_calls(self, class_vars, returned, load_data_mock):
            assert load_data_mock.call_count == bool(not class_vars.get('skip_data'))

        @pytest.mark.parametrize('class_vars', (
            {},
            {'skip_moderation': False},
            {'skip_moderation': True},
        ))
        def test_load_moderation_calls(self, class_vars, returned, load_moderation_mock):
            assert load_moderation_mock.call_count == bool(not class_vars.get('skip_moderation'))

        @pytest.mark.parametrize('class_vars', (
            {},
            {'skip_oauth': False},
            {'skip_oauth': True},
        ))
        def test_load_oauth_calls(self, class_vars, returned, load_oauth_mock):
            assert load_oauth_mock.call_count == bool(not class_vars.get('skip_oauth'))

        @pytest.mark.parametrize('class_vars', (
            {'skip_parent': True, 'skip_data': True},
        ))
        def test_load_parent_calls(self, returned, load_parent_mock):
            load_parent_mock.assert_not_called()

        class TestWithParent:
            @pytest.fixture
            def params(self, merchant_with_parent):
                return {'uid': merchant_with_parent.uid}

            @pytest.mark.parametrize('class_vars', (
                {},
                {'skip_parent': False},
                {'skip_parent': True, 'skip_data': True},
            ))
            def test_with_parent__load_parent_calls(self, class_vars, returned, load_parent_mock):
                assert load_parent_mock.call_count == bool(not class_vars.get('skip_parent'))


class TestIsMerchantDraft:
    class TestMerchantDraftFobbiden:
        @pytest.fixture
        def class_vars(self):
            return {'draft_policy': MerchantDraftPolicy.MERCHANT_DRAFT_FORBIDDEN}

        def test_action_deny(self, action, merchant_draft):
            action.merchant = merchant_draft
            with pytest.raises(CoreActionDenyError):
                action._is_merchant_draft()

        def test_no_draft(self, action, merchant):
            action.merchant = merchant
            assert not action._is_merchant_draft()

    class TestMerchantDraftRequired:
        @pytest.fixture
        def class_vars(self):
            return {'draft_policy': MerchantDraftPolicy.MERCHANT_DRAFT_REQUIRED}

        def test_already_registered(self, action, merchant):
            action.merchant = merchant
            with pytest.raises(MerchantIsAlreadyRegistered):
                action._is_merchant_draft()

        def test_is_draft(self, action, merchant_draft):
            action.merchant = merchant_draft
            assert action._is_merchant_draft()

    class TestMerchantDraftAllowed:
        @pytest.fixture
        def class_vars(self):
            return {'draft_policy': MerchantDraftPolicy.MERCHANT_DRAFT_ALLOWED}

        def test_not_draft(self, action, merchant):
            action.merchant = merchant
            assert not action._is_merchant_draft()

        def test_is_merchant(self, action, merchant_draft):
            action.merchant = merchant_draft
            assert action._is_merchant_draft()


class TestFillMerchantFromPerson:
    @pytest.fixture
    def filled_merchant(self, merchant_entity, person_entity):
        BaseMerchantAction.fill_merchant_from_person(merchant_entity, person_entity)
        return merchant_entity

    @pytest.fixture
    def person_entity_no_post_address(self, person_entity):
        person_entity.address_city = None
        person_entity.address_home = None
        person_entity.address_street = None
        person_entity.address_postcode = None
        return person_entity

    @pytest.fixture
    def merchant_entity_no_bank(self, merchant_entity):
        merchant_entity.data.bank = None
        return merchant_entity

    @pytest.fixture
    def merchant_entity_no_organization(self, merchant_entity):
        merchant_entity.data.organization = None
        return merchant_entity

    def test_merchant_has_no_data(self, person_entity):
        with pytest.raises(Exception):
            BaseMerchantAction.fill_merchant_from_person(Merchant(), person_entity)

    def test_legal_address(self, person_entity, filled_merchant):
        assert [
            AddressData(
                type='legal',
                city=person_entity.legal_address_city,
                country='RUS',
                home=person_entity.legal_address_home,
                street=person_entity.legal_address_street,
                zip=person_entity.legal_address_postcode,
            )
        ] == [address for address in filled_merchant.data.addresses if address.type == 'legal']

    def test_post_address_exist(self, person_entity, filled_merchant):
        assert [
            AddressData(
                type='post',
                city=person_entity.address_city,
                country='RUS',
                home=person_entity.address_home,
                street=person_entity.address_street,
                zip=person_entity.address_postcode,
            )
        ] == [address for address in filled_merchant.data.addresses if address.type == 'post']

    def test_post_address_empty(self, merchant_entity, person_entity_no_post_address):
        BaseMerchantAction.fill_merchant_from_person(merchant_entity, person_entity_no_post_address)
        assert not [address for address in merchant_entity.data.addresses if address.type == 'post']

    def test_bank_exist(self, person_entity, merchant_entity, filled_merchant):
        assert BankData(
            account=person_entity.account,
            bik=person_entity.bik,
            correspondent_account=merchant_entity.data.bank.correspondent_account,
            name=merchant_entity.data.bank.name,
        ) == filled_merchant.data.bank

    def test_bank_empty(self, merchant_entity_no_bank, person_entity):
        BaseMerchantAction.fill_merchant_from_person(merchant_entity_no_bank, person_entity)
        assert BankData(
            account=person_entity.account,
            bik=person_entity.bik,
        ) == merchant_entity_no_bank.data.bank

    def test_persons_ceo(self, person_entity, merchant_entity, filled_merchant):
        assert [
            PersonData(
                type=PersonType.CEO,
                name=person_entity.fname,
                email=person_entity.email,
                phone=person_entity.phone,
                surname=person_entity.lname,
                patronymic=person_entity.mname,
                birth_date=merchant_entity.data.persons[0].birth_date,
            )
        ] == [person for person in filled_merchant.data.persons if person.type == PersonType.CEO]

    def test_persons_count(self, filled_merchant):
        assert 2 == len(filled_merchant.data.persons)

    def test_organization_exist(self, merchant_entity, person_entity, filled_merchant):
        organization: OrganizationData = merchant_entity.data.organization
        assert OrganizationData(
            type=organization.type,
            name=person_entity.name,
            english_name=organization.english_name,
            full_name=person_entity.longname,
            inn=person_entity.inn,
            kpp=person_entity.kpp,
            ogrn=person_entity.ogrn,
            site_url=organization.site_url,
            description=organization.description,
        ) == filled_merchant.data.organization

    def test_organization_empty(self,
                                merchant_entity_no_organization,
                                person_entity):
        BaseMerchantAction.fill_merchant_from_person(merchant_entity_no_organization, person_entity)
        assert OrganizationData(
            name=person_entity.name,
            full_name=person_entity.longname,
            inn=person_entity.inn,
            kpp=person_entity.kpp,
            ogrn=person_entity.ogrn,
        ) == merchant_entity_no_organization.data.organization

    def test_returns_true_if_data_has_changed(self, merchant_entity, person_entity):
        assert (BaseMerchantAction.fill_merchant_from_person(merchant_entity, person_entity),
                BaseMerchantAction.fill_merchant_from_person(merchant_entity, person_entity)) == (True, False)

    def test_returns_false_if_order_has_changed(self, merchant_entity, person_entity):
        BaseMerchantAction.fill_merchant_from_person(merchant_entity, person_entity)
        merchant_entity.data.addresses[0], merchant_entity.data.addresses[1] = \
            merchant_entity.data.addresses[1], merchant_entity.data.addresses[0]

        merchant_entity.data.persons[0], merchant_entity.data.persons[1] = \
            merchant_entity.data.persons[1], merchant_entity.data.persons[0]

        assert BaseMerchantAction.fill_merchant_from_person(merchant_entity, person_entity) is False


class TestLoadModeration:
    @pytest.mark.asyncio
    async def test_result(self, mocker, action_cls, action):
        mocker.patch.object(
            action_cls, 'get_moderation_data', mocker.AsyncMock(side_effect=lambda *args, **kwargs: mocker.Mock())
        )

        merchant = await action.run()

        assert all(m is not None for m in merchant.moderations.values())
        assert merchant.moderations[FunctionalityType.PAYMENTS] == merchant.moderation

    @pytest.mark.asyncio
    async def test_get_moderation_data_calls(self, mocker, action_cls, action):
        mock = mocker.patch.object(action_cls, 'get_moderation_data', mocker.AsyncMock())

        merchant = await action.run()

        assert mock.call_count == 2
        mock.assert_has_calls([
            mocker.call(
                match_equality(has_property('uid', merchant.uid)),
                functionality_type=functionality_type,
            )
            for functionality_type in FunctionalityType
        ])


@pytest.mark.asyncio
async def test_load_functionalities(merchant, action, storage):
    functionalities = (
        await storage.functionality.create(
            MerchantFunctionality(
                uid=merchant.uid,
                functionality_type=FunctionalityType.PAYMENTS,
                data=PaymentsFunctionalityData(),
            )
        ),
        await storage.functionality.create(
            MerchantFunctionality(
                uid=merchant.uid,
                functionality_type=FunctionalityType.YANDEX_PAY,
                data=YandexPayPaymentGatewayFunctionalityData(
                    partner_id=uuid.uuid4(),
                    payment_gateway_type=YandexPayPaymentGatewayType.PSP,
                    gateway_id='123',
                ),
            )
        )
    )
    expected_functionalities = Functionalities(**{
        functionality.functionality_type.value: functionality.data
        for functionality in functionalities
    })

    merchant = await action.run()

    assert merchant.functionalities == expected_functionalities
