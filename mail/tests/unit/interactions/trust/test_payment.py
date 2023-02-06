from random import choice

import pytest
import ujson

from sendr_utils import utcnow

from hamcrest import assert_that, contains, ends_with, equal_to, has_entries

from mail.payments.payments.core.entities.enums import AcquirerType, ReceiptType
from mail.payments.payments.core.entities.merchant_oauth import MerchantOAuth
from mail.payments.payments.interactions.trust.entities import PaymentMode
from mail.payments.payments.interactions.trust.exceptions import TrustException
from mail.payments.payments.tests.base import BaseAcquirerTest
from mail.payments.payments.tests.utils import dummy_coro
from mail.payments.payments.utils.helpers import without_none


class TestPayment(BaseAcquirerTest):
    @pytest.fixture
    def purchase_token(self, rands):
        return rands()

    @pytest.fixture
    def payment_completion_action(self, rands):
        return rands()

    @pytest.fixture
    def submerchant_id(self, rands):
        return rands()

    @pytest.fixture
    def oauth(self, rands, randn):
        oauth = MerchantOAuth(uid=rands(), expires=utcnow(), shop_id=randn())
        oauth.decrypted_access_token = oauth.decrypted_refresh_token = rands()
        return oauth

    @pytest.fixture(params=(True, False))
    def paymethod_id(self, rands, request):
        return rands() if request.param else None

    @pytest.fixture(params=(True, False))
    def return_path(self, rands, request):
        return rands() if request.param else None

    @pytest.fixture
    def payment_mode(self):
        return choice(list(PaymentMode))

    @pytest.fixture
    def customer_uid(self, randn):
        return randn()

    @pytest.fixture
    def order_data_data(self, order_data_version):
        return without_none({
            'receipt_type': 'complete',
            'version': order_data_version
        })

    @pytest.mark.asyncio
    async def test__create(self, trust_client, uid, acquirer):
        data = {'key': 'value'}
        headers = {'abc': 'def'}
        await trust_client._payment_create(uid=uid, acquirer=acquirer, payment_data=data, headers=headers)
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'POST',
                ends_with('/payments'),
                has_entries({
                    'json': data,
                    'headers': headers,
                    'uid': uid,
                    'acquirer': acquirer,
                    'params': {'show_trust_payment_id': 1}
                })
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('with_terminal_info', (True, False))
    async def test_get(self, trust_client, purchase_token, uid, acquirer, with_terminal_info):
        await trust_client._payment_get(uid=uid, acquirer=acquirer,
                                        purchase_token=purchase_token,
                                        with_terminal_info=with_terminal_info)

        kwargs = {'uid': uid, 'acquirer': acquirer}
        if with_terminal_info:
            kwargs['params'] = dict(with_terminal_info=1)

        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'GET',
                ends_with(f'/payments/{purchase_token}'),
                has_entries(kwargs)
            )
        )

    @pytest.mark.asyncio
    async def test__start(self, trust_client, purchase_token, uid, acquirer):
        await trust_client._payment_start(uid=uid, acquirer=acquirer, purchase_token=purchase_token)
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'POST',
                ends_with(f'/payments/{purchase_token}/start'),
                has_entries({
                    'uid': uid,
                    'acquirer': acquirer,
                })
            )
        )

    @pytest.mark.asyncio
    async def test__markup(self, trust_client, purchase_token, uid, acquirer):
        data = {'key': 'value'}
        await trust_client._payment_markup(uid=uid, acquirer=acquirer, purchase_token=purchase_token, markup_data=data)
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'POST',
                ends_with(f'/payments/{purchase_token}/markup'),
                has_entries({
                    'uid': uid,
                    'acquirer': acquirer,
                    'json': data,
                })
            )
        )

    @pytest.mark.asyncio
    async def test__unhold(self, trust_client, purchase_token, uid, acquirer):
        await trust_client.payment_unhold(uid=uid, acquirer=acquirer, purchase_token=purchase_token)
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'POST',
                ends_with(f'/payments/{purchase_token}/unhold'),
                has_entries({
                    'uid': uid,
                    'acquirer': acquirer,
                })
            )
        )

    @pytest.mark.asyncio
    async def test_clear(self, trust_client, purchase_token, uid, acquirer):
        await trust_client.payment_clear(uid=uid, acquirer=acquirer, purchase_token=purchase_token)
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'POST',
                ends_with(f'/payments/{purchase_token}/clear'),
                has_entries({
                    'uid': uid,
                    'acquirer': acquirer,
                })
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('order_data_version', [1, 2])
    async def test_resize(self, trust_client, purchase_token, acquirer, order, items, customer_uid):
        await trust_client.payment_resize(
            uid=order.uid,
            acquirer=acquirer,
            purchase_token=purchase_token,
            item=items[0],
            order=order,
            customer_uid=customer_uid
        )
        delimiter = {1: '.', 2: '-'}[order.data.version]
        url = f'/payments/{purchase_token}/orders/' \
              f'{order.uid}{delimiter}' \
              f'{order.order_id}{delimiter}' \
              f'{items[0].product_id}{delimiter}' \
              f'{customer_uid}/resize'

        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'POST',
                ends_with(url),
                has_entries({
                    'uid': order.uid,
                    'acquirer': acquirer,
                    'json': has_entries({
                        'amount': str(items[0].total_price),
                        'qty': str(items[0].amount)
                    })
                })
            )
        )

    @pytest.mark.asyncio
    async def test_get_error(self, mocker, trust_client, purchase_token, uid, acquirer):
        mocker.patch.object(
            trust_client,
            '_payment_get',
            mocker.Mock(return_value=dummy_coro({'status': 'error'})),
        )
        with pytest.raises(TrustException):
            await trust_client.payment_get(uid=uid, acquirer=acquirer, purchase_token=purchase_token)

    @pytest.mark.parametrize('payment_create_result', [
        {'status': 'error'},
        {'status': 'success', 'status_code': 'not payment_created'},
    ])
    @pytest.mark.asyncio
    async def test_create_error(self, mocker, trust_client, payment_create_result, submerchant_id, oauth):
        mocker.patch.object(
            trust_client,
            '_payment_create',
            mocker.Mock(return_value=dummy_coro(payment_create_result)),
        )
        mocker.patch.object(trust_client, '_trust_payment_data', mocker.Mock(return_value={}))
        with pytest.raises(TrustException):
            await trust_client.payment_create(
                uid=None,
                acquirer=None,
                order=None,
                items=None,
                return_url=None,
                template_tag=None,
                user_email=None,
                submerchant_id=submerchant_id,
                oauth=oauth
            )

    @pytest.mark.parametrize(
        'yandexuid,customer_uid,payments_service_slug,login_id,headers,payment_create_result,order_data_data',
        [
            (
                None, None, None, None, {},
                {
                    'status': 'success',
                    'status_code': 'payment_created',
                    'purchase_token': 'xxyy',
                    'trust_payment_id': 'vvzz',
                }, {'receipt_type': 'complete'}
            ),
            (
                11, 22, 'the-slug', 'the-loginid', {'X-YANDEXUID': '11', 'X-UID': '22'},
                {
                    'status': 'success',
                    'status_code': 'payment_created',
                    'purchase_token': 'yyzz',
                    'trust_payment_id': 'zzvv',
                }, {'receipt_type': 'prepaid'}
            ),
        ]
    )
    @pytest.mark.asyncio
    async def test_create_success(self,
                                  payments_settings,
                                  mocker,
                                  trust_client,
                                  yandexuid,
                                  payments_service_slug,
                                  customer_uid,
                                  payment_mode,
                                  paymethod_id,
                                  headers,
                                  payment_create_result,
                                  uid,
                                  randmail,
                                  acquirer,
                                  order,
                                  items,
                                  return_path,
                                  submerchant_id,
                                  oauth,
                                  payment_completion_action,
                                  login_id,
                                  ):
        _payment_create = mocker.patch.object(trust_client, '_payment_create',
                                              mocker.Mock(return_value=dummy_coro(payment_create_result)))
        data = without_none({
            'back_url': trust_client._make_callback_url(merchant_uid=uid, order_id=order.order_id),
            'currency': items[-1].currency,
            'developer_payload': ujson.dumps({"payment_completion_action": payment_completion_action}),
            'lang': 'ru',
            'orders': list(trust_client._trust_payment_orders_data(uid, order, items, customer_uid)),
            'pass_params': without_none({
                'oplata_yakassa_data': {
                    'merchant_oauth': oauth.decrypted_access_token
                } if acquirer == AcquirerType.KASSA else None,
                'submerchantIdRbs': submerchant_id if acquirer == AcquirerType.TINKOFF else None,
            }),
            'afs_params': without_none({
                'paymentsServiceSlug': payments_service_slug,
                'yandexuid': yandexuid,
                'login_id': login_id,
            }),
            'payment_mode': payment_mode.value,
            'payment_timeout': payments_settings.TRUST_PAYMENT_TIMEOUT,
            'paymethod_id': paymethod_id,
            'return_path': return_path,
            'user_email': randmail(),
            'fiscal_expects_delivery': order.data.receipt_type == ReceiptType.PREPAID,
        })

        assert_that(
            await trust_client.payment_create(
                uid=uid,
                acquirer=acquirer,
                order=order,
                items=items,
                return_url=return_path,
                template_tag=None,
                user_email=data['user_email'],
                yandexuid=yandexuid,
                payments_service_slug=payments_service_slug,
                login_id=login_id,
                customer_uid=customer_uid,
                submerchant_id=submerchant_id,
                oauth=oauth,
                payment_mode=payment_mode,
                paymethod_id=paymethod_id,
                payment_completion_action=payment_completion_action,
            ),
            equal_to({
                'purchase_token': payment_create_result['purchase_token'],
                'trust_payment_id': payment_create_result['trust_payment_id']
            }),
        )
        _payment_create.assert_called_once_with(uid=uid, acquirer=acquirer, payment_data=data, headers=headers)

    @pytest.mark.parametrize('payment_start_result', [
        {'status': 'error'},
        {'status': 'success', 'payment_status': 'not started'},
        {'status': 'success', 'payment_status': 'started'},
    ])
    @pytest.mark.asyncio
    async def test_start_error(self, mocker, trust_client, purchase_token, payment_start_result, uid, acquirer):
        mocker.patch.object(
            trust_client,
            '_payment_start',
            mocker.Mock(return_value=dummy_coro(payment_start_result)),
        )
        if payment_start_result.get('payment_status') == 'started':
            with pytest.raises(AssertionError):
                await trust_client.payment_start(
                    uid=uid,
                    acquirer=acquirer,
                    purchase_token=purchase_token,
                    without_url=False,
                )
        else:
            with pytest.raises(TrustException):
                await trust_client.payment_start(
                    uid=uid,
                    acquirer=acquirer,
                    purchase_token=purchase_token,
                    without_url=False,
                )

    @pytest.mark.parametrize('payment_start_result,without_url', [
        (
            {
                'status': 'success',
                'payment_status': 'started',
            },
            True
        ),
        (
            {
                'status': 'success',
                'payment_status': 'started',
                'payment_url': 'http://yandex.ru',
            },
            False
        )
    ])
    @pytest.mark.asyncio
    async def test_start_success(self,
                                 mocker,
                                 trust_client,
                                 purchase_token,
                                 payment_start_result,
                                 acquirer,
                                 without_url,
                                 ):
        mocker.patch.object(
            trust_client,
            '_payment_start',
            mocker.Mock(return_value=dummy_coro(payment_start_result)),
        )
        assert_that(
            await trust_client.payment_start(
                uid=1,
                acquirer=acquirer,
                purchase_token=purchase_token,
                without_url=without_url,
            ),
            equal_to({
                'purchase_token': purchase_token,
                'payment_url': payment_start_result.get('payment_url'),
            }),
        )

    @pytest.mark.asyncio
    async def test_markup_error(self, mocker, trust_client, purchase_token, uid, acquirer, items, order, customer_uid):
        mocker.patch.object(
            trust_client,
            '_payment_markup',
            mocker.Mock(return_value=dummy_coro({'status': 'error'})),
        )
        with pytest.raises(TrustException):
            await trust_client.payment_markup(
                uid=uid,
                acquirer=acquirer,
                purchase_token=purchase_token,
                items=items,
                order=order,
                customer_uid=customer_uid,
            )

    @pytest.mark.asyncio
    async def test_markup_success(
        self, mocker, trust_client, purchase_token, uid, acquirer, items, order, customer_uid
    ):
        mocker.patch.object(
            trust_client,
            '_payment_markup',
            mocker.Mock(return_value=dummy_coro({'status': 'success'})),
        )
        await trust_client.payment_markup(
            uid=uid,
            acquirer=acquirer,
            purchase_token=purchase_token,
            items=items,
            order=order,
            customer_uid=customer_uid,
        )

    @pytest.mark.asyncio
    async def test_deliver(self, trust_client, purchase_token, uid, acquirer, order, items):
        await trust_client.payment_deliver(uid=uid,
                                           acquirer=acquirer,
                                           purchase_token=purchase_token,
                                           order=order,
                                           items=items)
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'POST',
                ends_with(f'/payments/{purchase_token}/deliver'),
                has_entries({
                    'uid': uid,
                    'acquirer': acquirer,
                    'json': {
                        'orders': [
                            {
                                'fiscal_nds': item.product.nds.value,
                                'order_id': trust_client.make_order_id(
                                    uid, order.order_id, item.product_id, order.customer_uid, order.data.version
                                )
                            } for item in items
                        ]
                    }
                })
            )
        )
