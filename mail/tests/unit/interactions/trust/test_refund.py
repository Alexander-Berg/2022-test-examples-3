from decimal import Decimal

import pytest

from hamcrest import assert_that, contains, contains_inanyorder, ends_with, equal_to, has_entries

from mail.payments.payments.core.entities.enums import AcquirerType, ShopType
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.interactions.trust.exceptions import TrustException
from mail.payments.payments.tests.base import BaseAcquirerTest
from mail.payments.payments.tests.utils import dummy_coro
from mail.payments.payments.utils.helpers import without_none


class TestCreate(BaseAcquirerTest):
    @pytest.fixture
    def order_keys(self, uid, randn):
        return {
            'uid': uid,
            'order_id': randn(),
        }

    @pytest.fixture
    def trust_refund_id(self):
        return 'test-create-trust-refund-id'

    @pytest.fixture
    def response_json(self, trust_refund_id):
        return {
            'status': 'success',
            'trust_refund_id': trust_refund_id,
        }

    @pytest.fixture
    async def oauth(self, storage, default_merchant_shops, merchant):
        if merchant.acquirer == AcquirerType.KASSA:
            return await storage.merchant_oauth.get_by_shop_id(
                uid=merchant.uid,
                shop_id=default_merchant_shops[ShopType.PROD].shop_id,
            )
        return None

    @pytest.fixture
    def kwargs(self, oauth, order_keys, uid, randn, merchant, acquirer, default_merchant_shops):
        return {
            'uid': uid,
            'acquirer': acquirer,
            'original_order_id': randn(),
            'customer_uid': randn(),
            'caption': str(randn()),
            'purchase_token': str(randn()),
            'submerchant_id': merchant.get_submerchant_id(),
            'oauth': oauth,
            'items': [
                Item(
                    **order_keys,
                    product_id=randn(),
                    amount=Decimal(str(randn())),
                ),
                Item(
                    **order_keys,
                    product_id=randn(),
                    amount=Decimal(str(randn())),
                )
            ],
            'version': 1,
        }

    @pytest.fixture
    def single_kwargs(self, oauth, uid, randn, merchant, acquirer, default_merchant_shops):
        return {
            'uid': uid,
            'acquirer': acquirer,
            'customer_uid': randn(),
            'caption': str(randn()),
            'purchase_token': str(randn()),
            'submerchant_id': merchant.get_submerchant_id(),
            'oauth': oauth,
            'quantity': randn(),
            'trust_order_id': str(randn()),
        }

    @pytest.fixture
    async def returned(self, trust_client, kwargs):
        return await trust_client.refund_create(**kwargs)

    @pytest.fixture
    async def returned_single(self, trust_client, single_kwargs):
        return await trust_client.refund_create_single(**single_kwargs)

    def test_returns_trust_refund_id(self, trust_refund_id, returned):
        assert returned == trust_refund_id

    @pytest.mark.asyncio
    async def test_raises_trust_exception(self, response_json, trust_client, kwargs):
        response_json['status'] = 'not success'
        with pytest.raises(TrustException):
            await trust_client.refund_create(**kwargs)

    def test_request_json(self, acquirer, kwargs, returned, trust_call):
        assert_that(
            trust_call,
            has_entries({
                'json': has_entries({
                    'orders': contains_inanyorder(*[
                        has_entries({
                            'order_id': '.'.join(map(str, [
                                kwargs['uid'],
                                kwargs['original_order_id'],
                                item.product_id,
                                kwargs['customer_uid'],
                            ])),
                            'delta_qty': str(item.amount),
                        })
                        for item in kwargs['items']
                    ]),
                    'pass_params': without_none({
                        'oplata_yakassa_data': {
                            'merchant_oauth': kwargs['oauth'].decrypted_access_token
                        } if acquirer == AcquirerType.KASSA else None,
                        'submerchantIdRbs': kwargs['submerchant_id'] if acquirer == AcquirerType.TINKOFF else None
                    }),
                    'purchase_token': kwargs['purchase_token'],
                    'reason_desc': kwargs['caption'],
                }),
                'headers': has_entries({
                    'X-UID': str(kwargs['customer_uid']),
                }),
                'uid': kwargs['uid'],
                'acquirer': kwargs['acquirer'],
            })
        )

    def test_request_single_json(self, acquirer, single_kwargs, returned_single, trust_call):
        assert_that(
            trust_call,
            has_entries({
                'json': has_entries({
                    'orders': contains_inanyorder(*[
                        has_entries({
                            'order_id': item['id'],
                            'delta_qty': str(item['qty']),
                        })
                        for item in [{'id': single_kwargs['trust_order_id'], 'qty': str(single_kwargs['quantity'])}]
                    ]),
                    'pass_params': without_none({
                        'oplata_yakassa_data': {
                            'merchant_oauth': single_kwargs['oauth'].decrypted_access_token
                        } if acquirer == AcquirerType.KASSA else None,
                        'submerchantIdRbs': single_kwargs['submerchant_id']
                        if acquirer == AcquirerType.TINKOFF else None
                    }),
                    'purchase_token': single_kwargs['purchase_token'],
                    'reason_desc': single_kwargs['caption'],
                }),
                'headers': has_entries({
                    'X-UID': str(single_kwargs['customer_uid']),
                }),
                'uid': single_kwargs['uid'],
                'acquirer': single_kwargs['acquirer'],
            })
        )


@pytest.mark.asyncio
async def test_get(trust_client, uid, acquirer, randn):
    refund_id = randn()
    await trust_client.refund_get(uid=uid, acquirer=acquirer, refund_id=refund_id)
    assert_that(
        (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
        contains(
            'GET',
            ends_with(f'/refunds/{refund_id}'),
            has_entries({
                'uid': uid,
                'acquirer': acquirer,
            })
        )
    )


@pytest.mark.asyncio
async def test__start(trust_client, uid, acquirer, randn):
    refund_id = randn()
    await trust_client._refund_start(uid=uid, acquirer=acquirer, refund_id=refund_id)
    assert_that(
        (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
        contains(
            'POST',
            ends_with(f'/refunds/{refund_id}/start'),
            has_entries({
                'uid': uid,
                'acquirer': acquirer,
            })
        )
    )


@pytest.mark.parametrize('refund_start_result', [
    {'status': 'not success or wait_for_notification'},
])
@pytest.mark.asyncio
async def test_start_error(mocker, trust_client, refund_start_result, acquirer):
    mocker.patch.object(
        trust_client,
        '_refund_start',
        mocker.Mock(return_value=dummy_coro(refund_start_result)),
    )

    with pytest.raises(TrustException):
        await trust_client.refund_start(uid=1, acquirer=acquirer, refund_id=123)


@pytest.mark.parametrize('refund_start_result', [
    {'status': 'success'},
    {'status': 'wait_for_notification'},
])
@pytest.mark.asyncio
async def test_start_success(mocker, trust_client, refund_start_result, uid, acquirer, randn):
    _refund_start = mocker.patch.object(
        trust_client,
        '_refund_start',
        mocker.Mock(return_value=dummy_coro(refund_start_result)),
    )
    refund_id = randn()

    assert_that(
        await trust_client.refund_start(uid=uid, acquirer=acquirer, refund_id=refund_id),
        equal_to(refund_id),
    )
    _refund_start.assert_called_once_with(uid=uid, acquirer=acquirer, refund_id=refund_id)
