import pytest

from hamcrest import assert_that, contains, equal_to


@pytest.fixture
def url(rands):
    return rands()


@pytest.fixture
def interaction_method(rands):
    return rands()


@pytest.fixture
def kwargs(rands):
    return {rands(): rands() for _ in range(10)}


def test_endpoint_url(abstract_client, url):
    assert abstract_client.endpoint_url(url) == f'{abstract_client.BASE_URL}/{url}'


@pytest.mark.parametrize('method', ('get', 'post', 'put', 'patch', 'delete'))
@pytest.mark.asyncio
async def test_methods(abstract_client, interaction_method, url, method, kwargs):
    await getattr(abstract_client, method)(interaction_method, url, **kwargs)
    assert_that(
        (abstract_client.call_args, abstract_client.call_kwargs),
        contains(equal_to((interaction_method, method.upper(), url)), equal_to(kwargs))
    )


def test_get_merchant_setting(abstract_client, payments_settings, randn, rands):
    uid = randn()
    key = rands()
    value = rands()
    payments_settings.INTERACTION_MERCHANT_SETTINGS[uid] = {key: value}

    assert all((
        abstract_client.get_merchant_setting(uid, key) == value,
        abstract_client.get_merchant_setting(uid, rands()) is None,
        abstract_client.get_merchant_setting(uid, rands(), value) == value,
    ))
