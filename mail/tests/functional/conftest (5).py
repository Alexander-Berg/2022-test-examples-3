import base64
import json
import uuid
from contextlib import contextmanager
from datetime import date, timedelta
from typing import AsyncGenerator

import pytest  # noqa
from aiohttp.test_utils import TestClient
from aiopg.sa.connection import SAConnection

from sendr_aiopg.engine import EngineClosingAcquireContextManager
from sendr_aiopg.engine.single import CustomEngine
from sendr_tvm.qloud_async_tvm import TicketCheckResult
from sendr_utils import copy_context, utcnow, without_none

from mail.payments.payments.core.actions.base.merchant import BaseMerchantAction
from mail.payments.payments.core.actions.interactions.developer import GetUidByKeyAction
from mail.payments.payments.core.actions.update_transaction import UpdateTransactionAction
from mail.payments.payments.core.entities.enums import (  # noqa
    AcquirerType, FunctionalityType, MerchantOAuthMode, MerchantType, ModerationType, PersonType, ShopType
)
from mail.payments.payments.core.entities.merchant import (
    AddressData, BankData, Merchant, MerchantData, OrganizationData, PersonData
)
from mail.payments.payments.core.entities.merchant_oauth import MerchantOAuth
from mail.payments.payments.core.entities.moderation import Moderation  # noqa
from mail.payments.payments.core.entities.serial import Serial
from mail.payments.payments.core.entities.service import Service, ServiceClient, ServiceMerchant
from mail.payments.payments.interactions.balance.entities import Person
from mail.payments.payments.interactions.spark.exceptions import SparkGetInfoError
from mail.payments.payments.storage.db.tables import metadata
from mail.payments.payments.utils.helpers import temp_setattr

from .entities import *  # noqa


@pytest.fixture
def test_logger():
    import logging

    from sendr_qlog import LoggerContext
    return LoggerContext(logging.getLogger('test_logger'), {})


@pytest.fixture
def db_engine(raw_db_engine: CustomEngine) -> CustomEngine:
    return raw_db_engine


@pytest.fixture
async def db_conn(db_engine) -> AsyncGenerator[SAConnection, None]:
    conn: SAConnection
    conn_ctx: EngineClosingAcquireContextManager = db_engine.acquire()
    async with conn_ctx as conn:
        yield conn


@pytest.fixture(autouse=True)
def payments_settings_setup(payments_settings):
    payments_settings.TVM_CHECK_SERVICE_TICKET = False
    payments_settings.CHECK_MERCHANT_USER = True
    payments_settings.DATABASE['TIMEOUT'] = 30
    payments_settings.DATABASE['CONNECT_TIMEOUT'] = 30


@pytest.fixture
def no_merchant_user_check(payments_settings):
    @contextmanager
    def _inner():
        with temp_setattr(payments_settings, 'CHECK_MERCHANT_USER', False):
            yield

    return _inner


@pytest.fixture
def crypto_mock(mocker):
    @contextmanager
    def dummy_decrypt(value):
        yield json.loads(base64.b64decode(value).decode('utf-8'))

    def dummy_encrypt(url_kind):
        def _inner(**kwargs):
            return base64.b64encode(
                json.dumps({**kwargs, 'url_kind': url_kind, 'version': 1}).encode('utf-8')
            ).decode('utf-8')

        return _inner

    mock = mocker.Mock()
    mock.encrypt_order = dummy_encrypt('order')
    mock.encrypt_payment = dummy_encrypt('payment')
    mock.decrypt_order = dummy_decrypt
    mock.decrypt_payment = dummy_decrypt

    return mock


@pytest.fixture
async def client(aiohttp_client, payments_app) -> TestClient:
    return await aiohttp_client(payments_app)


@pytest.fixture
async def admin_client(aiohttp_client, admin_app) -> TestClient:
    return await aiohttp_client(admin_app)


@pytest.fixture
async def sdk_client(aiohttp_client, sdk_app, mock_action, merchant) -> TestClient:
    mock_action(GetUidByKeyAction, merchant.uid)
    return await aiohttp_client(sdk_app)


@pytest.fixture
async def response_data(response):
    return (await response.json())['data']


@pytest.fixture
def merchant_uid(unique_rand, randn):
    return unique_rand(randn, basket='uid')


@pytest.fixture
def merchant_draft_uid(unique_rand, randn):
    return unique_rand(randn, basket='uid')


@pytest.fixture
def merchant_preregistered_uid(unique_rand, randn):
    return unique_rand(randn, basket='uid')


@pytest.fixture
def merchant_documents():
    return []


@pytest.fixture
def acquirer():
    return AcquirerType.TINKOFF


@pytest.fixture
def merchant_oauth_mode():
    return MerchantOAuthMode.PROD


@pytest.fixture
def merchant_inn():
    return '111222'


@pytest.fixture
def _create_merchant(storage, create_shops_for_merchant, merchant_inn):
    async def _inner(merchant_uid, acquirer, merchant_documents, merchant_oauth_mode=MerchantOAuthMode.PROD):
        async with storage.conn.begin():
            merchant = await storage.merchant.create(Merchant(
                uid=merchant_uid,
                name='Test merchant',
                client_id='test-client-id',
                person_id='test-person-id',
                contract_id='test-contract-id',
                submerchant_id='test-submerchant-id',
                acquirer=acquirer,
                data=MerchantData(
                    addresses=[
                        AddressData(
                            type='legal',
                            city='Moscow',
                            country='RUS',
                            home='16',
                            street='Lva Tolstogo',
                            zip='123456',
                        ),
                    ],
                    bank=BankData(
                        account='111111',
                        bik='222222',
                    ),
                    persons=[
                        PersonData(
                            type=PersonType.CEO,
                            name='Name',
                            surname='Surname',
                            patronymic='Patronymic',
                            email='email@ya.ru',
                            birth_date=date(year=1900, month=1, day=2),
                            phone='+711_phone',
                        ),
                    ],
                    organization=OrganizationData(
                        type=MerchantType.OOO,
                        name='Hoofs & Horns',
                        english_name='HH',
                        full_name='H & H',
                        inn=merchant_inn,
                        kpp='222_kpp',
                        ogrn='1234567890123',
                        schedule_text='merchant-schedule-text',
                        site_url='sell.yandex.ru',
                        description='test-organization-description',
                    ),
                ),
                documents=merchant_documents,
                token=str(uuid.uuid4()),
            ))
            await storage.conn.execute(f'''
                UPDATE {metadata.schema}.merchants
                SET billing = (
                    '{merchant.client_id}',
                    '{merchant.person_id}',
                    '{merchant.contract_id}',
                    '{merchant.client_id}',
                    '{merchant.submerchant_id}'
                )
                WHERE uid = {merchant_uid}
                ;
            ''')
            await storage.serial.create(Serial(uid=merchant_uid))
            shops = await create_shops_for_merchant(uid=merchant_uid)

            if acquirer == AcquirerType.KASSA:
                shop_type = ShopType.from_oauth_mode(merchant_oauth_mode)
                oauth_entity = MerchantOAuth(uid=merchant.uid,
                                             expires=utcnow() + timedelta(days=1),
                                             shop_id=shops[shop_type].shop_id,
                                             mode=merchant_oauth_mode)
                oauth_entity.decrypted_access_token = 'test-access-token'
                oauth_entity.decrypted_refresh_token = 'test-refresh-token'
                oauth = [await storage.merchant_oauth.create(oauth_entity)]
            else:
                oauth = []

        merchant.load_parent()
        merchant.load_data()
        merchant.oauth = oauth

        return merchant

    return _inner


@pytest.fixture
async def merchant(storage, _create_merchant, merchant_uid, acquirer, merchant_documents, merchant_oauth_mode):
    return await _create_merchant(merchant_uid, acquirer, merchant_documents,
                                  merchant_oauth_mode=merchant_oauth_mode)


@pytest.fixture
async def another_merchant(storage, _create_merchant, unique_rand, randn, acquirer, merchant_documents):
    return await _create_merchant(unique_rand(randn, basket='uid'), acquirer, merchant_documents)


@pytest.fixture
def merchant_data_draft():
    return {
        'addresses': {
            'legal': {
                'city': 'test-merchant-post-city',
                'country': 'RUS',
                'home': 'test-merchant-post-home',
                'street': 'test-merchant-post-street',
                'zip': '123456'
            }
        },
        'bank': {
            'account': 'test-merchant-post-account',
            'name': 'test-merchant-post-name',
            'correspondentAccount': '12345678901234567890',
            'bik': '123456789'
        },
        'organization': {
            'scheduleText': 'test-merchant-schedule-text',
            'fullName': 'test-merchant-post-full_name',
            'inn': '1234567890',
            'siteUrl': 'test-merchant-post-site_url',
            'name': 'test-merchant-post-name',
            'description': 'test-merchant-post-description',
            'kpp': '0987654321',
            'englishName': 'english_name',
            'ogrn': '1234567890123',
            'type': 'ooo'
        },
        'persons': {
            'ceo': {
                'name': 'test-merchant-post-ceo-name',
                'email': 'test-merchant-post-ceo-email@mail.ru',
                'phone': 'test-merchant-post-ceo-phone',
                'surname': 'test-merchant-post-ceo-surname',
                'patronymic': 'test-merchant-post-ceo-patronymic',
                'birthDate': '2019-03-14'
            },
            'signer': {
                'name': 'test-merchant-post-signer-name',
                'email': 'test-merchant-post-signer-email@gmail.com',
                'phone': 'test-merchant-post-signer-phone',
                'surname': 'test-merchant-post-signer-surname',
                'patronymic': 'test-merchant-post-signer-patronymic',
                'birthDate': '2019-03-13'
            }
        },
        'username': 'test-merchant-username'
    }


@pytest.fixture
async def merchant_draft(no_merchant_user_check,
                         client,
                         storage,
                         merchant_draft_uid,
                         merchant_data_draft,
                         ):
    merchant_data_draft.update({'name': 'Test merchant draft'})
    with no_merchant_user_check():
        r = await client.post(
            f'/v1/merchant/{merchant_draft_uid}/draft',
            json=merchant_data_draft,
        )
    assert r.status == 200
    return await storage.merchant.get(uid=merchant_draft_uid)


@pytest.fixture
def merchant_preregistration_request_data(randn):
    return {
        'inn': f'{randn()}',
        'services': [],
        'categories': [],
        'require_online': True
    }


@pytest.fixture
def balance_find_client_mock(balance_client_mocker):
    with balance_client_mocker('find_client', None) as mock:
        yield mock


@pytest.fixture
def spark_get_info_mock(spark_client_mocker):
    with spark_client_mocker('get_info', exc=SparkGetInfoError) as mock:
        yield mock


@pytest.fixture
async def merchant_preregistered(
    no_merchant_user_check,
    client,
    storage,
    merchant_preregistered_uid,
    merchant_preregistration_request_data,
    balance_find_client_mock,
    spark_get_info_mock
):
    with no_merchant_user_check():
        r = await client.post(
            f'/v1/merchant/{merchant_preregistered_uid}/preregister',
            json=merchant_preregistration_request_data,
        )
    assert r.status == 200
    return await storage.merchant.get(uid=merchant_preregistered_uid)


@pytest.fixture
async def moderation(storage, merchant):
    await storage.moderation.create(Moderation(
        uid=merchant.uid,
        revision=merchant.revision,
        moderation_type=ModerationType.MERCHANT,
        functionality_type=FunctionalityType.PAYMENTS,
        approved=True,
    ))


@pytest.fixture
def order_data():
    return without_none({
        'caption': 'abcde',
        'description': 'hehe',
        'items': [
            {
                'name': 'item 01',
                'nds': 'nds_10_110',
                'price': 100.77,
                'amount': 3.33,
                'currency': 'RUB',
                'image': {
                    'url': 'http://image.test',
                },
            },
        ],
    })


@pytest.fixture
def order_headers(shop):
    return {
        'X-Shop-Id': str(shop.shop_id),
    }


@pytest.fixture
async def order(no_merchant_user_check, moderation, tvm, client, storage, merchant, order_data, order_headers):
    with no_merchant_user_check():
        r = await client.post(
            f'/v1/order/{merchant.uid}/',
            json=order_data,
            headers=order_headers,
        )
    assert r.status == 200
    order_id = (await r.json())['data']['order_id']
    order = await storage.order.get(uid=merchant.uid, order_id=order_id)
    order.items = [
        item
        async for item in storage.item.get_for_order(uid=merchant.uid, order_id=order_id)
    ]
    return order


@pytest.fixture
def person_entity():
    return Person(
        client_id='person-client_id',
        person_id='person-person_id',

        account='person-account',
        bik='person-bik',
        fname='person-fname',
        lname='person-lname',
        mname='person-mname',
        email='person-email',
        phone='person-phone',

        name='person-name',
        longname='person-longname',
        inn='person-inn',
        kpp='person-kpp',
        ogrn='1234567890123',

        legal_address_city='person-legal_address_city',
        legal_address_home='person-legal_address_home',
        legal_address_postcode='person-legal_address_postcode',
        legal_address_street='person-legal_address_street',

        address_city='person-address_city',
        address_home='person-address_home',
        address_postcode='person-address_postcode',
        address_street='person-address_street',
    )


@pytest.fixture
def stored_person_entity(merchant):
    data = merchant.data
    if data is None:
        return Person(
            client_id=merchant.client_id,
            person_id=merchant.person_id,
        )
    ceo = data.get_person(PersonType.CEO)
    org = data.organization
    legal_address = None if data.addresses is None or len(data.addresses) == 0 else data.addresses[0]
    post_address = None if data.addresses is None or len(data.addresses) == 1 else data.addresses[1]
    return Person(
        client_id=merchant.client_id,
        person_id=merchant.person_id,

        account=None if data.bank is None else data.bank.account,
        bik=None if data.bank is None else data.bank.bik,
        fname=None if ceo is None else ceo.name,
        lname=None if ceo is None else ceo.surname,
        mname=None if ceo is None else ceo.patronymic,
        email=None if ceo is None else ceo.email,
        phone=None if ceo is None else ceo.phone,

        name=None if org is None else org.name,
        longname=None if org is None else org.full_name,
        inn=None if org is None else org.inn,
        kpp=None if org is None else org.kpp,
        ogrn=None if org is None else org.ogrn,

        legal_address_city=None if legal_address is None else legal_address.city,
        legal_address_home=None if legal_address is None else legal_address.home,
        legal_address_postcode=None if legal_address is None else legal_address.zip,
        legal_address_street=None if legal_address is None else legal_address.street,

        address_city=None if post_address is None else post_address.city,
        address_home=None if post_address is None else post_address.home,
        address_postcode=None if post_address is None else post_address.zip,
        address_street=None if post_address is None else post_address.street,
    )


@pytest.fixture
def balance_person_mock(balance_client_mocker, person_entity):
    with balance_client_mocker('get_person', person_entity) as mock:
        yield mock


@pytest.fixture
async def service(storage):
    return await storage.service.create(Service(name='Music'))


@pytest.fixture
async def service_client(storage, unique_rand, randn, service):
    return await storage.service_client.create(ServiceClient(
        service_id=service.service_id,
        tvm_id=unique_rand(randn, basket='tvm_id'),
        api_callback_url='test-service-client-api-callback-url',
    ))


@pytest.fixture
async def service_merchant(storage, merchant, service):
    service_merchant = await storage.service_merchant.create(ServiceMerchant(
        uid=merchant.uid,
        service_id=service.service_id,
        entity_id='some_entity',
        description='some description',
        enabled=True,
    ))
    service_merchant.service = service
    return service_merchant


@pytest.fixture
def tvm_src(service_client):
    """
    TVM ID сервиса, сделавшего обрабатываемый тестовый запрос.
    Мокаем src на результате проверки TVM-заголовков входящего запроса.
    """
    return service_client.tvm_id


@pytest.fixture
def tvm_uid(merchant_uid):
    """
    UID пользователя, сделавшего обрабатываемый тестовый запрос.
    Мокаем default_uid на результате проверки TVM-заголовков входящего запроса.
    """
    return merchant_uid


@pytest.fixture
def tvm(mocker, tvm_src, tvm_uid):
    mocker.patch.object(TicketCheckResult, 'src', tvm_src)
    mocker.patch.object(TicketCheckResult, 'default_uid', tvm_uid)


@pytest.fixture
def payment_url():
    return 'PAYMENT_URL_XXX'


@pytest.fixture
def trust_resp_code():
    return 'test_trust_code'


@pytest.fixture
def trust_payment_id():
    return 'test_payment_id'


@pytest.fixture
def user_email(randmail):
    return randmail()


@pytest.fixture
def user_description(rands):
    return f'my order {rands()}'


@pytest.fixture
async def _order_json(no_merchant_user_check, client, order):
    with no_merchant_user_check():
        r = await client.get(f'/v1/order/{order.uid}/{order.order_id}')
    return await r.json()


@pytest.fixture
def order_hash(_order_json):
    return _order_json['data']['order_hash']


@pytest.fixture
def payment_hash(_order_json):
    return _order_json['data']['payment_hash']


@pytest.fixture
def base_merchant_action_data_mock(mocker):
    async def load_data_mock(self):
        self.merchant.load_data()

    mocker.patch.object(BaseMerchantAction, '_load_data', load_data_mock)


@pytest.fixture
async def tasks(storage):
    return [t async for t in storage.task.find()]


@pytest.fixture
async def run_transaction(rands, test_logger, db_engine, pushers_mock):
    @copy_context
    async def run(transaction):
        UpdateTransactionAction.context.request_id = rands()
        UpdateTransactionAction.context.logger = test_logger
        UpdateTransactionAction.context.db_engine = db_engine
        UpdateTransactionAction.context.pushers = pushers_mock
        return await UpdateTransactionAction(transaction=transaction).run()

    return run
