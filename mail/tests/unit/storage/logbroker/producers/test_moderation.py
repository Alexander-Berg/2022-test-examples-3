import time
from collections import defaultdict
from dataclasses import asdict

import pytest

from mail.payments.payments.conf import settings
from mail.payments.payments.core.entities.document import Document, DocumentType
from mail.payments.payments.core.entities.enums import AcquirerType, FunctionalityType
from mail.payments.payments.core.entities.moderation import Moderation, ModerationType
from mail.payments.payments.storage.logbroker.producers.moderation import ModerationProducer
from mail.payments.payments.tests.utils import dummy_coro
from mail.payments.payments.utils.helpers import without_none


class TestModerationProducer:
    async def parent_uid(self, parent_merchant):
        return parent_merchant.uid

    @pytest.fixture
    def merchant_documents(self, randn):
        return [
            Document(
                document_type=DocumentType.PROXY,
                path='test-moderation-producer-document-path-0',
                size=randn(),
                name='test-moderation-producer-document',
                moderation_url='test-moderation-producer-document-url-0'
            ),
            Document(
                document_type=DocumentType.PROXY,
                path='test-moderation-producer-document-path-1',
                size=randn(),
                name='test-moderation-producer-document',
                moderation_url='test-moderation-producer-document-url-1'
            ),
            Document(
                document_type=DocumentType.OFFER,
                path='test-moderation-producer-document-path-2',
                size=randn(),
                name='test-moderation-producer-document',
                moderation_url='test-moderation-producer-document-url-2'
            ),
        ]

    @pytest.fixture
    def now(self, randn):
        return randn(min=1000, max=10 ** 6) / 1000

    @pytest.fixture(autouse=True)
    def setup(self, mocker, now, merchant, merchant_documents):
        # Adding fake documents, cause moderation_url is not kept in DB
        merchant.documents = merchant_documents

        # Mocking time
        mocker.patch(
            'mail.payments.payments.storage.logbroker.producers.moderation.time.time',
            mocker.Mock(return_value=now)
        )

    @pytest.fixture
    def moderation_entity(self, randn, merchant):
        return Moderation(
            moderation_id=randn(),
            uid=merchant.uid,
            revision=merchant.revision,
            moderation_type=ModerationType.MERCHANT,
            functionality_type=FunctionalityType.PAYMENTS,
        )

    @pytest.fixture(autouse=True)
    def write_dict_mock(self, mocker):
        coro = dummy_coro()
        yield mocker.patch.object(
            ModerationProducer,
            'write_dict',
            mocker.Mock(return_value=coro)
        )
        coro.close()

    @pytest.fixture
    async def producer(self, loop, lb_factory_mock):
        return ModerationProducer(lb_factory_mock)

    @pytest.fixture
    def moderation_data(self, merchant):
        org_data = asdict(merchant.organization)
        org_data['englishName'] = org_data.pop('english_name')
        org_data['fullName'] = org_data.pop('full_name')
        org_data.pop('schedule_text')
        org_data.pop('site_url')
        org_data.pop('description')

        bank_data = asdict(merchant.bank)
        bank_data.pop('correspondent_account')
        bank_data.pop('name')

        ceo_data = asdict(merchant.ceo)
        ceo_data.pop('birth_date')
        ceo_data.pop('type')

        addresses_data = {
            address.type: asdict(address)
            for address in merchant.addresses
        }
        for address in addresses_data.values():
            address.pop('type')

        documents_data = defaultdict(list)
        for document in merchant.documents:
            documents_data[document.document_type.value].append(document.moderation_url)

        offer_settings = merchant.options.offer_settings
        offer_type = offer_settings.slug if offer_settings.slug else merchant.acquirer.value

        return {
            **org_data,
            'bank': bank_data,
            'ceo': ceo_data,
            **addresses_data,
            **documents_data,
            'offer_type': offer_type
        }

    @pytest.fixture
    async def order_with_items(self, order, items):
        order.items = items
        return order

    @pytest.fixture
    async def order_moderation(self, storage, order):
        moderation = Moderation(uid=order.uid,
                                revision=order.revision,
                                entity_id=order.order_id,
                                moderation_type=ModerationType.ORDER)
        return await storage.moderation.create(moderation)

    @pytest.mark.parametrize('recheck', [True, False])
    @pytest.mark.parametrize('acquirer', (AcquirerType.KASSA, AcquirerType.TINKOFF))
    @pytest.mark.asyncio
    async def test_write_merchant_payments(self,
                                           payments_settings,
                                           merchant,
                                           now,
                                           moderation_entity,
                                           write_dict_mock,
                                           producer,
                                           moderation_data,
                                           recheck,
                                           ):
        await producer.write_merchant(moderation_entity, merchant, recheck)

        write_dict_mock.assert_called_once_with({
            'service': 'pay',
            'type': 'merchants',
            'meta': {
                'id': moderation_entity.moderation_id,
                'client_id': merchant.client_id,
                'merchant_id': merchant.submerchant_id,
                'env': payments_settings.MODERATION_ENV or 'default',
            },
            'data': moderation_data,
            'workflow': 'recheck' if recheck else 'common',
            'unixtime': int(now * 1000),
        })

    @pytest.mark.parametrize('recheck', [True, False])
    @pytest.mark.asyncio
    async def test_write_merchant_yandexpay(self,
                                            payments_settings,
                                            merchant,
                                            now,
                                            moderation_entity,
                                            write_dict_mock,
                                            producer,
                                            moderation_data,
                                            recheck,
                                            ):
        moderation_entity.functionality_type = FunctionalityType.YANDEX_PAY
        moderation_data['offer_type'] = FunctionalityType.YANDEX_PAY.value

        await producer.write_merchant(moderation_entity, merchant, recheck)

        write_dict_mock.assert_called_once_with({
            'service': 'pay',
            'type': 'merchants',
            'meta': {
                'id': moderation_entity.moderation_id,
                'env': payments_settings.MODERATION_ENV or 'default',
            },
            'data': moderation_data,
            'workflow': 'recheck' if recheck else 'common',
            'unixtime': int(now * 1000),
        })

    @pytest.mark.asyncio
    async def test_write_subscription(self, payments_settings, merchant, now, moderation_entity, write_dict_mock,
                                      producer, subscription):
        await producer.write_subscription(moderation_entity, merchant, subscription)

        write_dict_mock.assert_called_once_with({
            'service': 'pay',
            'type': 'subscription',
            'meta': {
                'id': moderation_entity.moderation_id,
                'client_id': merchant.client_id,
                'subscription_id': subscription.subscription_id,
                'env': payments_settings.MODERATION_ENV or 'default',
            },
            'data': without_none({
                'title': subscription.title,
                'fiscal_title': subscription.fiscal_title,
                'nds': subscription.nds.value,
                'period': subscription.period,
                'trial_period': subscription.trial_period,
                'prices': [
                    {
                        'price': str(price.price),
                        'currency': price.currency,
                        'region_id': price.region_id,
                    }
                    for price in subscription.prices
                ],
            }),
            'unixtime': int(now * 1000),
        })

    @pytest.mark.asyncio
    async def test_order_moderation_data(self, mocker, order_moderation, order_with_items, merchant, producer):
        """Проверяем правильность создания данных для отправки в модерацию."""

        mocker.patch('time.time', mocker.Mock(return_value=1))

        data = producer._make_order_moderation_data(moderation=order_moderation,
                                                    merchant=merchant,
                                                    order=order_with_items)

        expected = _correct_order_moderation_request_data(moderation=order_moderation,
                                                          merchant=merchant,
                                                          order=order_with_items)
        assert data == expected


def _correct_order_moderation_request_data(moderation, merchant, order):
    return {
        'service': 'pay',
        'type': 'order',
        'meta': {
            'id': moderation.moderation_id,
            'client_id': merchant.client_id,
            'order_id': order.order_id,
            'env': settings.MODERATION_ENV or 'default',
        },
        'data': {
            'products': [
                {
                    'id': item.product_id,
                    'name': item.product.name,
                    'currency': item.product.currency,
                    'price': str(item.price),
                    'amount': item.amount,
                    'nds': item.nds.value,
                    'total_price': item.total_price,
                } for item in order.items
            ],
            'total_price': order.price,
        },
        'unixtime': int(time.time() * 1000),
    }
