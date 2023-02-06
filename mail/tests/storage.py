import random
import uuid
from copy import copy, deepcopy
from datetime import date, timedelta
from decimal import Decimal
from random import choice, choices
from typing import Any, Dict, Optional
from unittest import mock

import pytest

from sendr_utils import alist, without_none

from mail.payments.payments.core.entities.arbitrage import Arbitrage
from mail.payments.payments.core.entities.category import Category
from mail.payments.payments.core.entities.change_log import ChangeLog, OperationKind
from mail.payments.payments.core.entities.customer_subscription import CustomerSubscription
from mail.payments.payments.core.entities.customer_subscription_transaction import CustomerSubscriptionTransaction
from mail.payments.payments.core.entities.document import Document
from mail.payments.payments.core.entities.enums import (
    NDS, AcquirerType, APICallbackSignMethod, DocumentType, FunctionalityType, MerchantOAuthMode, MerchantRole,
    MerchantStatus, MerchantType, OrderKind, PeriodUnit, PersonType, RefundStatus, Role, ShopType, TransactionStatus
)
from mail.payments.payments.core.entities.functionality import Functionalities
from mail.payments.payments.core.entities.image import Image
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.manager import Manager, ManagerRole
from mail.payments.payments.core.entities.merchant import (
    AddressData, APICallbackParams, BankData, Merchant, MerchantData, MerchantOptions, ModerationData, OrganizationData,
    PersonData
)
from mail.payments.payments.core.entities.merchant_oauth import MerchantOAuth
from mail.payments.payments.core.entities.merchant_preregistration import (
    MerchantPreregistration, MerchantPreregistrationData, PreregisterData
)
from mail.payments.payments.core.entities.merchant_user import MerchantUser
from mail.payments.payments.core.entities.moderation import Moderation, ModerationType
from mail.payments.payments.core.entities.order import Order, OrderData
from mail.payments.payments.core.entities.product import Product
from mail.payments.payments.core.entities.serial import Serial
from mail.payments.payments.core.entities.service import Service, ServiceClient, ServiceMerchant, ServiceOptions
from mail.payments.payments.core.entities.shop import Shop, ShopSettings
from mail.payments.payments.core.entities.subscription import Subscription, SubscriptionPrice
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.core.entities.user import User
from mail.payments.payments.core.entities.user_role import UserRole
from mail.payments.payments.interactions.balance.entities import Person
from mail.payments.payments.storage.exceptions import ShopNotFound
from mail.payments.payments.utils.datetime import utcnow


@pytest.fixture
def merchant_uid(randn):
    return randn()


@pytest.fixture
def merchant_draft_uid(randn):
    return randn()


@pytest.fixture
def merchant_preregistered_uid(randn):
    return randn()


@pytest.fixture
def client_id():
    return 'test-merchant-client-id'


@pytest.fixture
def person_id():
    return 'test-merchant-person-id'


@pytest.fixture
def contract_id():
    return 'test-merchant-contract-id'


@pytest.fixture
def merchant_api_callback_url():
    return 'test-merchant-api-callback-url'


@pytest.fixture
def acquirer():
    return AcquirerType.TINKOFF


@pytest.fixture
def order_acquirer():
    return None


@pytest.fixture
def subscription_acquirer():
    return None


@pytest.fixture
def parent_uid():
    return None


@pytest.fixture
def merchant_data():
    return MerchantData(
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
            correspondent_account='333333',
            name='444444',
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
            PersonData(
                type=PersonType.CONTACT,
                name='Name1',
                surname='Surname2',
                patronymic='Patronymic3',
                email='email@ya.ru4',
                birth_date=date(year=1901, month=1, day=2),
                phone='+711_phone5',
            ),
        ],
        organization=OrganizationData(
            type=MerchantType.OOO,
            name='Hoofs & Horns',
            english_name='HH',
            full_name='H & H',
            inn='111_inn',
            kpp='222_kpp',
            ogrn='1234567890123',  # string field but must be castable to integer
            site_url='sell.yandex.ru',
            description='test-merchant-description',
        ),
        username='test-merchant-username',
    )


@pytest.fixture
def merchant_data_draft(randitem, randn):
    return {
        'addresses': [
            {
                'type': 'legal',
                'city': 'test-merchant-post-city',
                'country': 'RUS',
                'home': 'test-merchant-post-home',
                'street': 'test-merchant-post-street',
                'zip': '123456'
            },
        ],
        'bank': {
            'account': 'test-merchant-post-account',
            'name': 'test-merchant-post-name',
            'correspondent_account': f'{randn()}',
            'bik': f'{randn()}'
        },
        'organization': {
            'schedule_text': 'test-merchant-schedule-text',
            'full_name': 'test-merchant-post-full_name',
            'inn': '1234567890',
            'site_url': 'test-merchant-post-site_url',
            'name': 'test-merchant-post-name',
            'description': 'test-merchant-post-description',
            'kpp': '0987654321',
            'english_name': 'english_name',
            'ogrn': f'{randn()}',
            'type': randitem(MerchantType).value
        },
        'persons': [
            {
                'type': 'ceo',
                'name': 'test-merchant-post-ceo-name',
                'email': 'test-merchant-post-ceo-email@mail.ru',
                'phone': 'test-merchant-post-ceo-phone',
                'surname': 'test-merchant-post-ceo-surname',
                'patronymic': 'test-merchant-post-ceo-patronymic',
                'birthDate': '2019-03-14'
            },
            {
                'type': 'signer',
                'name': 'test-merchant-post-signer-name',
                'email': 'test-merchant-post-signer-email@gmail.com',
                'phone': 'test-merchant-post-signer-phone',
                'surname': 'test-merchant-post-signer-surname',
                'patronymic': 'test-merchant-post-signer-patronymic',
                'birthDate': '2019-03-13'
            }
        ],
        'username': 'test-merchant-username'
    }


@pytest.fixture
def merchant_documents():
    return [
        Document(
            document_type=DocumentType.PASSPORT,
            path='test-passport-document-path',
            size=1234,
            name='test-passport-document',
        ),
    ]


@pytest.fixture
def merchant_moderation():
    return None


@pytest.fixture
def dialogs_org_id(rands):
    return rands()


@pytest.fixture
def merchant_options():
    return MerchantOptions(order_offline_abandon_period=42)


@pytest.fixture
def submerchant_id(randn):
    return f'{randn()}'


@pytest.fixture
def merchant_entity(merchant_uid,
                    client_id,
                    person_id,
                    submerchant_id,
                    merchant_api_callback_url,
                    contract_id,
                    parent_uid,
                    acquirer,
                    merchant_data,
                    merchant_documents,
                    merchant_moderation,
                    merchant_options,
                    dialogs_org_id):
    return Merchant(
        uid=merchant_uid,
        trustworthy=False,
        name='Test merchant',
        client_id=client_id,
        person_id=person_id,
        contract_id=contract_id,
        submerchant_id=submerchant_id,
        api_callback_url=merchant_api_callback_url,
        api_callback_params=APICallbackParams(sign_method=APICallbackSignMethod.ASYMMETRIC, secret='123'),
        options=merchant_options,
        parent_uid=parent_uid,
        data=merchant_data,
        documents=merchant_documents,
        acquirer=acquirer,
        token=str(uuid.uuid4()),
        dialogs_org_id=dialogs_org_id,
    )


@pytest.fixture
def create_shops_for_merchant(storage):
    async def _create_shops_for_merchant(uid: int) -> Dict[ShopType, Shop]:
        return {
            ShopType.PROD: await storage.shop.create(
                Shop(uid=uid, shop_type=ShopType.PROD, is_default=True, name='Основной', settings=ShopSettings()),
            ),
            ShopType.TEST: await storage.shop.create(
                Shop(uid=uid, shop_type=ShopType.TEST, is_default=True, name='Тестовый', settings=ShopSettings()),
            ),
        }

    return _create_shops_for_merchant


@pytest.fixture
def empty_moderation_data():
    return ModerationData(approved=False, has_ongoing=False, has_moderation=False)


@pytest.fixture
async def merchant(storage,
                   merchant_uid,
                   parent_uid,
                   acquirer,
                   merchant_moderation,
                   create_merchant_oauth,
                   merchant_entity,
                   merchant_oauth_mode,
                   create_shops_for_merchant,
                   empty_moderation_data,
                   ):
    async with storage.conn.begin():
        parent = None if parent_uid is None else await storage.merchant.get(parent_uid)
        merchant = deepcopy(merchant_entity)
        merchant.parent = parent
        merchant.load_parent()
        merchant = await storage.merchant.create(merchant)
        await storage.serial.create(Serial(uid=merchant_uid))

    shops = await create_shops_for_merchant(merchant.uid)

    merchant.parent = parent
    merchant.moderation = merchant_moderation
    if merchant_moderation:
        merchant.moderations = {
            FunctionalityType.PAYMENTS: merchant_moderation,
            FunctionalityType.YANDEX_PAY: empty_moderation_data,
        }
    merchant.load_parent()
    merchant.load_data()

    if acquirer == AcquirerType.KASSA:
        oauth = [
            await create_merchant_oauth(
                uid=merchant.uid,
                shop_id=shops[ShopType.from_oauth_mode(merchant_oauth_mode)].shop_id,
                mode=merchant_oauth_mode,
            )
        ]
    else:
        oauth = []

    merchant.oauth = oauth
    merchant.functionalities = Functionalities()

    return merchant


@pytest.fixture
def get_default_merchant_shops(storage):
    async def _get_default_merchant_shops(merchant):
        result = {}
        for type_ in list(ShopType):
            try:
                result[type_] = await storage.shop.get_default_for_merchant(uid=merchant.uid, shop_type=type_)
            except ShopNotFound:
                result[type_] = None
        return result

    return _get_default_merchant_shops


@pytest.fixture
async def default_merchant_shops(storage, merchant, get_default_merchant_shops):
    return await get_default_merchant_shops(merchant)


@pytest.fixture
async def merchant_draft(storage,
                         merchant_draft_uid,
                         merchant_data_draft):
    async with storage.conn.begin():
        merchant_draft = Merchant(
            uid=merchant_draft_uid,
            status=MerchantStatus.DRAFT,
            name='Test merchant draft',
            draft_data=merchant_data_draft,
        )
        merchant_draft = await storage.merchant.create(merchant_draft)
        merchant_draft.load_data()
        await storage.serial.create(Serial(uid=merchant_draft_uid))
    return merchant_draft


@pytest.fixture
def create_preregister_data(randn, merchant_uid, create_service, create_category):
    async def _inner():
        return PreregisterData(
            inn=str(randn()),
            require_online=True,
            services=[
                (await create_service()).service_id,
                (await create_service()).service_id,
            ],
            categories=[
                (await create_category()).category_id,
                (await create_category()).category_id,
            ],
        )

    return _inner


@pytest.fixture
async def preregister_data(create_preregister_data):
    return await create_preregister_data()


@pytest.fixture
def create_raw_preregister_data(randn, merchant_uid, create_service, create_category):
    async def _inner():
        return PreregisterData(
            inn=str(randn()),
            require_online=True,
            services=[
                (await create_service()).service_id,
                (await create_service()).service_id,
            ],
            categories=[
                (await create_category()).category_id,
                (await create_category()).category_id,
            ],
        )

    return _inner


@pytest.fixture
async def create_merchant_preregistration_entity(
    merchant_preregistered_uid,
    create_preregister_data,
    create_raw_preregister_data,
):
    async def _inner(fields: Optional[Dict[str, Any]] = None):
        data = dict(
            uid=merchant_preregistered_uid,
            data=MerchantPreregistrationData(
                preregister_data=await create_preregister_data(),
                raw_preregister_data=await create_raw_preregister_data(),
            ),
        )
        data.update(fields or {})
        entity = MerchantPreregistration(**data)
        return entity

    return _inner


@pytest.fixture
async def create_merchant_preregistration(create_merchant_preregistration_entity, storage):
    async def _inner(fields: Optional[Dict[str, Any]] = None):
        registration_entity = await create_merchant_preregistration_entity(fields=fields)
        registration_created = await storage.merchant_preregistration.create(registration_entity)
        return registration_created

    return _inner


@pytest.fixture
async def merchant_preregistration(create_merchant_preregistration):
    return await create_merchant_preregistration()


@pytest.fixture
def create_preregistered_merchant(storage, merchant_preregistration):
    async def _inner():
        async with storage.conn.begin():
            merchant_entity = Merchant(
                token=str(uuid.uuid4()),
                uid=merchant_preregistration.uid,
                data=MerchantData(
                    organization=OrganizationData(
                        inn=merchant_preregistration.data.preregister_data.inn,
                    ),
                ),
            )
            merchant = await storage.merchant.create(merchant_entity)
            merchant.load_data()
            merchant.load_parent()
            await storage.serial.create(Serial(uid=merchant.uid))
        return merchant

    return _inner


@pytest.fixture
async def merchant_preregistered(create_preregistered_merchant):
    return await create_preregistered_merchant()


@pytest.fixture
def create_merchant(storage, create_shops_for_merchant, unique_rand, randn, parent_uid, merchant_entity):
    async def _inner(create_shops: bool = True, **kwargs):
        async with storage.conn.begin():
            parent = None if parent_uid is None else await storage.merchant.get(parent_uid)
            m = deepcopy(merchant_entity)
            m.parent = parent
            m.load_parent()
            m.uid = unique_rand(randn, basket='uid')
            m.merchant_id = str(m.uid)
            for key, value in kwargs.items():
                if hasattr(m.data, key):
                    setattr(m.data, key, value)
                else:
                    setattr(m, key, value)
            m = await storage.merchant.create(m)
            await storage.serial.create(Serial(uid=m.uid))
            if create_shops:
                await create_shops_for_merchant(m.uid)

            if 'updated' in kwargs or 'created' in kwargs:
                m.created = kwargs.get('created', m.created)
                updated = kwargs.get('updated', m.updated)

                utcnow_mock = mock.Mock(return_value=updated)
                with mock.patch('mail.payments.payments.storage.mappers.merchant.merchant.utcnow', utcnow_mock):
                    m = await storage.merchant.save(m)
            return m

    return _inner


@pytest.fixture
async def parent_merchant(storage, acquirer, unique_rand, randn, rands, merchant_data, create_shops_for_merchant):
    async with storage.conn.begin():
        m = Merchant(
            uid=unique_rand(randn, basket='uid'),
            token=str(uuid.uuid4()),
            name='test-parent-name',
            data=merchant_data,
            submerchant_id=rands(),
            acquirer=acquirer,
        )
        m.load_parent()
        m = await storage.merchant.create(m)
        await storage.serial.create(Serial(m.uid))
        await create_shops_for_merchant(m.uid)
        return m


@pytest.fixture
async def merchant_with_parent(storage, merchant, create_shops_for_merchant):
    child = copy(merchant)
    child.uid += 1
    child.merchant_id = str(child.uid)
    child.parent_uid = merchant.uid
    child.client_id = None
    child.person_id = None
    child.submerchant_id = None
    async with storage.conn.begin():
        child = await storage.merchant.create(child)
        await storage.serial.create(Serial(child.uid))
        await create_shops_for_merchant(child.uid)
        child.parent = merchant
        child.load_parent()
        child.functionalities = Functionalities()
        child.load_data()

    return child


@pytest.fixture
def create_product(rands, randdecimal, storage, merchant):
    async def _inner(**kwargs):
        default_kwargs = dict(
            uid=merchant.uid,
            name=rands(),
            price=randdecimal(min=1, max=10),
            nds=choice(list(NDS)),
        )
        default_kwargs.update(kwargs)
        return await storage.product.create(Product(**default_kwargs))

    return _inner


@pytest.fixture
def products_data():
    return [
        {
            'name': 'product 1',
            'price': Decimal('10.01'),
            'nds': NDS.NDS_0,
        },
        {
            'name': 'product 2',
            'price': Decimal('99.97'),
            'nds': NDS.NDS_10,
        }
    ]


@pytest.fixture
async def products(create_product, products_data):
    return [await create_product(**data) for data in products_data]


@pytest.fixture
def service_callback_url():
    return 'http://music.domain'


@pytest.fixture
def service_fee():
    return None


@pytest.fixture
def service_options(service_fee, rands):
    return ServiceOptions(
        service_fee=service_fee,
        icon_url=rands(),
    )


@pytest.fixture
def service_entity(rands, service_options):
    return Service(name=rands(), options=service_options)


@pytest.fixture
def create_service(rands, storage, service_entity):
    async def _inner(**kwargs):
        entity = deepcopy(service_entity)
        entity.name = rands()
        for k, v in kwargs.items():
            setattr(entity, k, v)
        return await storage.service.create(entity)

    return _inner


@pytest.fixture
async def service(create_service):
    """Фикстура сервиса, сохраненного в базе. Имеет смысл только если тесты на маппер сервиса удачны,
    а сюда она попала потому что нужен общий базовый набор фикстур для сущностей.
    """
    return await create_service(order_moderation_enabled=False)


@pytest.fixture
def service_merchant_entity(rands, merchant, service):
    return ServiceMerchant(
        service_id=service.service_id,
        uid=merchant.uid,
        entity_id=rands(),
        description=rands(),
        enabled=True,
    )


@pytest.fixture
def create_service_merchant(rands, storage, service_merchant_entity):
    async def _inner(**kwargs):
        entity = deepcopy(service_merchant_entity)
        entity.entity_id = rands()
        for k, v in kwargs.items():
            setattr(entity, k, v)
        return await storage.service_merchant.create(entity)

    return _inner


@pytest.fixture
async def service_merchant(service, create_service_merchant):
    entity = await create_service_merchant(service_id=service.service_id)
    entity.service = service
    return entity


@pytest.fixture
async def service_client(storage, randn, unique_rand, service, service_callback_url):
    return await storage.service_client.create(ServiceClient(
        service_id=service.service_id,
        tvm_id=unique_rand(randn, basket='tvm_id'),
        api_callback_url=service_callback_url,
    ))


@pytest.fixture
def service_with_related(service, service_client, service_merchant):
    s = copy(service)
    s.service_client = service_client
    s.service_merchant = service_merchant
    return s


@pytest.fixture
def period_amount(payments_settings):
    return payments_settings.CUSTOMER_SUBSCRIPTION_MIN_PERIOD


@pytest.fixture
def period_units():
    return PeriodUnit.SECOND


@pytest.fixture
def subscription_data(period_amount, period_units, merchant_oauth_mode, randn, randdecimal, rands,
                      subscription_acquirer, unique_rand):
    return {
        'title': rands(),
        'fiscal_title': rands(),
        'nds': choice(list(NDS)),
        'period_amount': period_amount,
        'period_units': period_units,
        'trial_period_amount': randn(max=100),
        'trial_period_units': choice(list(PeriodUnit)),
        'merchant_oauth_mode': merchant_oauth_mode,
        'prices': [
            SubscriptionPrice(currency='RUB',
                              price=randdecimal(min=10, max=100),
                              region_id=unique_rand(randn, max=1000000, basket='subscription-region-id'))
            for _ in range(3)
        ],
        'acquirer': subscription_acquirer
    }


@pytest.fixture
def subscription_uid(merchant):
    return merchant.uid


@pytest.fixture
def subscription_entity(subscription_uid, service_client, service_merchant, subscription_data):
    return Subscription(
        uid=subscription_uid,
        service_client_id=service_client.service_client_id,
        service_merchant_id=service_merchant.service_merchant_id,
        **subscription_data,
    )


@pytest.fixture
async def subscription(storage, subscription_data, subscription_entity):
    return await storage.subscription.create(subscription_entity)


@pytest.fixture
def customer_subscription_data(randn, subscription_data, rands):
    return {
        'user_ip': rands(),
        'region_id': random.choice(subscription_data['prices']).region_id,
        'quantity': randn(max=100),
        'enabled': False,
        'time_until': utcnow() + timedelta(days=1),
        'time_finish': None,
    }


@pytest.fixture
def customer_subscription_entity(subscription_uid, subscription, order, service_client, service_merchant,
                                 customer_subscription_data):
    return CustomerSubscription(
        uid=subscription_uid,
        order_id=order.order_id,
        subscription_id=subscription.subscription_id,
        service_client_id=service_client.service_client_id,
        service_merchant_id=service_merchant.service_merchant_id,
        **customer_subscription_data,
    )


@pytest.fixture
async def customer_subscription(storage, subscription, customer_subscription_entity):
    customer_subscription = await storage.customer_subscription.create(customer_subscription_entity)
    customer_subscription.subscription = subscription
    return customer_subscription


@pytest.fixture
def customer_subscription_transaction_data(rands):
    data = {rands(): rands() for _ in range(10)}
    return {
        'payment_status': TransactionStatus.HELD,
        'data': {
            **data,
            'amount': '183.48',
        },
    }


@pytest.fixture
def customer_subscription_transaction_entity(rands, customer_subscription_transaction_data, customer_subscription):
    return CustomerSubscriptionTransaction(
        uid=customer_subscription.uid,
        customer_subscription_id=customer_subscription.customer_subscription_id,
        purchase_token=rands(),
        **customer_subscription_transaction_data,
    )


@pytest.fixture
async def customer_subscription_transaction(storage, subscription, customer_subscription_transaction_entity):
    customer_subscription_transaction, created = \
        await storage.customer_subscription_transaction.create_or_update(customer_subscription_transaction_entity)
    return customer_subscription_transaction


@pytest.fixture
def shop_type():
    return ShopType.PROD


@pytest.fixture
def shop_is_default():
    return False


@pytest.fixture
def shop_entity(merchant, shop_type, shop_is_default) -> Shop:
    return Shop(
        uid=merchant.uid,
        name='Основной' if shop_is_default else 'Не основной',
        is_default=shop_is_default,
        shop_type=shop_type,
        settings=ShopSettings(),
    )


@pytest.fixture
async def shop(storage, shop_entity) -> Shop:
    shop, _ = await storage.shop.get_or_create(shop_entity, lookup_fields=('uid', 'shop_type', 'is_default'))
    return shop


@pytest.fixture
def create_shop(storage):
    async def _create_shop(**kwargs):
        return await storage.shop.create(
            Shop(
                **kwargs,
            )
        )

    return _create_shop


@pytest.fixture
async def default_prod_shop(create_shop, merchant):
    return await create_shop(
        uid=merchant.uid,
        name='Основной',
    )


@pytest.fixture
def order_data_version():
    return None


@pytest.fixture
def order_data_data(order_data_version):
    return without_none({
        'trust_template': 'desktop',
        'version': order_data_version
    })


@pytest.fixture
def order_data__user_email():
    return 'alice@example.com'


@pytest.fixture
def order_data(order_data_data, order_acquirer, order_data__user_email):
    return {
        'data': OrderData(**order_data_data),
        'acquirer': order_acquirer,
        'user_email': order_data__user_email,
    }


@pytest.fixture
def merchant_oauth_mode():
    return MerchantOAuthMode.PROD


@pytest.fixture
def create_order(storage, merchant, shop):
    async def _inner(**kwargs):
        default_kwargs = dict(uid=merchant.uid, shop_id=shop.shop_id)
        default_kwargs.update(kwargs)
        order = await storage.order.create(Order(**default_kwargs))
        return await storage.order.get(
            order.uid,
            order.order_id,
            select_customer_subscription=bool(order.customer_subscription_id)
        )

    return _inner


@pytest.fixture
async def order(storage, merchant, merchant_oauth_mode, order_data, create_order):
    order = await create_order(**order_data)
    return await storage.order.get(order.uid,
                                   order.order_id,
                                   select_customer_subscription=bool(order.customer_subscription_id))


@pytest.fixture
async def multi_order(storage, merchant, shop, order, products, items, order_data):
    data = {}
    data.update(order_data)
    data.update({
        'uid': merchant.uid,
        'kind': OrderKind.MULTI,
        'shop_id': shop.shop_id,
        'pay_status': None,
        'refund_status': None,
        'original_order_id': None
    })

    parent_order = await storage.order.create(Order(**data))

    parent_order.items = []

    for item in items:
        item = copy(item)
        item.order_id = parent_order.order_id
        await storage.item.create(item)
        parent_order.items.append(item)

    order.parent_order_id = parent_order.order_id
    await storage.order.save(order)

    return parent_order


@pytest.fixture
async def order_with_service(storage, order, order_data, merchant, service_client, service_merchant):
    order.service_client_id = service_client.service_client_id
    order.service_merchant_id = service_merchant.service_merchant_id
    await storage.order.save(order)

    return await storage.order.get(
        order.uid,
        order.order_id,
        select_customer_subscription=bool(order.customer_subscription_id)
    )


@pytest.fixture
async def order_with_customer_subscription(storage, order, merchant, order_data, customer_subscription):
    order.customer_subscription_id = customer_subscription.customer_subscription_id
    await storage.order.save(order)

    return await storage.order.get(
        order.uid,
        customer_subscription_id=order.customer_subscription_id,
        with_customer_subscription=True
    )


@pytest.fixture
async def order_with_service_and_customer_subscription(storage, order_with_service, merchant, order_data,
                                                       customer_subscription):
    order_with_service.customer_subscription_id = customer_subscription.customer_subscription_id
    await storage.order.save(order_with_service)
    return await storage.order.get(order_with_service.uid,
                                   customer_subscription_id=order_with_service.customer_subscription_id,
                                   with_customer_subscription=True)


@pytest.fixture
def create_item(storage, randdecimal, create_product):
    async def _inner(order, product=None, amount=None, **kwargs):
        if product is None:
            product = await create_product()
        if amount is None:
            amount = randdecimal(min=1, max=10)
        item = await storage.item.create(Item(
            uid=order.uid,
            product_id=product.product_id,
            order_id=order.order_id,
            amount=amount,
            **kwargs,
        ))
        item.product = product
        return item

    return _inner


@pytest.fixture
def items_amount():
    return 2


@pytest.fixture
def items_data(items_amount):
    data = [
        {
            'product_index': 0,
            'amount': Decimal('10'),
            'markup': {
                'virtual::new_promocode': '20',
                'card': '80.1',
            },
        },
        {
            'product_index': 1,
            'amount': Decimal('3.33'),
            'image_index': 0,
        },
    ]

    return data[0:items_amount]


@pytest.fixture
def create_items(storage, order, products, images_data, items_data):
    async def _inner(order=order):
        order.items = []

        for item_data in items_data:
            product = products[item_data['product_index']]
            image_id = None
            image = None
            if 'image_index' in item_data:
                image = images_data[item_data['image_index']]
                image_id = image.image_id
            markup = item_data.get('markup')

            item = await storage.item.create(Item(
                uid=order.uid,
                order_id=order.order_id,
                product_id=product.product_id,
                amount=item_data['amount'],
                image_id=image_id,
                markup=markup,
            ))

            item.product = product
            item.image = image
            order.items.append(item)

        return order.items

    return _inner


@pytest.fixture
async def items(storage, order, products, images_data, items_data, create_items):
    return await create_items(order)


@pytest.fixture
def create_image(rands, unique_rand, storage, merchant):
    async def _inner(**kwargs):
        default_kwargs = dict(
            uid=merchant.uid,
            url=rands(),
            md5=unique_rand(rands, basket='image-md5'),
            sha256=unique_rand(rands, basket='image-sha256'),
            stored_path=rands(),
        )
        default_kwargs.update(kwargs)
        return await storage.image.create(Image(**default_kwargs))

    return _inner


@pytest.fixture
async def images_data(create_image):
    return (
        await create_image(stored_path='the/path', url='http://url.test'),
    )


@pytest.fixture
async def image(create_image):
    return await create_image()


@pytest.fixture
def refund_data(order_data_data):
    return {}


@pytest.fixture
def create_refund(storage, order, shop, refund_data, items):
    async def _inner(order=order, shop=shop, refund_data=refund_data, items=items):
        params = dict(
            uid=order.uid,
            original_order_id=order.order_id,
            shop_id=order.shop_id,
            kind=OrderKind.REFUND,
            pay_status=None,
            refund_status=RefundStatus.CREATED,
        )
        params.update(refund_data)
        refund = await storage.order.create(Order(**params))
        refund = await storage.order.get(refund.uid, refund.order_id)
        for item in items:
            item = copy(item)
            item.order_id = refund.order_id
            await storage.item.create(item)
        return refund

    return _inner


@pytest.fixture
async def refund(create_refund):
    return await create_refund()


@pytest.fixture
def transaction_data():
    return {}


@pytest.fixture
async def transaction(storage, rands, order, transaction_data):
    if transaction_data is None:
        return None

    data = {
        'uid': order.uid,
        'order_id': order.order_id,
        'trust_purchase_token': rands(),
    }
    data.update(transaction_data)
    return await storage.transaction.create(Transaction(**data))


@pytest.fixture
def moderations_data():
    return []


@pytest.fixture
async def moderations(storage, merchant, parent_uid, moderations_data):
    result = []
    current_merchant = merchant if merchant.parent is None else merchant.parent

    for moderation_data in moderations_data:
        moderation_data = copy(moderation_data)
        moderation_data.setdefault('moderation_type', ModerationType.MERCHANT)
        if moderation_data['moderation_type'] == ModerationType.MERCHANT:
            moderation_data.setdefault('functionality_type', FunctionalityType.PAYMENTS)
        if 'revision' not in moderation_data and 'uid' not in moderation_data:
            current_merchant = await storage.merchant.save(current_merchant)
        moderation_data.setdefault('revision', current_merchant.revision)
        moderation_data.setdefault('uid', current_merchant.uid)
        result.append(await storage.moderation.create(Moderation(**moderation_data)))

    if merchant.parent:
        merchant.parent.revision = current_merchant.revision
    else:
        merchant.revision = current_merchant.revision

    return result


@pytest.fixture
def order_with_refunds_data(order_acquirer):
    return {
        'caption': 'Some test order',
        'description': 'Some description',
        'items': [
            {
                'name': 'item 02',
                'amount': 2,
                'price': 100,
                'nds': 'nds_10_110',
                'currency': 'RUB',
            },
            {
                'name': 'item 01',
                'nds': 'nds_10_110',
                'price': 100.77,
                'amount': 3.33,
                'currency': 'RUB',
            },
            {
                'name': 'item 03',
                'nds': 'nds_10_110',
                'price': 10,
                'amount': 34,
                'currency': 'RUB',
            }
        ],
        'acquirer': order_acquirer
    }


@pytest.fixture
async def order_with_refunds(storage, shop, merchant, order_with_refunds_data):
    order = await storage.order.create(Order(
        uid=merchant.uid,
        shop_id=shop.shop_id,
        **order_with_refunds_data,
    ))
    return order


@pytest.fixture
def existing_refunds_data():
    return [
        {
            'caption': 'Some test order',
            'description': 'Some description',
            'items': [
                {
                    'name': 'item 02',
                    'amount': 1,
                    'price': 100,
                    'nds': 'nds_10_110',
                    'currency': 'RUB',
                },
                {
                    'name': 'item 01',
                    'nds': 'nds_10_110',
                    'price': 100.77,
                    'amount': 1.33,
                    'currency': 'RUB',
                }
            ],
        },
        {
            'caption': 'Some test order',
            'description': 'Some description',
            'items': [
                {
                    'name': 'item 01',
                    'nds': 'nds_10_110',
                    'price': 100.77,
                    'amount': 0.73,
                    'currency': 'RUB',
                }
            ],
        }
    ]


@pytest.fixture
async def existing_refunds(storage, merchant, shop, order_with_refunds, existing_refunds_data, rands):
    # Creating refunds in database
    refunds = []
    for refund_data in existing_refunds_data:
        refund = await storage.order.create(Order(
            uid=order_with_refunds.uid,
            original_order_id=order_with_refunds.order_id,
            caption=refund_data.get('caption', 'test refund order'),
            description=refund_data.get('description', 'test duplicate refund'),
            kind=OrderKind.REFUND,
            pay_status=None,
            refund_status=refund_data.get('refund_status', RefundStatus.CREATED),
            shop_id=order_with_refunds.shop_id,
            trust_refund_id=rands(),
            acquirer=order_with_refunds.acquirer,
        ))
        for item_data in refund_data.get('items', []):
            product, _ = await storage.product.get_or_create(Product(
                uid=merchant.uid,
                name=item_data['name'],
                price=item_data['price'],
                nds=item_data['nds'],
            ))
            item = Item(
                uid=refund.uid,
                order_id=refund.order_id,
                product_id=product.product_id,
                amount=item_data['amount'],
                product=product
            )
            await storage.item.create(item)
        refund_created = await storage.order.get(uid=merchant.uid, order_id=refund.order_id)
        refund_created.shop = shop
        refunds.append(refund_created)
    return refunds


@pytest.fixture
async def manager_admin(storage, randn, unique_rand):
    """Manager with Admin role"""
    manager = Manager(
        uid=unique_rand(randn, basket='uid'),
        domain_login=''.join(choices('Mr. Rrrrr-obotnik', k=10)),
    )
    await storage.manager.create(manager)
    manager_role = ManagerRole(manager_uid=manager.uid, role=Role.ADMIN)
    await storage.manager_role.create(manager_role)

    return manager


@pytest.fixture
async def manager_assessor(storage, randn, unique_rand):
    """Manager with Assessor role"""
    manager = Manager(
        uid=unique_rand(randn, basket='uid'),
        domain_login=''.join(choices('manager_assessor_domain_login', k=10)),
    )
    await storage.manager.create(manager)
    manager_role = ManagerRole(manager_uid=manager.uid, role=Role.ASSESSOR)
    await storage.manager_role.create(manager_role)

    return manager


@pytest.fixture
async def manager_accountant(storage, unique_rand, randn):
    """Manager with Assessor role"""
    manager = Manager(
        uid=unique_rand(randn, basket='uid'),
        domain_login=''.join(choices('manager_accountant_domain_login', k=10)),
    )
    await storage.manager.create(manager)
    manager_role = ManagerRole(manager_uid=manager.uid, role=Role.ACCOUNTANT)
    await storage.manager_role.create(manager_role)

    return manager


@pytest.fixture
def managers(manager_assessor, manager_admin, manager_accountant):
    return {'assessor': manager_assessor, 'admin': manager_admin, 'accountant': manager_accountant}


@pytest.fixture
def manager_plain_roles():
    return [Role.ADMIN]


@pytest.fixture
async def manager(storage, unique_rand, randn, rands, manager_plain_roles):
    manager = Manager(
        uid=unique_rand(randn, basket='uid'),
        domain_login=rands(),
    )
    await storage.manager.create(manager)

    for role in manager_plain_roles:
        manager_role = ManagerRole(manager_uid=manager.uid, role=role)
        await storage.manager_role.create(manager_role)

    return manager


@pytest.fixture
def change_log_number():
    return 5


@pytest.fixture
async def change_logs(storage, unique_rand, randn, change_log_number):
    return [
        await storage.change_log.create(ChangeLog(
            uid=unique_rand(randn, basket='uid'),
            revision=randn(),
            operation=choice(list(OperationKind))
        ))
        for _ in range(change_log_number)
    ]


@pytest.fixture
def get_tasks(storage):
    async def _inner(**kwargs):
        return await alist(storage.task.find(**kwargs))

    return _inner


@pytest.fixture
def get_moderations(storage):
    async def _inner(*args, **kwargs):
        return [moderation async for moderation in storage.moderation.find(*args, **kwargs)]

    return _inner


@pytest.fixture
def created_now():
    return utcnow()


@pytest.fixture
async def user_entity(storage, merchant_uid, created_now):
    return User(uid=merchant_uid, email='test-email@ya.ru', created=created_now, updated=created_now)


@pytest.fixture
async def user(storage, user_entity):
    return await storage.user.create(user_entity)


@pytest.fixture
def create_user_roles(merchant, randn, storage):
    async def _inner(merchant_id):
        user_roles = []
        for role in [MerchantRole.VIEWER, MerchantRole.OPERATOR, MerchantRole.ADMIN]:
            uid = randn()
            user = await storage.user.create(User(uid=uid, email=f'{uid}@ya.ru'))
            user_role = await storage.user_role.create(
                UserRole(uid=user.uid, merchant_id=merchant_id, role=role))
            user_roles.append(user_role)
        return user_roles

    return _inner


@pytest.fixture
async def user_roles(merchant, create_user_roles):
    return await create_user_roles(merchant.merchant_id)


@pytest.fixture
async def user_roles_draft(merchant_draft, create_user_roles):
    return await create_user_roles(merchant_draft.merchant_id)


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
        ogrn='person-ogrn',

        legal_address_city='person-legal_address_city',
        legal_address_home='person-legal_address_home',
        legal_address_postcode='person-legal_address_zip',
        legal_address_street='person-legal_address_street',

        address_city='person-post_address_city',
        address_home='person-post_address_home',
        address_postcode='person-post_address_zip',
        address_street='person-post_address_street',
    )


@pytest.fixture
def create_merchant_oauth_entity(randn, rands):
    def _inner(uid, shop_id, mode):
        merchant_oauth = MerchantOAuth(
            uid=uid,
            mode=mode,
            shop_id=shop_id,
            poll=True,
            data={'account_id': randn()},
            expires=utcnow() + timedelta(days=1),
        )
        merchant_oauth.decrypted_access_token = rands()
        merchant_oauth.decrypted_refresh_token = rands()
        return merchant_oauth

    return _inner


@pytest.fixture
def create_merchant_oauth(storage, create_merchant_oauth_entity, get_default_merchant_shops):
    async def _inner(uid, shop_id=None, mode=MerchantOAuthMode.PROD, **kwargs):
        if shop_id is None:
            merchant = await storage.merchant.get(uid)
            shops = await get_default_merchant_shops(merchant)
            shop_id = shops[ShopType.from_oauth_mode(mode)].shop_id

        try:
            return await storage.merchant_oauth.get_by_shop_id(uid, shop_id)
        except MerchantOAuth.DoesNotExist:
            return await storage.merchant_oauth.create(
                create_merchant_oauth_entity(uid=uid, shop_id=shop_id, mode=mode),
            )

    return _inner


@pytest.fixture
async def merchant_oauth_entity(merchant, create_merchant_oauth_entity, get_default_merchant_shops):
    shops = await get_default_merchant_shops(merchant)
    return create_merchant_oauth_entity(
        uid=merchant.uid,
        shop_id=shops[ShopType.PROD].shop_id,
        mode=MerchantOAuthMode.PROD,
    )


@pytest.fixture
async def merchant_oauth(storage, merchant_oauth_entity):
    return await storage.merchant_oauth.create(merchant_oauth_entity)


@pytest.fixture
def arbitrage_data():
    return {}


@pytest.fixture
async def arbitrage_entity(storage, rands, order, arbitrage_data):
    return Arbitrage(**{
        'uid': order.uid,
        'order_id': order.order_id,
        'chat_id': rands(),
        'consultation_id': rands(),
        'arbiter_chat_id': rands(),
        'escalate_id': rands(),
        **arbitrage_data
    })


@pytest.fixture
async def arbitrage(storage, arbitrage_entity):
    return await storage.arbitrage.create(arbitrage_entity)


@pytest.fixture
async def positive_merchant_moderation(storage, merchant: Merchant):
    for uid in [merchant.uid, merchant.parent_uid]:
        if uid:
            await storage.moderation.create(
                Moderation(
                    uid=uid,
                    revision=merchant.revision,
                    moderation_type=ModerationType.MERCHANT,
                    functionality_type=FunctionalityType.PAYMENTS,
                    approved=True
                )
            )


@pytest.fixture
def create_category(storage, unique_rand, rands):
    async def _create_category(title=None, **kwargs):
        if title is None:
            title = unique_rand(rands, basket='category-title')
        entity = Category(title=title, **kwargs)
        return await storage.category.create(entity)

    return _create_category


@pytest.fixture
def create_acting_user(randn, storage, merchant: Merchant):
    """
    Функция для создания пользователя с ролью в мерчанте, от имени которого будет запускаться экшен,
    чтобы протестировать проверку ролей.
    BaseAction.context.merchant_user должен будет содержать ассоциацию между эти пользователем и мерчантом.
    """

    async def _inner(role: MerchantRole = MerchantRole.VIEWER, uid=None, email='acting_user@example.com'):
        user = await storage.user.create(User(
            uid=randn() if uid is None else uid,
            email=email,
        ))
        user_role = await storage.user_role.create(UserRole(
            uid=user.uid,
            merchant_id=str(merchant.uid),
            role=role,
        ))
        return dict(
            user=user,
            user_role=user_role,
            merchant_user=MerchantUser(
                user_uid=user.uid,
                merchant_id=merchant.merchant_id,
            )
        )

    return _inner
