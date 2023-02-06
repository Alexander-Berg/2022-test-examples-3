import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.payments.payments.core.actions.category import GetCategoryListAction
from mail.payments.payments.core.entities.category import Category
from mail.payments.payments.core.entities.enums import AcquirerType


@pytest.fixture
def categories():
    return [
        Category(category_id=1, title='123', required_acquirer=AcquirerType.KASSA),
        Category(category_id=2, title='456', required_acquirer=None),
    ]


@pytest.fixture(autouse=True)
def action(mock_action, categories):
    return mock_action(GetCategoryListAction, categories)


@pytest.fixture
async def response(service, payments_client):
    return await payments_client.get('/v1/category')


def test_called(response, action):
    action.assert_called_once_with()


@pytest.mark.asyncio
async def test_returned(response, categories):
    assert_that(
        await response.json(),
        has_entries({
            'data': has_entries({
                'categories': contains_inanyorder(*[
                    {
                        'category_id': category.category_id,
                        'required_acquirer': category.required_acquirer.value if category.required_acquirer else None,
                        'title': category.title,
                    }
                    for category in categories
                ])
            }),
            'status': 'success',
            'code': 200,
        })
    )
