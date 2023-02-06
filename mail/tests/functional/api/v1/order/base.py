import pytest

from mail.payments.payments.core.entities.enums import OrderKind


class BaseTestOrder:
    @pytest.fixture(params=('service_merchant', 'uid'))
    def id_type(self, request):
        return request.param

    @pytest.fixture
    def check_hashes(self, id_type, payments_settings):
        def _inner(crypto, data):
            if id_type == 'service_merchant':
                assert all((
                    'order_hash' not in data,
                    'order_url' not in data,
                    'payment_hash' not in data,
                    'payment_url' not in data,
                    'payments_url' in data,
                ))
                hash_ = data['payments_url'].replace(payments_settings.FRONT_PAYMENT_URL, '')
                with crypto.decrypt_order(hash_) as order:
                    assert order == {
                        'uid': data['uid'],
                        'order_id': data['order_id'],
                        'url_kind': 'order',
                        'version': 1,
                    }
            else:
                for hash_ in [
                    data['order_url'].split('/')[-1],
                    data['order_hash'],
                ]:
                    with crypto.decrypt_order(hash_) as order:
                        assert order == {
                            'uid': data['uid'],
                            'order_id': data['order_id'],
                            'url_kind': 'order',
                            'version': 1,
                        }
                for hash_ in [
                    data['payment_url'].split('/')[-1],
                    data['payment_hash'],
                ]:
                    with crypto.decrypt_payment(hash_) as payment:
                        assert payment == {
                            'uid': data['uid'],
                            'order_id': data['order_id'],
                            'url_kind': 'payment',
                            'version': 1,
                        }

        return _inner

    @pytest.fixture
    async def order(self, no_merchant_user_check, client, storage, merchant, order_data, order_headers):
        with no_merchant_user_check():
            r = await client.post(
                f'/v1/order/{merchant.uid}/',
                json=order_data,
                headers=order_headers,
            )
        assert r.status == 200
        order_id = (await r.json())['data']['order_id']
        order = await storage.order.get(uid=merchant.uid, order_id=order_id)
        return order

    @pytest.fixture
    async def multi_order(self, no_merchant_user_check, client, storage, merchant, order_data, order_headers):
        with no_merchant_user_check():
            r = await client.post(
                f'/v1/order/{merchant.uid}/',
                json={**order_data, 'kind': OrderKind.MULTI.value},
                headers=order_headers,
            )
        assert r.status == 200
        order_id = (await r.json())['data']['order_id']
        order = await storage.order.get(uid=merchant.uid, order_id=order_id)
        return order

    @pytest.fixture
    async def service_merchant_order(self, client, storage, service_merchant, order_data, tvm, order_headers):
        r = await client.post(
            f'/v1/internal/order/{service_merchant.service_merchant_id}/',
            json=order_data,
            headers=order_headers,
        )
        assert r.status == 200
        order_id = (await r.json())['data']['order_id']
        order = await storage.order.get(
            uid=service_merchant.uid,
            order_id=order_id,
            service_merchant_id=service_merchant.service_merchant_id,
        )
        return order

    @pytest.fixture
    def bad_merchant_uid(self, merchant_uid):
        return merchant_uid * 100

    @pytest.fixture
    def bad_service_merchant_id(self, service_merchant):
        return service_merchant.service_merchant_id * 100

    @pytest.fixture
    def bad_order_id(self, order):
        return order.order_id * 100

    @pytest.fixture
    def service_merchant_order_test_data(self, service_merchant, service_merchant_order):
        return {
            'path': f'/v1/internal/order/{service_merchant.service_merchant_id}/{service_merchant_order.order_id}',
            'order': service_merchant_order,
        }

    @pytest.fixture
    def uid_order_test_data(self, order):
        return {
            'path': f'/v1/order/{order.uid}/{order.order_id}',
            'order': order,
        }

    @pytest.fixture
    def test_data(self, id_type, service_merchant_order_test_data, uid_order_test_data):
        data = {
            'service_merchant': service_merchant_order_test_data,
            'uid': uid_order_test_data,
        }
        return data[id_type]

    @pytest.fixture
    def subscription_uid(self, id_type, merchant, service_merchant):
        return merchant.uid if id_type == 'uid' else service_merchant.uid
