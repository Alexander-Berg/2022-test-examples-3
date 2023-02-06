import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.payments.payments.core.entities.enums import AcquirerType


@pytest.fixture(autouse=True)
async def categories(storage, create_category):
    async for category in storage.category.find():
        await storage.category.delete(category)
    return (
        await create_category(required_acquirer=AcquirerType.KASSA),
        await create_category(required_acquirer=None),
    )


@pytest.fixture
async def response_json(client):
    r = await client.get('/v1/category')
    assert r.status == 200
    return await r.json()


def test_response(response_json, categories):
    assert_that(
        response_json,
        has_entries({
            'status': 'success',
            'code': 200,
            'data': has_entries({
                'categories': contains_inanyorder(*[
                    {
                        'category_id': category.category_id,
                        'required_acquirer': category.required_acquirer.value if category.required_acquirer else None,
                        'title': category.title,
                    }
                    for category in categories
                ])
            })
        })
    )
