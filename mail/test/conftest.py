import pytest

from mail.payments.payments.core.entities.service import ServiceMerchant


@pytest.fixture(params=['True', 'False'])
async def autoclear(request):
    return request.param


@pytest.fixture
async def service_merchant(storage, merchant, service):
    return await storage.service_merchant.create(ServiceMerchant(
        uid=merchant.uid,
        service_id=service.service_id,
        entity_id='some_entity',
        description='some description',
        enabled=True,
    ))


@pytest.fixture
async def test_param():
    return None


@pytest.fixture
async def test_order(
    client,
    storage,
    autoclear,
    service_merchant,
    order_data,
    test_param,
    tvm
):
    order_data['test'] = test_param.value
    order_data['autoclear'] = autoclear

    r = await client.post(
        f'/v1/internal/order/{service_merchant.service_merchant_id}/',
        json=order_data,
    )

    assert r.status == 200
    order_id = (await r.json())['data']['order_id']
    return await storage.order.get(uid=service_merchant.uid,
                                   order_id=order_id,
                                   service_merchant_id=service_merchant.service_merchant_id)


@pytest.fixture
def pay_req_json(user_email, user_description):
    return {
        'yandexuid': 'xx_yandexuid',
        'email': user_email,
        'return_url': 'ya.ru',
        'description': user_description,
    }


@pytest.fixture
def pay_func(client, service_merchant, test_order, pay_req_json, tvm):
    async def pay():
        return await client.post(
            f'/v1/internal/order/{service_merchant.service_merchant_id}/{test_order.order_id}/start',
            json=pay_req_json,
        )

    return pay


@pytest.fixture
async def pay_response(pay_func):
    r = await pay_func()
    assert r.status == 200
    return (await r.json())['data']
