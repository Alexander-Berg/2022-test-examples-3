from dataclasses import asdict
from datetime import date, datetime, timedelta
from typing import Iterable

import pytest
import pytz as pytz

from sendr_utils import alist

from hamcrest import (
    assert_that, contains, contains_inanyorder, equal_to, has_entries, has_item, has_properties, has_property,
    match_equality
)

from mail.payments.payments.core.actions.init_client import InitClientAction
from mail.payments.payments.core.actions.init_submerchant import InitSubmerchantAction
from mail.payments.payments.core.actions.merchant.create import CreateMerchantAction
from mail.payments.payments.core.actions.merchant.create_entity import CreateMerchantEntityAction
from mail.payments.payments.core.actions.merchant.functionality import (
    PutPaymentsMerchantFunctionalityAction, PutYandexPayMerchantFunctionalityAction
)
from mail.payments.payments.core.actions.merchant.get import GetMerchantAction
from mail.payments.payments.core.actions.merchant.preregister import (
    GetMerchantPreregistrationAction, MerchantPreregistrationAcquirerChooser, PreregisterMerchantAction
)
from mail.payments.payments.core.entities.bank_requisites import BankRequisites
from mail.payments.payments.core.entities.client import Client
from mail.payments.payments.core.entities.enums import AcquirerType, MerchantRole, MerchantType, OperationKind
from mail.payments.payments.core.entities.functionality import (
    PaymentsFunctionalityData, YandexPayMerchantFunctionalityData
)
from mail.payments.payments.core.entities.merchant import (
    AddressData, BankData, OrganizationData, PersonData, PersonType
)
from mail.payments.payments.core.entities.merchant_preregistration import PreregisterData
from mail.payments.payments.core.entities.not_fetched import NOT_FETCHED
from mail.payments.payments.core.entities.service import Service, ServiceOptions
from mail.payments.payments.core.entities.spark import (
    LeaderData, PhoneData, SparkAddressData, SparkData, SparkOrganizationData
)
from mail.payments.payments.core.entities.userinfo import UserInfo
from mail.payments.payments.core.exceptions import (
    CategoryNotFoundError, ConflictingAcquirerType, CoreFailError, InnIsEmptyError, MerchantExistsPreregisterError,
    MerchantInactivePreregisterError, MerchantPreregistrationNotFoundError, ServiceNotFoundError
)
from mail.payments.payments.interactions.balance.entities import Person
from mail.payments.payments.interactions.balance.exceptions import BaseBalanceError
from mail.payments.payments.interactions.refs.exceptions import RefsClientNotFoundError
from mail.payments.payments.interactions.search_wizard.entities import Fio
from mail.payments.payments.interactions.spark.exceptions import SparkGetInfoError


class TestPreregisterAction:
    @pytest.fixture
    def functionality(self):
        return PaymentsFunctionalityData()

    @pytest.fixture
    def functionality_action(self, mock_action):
        return PutYandexPayMerchantFunctionalityAction

    @pytest.fixture
    def mock_put_functionality(self, mock_action, functionality_action):
        return mock_action(functionality_action)

    @pytest.fixture(autouse=True)
    def blackbox_userinfo_mock(self, blackbox_client_mocker, merchant_preregistered_uid, rands):
        user_info = UserInfo(
            uid=merchant_preregistered_uid,
            default_email=rands(),
        )
        with blackbox_client_mocker('userinfo', user_info) as mock:
            yield mock

    @pytest.fixture
    def balance_client(self, client_id, rands):
        return Client(
            client_id=client_id,
            name=rands(),
            email=rands(),
            phone=rands()
        )

    @pytest.fixture(autouse=True)
    def balance_find_client_mock(self, balance_client_mocker, balance_client):
        with balance_client_mocker('find_client', balance_client) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def balance_get_client_persons_mock(
        self,
        balance_client_mocker,
        person_entity,
    ):
        with balance_client_mocker('get_client_persons', [person_entity]) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def spark_get_info_mock(self, spark_client_mocker):
        with spark_client_mocker('get_info', exc=SparkGetInfoError) as mock:
            yield mock

    @pytest.fixture
    def inn(self, randn):
        return str(randn())

    @pytest.fixture
    def spark_id(self, randn):
        return randn()

    @pytest.fixture
    def contact_person_raw(self):
        return {
            'name': 'Foo',
            'email': 'contact@yandex.test',
            'phone': '+0 123 456 78 90',
            'surname': 'Bar',
        }

    @pytest.fixture
    def contact_person(self, contact_person_raw):
        return PersonData(type=PersonType.CONTACT, **contact_person_raw)

    @pytest.fixture
    async def action_params(
        self, merchant_preregistered_uid, preregister_data, inn, spark_id, functionality
    ):
        return {
            **asdict(preregister_data),
            'uid': merchant_preregistered_uid,
            'inn': inn,
            'spark_id': spark_id,
            'contact': None,
            'functionality': functionality,
        }

    @pytest.fixture
    def run_action(self, action_params):
        async def _inner(**kwargs):
            # not action_params.update so keys can be deleted
            action_preregister_params = kwargs or action_params
            return await PreregisterMerchantAction(**action_preregister_params).run()

        return _inner

    @pytest.fixture
    async def action_result(self, run_action):
        return await run_action()

    @pytest.fixture
    def client_id(self):
        return '58613149'

    @pytest.fixture
    def balance_person_factory(self, client_id):
        def _builder(suffix, inn, date):
            return Person(
                client_id=client_id,
                person_id=f'person_id_{suffix}',

                account=f'account_{suffix}',
                bik='',  # disable prefill bank requisites
                fname=f'first_name_{suffix}',
                lname=f'last_name_{suffix}',
                email=f'example@mail.{suffix}',
                phone=f'phone_{suffix}',

                name=f'name_{suffix}',
                longname=f'longname_{suffix}',
                inn=inn,
                kpp=f'kpp_{suffix}',
                ogrn=f'ogrn_{suffix}',

                legal_address_city=f'legal_address_city_{suffix}',
                legal_address_home=f'legal_address_home_{suffix}',
                legal_address_postcode=f'legal_address_postcode_{suffix}',
                legal_address_street=f'legal_address_street_{suffix}',

                mname=f'middle_name_{suffix}',

                address_city=f'address_city_{suffix}',
                address_home=f'address_home_{suffix}',
                address_postcode=f'address_postcode_{suffix}',
                address_street=f'address_street_{suffix}',

                date=date.replace(tzinfo=pytz.timezone('Europe/Moscow'))
            )

        return _builder

    @pytest.fixture
    def leader_name(self):
        return 'Волож Аркадий Юрьевич'

    @pytest.fixture
    def fio(self):
        return Fio(
            first_name='Аркадий',
            middle_name='Юрьевич',
            last_name='Волож',
        )

    @pytest.fixture
    def organization_is_active(self):
        return True

    @pytest.fixture
    def spark_data_addresses_actual_date(self):
        return date(2020, 12, 3)

    @pytest.fixture
    def spark_data(self, leader_name, inn, organization_is_active, spark_data_addresses_actual_date):
        return SparkData(
            spark_id='some_spark_id',
            registration_date=None,
            organization_data=SparkOrganizationData(
                organization=OrganizationData(
                    type=MerchantType.IP,
                    name='spark_name',
                    english_name='spark_english_name',
                    full_name='spark_full_name',
                    inn=inn,
                    kpp='spark_kpp',
                    ogrn='spark_ogrn',
                ),
                actual_date=date(2020, 1, 1),
            ),
            okved_list=[],
            leaders=[
                LeaderData(
                    name=leader_name,
                    birth_date=date(1964, 2, 11),
                    actual_date=date(2020, 12, 2),
                ),
            ],
            addresses=[
                SparkAddressData(
                    address=AddressData(
                        type='legal',
                        city='spark_city',
                        country='spark_country',
                        home='spark_home',
                        street='spark_street',
                        zip='spark_zip',
                    ),
                    actual_date=spark_data_addresses_actual_date,
                ),
            ],
            phones=[
                PhoneData(
                    code='123',
                    number='456 78 90',
                    verification_date=date(2020, 12, 2),
                ),
            ],
            active=organization_is_active,
        )

    class TestPreregisterActionCreates:
        @pytest.fixture
        def get_requires_online_mock_value(self):
            return True

        @pytest.fixture
        def get_requires_online_mock(self, mocker, get_requires_online_mock_value):
            return mocker.patch.object(
                MerchantPreregistrationAcquirerChooser,
                'get_requires_online',
                return_value=get_requires_online_mock_value,
            )

        @pytest.fixture
        def get_acquirer_mock_value(self):
            return AcquirerType.TINKOFF

        @pytest.fixture
        def get_acquirer_mock(self, mocker, get_acquirer_mock_value):
            return mocker.patch.object(
                MerchantPreregistrationAcquirerChooser,
                'get_acquirer',
                return_value=get_acquirer_mock_value,
            )

        @pytest.fixture
        def get_can_skip_registration_mock_value(self):
            return False

        @pytest.fixture
        def get_can_skip_registration_mock(self, mocker, get_can_skip_registration_mock_value):
            return mocker.patch.object(
                MerchantPreregistrationAcquirerChooser,
                'get_can_skip_registration',
                return_value=get_can_skip_registration_mock_value,
            )

        class TestCreatesOrUpdatesRegistrationAlways:
            """Ошибки создания мерчанта или ошибки валидации не препятствуют созданию записи о регистрации"""

            @pytest.fixture(autouse=True)
            def fail_merchant_creation(self, mocker):
                mocker.patch.object(
                    PreregisterMerchantAction,
                    '_create_or_update_preregistered_merchant',
                    side_effect=Exception(),
                )

            @pytest.mark.asyncio
            async def test_raises_exception(self, run_action):
                with pytest.raises(CoreFailError):
                    await run_action()

            @pytest.mark.asyncio
            async def test_registration_is_created(
                self, storage, action_params, run_action,
            ):
                try:
                    await run_action()
                except Exception:
                    pass

                registration = await storage.merchant_preregistration.get(uid=action_params['uid'])

                assert registration.data.raw_preregister_data == PreregisterData(
                    inn=action_params['inn'],
                    services=action_params['services'],
                    categories=action_params['categories'],
                    require_online=action_params['require_online'],
                )

        @pytest.mark.asyncio
        async def test_raises_service_not_found(self, run_action, action_params):
            action_params['services'] = [1000000000]
            with pytest.raises(ServiceNotFoundError):
                await run_action(**action_params)

        @pytest.mark.asyncio
        async def test_raises_category_not_found(self, run_action, action_params):
            action_params['categories'] = [1000000000]
            with pytest.raises(CategoryNotFoundError):
                await run_action(**action_params)

        @pytest.mark.asyncio
        async def test_creates_merchant(self, action_result, storage):
            action_result.preregistration = None
            action_result.functionalities = NOT_FETCHED
            assert action_result == await storage.merchant.get(uid=action_result.uid)

        @pytest.mark.asyncio
        async def test_raises_error_if_merchant_has_registered_flag(
            self, storage, action_result, run_action,
        ):
            action_result.data.registered = True
            await storage.merchant.save(action_result)

            with pytest.raises(MerchantExistsPreregisterError):
                await run_action()

        @pytest.mark.asyncio
        async def test_raises_error_on_existing_merchant(
            self, run_action, action_params, create_merchant,
        ):
            merchant = await create_merchant()
            action_params['uid'] = merchant.uid
            with pytest.raises(MerchantExistsPreregisterError):
                await run_action(**action_params)

        def test_created_merchant_inn(self, action_result, inn):
            assert action_result.organization.inn == inn

        @pytest.mark.asyncio
        async def test_merchant_registration_data(self, storage, action_result, action_params):
            registration = await storage.merchant_preregistration.get(uid=action_result.uid)
            expected_preregister_data = PreregisterData(
                inn=action_params['inn'],
                services=action_params['services'],
                categories=action_params['categories'],
                require_online=action_params['require_online'],
            )
            expected_raw_preregister_data = PreregisterData(
                inn=action_params['inn'],
                services=action_params['services'],
                categories=action_params['categories'],
                require_online=action_params['require_online'],
            )
            assert_that(
                registration.data,
                has_properties(dict(
                    preregister_data=expected_preregister_data,
                    raw_preregister_data=expected_raw_preregister_data,
                ))
            )

        @pytest.mark.asyncio
        async def test_reregister_action_creates__changelog_has_preregister_event_record(
            self, run_action, action_result, storage,
        ):
            assert_that(
                await alist(storage.change_log.find()),
                contains_inanyorder(
                    has_properties(dict(
                        revision=action_result.revision,
                        operation=OperationKind.ADD_MERCHANT_PREREGISTRATION,
                    )),
                )
            )

        @pytest.mark.asyncio
        async def test_creates_user_role(self, action_result, storage):
            user_role = await storage.user_role.get(
                uid=action_result.uid,
                merchant_id=action_result.merchant_id,
            )
            role = user_role.role
            assert role == MerchantRole.OWNER

        @pytest.mark.asyncio
        async def test_no_services_no_categories_no_online(self, storage, run_action,
                                                           action_params):
            action_params['require_online'] = False
            action_params['services'] = []
            action_params['categories'] = []

            merchant = await run_action(**action_params)
            registration = await storage.merchant_preregistration.get(uid=merchant.uid)

            assert not registration.data.preregister_data.require_online
            assert merchant.acquirer is None

        @pytest.mark.asyncio
        async def test_no_services_no_categories_require_online(
            self, storage, run_action, action_params, payments_settings,
        ):
            action_params['require_online'] = True
            action_params['services'] = []
            action_params['categories'] = []
            merchant = await run_action(**action_params)
            registration = await storage.merchant_preregistration.get(uid=merchant.uid)

            assert merchant.acquirer == AcquirerType(payments_settings.DEFAULT_ACQUIRER)
            assert registration.data.preregister_data.require_online

        @pytest.mark.prametrize('get_can_skip_registration_mock_value', [True, False])
        def test_can_skip_registration(self, get_can_skip_registration_mock, get_can_skip_registration_mock_value,
                                       action_result):
            assert action_result.options.can_skip_registration == get_can_skip_registration_mock_value

        class TestPreregisterActionUsesAcquirerChooser:
            """
            Проверяем, что экшен использует значения на основе MerchantPreregistrationAcquirerChooser
            """

            @pytest.mark.prametrize('get_requires_online_mock_value', [True])
            @pytest.mark.prametrize('get_acquirer_mock_value', [AcquirerType.TINKOFF, AcquirerType.KASSA])
            def test_uses_get_acquirer_method(
                self,
                get_acquirer_mock,
                get_acquirer_mock_value,
                action_result,
            ):
                assert get_acquirer_mock.called
                assert action_result.acquirer == get_acquirer_mock_value

            @pytest.mark.asyncio
            @pytest.mark.prametrize('get_requires_online_mock_value', [True, False])
            async def test_uses_get_requires_online_method(
                self,
                storage,
                get_requires_online_mock,
                get_requires_online_mock_value,
                action_result,
            ):
                registration = await storage.merchant_preregistration.get(uid=action_result.uid)
                assert get_requires_online_mock.called
                assert registration.data.preregister_data.require_online == get_requires_online_mock_value

        @pytest.mark.parametrize('functionality, functionality_action', (
            (
                YandexPayMerchantFunctionalityData(),
                PutYandexPayMerchantFunctionalityAction,
            ),
            (PaymentsFunctionalityData(), PutPaymentsMerchantFunctionalityAction),
        ))
        def test_ensures_functionality(self, mock_put_functionality, action_result, functionality):
            mock_put_functionality.assert_called_once_with(
                merchant=match_equality(has_property('uid', action_result.uid)),
                data=functionality,
            )

        @pytest.mark.asyncio
        async def test_creates_contact_person(
            self, run_action, storage, action_params, contact_person_raw, contact_person,
        ):
            action_params['contact'] = contact_person_raw

            merchant = await run_action(**action_params)

            assert_that(
                merchant.contact,
                equal_to(contact_person),
            )

        @pytest.mark.asyncio
        async def test_create_entity_called(self, mocker, run_action):
            spy = mocker.spy(CreateMerchantEntityAction, 'run')

            await run_action()

            spy.assert_called_once()

    class TestPreregisterActionUpdates:
        @pytest.fixture
        def preregister_merchant(
            self, create_preregister_data, merchant_preregistered_uid, run_action, functionality
        ):
            async def _inner():
                action_params = asdict(await create_preregister_data())
                action_params['uid'] = merchant_preregistered_uid
                action_params['functionality'] = functionality
                return await run_action(**action_params)

            return _inner

        @pytest.fixture
        async def merchant_created(self, preregister_merchant, rands, storage):
            merchant = await preregister_merchant()
            merchant.person_id = rands()
            await storage.merchant.save(merchant)
            return merchant

        @pytest.fixture
        async def merchant_updated(self, preregister_merchant):
            return await preregister_merchant()

        def test_preregister_action_updates__merchant_not_equals(self, merchant_created, merchant_updated):
            assert merchant_created != merchant_updated

        def test_preregister_action_updates__refresh_parent_id(self, merchant_updated):
            assert merchant_updated.person_id is None

        @pytest.mark.asyncio
        async def test_preregister_action_updates__merchant_stored(self,
                                                                   merchant_updated,
                                                                   merchant_preregistered_uid,
                                                                   storage):
            merchant_updated.preregistration = None
            merchant_updated.functionalities = NOT_FETCHED
            assert merchant_updated == await storage.merchant.get(uid=merchant_preregistered_uid)

        @pytest.mark.asyncio
        async def test_preregister_action_updates__changelog_has_preregister_event_record(
            self, run_action, action_result, storage,
        ):
            merchant_updated = await run_action()
            assert_that(
                await alist(storage.change_log.find()),
                has_item(
                    has_properties(dict(
                        revision=merchant_updated.revision,
                        operation=OperationKind.EDIT_MERCHANT_PREREGISTRATION,
                    ))
                )
            )

        @pytest.mark.asyncio
        async def test_preregister_action_updates__ignore_ongoing_moderations(self,
                                                                              merchant_created,
                                                                              action_params,
                                                                              functionality,
                                                                              mocker):
            action = PreregisterMerchantAction(**action_params)
            mock = mocker.patch.object(action, 'ignore_ongoing_moderations')
            merchant = await action.run()
            mock.assert_called_once_with(match_equality(has_property('uid', merchant.uid)), functionality.type)

    class TestCreateMerchantAfterPreregistration:
        @pytest.fixture(autouse=True)
        def map_mock(self, mocker, merchant_data, inn):
            merchant_data.organization.inn = inn
            return mocker.patch.object(
                CreateMerchantAction,
                '_map_merchant_data',
                mocker.Mock(return_value=merchant_data),
            )

        @pytest.fixture(autouse=True)
        def init_client_mock(self, mock_action):
            return mock_action(InitClientAction)

        @pytest.fixture(autouse=True)
        def init_submerchant_mock(self, mock_action):
            return mock_action(InitSubmerchantAction)

        @pytest.fixture
        def merchant_name(self):
            return 'test-create-merchant-name'

        @pytest.fixture
        def merchant_data_params(self):
            return {
                'addresses': 'test-merchant-data-params-address',
                'bank': 'test-merchant-data-params-bank',
                'organization': 'test-merchant-data-params-organization',
                'persons': 'test-merchant-data-params-persons',
                'username': 'test-merchant-data-params-username',
            }

        @pytest.fixture
        def create_merchant_params(
            self, merchant_preregistered_uid, merchant_name, merchant_data_params, functionality,
        ):
            return {
                'uid': merchant_preregistered_uid,
                'name': merchant_name,
                'functionality': functionality,
                **merchant_data_params
            }

        @pytest.fixture
        def create_merchant_func(self, create_merchant_params):
            async def _inner():
                return await CreateMerchantAction(**create_merchant_params).run()

            return _inner

        @pytest.fixture
        async def create_merchant_returned(self, create_merchant_func):
            return await create_merchant_func()

        @pytest.fixture
        async def created_merchant(self, storage, action_result):
            return await GetMerchantAction(action_result.uid, skip_moderation=True, skip_preregistration=True).run()

        def test_returned(self, action_result, create_merchant_returned, preregister_data, merchant_preregistered_uid,
                          merchant_name,
                          merchant_data):
            assert_that(
                create_merchant_returned,
                has_properties({
                    'uid': merchant_preregistered_uid,
                    'name': merchant_name,
                    'data': merchant_data,
                })
            )

        def test_created(self, action_result, create_merchant_returned, created_merchant):
            assert created_merchant == create_merchant_returned

    class TestPreregisterActionPrefillMerchantFromBalance:
        @pytest.fixture
        def balance_person_other_inn(self, balance_person_factory, rands):
            return balance_person_factory(
                suffix='kind__other_inn',
                inn=rands(),
                date=datetime(2020, 11, 18)
            )

        @pytest.fixture
        def balance_person_old(self, balance_person_factory, inn):
            return balance_person_factory(
                suffix='kind__older_than_target',
                inn=inn,
                date=datetime(2020, 11, 16)
            )

        @pytest.fixture
        def balance_person_target(self, balance_person_factory, inn):
            return balance_person_factory(
                suffix='kind__target',
                inn=inn,
                date=datetime(2020, 11, 17)
            )

        @pytest.fixture
        def balance_client_exception(self):
            return None

        @pytest.fixture(autouse=True)
        def balance_find_client_mock(self,
                                     balance_client_mocker,
                                     balance_client,
                                     balance_client_exception):
            with balance_client_mocker('find_client', balance_client, exc=balance_client_exception) as mock:
                yield mock

        @pytest.fixture(autouse=True)
        def balance_get_client_persons_mock(self,
                                            balance_client_mocker,
                                            balance_person_other_inn,
                                            balance_person_old,
                                            balance_person_target):
            persons: Iterable[Person] = [
                balance_person_other_inn,
                balance_person_old,
                balance_person_target,
            ]
            with balance_client_mocker('get_client_persons', persons) as mock:
                yield mock

        def test_balance_find_client_call(self,
                                          action_result,
                                          balance_find_client_mock,
                                          merchant_preregistered_uid,
                                          payments_settings):
            balance_find_client_mock.assert_called_once_with(merchant_preregistered_uid,
                                                             total_timeout=payments_settings.BALANCE_PREFILL_TIMEOUT)

        def test_balance_get_client_persons_call(self,
                                                 action_result,
                                                 balance_get_client_persons_mock,
                                                 client_id,
                                                 payments_settings):
            balance_get_client_persons_mock.assert_called_once_with(
                client_id, total_timeout=payments_settings.BALANCE_PREFILL_TIMEOUT)

        def test_prefill_legal_address(self, action_result, balance_person_target):
            assert_that([address for address in action_result.data.addresses if address.type == 'legal'], contains(
                AddressData(
                    type='legal',
                    city=balance_person_target.legal_address_city,
                    country='RUS',
                    home=balance_person_target.legal_address_home,
                    street=balance_person_target.legal_address_street,
                    zip=balance_person_target.legal_address_postcode
                )
            ))

        def test_prefill_post_address(self, action_result, balance_person_target):
            assert_that([address for address in action_result.data.addresses if address.type == 'post'], contains(
                AddressData(
                    type='post',
                    city=balance_person_target.address_city,
                    country='RUS',
                    home=balance_person_target.address_home,
                    street=balance_person_target.address_street,
                    zip=balance_person_target.address_postcode,
                )
            ))

        def test_prefill_addresses_count(self, action_result):
            assert len(action_result.data.addresses) == 2

        def test_prefill_bank(self, action_result, balance_person_target):
            assert BankData(
                account=balance_person_target.account,
                bik=balance_person_target.bik,
            ) == action_result.data.bank

        def test_prefill_persons(self, action_result, balance_person_target):
            assert_that(action_result.data.persons, contains(
                PersonData(
                    type=PersonType.CEO,
                    name=balance_person_target.fname,
                    email=balance_person_target.email,
                    phone=balance_person_target.phone,
                    surname=balance_person_target.lname,
                    patronymic=balance_person_target.mname,
                )
            ))

        def test_prefill_organization(self, action_result, balance_person_target):
            assert OrganizationData(
                name=balance_person_target.name,
                full_name=balance_person_target.longname,
                inn=balance_person_target.inn,
                kpp=balance_person_target.kpp,
                ogrn=balance_person_target.ogrn,
            ) == action_result.data.organization

        @pytest.mark.asyncio
        async def test_save_prefilled_merchant(self, action_result, storage):
            action_result.preregistration = None
            action_result.functionalities = NOT_FETCHED
            assert action_result == await storage.merchant.get(uid=action_result.uid)

        @pytest.mark.parametrize('balance_client_exception', (BaseBalanceError,))
        def test_prefill_on_balance_exception(self, action_result, balance_person_target):
            assert not balance_person_target.name == action_result.data.organization.name

        @pytest.mark.parametrize('inn', ('012345678901',))  # INN of entrepreneur
        def test_no_prefill_entrepreneur_from_balance(self,
                                                      balance_person_target,
                                                      action_result,
                                                      balance_find_client_mock,
                                                      balance_get_client_persons_mock):
            assert balance_person_target.name != action_result.data.organization.name
            balance_find_client_mock.assert_not_called()
            balance_get_client_persons_mock.assert_not_called()

    class TestPreregisterActionPrefillMerchantFromSpark:
        @pytest.fixture
        def split_fio_exc(self):
            return None

        @pytest.fixture(autouse=True)
        def spark_get_info_mock(self, spark_client_mocker, spark_data):
            with spark_client_mocker('get_info', spark_data) as mock:
                yield mock

        @pytest.fixture(autouse=True)
        def search_wizard_split_fio_mock(self, search_wizard_client_mocker, fio, split_fio_exc):
            with search_wizard_client_mocker('split_fio', result=fio, exc=split_fio_exc) as mock:
                yield mock

        def test_spark_get_info_call(self, spark_get_info_mock, inn, spark_id, action_result):
            spark_get_info_mock.assert_called_once_with(inn, spark_id)

        def test_search_wizard_split_fio_call(self, search_wizard_split_fio_mock, leader_name, action_result):
            search_wizard_split_fio_mock.assert_called_once_with(leader_name)

        def test_prefill_addresses_from_spark(self, spark_data, action_result):
            assert [spark_data.addresses[0].address] == action_result.data.addresses

        def test_prefill_organization_from_spark(self, spark_data, action_result):
            assert spark_data.organization_data.organization == action_result.data.organization

        def test_prefill_persons_from_spark(self, spark_data, action_result, fio):
            assert_that(action_result.data.persons, contains(
                PersonData(
                    type=PersonType.CEO,
                    name=fio.first_name,
                    surname=fio.last_name,
                    patronymic=fio.middle_name,
                    birth_date=date(1964, 2, 11),
                    email='',
                    phone='+7 123 456 78 90',
                )
            ))

        @pytest.mark.parametrize('split_fio_exc', (SparkGetInfoError,))
        def test_prefill_persons_split_fio_exc(self, spark_data, action_result, leader_name):
            assert_that(action_result.data.persons, contains(
                PersonData(
                    type=PersonType.CEO,
                    name='',
                    surname=leader_name,
                    patronymic=None,
                    birth_date=date(1964, 2, 11),
                    email='',
                    phone='+7 123 456 78 90',
                )
            ))

        @pytest.mark.asyncio
        async def test_save_prefilled_merchant_from_spark(self, action_result, storage):
            action_result.preregistration = None
            action_result.functionalities = NOT_FETCHED
            assert action_result == await storage.merchant.get(uid=action_result.uid)

        @pytest.mark.parametrize('organization_is_active', (False,))
        @pytest.mark.asyncio
        async def test_raise_on_inactive_organization_by_spark(self, run_action, action_params):
            with pytest.raises(MerchantInactivePreregisterError):
                await run_action(**action_params)

        class TestExist:
            @pytest.fixture
            async def merchant_preregistered_uid(self, merchant, storage):
                merchant.data.registered = False
                await storage.merchant.save(merchant)
                return merchant.uid

            @pytest.fixture
            def spark_data_addresses_actual_date(self, merchant):
                return merchant.updated + timedelta(days=1)

            @pytest.fixture(autouse=True)
            def balance_get_client_persons_mock(self, inn, balance_client_mocker, balance_person_factory):
                person = balance_person_factory(suffix='balance', inn=inn, date=datetime(2020, 11, 17))
                with balance_client_mocker('get_client_persons', [person]) as mock:
                    yield mock

            @pytest.mark.parametrize('spark_data_addresses_actual_date', [date(2020, 12, 3)])
            def test_not_prefill_addresses_from_spark_exist(self, action_result):
                assert_that(action_result.data.addresses, contains_inanyorder(
                    AddressData(
                        type='legal',
                        city='legal_address_city_balance',
                        country='RUS',
                        street='legal_address_street_balance',
                        zip='legal_address_postcode_balance',
                        home='legal_address_home_balance'
                    ),
                    AddressData(
                        type='post',
                        city='address_city_balance',
                        country='RUS',
                        street='address_street_balance',
                        zip='address_postcode_balance',
                        home='address_home_balance'
                    )
                ))

            def test_prefill_addresses_from_spark_exist(self, spark_data, action_result):
                assert_that(action_result.data.addresses, has_item(spark_data.addresses[0].address))

    class TestPreregisterActionChoiseFreshnessDataForPrefill:
        """
        Test of freshness data in Balance API or SPARK web API. Timeline:
        | 2020-01-01 - spark organization data  [not chosen]
        | 2020-11-17 - balance data             [chosen for organisation data]
        | 2020-12-02 - spark leader data        [chosen]
        | 2020-12-03 - spark address data       [chosen]
        v
        now
        """

        @pytest.fixture
        def balance_person(self, balance_person_factory, inn):
            return balance_person_factory(
                suffix='balance',
                inn=inn,
                date=datetime(2020, 11, 17)
            )

        @pytest.fixture(autouse=True)
        def balance_find_client_mock(self, balance_client_mocker, balance_client):
            with balance_client_mocker('find_client', balance_client) as mock:
                yield mock

        @pytest.fixture(autouse=True)
        def balance_get_client_persons_mock(self, balance_client_mocker, balance_person):
            with balance_client_mocker('get_client_persons', [balance_person]) as mock:
                yield mock

        @pytest.fixture(autouse=True)
        def spark_get_info_mock(self, spark_client_mocker, spark_data):
            with spark_client_mocker('get_info', spark_data) as mock:
                yield mock

        @pytest.fixture(autouse=True)
        def search_wizard_split_fio_mock(self, search_wizard_client_mocker, fio):
            with search_wizard_client_mocker('split_fio', fio) as mock:
                yield mock

        def test_select_freshness_data_for_prefill__organization(self, balance_person, spark_data, action_result):
            assert OrganizationData(
                type=MerchantType.IP,
                # Prefill from balance
                name=balance_person.name,
                full_name=balance_person.longname,
                inn=balance_person.inn,
                kpp=balance_person.kpp,
                ogrn=balance_person.ogrn,
                # Prefill from SPARK
                english_name=spark_data.organization_data.organization.english_name,
            ) == action_result.data.organization

        def test_select_freshness_data_for_prefill__address(self, balance_person, spark_data, action_result):
            assert_that(action_result.data.addresses, contains(
                # Prefill from balance
                AddressData(
                    type='post',
                    city=balance_person.address_city,
                    country='RUS',
                    home=balance_person.address_home,
                    street=balance_person.address_street,
                    zip=balance_person.address_postcode,
                ),
                # Prefill from SPARK
                spark_data.addresses[0].address,
            ))

        def test_select_freshness_data_for_prefill__persons(self, balance_person, spark_data, action_result, fio):
            assert_that(action_result.data.persons, contains(
                PersonData(
                    type=PersonType.CEO,
                    # Prefill from balance
                    email=balance_person.email,
                    # Prefill from SPARK
                    name=fio.first_name,
                    surname=fio.last_name,
                    patronymic=fio.middle_name,
                    birth_date=date(1964, 2, 11),
                    phone='+7 123 456 78 90',
                )
            ))

    class TestINN:
        @pytest.mark.asyncio
        async def test_raises_when_inn_is_empty_and_functionality_is_not_yandex_pay(
            self, run_action, action_params
        ):
            action_params['inn'] = None
            action_params['functionality'] = PaymentsFunctionalityData()

            with pytest.raises(InnIsEmptyError):
                await run_action(**action_params)

        @pytest.mark.asyncio
        async def test_calls_create_yandex_pay_functionality_when_inn_is_empty(
            self, run_action, action_params, mock_action
        ):
            mock = mock_action(PutYandexPayMerchantFunctionalityAction)
            action_params['inn'] = None
            action_params['functionality'] = YandexPayMerchantFunctionalityData()

            await run_action(**action_params)

            mock.assert_called_once()

    class TestPreregisterActionPrefilliBankFromCbrf:
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
                corr=corr,
            )

        @pytest.fixture
        def cbrf_bank_exc(self):
            return None

        @pytest.fixture(autouse=True)
        def balance_find_client_mock(self, balance_client_mocker, balance_client):
            with balance_client_mocker('find_client', balance_client) as mock:
                yield mock

        @pytest.fixture(autouse=True)
        def balance_get_client_persons_mock(self,
                                            balance_client_mocker,
                                            bic,
                                            inn,
                                            person_entity):
            person_entity.bik = bic
            person_entity.inn = inn
            with balance_client_mocker('get_client_persons', [person_entity]) as mock:
                yield mock

        @pytest.fixture(autouse=True)
        def refs_cbrf_bank_mock(self, refs_client_mocker, bank_requisites, cbrf_bank_exc):
            with refs_client_mocker('cbrf_bank', result=bank_requisites, exc=cbrf_bank_exc) as mock:
                yield mock

        def test_refs_cbrf_bank_call(self, action_result, refs_cbrf_bank_mock, bic):
            refs_cbrf_bank_mock.assert_called_once_with(bic)

        def test_cbrf_bank_name(self, action_result, name_full):
            assert name_full == action_result.data.bank.name

        def test_cbrf_bank_corr(self, action_result, corr):
            assert corr == action_result.data.bank.correspondent_account

        @pytest.mark.parametrize('cbrf_bank_exc', (RefsClientNotFoundError,))
        def test_refs_client_exception(self, action_result, name_full):
            assert not name_full == action_result.data.bank.name


class TestGetPreregistrationAction:
    @pytest.fixture
    def action_params(self, merchant_preregistered_uid):
        return dict(uid=merchant_preregistered_uid)

    @pytest.fixture
    def run_action(self, action_params):
        async def _inner(**custom_params):
            return await GetMerchantPreregistrationAction(**(custom_params or action_params)).run()

        return _inner

    @pytest.fixture
    async def action_result(self, run_action):
        return await run_action()

    def test_action_returns_preregistration(self, merchant_preregistration, action_result):
        assert action_result == merchant_preregistration

    @pytest.mark.asyncio
    async def test_raises_not_found_error(self, run_action):
        with pytest.raises(MerchantPreregistrationNotFoundError):
            await run_action(uid=1000000)


class TestMerchantPreregistrationAcquirerChooser:
    @pytest.fixture
    async def services(self, create_service):
        return {
            'online=False, acquirer=None': await create_service(
                options=ServiceOptions(require_online=False, required_acquirer=None)
            ),
            'online=True, acquirer=None': await create_service(
                options=ServiceOptions(require_online=True, required_acquirer=None)
            ),
            'online=True, acquirer=tinkoff': await create_service(
                options=ServiceOptions(require_online=True, required_acquirer=AcquirerType.TINKOFF)
            ),
            'online=True, acquirer=kassa': await create_service(
                options=ServiceOptions(require_online=True, required_acquirer=AcquirerType.KASSA)
            ),
        }

    @pytest.fixture
    async def categories(self, create_category):
        return {
            'acquirer=None': await create_category(required_acquirer=None),
            'acquirer=tinkoff': await create_category(required_acquirer=AcquirerType.TINKOFF),
            'acquirer=kassa': await create_category(required_acquirer=AcquirerType.KASSA),
        }

    @pytest.mark.parametrize('require_online', [False])
    def test_empty(self, require_online, payments_settings):
        chooser = MerchantPreregistrationAcquirerChooser(
            requires_online=require_online,
            services=[],
            categories=[],
        )
        assert chooser.get_requires_online() == require_online
        assert chooser.get_acquirer() is None

    @pytest.mark.parametrize('require_online,choose_services,expected_value', [
        (True, [], True),
        (True, ['online=False, acquirer=None'], True),
        (True, ['online=True, acquirer=None'], True),
        (True, ['online=True, acquirer=tinkoff'], True),
        (False, [], False),
        (False, ['online=False, acquirer=None'], False),
        (False, ['online=True, acquirer=None'], True),
        (False, ['online=True, acquirer=tinkoff'], True),
    ])
    def test_get_requires_online_return_value(
        self, require_online, payments_settings, services, categories, choose_services, expected_value
    ):
        chooser = MerchantPreregistrationAcquirerChooser(
            requires_online=require_online,
            services=[services[index] for index in choose_services],
            categories=[],
        )
        assert chooser.get_requires_online() == expected_value

    @pytest.mark.parametrize('choose_services,choose_categories,expected_value', [
        # chooses default acquirer
        ([], [], None),
        (['online=False, acquirer=None'], [], None),
        ([], ['acquirer=None'], None),
        (['online=False, acquirer=None'], ['acquirer=None'], None),
        # chooses service acquirer
        (['online=True, acquirer=tinkoff'], [], AcquirerType.TINKOFF),
        (['online=True, acquirer=tinkoff'], ['acquirer=None'], AcquirerType.TINKOFF),
        (['online=True, acquirer=kassa'], [], AcquirerType.KASSA),
        (['online=True, acquirer=kassa'], ['acquirer=None'], AcquirerType.KASSA),
        # chooses category acquirer
        ([], ['acquirer=tinkoff'], AcquirerType.TINKOFF),
        (['online=True, acquirer=None'], ['acquirer=tinkoff'], AcquirerType.TINKOFF),
        ([], ['acquirer=kassa'], AcquirerType.KASSA),
        (['online=True, acquirer=None'], ['acquirer=kassa'], AcquirerType.KASSA),
        # common acquirer
        (['online=True, acquirer=kassa'], ['acquirer=kassa'], AcquirerType.KASSA),
        (['online=True, acquirer=tinkoff'], ['acquirer=tinkoff'], AcquirerType.TINKOFF),
    ])
    def test_get_acquirer_return_value(
        self, payments_settings, services, categories, choose_services, choose_categories, expected_value,
    ):
        chooser = MerchantPreregistrationAcquirerChooser(
            requires_online=True,
            services=[services[key] for key in choose_services],
            categories=[categories[key] for key in choose_categories],
        )
        assert chooser.get_acquirer() == (expected_value or AcquirerType(payments_settings.DEFAULT_ACQUIRER))

    @pytest.mark.parametrize('context', [
        pytest.param(dict(
            choose_services=['online=True, acquirer=tinkoff', 'online=True, acquirer=kassa'],
            choose_categories=[],
        ), id='services_conflict'),
        pytest.param(dict(
            choose_services=[],
            choose_categories=['acquirer=tinkoff', 'acquirer=kassa'],
        ), id='categories_conflict'),
        pytest.param(dict(
            choose_services=['online=True, acquirer=tinkoff'],
            choose_categories=['acquirer=kassa'],
        ), id='service_category_conflict'),
    ])
    def test_get_acquirer_raises_conflict_error(self, payments_settings, services, categories, context):
        chosen_services = [services[key] for key in context['choose_services']]
        chosen_categories = [categories[key] for key in context['choose_categories']]
        services_params = [
            {'service_id': s.service_id, 'required_acquirer': s.options.required_acquirer.value}
            for s in chosen_services
        ]
        category_params = [
            {'category_id': c.category_id, 'required_acquirer': c.required_acquirer.value}
            for c in chosen_categories
        ]
        chooser = MerchantPreregistrationAcquirerChooser(
            requires_online=True,
            services=chosen_services,
            categories=chosen_categories,
        )

        with pytest.raises(ConflictingAcquirerType) as exc:
            chooser.get_acquirer()

        assert_that(exc.value, has_properties({
            'params': has_entries({
                'services': contains_inanyorder(*services_params),
                'categories': contains_inanyorder(*category_params),
            })
        }))

    @pytest.mark.parametrize('can_skip_registration1', (True, False))
    @pytest.mark.parametrize('can_skip_registration2', (True, False))
    def test_get_can_skip_registration(self, rands, can_skip_registration1, can_skip_registration2):
        services = [
            Service(name=rands(), options=ServiceOptions(can_skip_registration=can_skip_registration1)),
            Service(name=rands(), options=ServiceOptions(can_skip_registration=can_skip_registration2))
        ]
        chooser = MerchantPreregistrationAcquirerChooser(True, services=services, categories=[])

        assert chooser.get_can_skip_registration() == any([
            service.options.can_skip_registration for service in services
        ])
