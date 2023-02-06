import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, equal_to, has_properties, not_none

from mail.payments.payments.core.entities.category import Category
from mail.payments.payments.core.entities.enums import AcquirerType


@pytest.fixture
def category_entity():
    return Category(
        title='the title',
        required_acquirer=AcquirerType.TINKOFF,
    )


@pytest.fixture
async def created_category(category_entity, storage):
    return await storage.category.create(category_entity)


@pytest.mark.asyncio
async def test_create(storage, category_entity):
    assert_that(
        await storage.category.create(category_entity),
        has_properties({
            'title': 'the title',
            'required_acquirer': AcquirerType.TINKOFF,
            'category_id': not_none(),
            'created': not_none(),
            'updated': not_none(),
        })
    )


@pytest.mark.asyncio
async def test_get(storage, created_category):
    assert_that(
        await storage.category.get(created_category.category_id),
        equal_to(created_category),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage, created_category):
    with pytest.raises(Category.DoesNotExist):
        await storage.category.get(created_category.category_id + 1)


@pytest.mark.asyncio
async def test_save(storage, created_category):
    created_category.required_acquirer = AcquirerType.KASSA
    assert_that(
        await storage.category.save(created_category),
        has_properties({
            'category_id': created_category.category_id,
            'required_acquirer': created_category.required_acquirer,
            'created': created_category.created,
            'title': created_category.title,
        }),
    )


@pytest.mark.asyncio
async def test_delete(storage, created_category):
    await storage.category.delete(created_category),
    with pytest.raises(Category.DoesNotExist):
        await storage.category.get(created_category.category_id)


@pytest.mark.asyncio
async def test_find(storage, create_category):
    categories = (
        await create_category(),
        await create_category(),
    )
    assert_that(
        await alist(storage.category.find()),
        contains_inanyorder(*categories),
    )


@pytest.mark.asyncio
async def test_find_category_id_list(storage, create_category):
    categories = sorted(
        [await create_category(), await create_category()],
        key=lambda c: c.category_id,
    )
    category_ids = [c.category_id for c in categories]

    categories_fetched = sorted(
        await alist(storage.category.find(category_ids=category_ids)),
        key=lambda c: c.category_id,
    )

    assert categories == categories_fetched
