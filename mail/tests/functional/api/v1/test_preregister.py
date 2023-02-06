from datetime import date
from uuid import UUID

import pytest
import pytz

from sendr_pytest.matchers import convert_then_match

from hamcrest import (
    assert_that, contains_inanyorder, equal_to, greater_than, has_entries, has_length, has_properties, instance_of,
    match_equality
)

from mail.payments.payments.core.actions.merchant.preregister import PreregisterMerchantAction
from mail.payments.payments.core.entities.enums import AcquirerType, MerchantRole, MerchantType
from mail.payments.payments.core.entities.merchant import AddressData, OrganizationData
from mail.payments.payments.core.entities.merchant_preregistration import PreregisterData
from mail.payments.payments.core.entities.service import ServiceOptions
from mail.payments.payments.core.entities.spark import (
    LeaderData, PhoneData, SparkAddressData, SparkData, SparkOrganizationData
)
from mail.payments.payments.core.entities.userinfo import UserInfo
from mail.payments.payments.core.exceptions import ConflictingAcquirerType
from mail.payments.payments.interactions.search_wizard.entities import Fio


@pytest.fixture
def merchant_uid(merchant_preregistered_uid):
    return merchant_preregistered_uid


class TestGetPreregistration:
    @pytest.fixture
    def make_request(self, client, tvm, merchant_uid):
        async def _inner(uid=merchant_uid):
            return await client.get(f'/v1/merchant/{uid}/preregister')

        return _inner

    def _get_expected_response(self, merchant_preregistration):
        preregister_data = merchant_preregistration.preregister_data
        raw_preregister_data = merchant_preregistration.raw_preregister_data
        expected_response = {
            'data': {
                'preregister_data': {
                    'inn': preregister_data.inn,
                    'require_online': preregister_data.require_online,
                    'categories': preregister_data.categories,
                    'services': preregister_data.services,
                },
                'raw_preregister_data': {
                    'inn': raw_preregister_data.inn,
                    'require_online': raw_preregister_data.require_online,
                    'categories': raw_preregister_data.categories,
                    'services': raw_preregister_data.services,
                }
            }
        }
        return expected_response

    @pytest.mark.asyncio
    async def test_response(self, make_request, merchant_preregistration):
        response = await make_request()
        response_json = await response.json()
        assert response.status == 200
        assert response_json == self._get_expected_response(merchant_preregistration)


class TestPreregisterMerchant:
    @pytest.fixture(autouse=True)
    def blackbox_mock(self, blackbox_client_mocker, merchant_uid, rands):
        user_info = UserInfo(uid=merchant_uid, default_email=rands())
        with blackbox_client_mocker('userinfo', user_info) as mock:
            yield mock

    @pytest.fixture
    def spark_data(self):
        return SparkData(
            spark_id='some_spark_id',
            registration_date=None,
            organization_data=SparkOrganizationData(
                organization=OrganizationData(
                    type=MerchantType.IP,
                    name='spark_name',
                    english_name='spark_english_name',
                    full_name='spark_full_name',
                    inn='spark_inn',
                    kpp='spark_kpp',
                    ogrn='spark_ogrn',
                ),
                actual_date=date(2020, 12, 1),
            ),
            okved_list=[],
            leaders=[
                LeaderData(
                    name='Волож Аркадий Юрьевич',
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
                    actual_date=date(2020, 12, 3),
                ),
            ],
            phones=[
                PhoneData(
                    code='123',
                    number='456 78 90',
                ),
            ],
            active=True,
        )

    @pytest.fixture(autouse=True)
    def spark_get_info_mock(self, spark_client_mocker, spark_data):
        with spark_client_mocker('get_info', spark_data) as mock:
            yield mock

    @pytest.fixture
    def fio(self):
        return Fio(
            first_name='Аркадий',
            middle_name='Юрьевич',
            last_name='Волож',
        )

    @pytest.fixture(autouse=True)
    def search_wizard_split_fio_mock(self, search_wizard_client_mocker, fio):
        with search_wizard_client_mocker('split_fio', fio) as mock:
            yield mock

    @pytest.fixture
    def preregister_request_json(self, preregister_data):
        return dict(
            require_online=preregister_data.require_online,
            services=preregister_data.services,
            inn=preregister_data.inn,
            categories=preregister_data.categories,
        )

    @pytest.fixture
    async def make_preregister_request(self,
                                       client,
                                       tvm,
                                       merchant_uid,
                                       preregister_request_json,
                                       balance_find_client_mock,
                                       spark_get_info_mock,
                                       search_wizard_split_fio_mock):
        async def _inner(uid=merchant_uid, json=None):
            json = json or preregister_request_json
            return await client.post(f'/v1/merchant/{uid}/preregister', json=json)

        return _inner

    @pytest.fixture
    def make_get_merchant_request(self, client, tvm):
        async def _inner(uid):
            return await client.get(f'/v1/merchant/{uid}')

        return _inner

    @pytest.mark.asyncio
    async def test_forbidden(self, make_preregister_request, unique_rand, randn):
        response = await make_preregister_request(uid=unique_rand(randn, basket="uid"))
        assert response.status == 403

    @pytest.mark.asyncio
    async def test_response_is_ok(self, make_preregister_request, preregister_request_json, merchant_uid):
        response = await make_preregister_request(uid=merchant_uid, json=preregister_request_json)
        assert response.status == 200

    @pytest.mark.asyncio
    async def test_creates_merchant(self, make_preregister_request, storage):
        response = await make_preregister_request()
        response_json = await response.json()
        await storage.merchant.get(uid=response_json['data']['uid'])

    @pytest.mark.asyncio
    async def test_response_format(
        self,
        merchant_uid,
        storage,
        preregister_request_json,
        make_preregister_request,
    ):
        response = await make_preregister_request()
        response_json = await response.json()
        merchant = await storage.merchant.get(uid=response_json['data']['uid'])
        registration = await storage.merchant_preregistration.get(uid=merchant_uid)

        response_json_matcher = has_entries({
            'data': has_entries({
                'registration_route': merchant.registration_route.value,
                'username': None,
                'created': merchant.created.astimezone(pytz.UTC).isoformat(),
                'updated': merchant.updated.astimezone(pytz.UTC).isoformat(),
                'persons': {
                    'ceo': {
                        'surname': 'Волож',
                        'patronymic': 'Юрьевич',
                        'phone': '+7 123 456 78 90',
                        'birthDate': '1964-02-11',
                        'email': '',
                        'name': 'Аркадий',
                    },
                },
                'bank': None,
                'options': has_entries({
                    'allow_create_orders_via_ui': instance_of(bool),
                    'allow_create_service_merchants': instance_of(bool),
                    'hide_commission': instance_of(bool),
                }),
                'revision': merchant.revision,
                'organization': {
                    'scheduleText': None,
                    'englishName': 'spark_english_name',
                    'siteUrl': None,
                    'name': 'spark_name',
                    'inn': preregister_request_json['inn'],
                    'ogrn': 'spark_ogrn',
                    'kpp': 'spark_kpp',
                    'fullName': 'spark_full_name',
                    'type': 'ip',
                    'description': None,
                },
                'acquirer': merchant.acquirer.value,
                'name': merchant.name,
                'addresses': {
                    'legal': {
                        'street': 'spark_street',
                        'city': 'spark_city',
                        'zip': 'spark_zip',
                        'country': 'spark_country',
                        'home': 'spark_home',
                    },
                },
                'parent_uid': None,
                'merchant_id': merchant.merchant_id,
                'uid': merchant.uid,
                'status': merchant.status.value,
                'preregister_data': dict(
                    inn=registration.data.preregister_data.inn,
                    services=registration.data.preregister_data.services,
                    categories=registration.data.preregister_data.categories,
                    require_online=registration.data.preregister_data.require_online,
                )
            })
        })
        assert_that(response_json, response_json_matcher)

    @pytest.mark.asyncio
    async def test_created_merchant_preregistration_data(
        self,
        merchant_uid,
        storage,
        preregister_request_json,
        make_preregister_request,
    ):
        await make_preregister_request()
        preregistration = await storage.merchant_preregistration.get(uid=merchant_uid)
        assert_that(preregistration.data, has_properties(dict(
            preregister_data=has_properties(dict(
                services=preregister_request_json['services'],
                categories=preregister_request_json['categories'],
                inn=preregister_request_json['inn'],
                require_online=preregister_request_json['require_online'],
            )),
            raw_preregister_data=has_properties(dict(
                services=preregister_request_json['services'],
                categories=preregister_request_json['categories'],
                inn=preregister_request_json['inn'],
                require_online=preregister_request_json['require_online'],
            ))
        )))

    class TestMerchantPreregistrationCreatedAlways:
        @pytest.fixture(autouse=True)
        def ensure_merchant_creation_fails(self, mocker):
            mocker.patch.object(
                PreregisterMerchantAction,
                '_create_or_update_preregistered_merchant',
                side_effect=ConflictingAcquirerType(),
            )

        @pytest.mark.asyncio
        async def test_request_is_bad(self, make_preregister_request):
            response = await make_preregister_request()
            assert response.status == 400

        @pytest.mark.asyncio
        async def test_merchant_preregistration_exists_on_merchant_creation_error(
            self,
            merchant_uid,
            storage,
            create_service,
            make_preregister_request,
            preregister_request_json,
        ):
            await make_preregister_request()
            preregistration = await storage.merchant_preregistration.get(uid=merchant_uid)
            expected_raw_preregister_data = PreregisterData(
                inn=preregister_request_json['inn'],
                services=preregister_request_json['services'],
                categories=preregister_request_json['categories'],
                require_online=preregister_request_json['require_online'],
            )
            assert preregistration.data.raw_preregister_data == expected_raw_preregister_data

    @pytest.mark.asyncio
    async def test_creates_user_role_of_owner_type(self, make_preregister_request, storage):
        response = await make_preregister_request()
        response_json = await response.json()
        user_role = await storage.user_role.get(
            uid=response_json['data']['uid'],
            merchant_id=response_json['data']['merchant_id'],
        )
        assert user_role.role == MerchantRole.OWNER

    @pytest.mark.asyncio
    async def test_can_make_request_twice(self, make_preregister_request):
        for _ in range(2):
            response = await make_preregister_request()
            assert response.status == 200

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('balance_person_mock')
    async def test_get_merchant_handler_for_preregistered_merchant_response(
        self, make_preregister_request, preregister_request_json, merchant_uid, make_get_merchant_request, storage
    ):
        """Обычная ручка на GET Merchant в состоянии отдать что-то для пререгистрированного мерчанта."""
        await make_preregister_request()
        response = await make_get_merchant_request(uid=merchant_uid)
        assert response.status == 200
        response_json = await response.json()
        merchant = await storage.merchant.get(uid=response_json['data']['uid'])
        merchant.load_data()
        registration = await storage.merchant_preregistration.get(uid=merchant_uid)

        assert_that(response_json, has_entries({
            'data': has_entries({
                'registration_route': merchant.registration_route.value,
                'username': None,
                'created': merchant.created.astimezone(pytz.UTC).isoformat(),
                'updated': merchant.updated.astimezone(pytz.UTC).isoformat(),
                'persons': {
                    'ceo': {
                        'surname': 'Волож',
                        'patronymic': 'Юрьевич',
                        'phone': '+7 123 456 78 90',
                        'birthDate': '1964-02-11',
                        'email': '',
                        'name': 'Аркадий',
                    },
                },
                'bank': None,
                'options': has_entries({
                    'allow_create_orders_via_ui': instance_of(bool),
                    'allow_create_service_merchants': instance_of(bool),
                    'hide_commission': instance_of(bool),
                }),
                'revision': merchant.revision,
                'organization': {
                    'scheduleText': None,
                    'englishName': 'spark_english_name',
                    'siteUrl': None,
                    'name': 'spark_name',
                    'inn': preregister_request_json['inn'],
                    'ogrn': 'spark_ogrn',
                    'kpp': 'spark_kpp',
                    'fullName': 'spark_full_name',
                    'type': 'ip',
                    'description': None,
                },
                'acquirer': merchant.acquirer.value,
                'name': merchant.name,
                'addresses': {
                    'legal': {
                        'zip': 'spark_zip',
                        'home': 'spark_home',
                        'city': 'spark_city',
                        'country': 'spark_country',
                        'street': 'spark_street',
                    },
                },
                'parent_uid': None,
                'merchant_id': merchant.merchant_id,
                'uid': merchant.uid,
                'registered': merchant.registered,
                'status': merchant.status.value,
                'preregister_data': {
                    'inn': registration.data.preregister_data.inn,
                    'services': registration.data.preregister_data.services,
                    'categories': registration.data.preregister_data.categories,
                    'require_online': registration.data.preregister_data.require_online
                }
            })
        }))

    @pytest.mark.asyncio
    async def test_service_conflicting_acquirer_error_response(
        self, make_preregister_request, preregister_request_json, merchant_uid, create_service, service_options,
    ):
        # можно было просто мокнуть класс, выбирающий эквайера, но так кода тоже немного
        service_options_a = ServiceOptions(require_online=True, required_acquirer=AcquirerType.TINKOFF)
        service_options_b = ServiceOptions(require_online=True, required_acquirer=AcquirerType.KASSA)
        service_a = await create_service(options=service_options_a)
        service_b = await create_service(options=service_options_b)

        preregister_request_json['require_online'] = True
        preregister_request_json['services'] = [service_a.service_id, service_b.service_id]
        preregister_request_json['categories'] = []

        response = await make_preregister_request(uid=merchant_uid, json=preregister_request_json)
        response_json = await response.json()
        assert_that(response_json, has_entries({
            'data': has_entries({
                'message': 'CONFLICTING_ACQUIRER_TYPE',
                'params': has_entries({
                    'services': contains_inanyorder(
                        {
                            'service_id': service_a.service_id,
                            'required_acquirer': service_a.options.required_acquirer.value},
                        {
                            'service_id': service_b.service_id,
                            'required_acquirer': service_b.options.required_acquirer.value},
                    ),
                    'categories': []
                })
            }),
            'code': 400,
            'status': 'fail'
        }))

    @pytest.mark.asyncio
    async def test_category_conflicting_acquirer_error_response(
        self, make_preregister_request, preregister_request_json, merchant_uid, create_category,
    ):
        category_a = await create_category(required_acquirer=AcquirerType.TINKOFF)
        category_b = await create_category(required_acquirer=AcquirerType.KASSA)

        preregister_request_json['require_online'] = True
        preregister_request_json['categories'] = [category_a.category_id, category_b.category_id]
        preregister_request_json['services'] = []

        response = await make_preregister_request(uid=merchant_uid, json=preregister_request_json)
        response_json = await response.json()
        assert_that(response_json, has_entries({
            'data': has_entries({
                'message': 'CONFLICTING_ACQUIRER_TYPE',
                'params': has_entries({
                    'categories': contains_inanyorder(
                        {
                            'category_id': category_a.category_id,
                            'required_acquirer': category_a.required_acquirer.value},
                        {
                            'category_id': category_b.category_id,
                            'required_acquirer': category_b.required_acquirer.value},
                    ),
                    'services': []
                })
            }),
            'code': 400,
            'status': 'fail'
        }))

    class TestPreregisterYandexPayMerchant:
        @pytest.fixture(autouse=True)
        def mock_yandex_pay_admin(self, aioresponses_mocker, payments_settings):
            return aioresponses_mocker.put(
                f'{payments_settings.YANDEX_PAY_ADMIN_URL.rstrip("/")}/api/v1/partner',
                payload={'data': {}}
            )

        @pytest.mark.asyncio
        async def test_preregister_yandex_pay_merchant__returns_functionality(
            self, make_preregister_request, preregister_request_json, merchant_uid,
        ):
            preregister_request_json['functionality'] = {
                'type': 'yandex_pay',
                'partner_type': 'merchant',
                'merchant_desired_gateway': 'sberbank',
                'merchant_gateway_id': 'yandex',
            }

            response = await make_preregister_request(uid=merchant_uid, json=preregister_request_json)
            response_json = await response.json()
            assert_that(
                response_json,
                equal_to({
                    'code': 200,
                    'status': 'success',
                    'data': match_equality(has_entries({
                        'functionalities': {
                            'payments': None,
                            'yandex_pay': {
                                'type': 'yandex_pay',
                                'partner_type': 'merchant',
                                'merchant_desired_gateway': 'sberbank',
                                'merchant_gateway_id': 'yandex',
                                'partner_id': match_equality(
                                    convert_then_match(UUID, match_equality(instance_of(UUID)))
                                ),
                            }
                        }
                    }))
                })
            )

        @pytest.mark.asyncio
        async def test_preregister_yandex_pay_merchant__calls_ya_pay_admin(
            self, make_preregister_request, preregister_request_json, merchant_uid, mock_yandex_pay_admin, spark_data
        ):
            preregister_request_json['functionality'] = {
                'type': 'yandex_pay',
                'partner_type': 'merchant',
                'merchant_desired_gateway': 'sberbank',
                'merchant_gateway_id': 'yandex',
            }
            preregister_request_json['contact'] = {
                'email': 'email@test.ru',
                'phone': '+7 123 456 78 90',
                'name': 'Аркадий',
                'surname': 'Волож',
                'patronymic': 'Юрьевич',
            }

            await make_preregister_request(uid=merchant_uid, json=preregister_request_json)
            mock_yandex_pay_admin.assert_called_once_with(
                json={
                    'type': 'merchant',
                    'merchant_desired_gateway': 'sberbank',
                    'merchant_gateway_id': 'yandex',
                    'partner_id': match_equality(convert_then_match(UUID, match_equality(instance_of(UUID)))),
                    'name': spark_data.organization_data.organization.full_name,
                    'uid': merchant_uid,
                    'contact': {
                        'email': 'email@test.ru',
                        'phone': '+7 123 456 78 90',
                        'name': 'Аркадий',
                        'surname': 'Волож',
                        'patronymic': 'Юрьевич',
                    },
                },
                headers={
                    'X-Request-Id': match_equality(has_length(greater_than(0))),
                    'X-Ya-Service-Ticket': match_equality(has_length(greater_than(0))),
                }
            )
