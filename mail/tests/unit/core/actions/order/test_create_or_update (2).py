from datetime import timedelta
from decimal import Decimal

import pytest

from sendr_utils import alist, utcnow

from hamcrest import (
    all_of, assert_that, contains_inanyorder, has_entries, has_item, has_properties, is_, match_equality, not_,
    not_none
)

from mail.payments.payments.core.actions.interactions.trust import GetTrustCredentialParamsAction
from mail.payments.payments.core.actions.order.create_or_update import (
    CreateOrUpdateOrderAction, CreateOrUpdateOrderServiceMerchantAction
)
from mail.payments.payments.core.actions.order.download_image import DownloadImageAction
from mail.payments.payments.core.actions.shop.get_or_ensure_default import GetOrEnsureDefaultShopAction
from mail.payments.payments.core.entities.enums import (
    NDS, PAYMETHOD_ID_OFFLINE, AcquirerType, MerchantOAuthMode, OrderKind, OrderSource, PaymentsTestCase, PayStatus,
    ReceiptType, ShopType, TaskState, TaskType
)
from mail.payments.payments.core.entities.image import Image
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.log import OrderCreatedLog, OrderUpdatedLog
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.product import Product
from mail.payments.payments.core.entities.service import ServiceOptions
from mail.payments.payments.core.exceptions import (
    BadCommissionError, CommissionDeniedError, OrderAbandonDeadlineError, OrderAbandonProlongationAmountError,
    OrderAlreadyHaveTransactions, OrderInvalidKind, OrderNotFoundError, OrderSourceDeniedError,
    PaymentWithout3dsNotAllowed, PaymethodIdNotFound, RecurrentPaymentModeNotAllowed, ShopNotFoundError
)
from mail.payments.payments.tests.base import BaseAcquirerTest, BaseTestOrderAction, parametrize_shop_type


class BaseTestCreateOrUpdateOrderAction(BaseAcquirerTest, BaseTestOrderAction):
    @pytest.fixture(params=(OrderKind.PAY, OrderKind.MULTI))
    def kind(self, request):
        return request.param

    @pytest.fixture
    def parent_order_id(self):
        return None

    @pytest.fixture
    def items_amounts(self, randdecimal):
        return [randdecimal(max=10) for _ in range(5)]

    @pytest.fixture
    def items_data(self, rands, randdecimal, items_amounts, currency):
        return [
            {
                'name': rands(),
                'price': randdecimal(max=100),
                'amount': amount,
                'currency': currency,
                'nds': NDS.NDS_20,
                'image': None,
            } for amount in items_amounts
        ]

    @pytest.fixture
    def items(self, merchant, items_data, products):
        return [
            Item(
                uid=merchant.uid,
                product_id=product.product_id,
                amount=item_data['amount'],
                product=product,
                image=Image(
                    uid=merchant.uid,
                    url=item_data['image']['url'],
                ) if item_data.get('image') is not None else None,
            )
            for item_data, product in zip(items_data, products)
        ]

    @pytest.fixture
    def currency(self, rands):
        return rands(k=3).upper()

    @pytest.fixture
    async def products(self, storage, merchant, items_data):
        products = []
        for item in items_data:
            products.append(
                await storage.product.create(
                    Product(
                        uid=merchant.uid,
                        name=item['name'],
                        price=item['price'],
                        nds=item['nds'],
                        currency=item['currency'],
                    )
                )
            )
        return products

    @pytest.fixture
    def with_customer_subscription(self):
        return False

    @pytest.fixture
    def select_customer_subscription(self):
        return False

    @pytest.fixture(params=('by_dict', 'by_object'))
    def items_to_action(self, items_data, items, request):
        return {
            'by_dict': items_data,
            'by_object': items,
        }[request.param]

    @pytest.fixture
    def params_extra(self):
        return {}

    @pytest.fixture(params=('uid', 'service_merchant'))
    def mode(self, request):
        return request.param

    @pytest.fixture
    def paymethod_id(self):
        return None

    @pytest.fixture
    def offline_abandon_deadline(self):
        return None

    @pytest.fixture
    def param_shop_id(self):
        return None

    @pytest.fixture
    def param_default_shop_type(self, shop_type):
        """default_shop_type depends on shop_type for easy test parametrization"""
        return shop_type

    @pytest.fixture
    def param_caption(self, rands):
        return rands()

    @pytest.fixture
    def param_pop_key(self):
        return None

    @pytest.fixture
    def fast_moderation(self, randbool):
        return randbool()

    @pytest.fixture
    def params(self,
               kind,
               rands,
               randbool,
               merchant,
               service_client,
               service_merchant,
               mode,
               moderations,
               with_customer_subscription,
               customer_subscription,
               items_to_action,
               parent_order_id,
               test_param,
               paymethod_id,
               offline_abandon_deadline,
               param_shop_id,
               param_default_shop_type,
               randitem,
               params_extra,
               param_caption,
               param_pop_key,
               fast_moderation):
        data = {
            'uid': {'uid': merchant.uid},
            'service_merchant': {
                'service_tvm_id': service_client.tvm_id,
                'service_merchant_id': service_merchant.service_merchant_id
            }
        }
        params = {
            'caption': param_caption,
            'items': items_to_action,
            'autoclear': randbool(),
            'description': rands(),
            'test': test_param,
            'kind': kind,
            'parent_order_id': parent_order_id,
            'paymethod_id': paymethod_id,
            'default_shop_type': param_default_shop_type,
            'shop_id': param_shop_id,
            'offline_abandon_deadline': offline_abandon_deadline,
            'receipt_type': randitem(ReceiptType),
            'fast_moderation': fast_moderation,
            **data[mode],
            **params_extra
        }
        if param_pop_key is not None:
            params.pop(param_pop_key)
        if with_customer_subscription:
            params['customer_subscription_id'] = customer_subscription.customer_subscription_id
        return params

    @pytest.fixture
    def returned_func(self, params):
        async def _inner(**kwargs):
            params.update(kwargs)
            if bool(params.get('service_merchant_id')):
                action = CreateOrUpdateOrderServiceMerchantAction(**params)
            else:
                action = CreateOrUpdateOrderAction(**params)
            return await action.run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    async def created_order(self, storage, with_customer_subscription, select_customer_subscription, returned):
        order = await storage.order.get(
            returned.uid,
            returned.order_id,
            select_customer_subscription=select_customer_subscription,
            with_customer_subscription=with_customer_subscription
        )
        return order

    @pytest.fixture
    async def created_items(self, storage, returned):
        return await alist(storage.item.get_for_order(returned.uid, returned.order_id))

    @parametrize_shop_type
    def test_created_order(self, returned, created_order):
        returned.items = created_order.items
        returned.order_hash = created_order.order_hash
        assert returned == created_order

    @pytest.mark.parametrize('param_caption', ['abc', '', None])
    def test_create_order_captions(self, returned, created_order):
        returned.items = created_order.items
        returned.order_hash = created_order.order_hash
        assert returned == created_order

    @pytest.mark.parametrize('param_pop_key', [None, 'caption'])
    def test_create_order_absent_caption(self, returned, created_order):
        returned.items = created_order.items
        returned.order_hash = created_order.order_hash
        assert returned == created_order

    @pytest.mark.parametrize('param_default_shop_type', [None])
    def test_force_shop_type_prod(self, returned):
        assert returned.shop.shop_type == ShopType.PROD

    def test_created_order_service_client_id(self, service_client, params, created_order):
        service_client_id = service_client.service_client_id if 'service_tvm_id' in params else None
        assert created_order.service_client_id == service_client_id

    def test_created_order_service_merchant_id(self, params, created_order):
        assert created_order.service_merchant_id == params.get('service_merchant_id')

    @pytest.mark.asyncio
    async def test_service_data(self, rands, storage, returned_func):
        service_data = {rands(): rands()}
        order = await returned_func(service_data=service_data)
        created_order = await storage.order.get(order.uid, order.order_id)
        assert service_data == order.data.service_data and service_data == created_order.data.service_data

    def test_pay_status(self, created_order, kind):
        expected = {
            OrderKind.PAY: PayStatus.NEW,
            OrderKind.MULTI: None
        }
        assert created_order.pay_status == expected[kind]

    class BasdeTestGeneric:
        def check_created_items(self, items_data, created_items):
            assert_that(
                [
                    {
                        'name': item.name,
                        'price': item.price,
                        'amount': item.amount,
                        'currency': item.currency,
                        'nds': item.nds,
                        'image': {
                            'url': item.image.url,
                        } if item.image is not None else None,
                    }
                    for item in created_items
                ],
                contains_inanyorder(*items_data)
            )

    def test_returns_with_items(self, returned, created_items):
        assert_that(returned.items, contains_inanyorder(*created_items))

    def test_returns_with_hashes(self, some_hash, returned):
        assert returned.order_hash == some_hash

    def test_reuses_products(self, products, created_items):
        assert_that([item.product for item in created_items], contains_inanyorder(*products))

    @pytest.mark.asyncio
    async def test_send_to_history_task_created(self, storage, created_order):
        task = await (storage.task.find()).__anext__()
        assert all((
            task.action_name == 'send_to_history_order_action',
            task.params['action_kwargs'] == {'uid': created_order.uid, 'order_id': created_order.order_id},
        ))

    @pytest.mark.asyncio
    async def test_deny_create_order_by_source(self, mode, merchant, storage, returned_func):
        merchant.options.allowed_order_sources = [OrderSource.SERVICE if mode == 'uid' else OrderSource.UI]
        await storage.merchant.save(merchant)

        with pytest.raises(OrderSourceDeniedError):
            await returned_func()

    def test_logged(self, returned, merchant, pushers_mock):
        assert_that(
            pushers_mock.log.push.call_args[0][0],
            all_of(
                is_(OrderCreatedLog) or is_(OrderUpdatedLog),
                has_properties(dict(
                    merchant_name=merchant.name,
                    merchant_uid=merchant.uid,
                    merchant_acquirer=returned.get_acquirer(merchant.acquirer),
                    order_id=returned.order_id,
                    kind=None if returned.kind is None else returned.kind.value,
                    status=None if returned.pay_status is None else returned.pay_status.value,
                    price=returned.log_price,
                    items=[item.dump() for item in returned.items],
                    customer_subscription_id=returned.customer_subscription_id,
                    customer_uid=returned.customer_uid,
                    merchant_oauth_mode=(
                        None
                        if returned.merchant_oauth_mode is None
                        else returned.merchant_oauth_mode.value
                    ),
                    sdk_api_created=returned.created_by_source == OrderSource.SDK_API,
                    sdk_api_pay=returned.pay_by_source == OrderSource.SDK_API,
                    created_by_source=returned.created_by_source,
                    pay_by_source=returned.pay_by_source,
                ))
            )
        )

    class TestCommission:
        @pytest.mark.parametrize('mode', ('service_merchant',))
        @pytest.mark.parametrize('params_extra', ({'commission': 300},))
        def test_commission_ok_for_service_merchant(self, service_client, params, created_order):
            assert created_order.commission == 300

        @pytest.mark.parametrize('mode', ('uid',))
        @pytest.mark.asyncio
        async def test_no_commission_for_uid_mode(self, returned_func):
            with pytest.raises(CommissionDeniedError):
                await returned_func(commission=300)

        @pytest.mark.parametrize('mode', ('service_merchant',))
        @pytest.mark.asyncio
        async def test_commission_too_big(self, returned_func):
            with pytest.raises(BadCommissionError):
                await returned_func(commission=11000)

        @pytest.mark.parametrize('mode', ('service_merchant',))
        @pytest.mark.asyncio
        async def test_commission_too_small(self, returned_func):
            with pytest.raises(BadCommissionError):
                await returned_func(commission=10)

        @pytest.mark.parametrize('mode', ('service_merchant',))
        @pytest.mark.parametrize('params_extra', ({'commission': None},))
        def test_default_commission(self, service_client, params, created_order):
            assert created_order.commission == 215

    class TestRecurrentOrder:
        @pytest.fixture(params=(True, False))
        def allow_payment_mode_recurrent(self, request):
            return request.param

        @pytest.fixture
        def service_options(service_fee, rands, allow_payment_mode_recurrent):
            return ServiceOptions(
                service_fee=service_fee,
                icon_url=rands(),
                allow_payment_mode_recurrent=allow_payment_mode_recurrent,
            )

        @pytest.mark.parametrize('allow_payment_mode_recurrent', (False,))
        @pytest.mark.parametrize('mode', ('service_merchant',))
        @pytest.mark.parametrize('params_extra', ({'recurrent': True},))
        @pytest.mark.asyncio
        async def test_recurrent_payment_not_allowed(self, returned_func):
            with pytest.raises(RecurrentPaymentModeNotAllowed):
                await returned_func()

        @pytest.mark.parametrize('param_pop_key', ['paymethod_id'])
        @pytest.mark.parametrize('allow_payment_mode_recurrent', (True,))
        @pytest.mark.parametrize('mode', ('service_merchant',))
        @pytest.mark.parametrize('params_extra', ({'recurrent': True},))
        @pytest.mark.asyncio
        async def test_recurrent_payment_id_not_found(self, returned_func):
            with pytest.raises(PaymethodIdNotFound):
                await returned_func()

        @pytest.mark.parametrize('allow_payment_mode_recurrent', (True,))
        @pytest.mark.parametrize('mode', ('service_merchant',))
        @pytest.mark.parametrize('params_extra', ({'recurrent': True},))
        @pytest.mark.asyncio
        async def test_success_create_recurrent_order(self, returned_func):
            order: Order = await returned_func()
            assert order.data.recurrent is True

    class TestPaymentWithout3ds:
        @pytest.fixture(params=(True, False))
        def allow_payment_without_3ds(self, request):
            return request.param

        @pytest.fixture
        def service_options(service_fee, rands, allow_payment_without_3ds):
            return ServiceOptions(
                service_fee=service_fee,
                icon_url=rands(),
                allow_payment_without_3ds=allow_payment_without_3ds,
            )

        @pytest.mark.parametrize('allow_payment_without_3ds', (False,))
        @pytest.mark.parametrize('mode', ('service_merchant',))
        @pytest.mark.parametrize('params_extra', ({'without_3ds': True},))
        @pytest.mark.asyncio
        async def test_payment_without_3ds_not_allowed(self, returned_func):
            with pytest.raises(PaymentWithout3dsNotAllowed):
                await returned_func()

        @pytest.mark.parametrize('param_pop_key', ['paymethod_id'])
        @pytest.mark.parametrize('allow_payment_without_3ds', (True,))
        @pytest.mark.parametrize('mode', ('service_merchant',))
        @pytest.mark.parametrize('params_extra', ({'without_3ds': True},))
        @pytest.mark.asyncio
        async def test_paymethod_id_not_found(self, returned_func):
            with pytest.raises(PaymethodIdNotFound):
                await returned_func()

        @pytest.mark.parametrize('allow_payment_without_3ds', (True,))
        @pytest.mark.parametrize('mode', ('service_merchant',))
        @pytest.mark.parametrize('params_extra', ({'without_3ds': True},))
        @pytest.mark.asyncio
        async def test_success_create_order_without_3ds(self, returned_func):
            order: Order = await returned_func()
            assert order.data.without_3ds is True

    class TestExcludeStats:
        @pytest.fixture(autouse=True)
        async def setup(self, create_merchant_oauth, merchant_oauth_mode, default_merchant_shops, merchant):
            uid = merchant.uid
            await create_merchant_oauth(
                uid,
                shop_id=default_merchant_shops[ShopType.from_oauth_mode(merchant_oauth_mode)].shop_id,
                mode=merchant_oauth_mode,
            )

        @parametrize_shop_type
        @pytest.mark.parametrize('acquirer', [AcquirerType.KASSA])
        @pytest.mark.parametrize('test_param', (PaymentsTestCase.TEST_OK_HELD, None))
        @pytest.mark.asyncio
        async def test_exclude_stats(self, returned, test_param):
            assert returned.exclude_stats == (returned.shop.shop_type == ShopType.TEST or test_param is not None)

    class TestParentOrderId:
        @pytest.fixture
        def parent_order_id(self, multi_order):
            return multi_order.order_id

        def test_parent_order_id(self, created_order, multi_order):
            assert created_order.parent_order_id == multi_order.order_id

    class BaseTestOldNDSMapping:
        @staticmethod
        def _mapped_nds(nds: NDS) -> NDS:
            return {
                NDS.NDS_18: NDS.NDS_20,
                NDS.NDS_18_118: NDS.NDS_20_120,
            }.get(nds, nds)

        @pytest.fixture
        def items_data(self):
            return [
                {
                    'name': 'item 01',
                    'price': Decimal('12.34'),
                    'amount': Decimal('56.78'),
                    'currency': 'RUB',
                    'nds': NDS.NDS_18,
                },
                {
                    'name': 'item 02',
                    'price': Decimal('12.34'),
                    'amount': Decimal('56.78'),
                    'currency': 'RUB',
                    'nds': NDS.NDS_18_118,
                },
                {
                    'name': 'item 03',
                    'price': Decimal('12.34'),
                    'amount': Decimal('56.78'),
                    'currency': 'RUB',
                    'nds': NDS.NDS_10,
                }
            ]

        def check_created_items(self, items_data, created_items):
            assert_that(
                [(item.name, item.product.nds) for item in created_items],
                contains_inanyorder(*[
                    (item_data['name'], self._mapped_nds(item_data['nds']))
                    for item_data in items_data
                ])
            )

    class BaseTestUpdateCurrency:
        @pytest.fixture(autouse=True)
        def setup(self, payments_settings, merchant):
            payments_settings.INTERACTION_MERCHANT_SETTINGS[merchant.uid] = {'currency_from_trust': True}

        @pytest.fixture
        def enabled_currency(self, rands):
            return rands(k=3).upper()

        @pytest.fixture
        def param_shop_id(self, shop):
            return shop.shop_id

        @pytest.fixture(autouse=True)
        def mock_get_enabled_currency(self, shop_type, trust_client_mocker, enabled_currency):
            with trust_client_mocker(shop_type, 'get_enabled_currency', enabled_currency) as mock:
                yield mock

        @parametrize_shop_type
        def test_mock_get_enabled_currency(self, returned, merchant, mock_get_enabled_currency):
            mock_get_enabled_currency.assert_called_once_with(uid=merchant.uid, acquirer=merchant.acquirer)

        def check_created_items(self, items_data, created_items, enabled_currency):
            assert_that(
                [
                    {
                        'name': item.name,
                        'price': item.price,
                        'amount': item.amount,
                        'currency': enabled_currency,
                        'nds': item.nds,
                        'image': {
                            'url': item.image.url,
                        } if item.image is not None else None,
                    }
                    for item in created_items
                ],
                contains_inanyorder(*[
                    {**item_data, 'currency': enabled_currency} for item_data in items_data
                ])
            )

    class TestPayMethodOffline:
        @pytest.fixture
        def kind(self):
            return OrderKind.PAY

        @pytest.fixture
        def now(self, mocker):
            now = utcnow()
            mocker.patch(
                'mail.payments.payments.core.actions.order.create_or_update.utcnow',
                mocker.Mock(return_value=now)
            )
            return now

        @pytest.fixture
        async def paymethod_id(self):
            return PAYMETHOD_ID_OFFLINE

        def test_paymethod_id(self, returned):
            assert returned.paymethod_id == PAYMETHOD_ID_OFFLINE

        def test_deadline(self, now, returned, merchant):
            deadline = now + timedelta(seconds=merchant.options.order_offline_abandon_period)
            assert returned.offline_abandon_deadline == deadline

        class TestOfflineAbandonDeadline:
            @pytest.fixture
            def seconds(self, payments_settings):
                return payments_settings.ORDER_OFFLINE_ABANDON_PERIOD_MAX + 1

            @pytest.fixture
            def offline_abandon_deadline(self, seconds):
                return utcnow() + timedelta(seconds=seconds)

            @pytest.mark.asyncio
            async def test_deadline_error(self, returned_func):
                with pytest.raises(OrderAbandonDeadlineError):
                    await returned_func()

    class TestImage:
        @pytest.fixture
        def image_url(self):
            return 'http://image.test/image'

        @pytest.fixture
        def items_data(self, rands, randdecimal, image_url, currency):
            return [
                {
                    'name': 'teh namhe',
                    'price': randdecimal(max=100),
                    'amount': randdecimal(max=10),
                    'currency': currency,
                    'nds': NDS.NDS_20,
                    'image': {
                        'url': image_url,
                    },
                }
            ]

        def test_image__returned_items_has_image(self, returned, image_url):
            assert_that(
                returned.items,
                has_item(
                    has_properties({
                        'image_id': not_none(),
                        'image': has_properties({
                            'url': image_url,
                        })
                    })
                )
            )

        @pytest.fixture
        def check_task_not_created(self, returned, storage):
            async def _check_task_not_created():
                tasks = await alist(storage.task.find(
                    task_type=TaskType.RUN_ACTION,
                ))
                assert_that(
                    tasks,
                    not_(
                        has_item(
                            has_properties({
                                'action_name': DownloadImageAction.action_name,
                                'state': TaskState.PENDING,
                            })
                        )
                    )
                )

            return _check_task_not_created

        @pytest.mark.asyncio
        async def test_image__creates_download_task(self, returned, storage, merchant):
            tasks = await alist(storage.task.find(
                task_type=TaskType.RUN_ACTION,
            ))
            assert_that(
                tasks,
                has_item(
                    has_properties({
                        'action_name': DownloadImageAction.action_name,
                        'params': has_entries({
                            'action_kwargs': has_entries({
                                'image_id': not_none(),
                                'uid': merchant.uid,
                            }),
                        }),
                        'state': TaskState.PENDING,
                    })
                )
            )

        class TestWhenSettingDisabled:
            @pytest.fixture(autouse=True)
            def disable_downloading(self, payments_settings):
                payments_settings.ORDER_DOWNLOAD_IMAGES = False

            @pytest.mark.asyncio
            async def test_image_setting_disabled__not_creates_download_task(self, check_task_not_created):
                await check_task_not_created()

        class TestByObjectWithExistingImage:
            @pytest.fixture
            def items_to_action(self, items):
                return items

            @pytest.fixture
            async def image(self, create_image):
                return await create_image(stored_path=None, md5=None, sha256=None)

            @pytest.fixture
            def product(self, products):
                return products[0]

            @pytest.fixture
            def items(self, merchant, items_data, product, rands, randdecimal, image):
                return [
                    Item(
                        uid=merchant.uid,
                        product_id=product.product_id,
                        amount=randdecimal(max=100),
                        product=product,
                        image_id=image.image_id,
                        image=image,
                    )
                ]

            @pytest.mark.asyncio
            async def test_image_existing__not_creates_download_task(self, check_task_not_created):
                await check_task_not_created()

        class TestByObjectWithDownloadedImage:
            @pytest.fixture
            def items_to_action(self, items):
                return items

            @pytest.fixture
            def product(self, products):
                return products[0]

            @pytest.fixture
            def image(self, rands, merchant):
                return Image(
                    uid=merchant.uid,
                    url=rands(),
                    md5=rands(),
                    sha256=rands(),
                    stored_path=rands(),
                )

            @pytest.fixture
            def items(self, merchant, items_data, product, randdecimal, image):
                return [
                    Item(
                        uid=merchant.uid,
                        product_id=product.product_id,
                        amount=randdecimal(max=100),
                        product=product,
                        image=image,
                    )
                ]

            @pytest.mark.asyncio
            async def test_image_downloaded__image_created(self, returned, storage, image):
                created_image = await storage.image.get_by_digest(uid=image.uid, md5=image.md5, sha256=image.sha256)
                assert_that(
                    created_image,
                    has_properties({
                        'url': image.url,
                        'stored_path': image.stored_path,
                    })
                )

            @pytest.mark.asyncio
            async def test_image_downloaded__not_creates_download_task(self, check_task_not_created):
                await check_task_not_created()


class BaseTestShop:
    """
    Логика присванивания и извлечения shop_id не зависит от иных свойств заказа,
    поэтому нет нужды параметризовать фикстуры как обычно - хватит 1 базового набора
    """

    @pytest.fixture
    def acquirer(self):
        return AcquirerType.TINKOFF

    @pytest.fixture
    def kind(self):
        return OrderKind.PAY

    @pytest.fixture
    def mode(self):
        return 'uid'

    @pytest.fixture
    def with_parent(self):
        return False

    @pytest.fixture
    def items_to_action(self, items_data):
        return items_data


class BaseMerchantOAuthMode:
    """Тестируем проставление merchant_oauth_mode"""

    @pytest.fixture
    def kind(self):
        return OrderKind.PAY

    @pytest.fixture
    def mode(self):
        return 'uid'

    @pytest.fixture
    def items_to_action(self, items_data):
        return items_data


class TestCreateOrderAction(BaseTestCreateOrUpdateOrderAction):
    @pytest.mark.parametrize('param_default_shop_type', [ShopType.PROD, ShopType.TEST])
    @pytest.mark.asyncio
    async def test_still_set_merchant_oauth_mode_for_backward_compatibility(
        self,
        merchant,
        returned_func,
        param_default_shop_type,
        create_merchant_oauth,
        default_merchant_shops,
    ):
        await create_merchant_oauth(
            uid=merchant.uid,
            shop_id=default_merchant_shops[param_default_shop_type].shop_id,
            mode=MerchantOAuthMode.from_shop_type(param_default_shop_type),
        )

        order: Order = await returned_func()

        assert order.shop.shop_type == param_default_shop_type
        assert order.merchant_oauth_mode == MerchantOAuthMode.from_shop_type(order.shop.shop_type)

    class TestOrdinary(BaseTestCreateOrUpdateOrderAction.BasdeTestGeneric):
        def test_create_ordinary__created_items(self, items_data, created_items):
            self.check_created_items(items_data, created_items)

    class TestOldNDSMapping(BaseTestCreateOrUpdateOrderAction.BaseTestOldNDSMapping):
        def test_create_old_nds_mapping__created_items(self, items_data, created_items):
            self.check_created_items(items_data, created_items)

    class TestUpdateCurrency(BaseTestCreateOrUpdateOrderAction.BaseTestUpdateCurrency):
        @parametrize_shop_type
        def test_create_update_currency__created_items(self, items_data, created_items, enabled_currency):
            self.check_created_items(items_data, created_items, enabled_currency)

    class TestCustomerSubscription:
        @pytest.fixture
        def with_customer_subscription(self):
            return True

        @pytest.fixture
        def select_customer_subscription(self):
            return True

        def test_customer_subscription(self, created_order, customer_subscription):
            assert created_order.customer_subscription == customer_subscription

    class TestShop(BaseTestShop):
        @pytest.fixture
        def create_token_for_shop(self):
            return True

        @pytest.fixture(autouse=True)
        async def setup(
            self,
            acquirer,
            create_merchant_oauth,
            merchant,
            shop,
            payments_settings,
            create_token_for_shop,
            merchant_oauth_mode,
        ):
            if acquirer == AcquirerType.KASSA and create_token_for_shop:
                await create_merchant_oauth(uid=merchant.uid, shop_id=shop.shop_id, mode=merchant_oauth_mode)

        class TestCallsGetShopOrEnsureDefaultShopAction:
            @pytest.fixture(autouse=True)
            def action_spy(self, mocker):
                return mocker.spy(GetOrEnsureDefaultShopAction, 'run')

            def test_called(self, returned, action_spy):
                action_spy.assert_called_once()

        class TestExplicitShopArgument:
            @pytest.fixture
            def param_shop_id(self, shop):
                return shop.shop_id

            def test_explicit_shop_id_is_set(self, returned, param_shop_id):
                assert returned.shop_id == param_shop_id

        class TestRaisesShopNotFoundForExplicitArgument:
            @pytest.fixture
            def param_shop_id(self, shop):
                return shop.shop_id + 111

            @pytest.mark.asyncio
            async def test_raises_shop_not_found_error(self, returned_func):
                with pytest.raises(ShopNotFoundError):
                    await returned_func()

        @pytest.mark.parametrize('param_shop_id', [None])
        @pytest.mark.parametrize(
            ('acquirer', 'param_default_shop_type', 'merchant_oauth_mode'),
            [
                (AcquirerType.KASSA, ShopType.TEST, MerchantOAuthMode.TEST),
                (AcquirerType.KASSA, ShopType.PROD, MerchantOAuthMode.PROD),
                (AcquirerType.TINKOFF, ShopType.PROD, MerchantOAuthMode.PROD),
            ]
        )
        @pytest.mark.asyncio
        async def test_default_shop_type_parameter_matters(self, returned, order, storage, param_default_shop_type):
            """Если не указан shop_id, то тип дефолтного магазина должен определяться по default_shop_type"""
            shop = await storage.shop.get(uid=returned.uid, shop_id=returned.shop_id)
            assert shop.shop_type == param_default_shop_type

        class TestShopTypeAndMerchantOAuthModeValidation(BaseMerchantOAuthMode):
            """
            Для эквайера Касса для типа магазина должен быть подходящий OAuth токен,
            для эквайера Тинькофф наличие токена не нужно.
            """

            @pytest.fixture
            async def mock_get_trust_credentials_action(self, mock_action):
                return mock_action(GetTrustCredentialParamsAction)

            @pytest.mark.parametrize('params_extra', ({'acquirer': AcquirerType.KASSA},))
            @pytest.mark.asyncio
            async def test_calls_get_trust_credentials_action_with_acquirer(
                self,
                returned_func,
                merchant,
                mock_get_trust_credentials_action,
                params,
            ):
                """
                Вызов делается для валидации параметров
                """
                order = await returned_func()
                mock_get_trust_credentials_action.assert_called_once_with(
                    acquirer=params.get('acquirer'),
                    order=match_equality(
                        has_properties({
                            'uid': order.uid,
                            'acquirer': order.acquirer,
                            'shop_id': order.shop_id,
                            'shop': has_properties({
                                'shop_id': order.shop.shop_id,
                            })  # вызывался с загруженным шопом
                        })
                    ),
                    merchant=match_equality(
                        has_properties({
                            'uid': merchant.uid,
                        })
                    ),
                )

            @pytest.mark.asyncio
            async def test_calls_get_trust_credentials_action_without_acquirer(
                self,
                returned,
                mock_get_trust_credentials_action
            ):
                mock_get_trust_credentials_action.assert_not_called()


class TestUpdateOrderAction(BaseTestCreateOrUpdateOrderAction):
    @pytest.fixture
    def kind(self):
        return OrderKind.PAY

    @pytest.fixture
    async def another_products(self, randitem, rands, randdecimal, storage, merchant):
        currency = rands()

        return [
            await storage.product.create(
                Product(
                    uid=merchant.uid,
                    name=rands(),
                    price=randdecimal(max=100),
                    currency=currency,
                    nds=randitem(NDS),
                )
            ) for _ in range(5)
        ]

    @pytest.fixture(autouse=True)
    async def another_items(self, randdecimal, storage, order, items_amounts, another_products):
        return [
            await storage.item.create(
                Item(
                    uid=order.uid,
                    order_id=order.order_id,
                    product_id=product.product_id,
                    amount=randdecimal(max=100),
                    product=product
                )
            )
            for product in another_products
        ]

    @pytest.fixture
    async def params_extra(self, mode, order, storage, service_client, service_merchant):
        if mode == 'service_merchant':
            order.service_client_id = service_client.service_client_id
            order.service_merchant_id = service_merchant.service_merchant_id
            order = await storage.order.save(order)
        return {'order_id': order.order_id}

    @pytest.mark.asyncio
    async def test_service_data_not_updated_when_none(self, rands, storage, order, returned_func):
        service_data = order.data.service_data = {rands(): rands()}
        await storage.order.save(order)
        order = await returned_func(service_data=None)
        updated_order = await storage.order.get(order.uid, order.order_id)
        assert service_data == order.data.service_data and service_data == updated_order.data.service_data

    class TestOrdinary(BaseTestCreateOrUpdateOrderAction.BasdeTestGeneric):
        def test_update_ordinary__created_items(self, items_data, created_items):
            self.check_created_items(items_data, created_items)

    class TestOldNDSMapping(BaseTestCreateOrUpdateOrderAction.BaseTestOldNDSMapping):
        def test_update_old_nds_mapping__created_items(self, items_data, created_items):
            self.check_created_items(items_data, created_items)

    class TestUpdateCurrency(BaseTestCreateOrUpdateOrderAction.BaseTestUpdateCurrency):
        @parametrize_shop_type
        def test_update_update_currency__created_items(self, items_data, created_items, enabled_currency):
            self.check_created_items(items_data, created_items, enabled_currency)

    class TestNotFoundServiceMerchant:
        @pytest.fixture
        def mode(self):
            return 'uid'

        @pytest.fixture
        async def params_extra(self, order_with_service, storage, service_client, service_merchant):
            return {'order_id': order_with_service.order_id}

        @pytest.mark.asyncio
        async def test_not_found_service_merchant(self, returned_func):
            with pytest.raises(OrderNotFoundError):
                await returned_func()

    class TestOrderInvalidKindMulti:
        @pytest.fixture
        async def params_extra(self, storage, service_client, service_merchant, mode, multi_order):
            if mode == 'service_merchant':
                multi_order.service_client_id = service_client.service_client_id
                multi_order.service_merchant_id = service_merchant.service_merchant_id
                multi_order = await storage.order.save(multi_order)
            return {'order_id': multi_order.order_id}

        @pytest.mark.asyncio
        async def test_invalid_kind_multi(self, returned_func):
            with pytest.raises(OrderInvalidKind):
                await returned_func()

    class TestAlreadyHaveTransactions:
        @pytest.mark.asyncio
        async def test_already_have_transactions(self, transaction, returned_func):
            with pytest.raises(OrderAlreadyHaveTransactions):
                await returned_func()

    class TestIdempotentOffline:
        @pytest.fixture
        async def paymethod_id(self, storage, order, rands):
            order.paymethod_id = PAYMETHOD_ID_OFFLINE
            await storage.order.save(order)
            return rands()

        def test_idempotent(self, returned):
            assert returned.paymethod_id == PAYMETHOD_ID_OFFLINE

    class TestAbandonDeadlineCountUpdate:
        @pytest.fixture
        async def paymethod_id(self, storage, order, rands):
            return PAYMETHOD_ID_OFFLINE

        @pytest.fixture
        def offline_abandon_deadline(self):
            return utcnow()

        @pytest.mark.asyncio
        async def test_order_abandon_prolongation_amount_error(self, payments_settings, storage, order, returned_func):
            order.data.offline_prolongation_amount = payments_settings.ORDER_OFFLINE_PROLONGATION_AMOUNT_MAX
            await storage.order.save(order)
            with pytest.raises(OrderAbandonProlongationAmountError):
                await returned_func()

        @pytest.mark.asyncio
        async def test_order_abandon_prolongation_amount_inc(self, payments_settings, storage, order, returned_func):
            order.data.offline_prolongation_amount = 1
            await storage.order.save(order)
            returned = await returned_func()
            assert returned.data.offline_prolongation_amount == order.data.offline_prolongation_amount + 1

    class TestShopIdArgumentIgnoredOnOrderUpdate(BaseTestShop):
        """shop_id ставится при создании заказа и обновление его не изменяет"""

        @pytest.fixture
        async def another_shop(self, storage, shop_entity):
            shop_entity.is_default = False
            return await storage.shop.create(shop_entity)

        @pytest.fixture(params=('no_shop_id', 'shop_id'))
        def shop_id(self, shop, another_shop, request):
            assert shop.shop_id != another_shop.shop_id
            return {
                'no_shop_id': None,
                'shop_id': shop.shop_id,
            }[request.param]

        @pytest.mark.asyncio
        async def test_shop_id_is_not_changed(self, returned_func, order, storage, another_shop):
            order.shop_id = another_shop.shop_id
            await storage.order.save(order)

            returned = await returned_func()
            assert returned.shop_id == another_shop.shop_id
